## HubPlayer(OmxRealm)

![Version](https://img.shields.io/badge/Версия-1.0.13-blue.svg)
![API](https://img.shields.io/badge/Spigot%201.21%2B-blue.svg)

<h3 align="center">Discord: luckytsb</h3>

## ✨ Функции:

-️ :accessibility: Плагин добавляющий предмет для скрытия игроков в хабе. Сделано специально для сервера OmxRealm.

## 🚀 Установка:

- 😧 Скачайте <a href="https://github.com/Hacker123ter/HubPlayer-OmxRealm/raw/HubPlayer/target/HubPlayer-1.0.13-OmxRealm.jar" target="_blank">HubPlayer-1.0.13-OmxRealm.jar</a>.
- 🐈 Переместите его в папку "plugins" вашего сервера. (Убедитесь что Ядро и версия совместимы с плагином)
- 🪄 Перезапустите сервер.
- 😸 Радуйтесь жизни!

## 🎮 Использование:

0. После установки плагина появится папка HubItem (plugins/HubItem), в ней будет находится конфиг плагина (plugins/HubItem/config.yml)
1. Зайдите в конфиг с плагина и настройте значения:
```
item: 
item2: 
name: ""
name2: ""
description: ""
description2: ""
slot:
cooldown:
```
I. Что значит, каждая строчка и как настроить:
```
item: PUFFERFISH_BUCKET (Предмет который будет для скрытия игроков (если игроки показаны)).
item2: PUFFERFISH (Предмет который будет для показа игроков (если игроки скрыты)).
name: "&6Магическое &1Ведро" (Название предмета который будет для скрытия игроков (если игроки показаны)).
name2: "&6Зловещая &1Рыбка" (Название предмета который будет для показа игроков (если игроки скрыты)).
description: "&7Используйте этот предмет для &aскрытия игроков." (Описание предмета который будет для скрытия игроков (если игроки показаны)).
description2: "&7Используйте этот предмет для &aпоказа игроков." (Описание предмета который будет для показа игроков (если игроки скрыты)).
slot: 0 (Слот в хотбаре, в котором будет размещён предмет).
cooldown: 3 # В секундах (Кд между использованием предмета).
```
II. Так же, стоит задержка перед частым использование предмета (Настройка в конфиге), адаптирована надпись в actionbar.
2. После настройки config.yml перезагрузите сервер или пропишите "/hubplayer reload" - Нужны права Op или "hubplayer.reload".
3. Обновите предмет в руке (через инвентарь попробуйте его взять).
4. Радуйтесь плагину и жизни!

## 🏗️ Команды:
```
/hubplayer reload
```

## 🔒 Права:
```
hubplayer.reload
default: op
```
