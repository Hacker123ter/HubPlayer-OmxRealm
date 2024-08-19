package org.dw363;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HubPlayer extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private FileConfiguration config;
    private HashMap<UUID, Long> cooldowns = new HashMap<>();
    private HashMap<UUID, Boolean> isRightClicking = new HashMap<>();
    private HashMap<UUID, Boolean> hiddenPlayersStatus = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("hubplayer").setExecutor(this);
        getCommand("hubplayer").setTabCompleter(this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        boolean playersHidden = hiddenPlayersStatus.getOrDefault(player.getUniqueId(), false);
        if (playersHidden) {
            giveHubItem(player, true);
            hidePlayers(player, false);
        } else {
            giveHubItem(player, false);
        }

        ensureItemInSlot(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            int configuredSlot = config.getInt("slot");

            if (event.getSlot() == configuredSlot || event.getRawSlot() == configuredSlot) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(this, () -> ensureItemInSlot(player), 1L);
                return;
            }

            ItemStack currentItem = event.getCurrentItem();
            if (currentItem != null && isConfiguredItem(currentItem)) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(this, () -> ensureItemInSlot(player), 1L);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        if (isConfiguredItem(droppedItem)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (isConfiguredItem(itemInHand)) {
            event.setCancelled(true);

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

                if (isRightClicking.getOrDefault(player.getUniqueId(), false)) {
                    return;
                }

                isRightClicking.put(player.getUniqueId(), true);

                if (cooldowns.containsKey(player.getUniqueId()) && System.currentTimeMillis() < cooldowns.get(player.getUniqueId())) {
                    double timeLeft = (cooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000.0;
                    sendActionBar(player, ChatColor.RED + "Подождите " + formatTime(timeLeft) + " " + getSecondsString(timeLeft) + " перед использованием!");

                    isRightClicking.put(player.getUniqueId(), false);
                    return;
                }

                if (player.hasMetadata("players_hidden")) {
                    showPlayers(player);
                    giveHubItem(player, false);
                    hiddenPlayersStatus.put(player.getUniqueId(), false);
                } else {
                    hidePlayers(player, true);
                    giveHubItem(player, true);
                    hiddenPlayersStatus.put(player.getUniqueId(), true);
                }

                cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + config.getInt("cooldown") * 1000L);

                Bukkit.getScheduler().runTaskLater(this, () -> isRightClicking.put(player.getUniqueId(), false), 1L);
            }
        }
        ensureItemInSlot(player);
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack consumedItem = event.getItem();

        if (isConfiguredItem(consumedItem)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        ItemStack damagedItem = event.getItem();

        if (isConfiguredItem(damagedItem)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        cooldowns.remove(playerUUID);
        isRightClicking.remove(playerUUID);

        hiddenPlayersStatus.put(playerUUID, player.hasMetadata("players_hidden"));
    }

    private void giveHubItem(Player player, boolean playersHidden) {
        Material material;
        String name;
        List<String> description;

        try {
            if (playersHidden) {
                material = Material.valueOf(config.getString("item2").toUpperCase());
                name = config.getString("name2");
                description = config.getStringList("description2");
            } else {
                material = Material.valueOf(config.getString("item").toUpperCase());
                name = config.getString("name");
                description = config.getStringList("description");
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Ошибка: Неверный материал в конфигурации.");
            return;
        }

        ItemStack hubItem = new ItemStack(material);
        ItemMeta meta = hubItem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', parseHex(name)));
            List<String> formattedDescription = new ArrayList<>();
            for (String line : description) {
                formattedDescription.add(ChatColor.translateAlternateColorCodes('&', parseHex(line)));
            }
            meta.setLore(formattedDescription);
            hubItem.setItemMeta(meta);
        }

        player.getInventory().setItem(config.getInt("slot"), hubItem);
    }


    private void hidePlayers(Player player, boolean sendMessage) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) {
                player.hidePlayer(this, p);
            }
        }
        player.setMetadata("players_hidden", new FixedMetadataValue(this, true));
        if (sendMessage) {
            sendActionBar(player, ChatColor.GREEN + "Игроки скрыты");
        }
    }

    private void showPlayers(Player player) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            player.showPlayer(this, p);
        }
        player.removeMetadata("players_hidden", this);
        sendActionBar(player, ChatColor.GREEN + "Игроки показаны");
    }

    private String parseHex(String message) {
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(message);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = "§x§" + hex.charAt(0) + "§" + hex.charAt(1) + "§" + hex.charAt(2)
                    + "§" + hex.charAt(3) + "§" + hex.charAt(4) + "§" + hex.charAt(5);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }


    private boolean isConfiguredItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String displayName = item.getItemMeta().getDisplayName();
        return displayName.equals(ChatColor.translateAlternateColorCodes('&', parseHex(config.getString("name"))))
                || displayName.equals(ChatColor.translateAlternateColorCodes('&', parseHex(config.getString("name2"))));
    }

    private String formatTime(double seconds) {
        if (seconds < 1) {
            return String.format("%.1f", seconds);
        } else {
            return String.format("%.0f", Math.floor(seconds));
        }
    }

    private String getSecondsString(double seconds) {
        long roundedSeconds = Math.round(seconds * 10) / 10;
        if (seconds < 1.0) {
            if (roundedSeconds == 1) return "секунду";
            if (roundedSeconds >= 2 && roundedSeconds <= 4) return "секунды";
            return "секунд";
        }

        long fullSeconds = (long) seconds;
        if (fullSeconds == 1) return "секунду";
        if (fullSeconds >= 2 && fullSeconds <= 4) return "секунды";
        return "секунд";
    }

    private void sendActionBar(Player player, String message) {
        new BukkitRunnable() {
            @Override
            public void run() {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
            }
        }.runTask(this);
    }

    private void ensureItemInSlot(Player player) {
        int configuredSlot = config.getInt("slot");
        ItemStack itemInSlot = player.getInventory().getItem(configuredSlot);

        if (itemInSlot == null || !isConfiguredItem(itemInSlot)) {
            boolean playersHidden = hiddenPlayersStatus.getOrDefault(player.getUniqueId(), false);
            giveHubItem(player, playersHidden);
        }

        ItemStack configuredItem = player.getInventory().getItem(configuredSlot);
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (i != configuredSlot && isConfiguredItem(player.getInventory().getItem(i))) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только игроки могут использовать эту команду.");
            return true;
        }

        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("hubplayer")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "Используйте /hubplayer <hide|show|reload>");
                return true;
            }

            if (args[0].equalsIgnoreCase("hide")) {
                hidePlayers(player, true);
                hiddenPlayersStatus.put(player.getUniqueId(), true);
                giveHubItem(player, true);
            } else if (args[0].equalsIgnoreCase("show")) {
                showPlayers(player);
                hiddenPlayersStatus.put(player.getUniqueId(), false);
                giveHubItem(player, false);
            } else if (args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                config = getConfig();
                player.sendMessage(ChatColor.GREEN + "Конфигурация перезагружена.");
            } else {
                player.sendMessage(ChatColor.RED + "Неизвестная команда. Используйте /hubplayer <hide|show|reload>");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("hubplayer")) {
            if (args.length == 1) {
                if ("hide".startsWith(args[0].toLowerCase())) {
                    completions.add("hide");
                }
                if ("show".startsWith(args[0].toLowerCase())) {
                    completions.add("show");
                }
                if ("reload".startsWith(args[0].toLowerCase())) {
                    completions.add("reload");
                }
            }
        }
        return completions;
    }
}