# Aporia Client

**Aporia** — современный чит-клиент для Minecraft 1.21.11+ с модульной архитектурой, GPU-ускоренным рендерингом и интеграцией Discord.

---

## 🇷🇺 Русский

### 📋 Описание

Aporia — это продвинутый клиент для Minecraft, созданный на базе официального лаунчера с полным доступом к исходному коду игры. Клиент использует современные возможности GPU (Mojang Blaze3D API) для создания красивого интерфейса, расширенного визуала и оптимизированных механик.

**Основные особенности:**
- Модульная система с управлением жизненным циклом
- GPU-ускоренный рендеринг через Blaze3D (SDF-шейдеры, размытие, хроматическая аберрация)
- Система событий с автоматическим обнаружением обработчиков
- Серверная ротация (Smooth, HVH, Matrix, Vulcan, Grim режимы)
- Настраиваемая тема оформления (10+ тем на выбор)
- Полная мультиязычность (EN/RU/CN)

### 🎮 Возможности

#### Combat (Бой)
- **Aura** — урон по всей доступной области с интеллектуальной системой ротации
  - Режимы 1.8/1.9+ (поддержка cps и cooldown)
  - Мульти-таргеты (игроки, мобы, животные)
  - Ротации: Smooth, Snap, HVH, Matrix, Vulcan, Grim
  - Настройки: Jitter, Aim Offset, Hit Chance
  - Auto Crit с прыжком (1.9+ режим)
  - TPS Sync синхронизация с тиковыми циклами сервера

- **TPAura** — мгновенный урон с телепортацией к цели
- **AutoGapple** — автоматическое использование золотых яблок
- **AutoTotem** — автоматическая смена тотема при получении урона
- **Criticals** — автоматические критические удары
- **God** — защита от урона (проверка, не включена ли на сервере)

#### Move (Перемещение)
- **Speed** — увеличение скорости движения
  - Packet режим — мультипликатор позиции
  - Grim режим — дрейф позиции на каждом пакете
- **AutoSprint** — автоматический спринт при движении
- **Velocity** — игнорирование knockback от мобов и игроков

#### Render (Визуал)
- **HUD** — отображение статуса, времени, Discord аватара, UUID
- **Beautifully** — основной модуль визуала
  - Размытие (Kawase blur, 4 прохода)
  - Собственный чат
  - Пост-обработка (насыщенность, аберрация)
- **PlayerESP** — обводка игроков (цвет friends, enemies, animals)
- **EntityESP** — обводка всех сущностей
- **NameTags** — расширенные имена с HP, дистанцией, пингом

#### Player (Игрок)
- **NoPush** — отключение отталкивания от других игроков

#### World (Мир)
- **MiddleClick** — удаление блоков и копирование данных через среднюю кнопку мыши

#### Misc (Разное)
- **ClickGui** — современный интерфейс настроек
- **Discord RPC** — статус и аватар в Discord
- **ServerHelper** — помощь на сервере
- **AutoConfig** — автоматическое переключение конфигураций
- **AutoEZ** — автоматическое сообщение о победе

### ⚙️ Техническая архитектура

```
src/so/aporia/
├── Aporia.java                  # Главный класс, точка входа
├── module/
│   ├── Module.java              # Базовый класс модулей
│   ├── ModuleManager.java       # Реестр всех модулей
│   ├── Category.java            # Категории (COMBAT, MOVE, VISUAL, PLAYER, WORLD, MISC)
│   └── impl/
│       ├── combat/              # Модули боя
│       ├── move/                # Модули движения
│       ├── render/              # Модули рендеринга
│       ├── player/              # Модули игрока
│       ├── world/               # Модули мира
│       └── misc/                # Разные модули
└── utils/
    ├── events/                  # Система событий
    │   ├── Event.java
    │   ├── EventBus.java        # Центральная шина событий
    │   ├── EventHandler.java    # Аннотация обработчиков
    │   └── impl/                # Конкретные события (TickEvent, PacketEvent, RenderHudEvent)
    ├── packets/                 # Перехват пакетов
    │   └── PacketInterceptor.java
    ├── files/                   # Управление файлами
    │   ├── FilesManager.java    # Основной менеджер файлов
    │   ├── AprParser.java       # Парсер .apr/.zip архивов
    │   └── impl/                # ConfigFile, ChatFile и т.д.
    └── user/                    # Пользовательские системы
        ├── locale/              # Мультиязычность (LocaleManager)
        ├── rotation/            # Серверная ротация (RotationUtil)
        │   └── RotationUtil.java (Smooth, HVH, Matrix, Vulcan, Grim)
        ├── render/              # GPU рендеринг
        │   ├── core/            # AporiaRenderer (SDF, blur, shaders)
        │   ├── font/            # FontRenderer, FontAtlas, FontPipeline
        │   ├── animation/       # TypeAnim (анимация текста)
        │   ├── color/           # ColorUtil (RGB/RGBA/HEX генераторы)
        │   ├── theme/           # ThemeManager (10+ тем)
        │   └── ui/              # UI компоненты (ClickGuiScreen)
        ├── input/               # KeybindManager (управление клавишами)
        ├── inventory/           # InventoryManager
        ├── friend/              # FriendManager
        └── logger/              # Logger (INFO/WARN/ERROR/SUCCESS)
```

