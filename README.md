<div align="center">

![Aporia](src/main/resources/assets/Aporia.png)

# ✨ Aporia Client

**Продвинутый Minecraft клиент для версии 1.21.11**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-red?style=for-the-badge&logo=minecraft)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.18.4-yellow?style=for-the-badge&logo=fabric)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-26-green?style=for-the-badge&logo=openjdk)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/Version-0.4.1-blue?style=for-the-badge)](https://github.com/aporia/client/releases)

</div>

> 🕐 Last build: **15.03.2026 16:37:41 UTC** — v0.4.1


> 🕐 Last build: **15.03.2026 14:45:43 UTC** — v0.4.1


> 🕐 Last build: **15.03.2026 12:19:11 UTC** — v0.4.1


---

## 🚀 Быстрый старт

### Установка

1. Установите [Fabric Loader 0.18.4+](https://fabricmc.net/use/installer/)
2. Скачайте последнюю версию из [Releases](https://github.com/aporia/client/releases) или [beta/latest/](beta/latest/)
3. Поместите `.jar` файл в папку `mods`
4. Запускайте Minecraft с Fabric профилем

### Сборка из исходников

```bash
git clone https://github.com/aporia/client.git
cd client

# Сборка
./gradlew build

# Запуск клиента
./gradlew runClient

# Обфусцированный билд
./gradlew obfuscateRemappedJar
```

**Требования:**
- JDK 26
- Gradle 8.x
- Git

---

## 📦 Возможности

### 🔥 95+ Модулей

| Категория | Количество | Примеры |
|-----------|------------|---------|
| ⚔️ **Combat** | 19 | Aura, TpAura, AutoCrystal, Criticals, Velocity |
| 🏃 **Movement** | 15 | Fly, Speed, Jesus, Strafe, ElytraMotion |
| 👤 **Player** | 12 | AutoTool, ChestStealer, FreeCam, NoFall |
| 🎨 **Render** | 21 | ESP, BlockESP, FullBright, ChinaHat, Particles |
| 🔧 **Misc** | 13 | AutoBuy, DiscordRPC, ServerHelper, WindJump |

### 🎯 Ключевые особенности

<details>
<summary><b>⚔️ Боевая система</b></summary>

- **Aura** — Продвинутая система наведения с поддержкой нескольких режимов ротации
- **TpAura** — Телепортационная аура с обходом античитов
- **AutoCrystal** — Автоматическая установка и подрыв кристаллов
- **MaceTarget** — Авто-атака булавой с предсказанием движения
- **Velocity** — Полная защита от отбрасывания

</details>

<details>
<summary><b>🏃 Движение</b></summary>

- **Fly** — Несколько режимов полёта
- **Speed** — Увеличение скорости передвижения
- **Jesus** — Хождение по воде и лавам
- **ElytraMotion** — Улучшенное управление элитрами
- **Spider** — Лазание по стенам

</details>

<details>
<summary><b>🎨 Визуал</b></summary>

- **ESP** — Подсветка игроков, сундуков, ресурсов
- **BlockESP** — Подсветка конкретных блоков
- **ChinaHat** — Косметическая китайская шляпа
- **Particles** — Красивые частицы вокруг игрока
- **FullBright** — Полная яркость без_gamma

</details>

<details>
<summary><b>🔧 Утилиты</b></summary>

- **AutoBuy** — Автоматическая покупка предметов на аукционе
- **DiscordRPC** — Статус в Discord
- **Macro System** — Система макросов
- **Waypoints** — Путевые точки
- **Proxy Support** — Поддержка SOCKS4/5 и HTTP прокси

</details>

---

## 🎨 ClickGUI

Интуитивный интерфейс для управления модулями:

- 📁 **Категории** — Удобная группировка по разделам
- ⚙️ **Настройки** — Слайдеры, цвета, привязки клавиш
- 🎯 **Drag & Drop** — Перетаскивание окон
- 💾 **Профили** — Сохранение и загрузка конфигураций
- 🔍 **Поиск** — Быстрый поиск модулей

**Открытие:** `Right Shift` (настраивается)

---

## 💬 Команды

| Команда | Описание |
|---------|----------|
| `.help` | Показать все команды |
| `.toggle <модуль>` | Включить/выключить модуль |
| `.bind <модуль> <клавиша>` | Привязать модуль к клавише |
| `.friend add/remove <ник>` | Управление списком друзей |
| `.macro create/delete <имя>` | Управление макросами |
| `.way add/remove <название>` | Управление путевыми точками |
| `.prefix <символ>` | Изменить префикс команд |
| `.saveconfig` | Сохранить текущую конфигурацию |
| `.reload` | Перезагрузить конфигурацию |
| `.staff` | Показать стафф сервера |

---

## 🛡️ Unicode Chaos Obfuscation

Aporia использует передовую систему обфускации кода:

### Уровни защиты

| Уровень | Символы | Пример |
|---------|---------|--------|
| 🟢 LIGHT | ASCII | `C/x/eU` |
| 🟡 MEDIUM | Греческий + Кириллица | `Т/θ/а_У` |
| 🟠 HEAVY | Азиатские языки | `ड/し/ぬ大 f` |
| 🔴 EXTREME | Все языки + эмодзи | `›/ჩ/ཽ/Л可ძĐ"🔥` |

### Особенности

- 🔄 **Ежедневная ротация** маппингов в 00:00 UTC
- 🌍 **30+ языков** поддерживается (китайский, японский, руны, эмодзи...)
- 🔐 **RuntimeMapper** для деобфускации логов
- 🚫 **Миксины не обфусцируются** для стабильности

Подробнее: [UNICODE_CHAOS_GUIDE.md](src/main/java/anidumpproject/api/README.md)

---

## 📊 HUD Элементы

Настраиваемые элементы интерфейса:

- 🛡️ **ArmorHud** — Отображение брони
- 📍 **Coordinates** — Координаты XYZ
- 📊 **FPS Counter** — Счётчик кадров
- 📋 **ModuleList** — Список активных модулей
- 📶 **Ping** — Задержка соединения
- ⚗️ **Potions** — Активные эффекты
- 🎯 **TargetHud** — Информация о цели
- ©️ **Watermark** — Логотип клиента

Все элементы можно перетаскивать и настраивать!

---

## 🔧 Технологии

- **Fabric API** — Модлоадер нового поколения
- **Mixin** — Система инъекций в код Minecraft
- **LWJGL3** — Низкоуровневая графика
- **JOML** — Математическая библиотека
- **Netty** — Сетевая библиотека
- **Gson** — JSON парсинг
- **Lombok** — Упрощение кода

---

## 📁 Структура проекта

```
Aporia/
├── src/main/
│   ├── java/aporia/su/
│   │   ├── Initialization.java      # Точка входа
│   │   ├── mixin/                   # Mixin инъекции (50+)
│   │   ├── modules/                 # Модули (95+)
│   │   │   ├── combat/              # Боевые
│   │   │   ├── movement/            # Движение
│   │   │   ├── player/              # Игрок
│   │   │   ├── render/              # Рендер
│   │   │   └── misc/                # Разное
│   │   └── util/                    # Утилиты
│   │       ├── events/              # Система событий
│   │       ├── files/               # Конфиги
│   │       ├── render/              # Рендеринг
│   │       ├── chat/                # Чат и команды
│   │       └── network/             # Сеть
│   │
│   └── resources/
│       ├── assets/Aporia/           # Текстуры, шрифты
│       ├── fabric.mod.json          # Метаданные
│       ├── mixins.json              # Конфиг миксинов
│       └── accesswidener            # Access Widener
│
├── buildSrc/src/main/kotlin/        # Build-скрипты
│   ├── ChaosObfuscator.kt           # Unicode обфускация
│   ├── StringEncryptor.kt           # Шифрование строк
│   └── ...
│
└── beta/                            # Бета-билды
    ├── 0.4.1/
    └── latest/
```

Полная документация: [`context.md`](context.md)

---

## 🤝 Вклад в проект

Мы приветствуем вклад в развитие Aporia!

### Как помочь

1. **Fork** проект
2. Создайте **feature branch** (`git checkout -b feature/AmazingFeature`)
3. Внесите **изменения**
4. Сделайте **commit** (`git commit -m 'Add AmazingFeature'`)
5. Отправьте в **remote** (`git push origin feature/AmazingFeature`)
6. Откройте **Pull Request**

### Требования к коду

- ✅ Следуйте стилю кода проекта
- ✅ Добавляйте тесты для новых функций
- ✅ Документируйте сложные участки
- ✅ Проверяйте сборку перед отправкой

---

## 📝 Лицензия

Этот проект распространяется под лицензией **MIT**.  
Подробности в файле [`LICENSE`](LICENSE).

---

## ⚠️ Дисклеймер

> Этот проект создан в образовательных целях.  
> Использование на публичных серверах может нарушать их правила.  
> Авторы не несут ответственности за возможные последствия.

---

## 💎 Авторы

Создано с ❤️ командой **Aporia**

- **protect3ed** — Lead Developer
- Aporia Team — Contributors

---

## 📞 Контакты

- 🌐 **Website:** [aporia.su](https://aporia.su)
- 💬 **Discord:** [Присоединиться](https://discord.gg/aporia)
- 📧 **Email:** support@aporia.su
- 🐙 **GitHub:** [@aporia](https://github.com/aporia)

---

<div align="center">

### ⭐ Если вам нравится проект, поставьте звезду! ⭐

**Aporia Client** — *Твой путь к совершенству в Minecraft*

[![Downloads](https://img.shields.io/github/downloads/aporia/client/total?style=for-the-badge&color=green)](https://github.com/aporia/client/releases)
[![Stars](https://img.shields.io/github/stars/aporia/client?style=for-the-badge&color=yellow)](https://github.com/aporia/client/stargazers)
[![Issues](https://img.shields.io/github/issues/aporia/client?style=for-the-badge&color=red)](https://github.com/aporia/client/issues)

---

*Версия: 0.4.1 | Minecraft: 1.21.11 | Обновлено: Март 2026*

</div>