### 🔧 Требования

- **Java**: 26+
- **Minecraft**: 1.21.11+
- **GPU**: Поддержка OpenGL 4.5+ (для GPU-рендеринга)
- **Сборка**: `javac` напрямую (без Gradle/Maven)

### 🛠️ Сборка

```bash
# Клонирование репозитория
git clone <repo>
cd Aporia

# Использование build.bat (рекомендуется)
build.bat

# Или вручную
javac -d build -sourcepath src --release 26 -encoding UTF-8 -cp "libs/*.jar" \
  -processorpath "libs/lombok.jar" \
  -J-Xmx4g $(find src -name "*.java")
```

**Результат сборки:**
- `sborka/Aporia.jar` — обфусцированный jar-файл
- `sborka/Aporia.zip` — архив с jar и JSON-файлом версии

### 📁 Структура файлов

```
Aporia/
├── src/                         # Исходный код
│   ├── so/aporia/               # Основной код клиента
│   ├── aporia/cc/               # OS Manager, Auth, UserData
│   ├── assets/                  # Ресурсы (шрифты, текстуры, шейдеры)
│   ├── data/                    # Данные Minecraft
│   └── META-INF/MANIFEST.MF     # Манифест
├── libs/                        # Зависимости (90+ jar-файлов)
│   ├── lwjgl-*.jar              # LWJGL 3.3.3 (GPU рендеринг)
│   ├── asm-*.jar                # ASM 9.2 (трансформации)
│   ├── forge/*.jar              # Forge API
│   └── ...                      # Прочие зависимости
├── sborka/                      # Сборочные артефакты
│   ├── classes/                 # Скомпилированные классы
│   ├── merge/                   # Слияние jar-файлов
│   └── ChaosObfuscator.jar      # Обфускатор
└── build.bat                    # Скрипт сборки
```

### 🎨 Система рендеринга

Aporia использует современный GPU-рендеринг через **Blaze3D API**:

**AporiaRenderer (2D):**
- Прямоугольники (fill, circle, rounded rect)
- Текст (bold, regular с MSDF шрифтами)
- Изображения (загрузка из InputStream)
- Kawase blur (4-pass down/up sample)
- Post-processing (насыщенность, хроматическая аберрация)

**AporiaRenderer3D (3D):**
- 3D-линии, кубы, сферы
- Tesselator для сложной геометрии
- Перспективная проекция

**Шейдеры (GLSL):**
- `core/aporia` — основной шейдер (SDF, shapes, blur)
- `core/image` — рендеринг текстур
- `pipeline/aporia` — рендер пайплайн

### 🔄 Система событий

```java
// Регистрация обработчиков
@EventHandler
public void onTick(TickEvent event) {
    // Код при каждом тике
}

// Отправка событий
EventBus.INSTANCE.post(new TickEvent());
```

**Основные события:**
- `TickEvent` — тик игры (20 тиков/сек)
- `PacketEvent` — отправка/получение пакетов
- `RenderHudEvent` — рендер HUD
- `MouseMoveEvent`, `KeyChangeEvent`, `MouseClickEvent`

### 🎯 Система ротации

**Режимы ротации (RotationUtil):**
- **Smooth** — плавное вращение с GCD-фиксом
- **Snap** — мгновенный поворот (с проверкой скорости)
- **HVH** — режим для PvP (без GCD фикса)
- **Matrix** — имитация Matrix чита
- **Vulcan** — имитация Vulcan чита
- **Grim** — имитация Grim чита

**Глобальная синхронизация:**
```java
RotationUtil.sync();          // Синхронизация с клиентом
RotationUtil.update(target, speed);  // Обновление до цели
RotationUtil.getServerYaw();  // Получение server-side yaw
RotationUtil.getServerPitch(); // Получение server-side pitch
```

### 🌐 Мультиязычность

**Поддерживаемые языки:**
- `en_EU` — английский (EU)
- `ru_RU` — русский
- `ch_CH` — китайский

**Использование:**
```java
LocaleManager lm = LocaleManager.getInstance();
lm.get("module.aura.range"); // Вернет локализованную строку
```

**Файлы локализации:**
- `~/.apr/.assets/aporia/locale/en_EU.json`
- `~/.apr/.assets/aporia/locale/ru_RU.json`
- `~/.apr/.assets/aporia/locale/ch_CH.json`

### 🎨 Система тем

**Встроенные темы:**
1. DefaultAporia — классический дизайн
2. Amethyst — фиолетовый
3. Synthwave — ретро-футуризм
4. Matrix — зеленый терминал
5. Ocean — синий океан
6. Blood — красный кровавый
7. Midnight — темно-синий
8. Forest — зеленый лес
9. Sunrise — оранжевый рассвет
10. Cyberpunk — неоновый киберпанк

**Файлы тем:**
- `~/.apr/themes/DefaultAporia.apr`
- `~/.apr/themes/_selected.apr`

### 📝 Настройки модулей

**Типы настроек:**
- `BooleanSetting` — чекбокс (true/false)
- `SelectSetting` — выпадающий список (один выбор)
- `MultiSelectSetting` — множественный выбор
- `NumberSetting` — число (min/max/step)
- `RangeSetting` — диапазон
- `TextSetting` — текст
- `ButtonSetting` — кнопка
- `BindSetting` — привязка клавиши

**Пример:**
```java
public NumberSetting range = new NumberSetting(
    "Range", "Диапазон урона",
    3.5, 1.0, 6.0, 0.1
);
```

### 🔐 Безопасность

- Обфускация через ChaosObfuscator
- Скрытые файлы на Windows (attrib +s +h для ~/.apr)
- Встроенная система паника (PanicSystem)
- Отсутствие сбора персональных данных

### 📄 Лицензия

Aporia.cc Software License Agreement v1.0 — проприетарное ПО.

**Разрешено:**
- Установка и использование на одном устройстве
- Обучение (просмотр исходного кода)
- Создание модификаций для личного использования

**Запрещено:**
- Распространение (distribution)
- Коммерческое использование
- Создание конкурентов (reverse engineering)
- Удаление уведомлений об авторских правах

---

### 🔗 Ресурсы

- **Версия**: 1.0-dev
- **Minecraft**: 1.21.11
- **Java**: 26+
- **Статус**: Active development

---

## 🇬🇧 English

### Description

Aporia — a modern cheat client for Minecraft 1.21.11+ with modular architecture, GPU-accelerated rendering, and Discord integration.

### Features

See the Russian section for detailed features.

### Technical Architecture

Same structure as Russian version (see above).

### Requirements

- **Java**: 26+
- **Minecraft**: 1.21.11+
- **GPU**: OpenGL 4.5+ support
- **Build**: Direct `javac` (no Gradle/Maven)

### Build

See the Russian section for build instructions.

### License

Aporia.cc Software License Agreement v1.0 — proprietary software.

---

## 🇨🇳 中文

### 描述

Aporia — 面向 Minecraft 1.21.11+ 的现代作弊客户端，具有模块化架构、GPU 加速渲染和 Discord 集成。

### 功能

见俄语部分的详细功能。

### 技术架构

与俄语部分相同（参见上文）。

### 要求

- **Java**: 26+
- **Minecraft**: 1.21.11+
- **GPU**: OpenGL 4.5+ 支持
- **构建**: 直接使用 `javac`（无需 Gradle/Maven）

### 构建

见俄语部分的构建说明。

### 许可证

Aporia.cc 软件许可协议 v1.0 — 专有软件。

---

**Version**: 1.0-dev  
**Minecraft**: 1.21.11  
**Java**: 26+  
**Status**: Active development
