# Aporia Client — Full Project Analysis

## 📋 Project Overview

**Aporia** — современный чит-клиент для Minecraft 1.21.11+ на MCP (Mod Coder Pack).
Модульная архитектура, GPU-рендеринг через Blaze3D (OpenGL 4.5+), прямая сборка `javac` (без Gradle/Maven), Java 26+.

- **Entry point:** `src/mcp/client/Start.java` — запуск через `net.minecraft.client.main.Main.main()`
- **Main class:** `src/so/aporia/Aporia.kt` — `object Aporia : ResourceManagerReloadListener`
- **Модули:** 40+ штук, 6 категорий (COMBAT, MOVE, VISUAL, PLAYER, WORLD, MISC)
- **События:** 11 типов событий + StateMachine engine
- **Ротации:** 8 режимов (Smooth, Snap, HVH, Matrix, Vulcan, Grim, NCP, Intave)
- **Языки:** EN/RU/CN
- **Темы:** 10+ встроенных
- **Файлы:** кастомный формат `.apr` (ZIP-based), `FilesManager.ROOT = ~/.apr/`

---

## 📁 AntiCheats/ — Reverse Engineering & Analysis

Папка с сорцами популярных античитов для анализа и написания байпасов.

```
AntiCheats/
├── Grim-2.0/                          # Gradle multi-module
│   ├── common/                        # общие проверки
│   ├── bukkit/                        # Bukkit-платформа
│   ├── fabric/                        # Fabric-платформа
│   ├── build.gradle.kts
│   └── README.md
│
├── intave-master/                     # Gradle
│   ├── src/main/java/                 # сорцы Intave
│   ├── docs/
│   └── libs/
│
├── NoCheatPlus-master/                # Maven multi-module
│   ├── NCPCore/
│   ├── NCPPlugin/
│   ├── NCPCommons/
│   └── NCPCompatBukkit/
│
├── vulcan-anticheat-analysis-master/  # Web analysis tool
│   └── src/ (React + Tailwind)
│
├── Vulcant_old-master/                # Maven
│   └── src/main/java/
│
├── com/                               # Доп. библиотеки для анализа
└── org/                               # Доп. библиотеки для анализа
```

---

## 🎨 Shaders — Custom GLSL Shaders

32 своих шейдера (16 .fsh + 16 .vsh).
Шейдеры загружаются через **AssetsManager** удалённо — качаются с `https://gitlab.com/protect3ed/files-for-aporia/-/raw/main/aporia.apr`
и кешируются в `~/.apr/.assets/aporia/shaders/core/`.

| Тип | Шейдеры |
|-----|---------|
| **Fragment (.fsh)** | `aporia`, `blit`, `blur`, `entity_glow`, `image`, `kawase_down`, `kawase_up`, `logo`, `mainmenu`, `msdf`, `player_blur`, `player_outline`, `postprocess`, `render3d`, `render3d_mega`, `rounded_rect` |
| **Vertex (.vsh)** | `aporia`, `blit`, `blur`, `entity_glow`, `image`, `kawase_down`, `kawase_up`, `logo`, `mainmenu`, `msdf`, `player_blur`, `player_outline`, `postprocess`, `render3d`, `render3d_mega`, `rounded_rect` |

### entity_glow.json
Программный биндинг: `aporia:core/entity_glow`

### Как работает разрешение путей

**AssetsManager.kt** (`src/so/aporia/utils/assets/AssetManager.kt`):
- Качает ассеты с `https://gitlab.com/protect3ed/files-for-aporia/-/raw/main/aporia.apr`
- Сохраняет в `FilesManager.ROOT.resolve(".assets")` = `~/.apr/.assets/`
- `getResourcePath(id: Identifier)` — ищет `~/.apr/.assets/aporia/<path>`
- Если не находит — пробует `~/.apr/.assets/<namespace>/<path>`
- Формат `.apr` = ZIP с `content.dat` + `time.json`

**FilesManager.kt**:
- `ROOT = OsManager.mainDirectory` = `~/.apr/`
- `resolve(path)` — если absolute → возврат, иначе `ROOT.resolve(path)`
- `resolveTilde(path)` — заменяет `~` на `user.home`

> **ClientPackSources.java** — не найден в проекте. Вероятно, был выпилен или находится вне репозитория.

---

## 📂 src/ — Полная структура

```
src/
├── aporia/
│   ├── cc/                          # Core systems
│   │   ├── OsManager.kt             # OS abstraction (911 строк)
│   │   ├── PanicSystem.kt           # Паник-система
│   │   ├── UserData.kt              # Данные юзера
│   │   ├── UserGenerator.kt         # Генератор юзеров
│   │   └── network/                 # HTTP, ThreadManager
│   ├── language/
│   │   └── LanguageBase.kt          # Языковая база
│   └── webview/
│       ├── Webview.kt               # Abstract webview
│       ├── WebviewFactory.kt        # Фабрика
│       ├── WebviewScreen.kt         # Экран (290 строк)
│       ├── platform/                # WebView2, WKWebView, Android...
│       └── render/                  # Текстурный рендер webview
│
├── assets/
│   ├── aporia/                      # Шрифты (Inter-Regular.ttf), текстуры
│   └── minecraft/                   # Все стоковые ассеты MC (шейдеры, модели, lang...)
│
├── data/
│   ├── aporia/                      # Специфичные данные
│   └── minecraft/                   # Рецепты, loot tables, worldgen...
│
├── com/
│   ├── aporia/blaze3d/              # Blaze3D расширения
│   ├── ferra13671/discordipc/       # Discord IPC библиотека
│   ├── github/lunatrius/            # Litematica/Schematica API
│   ├── viaversion/viafabricplus/    # ViaFabricPlus (100+ файлов: протоколы, миксины, экраны)
│   └── mojang/blaze3d/              # Стоковый Blaze3D API
│   │   ├── audio/ buffers/ font/ framegraph/ opengl/
│   │   ├── pipeline/ platform/ preprocessor/
│   │   └── shaders/ systems/ textures/ vertex/
│
├── de/florianmichael/               # ViaLoadingBase + ViaMCP (протоколы)
│
├── dev/redstones/                   # Media player info
│
├── mcp/client/Start.java            # MCP entry point
│
├── mediaplayerinfo/                 # Нативная DLL
│
├── META-INF/MANIFEST.MF
│
├── net/minecraft/                   # Полные сорцы Minecraft
│   ├── client/ server/ world/
│   └── network/ util/
│
├── org/                             # Доп. библиотеки
│
└── so/aporia/                       # ⭐ ОСНОВНОЙ КОД КЛИЕНТА
    ├── Aporia.kt                    # Точка входа (main class)
    ├── module/
    │   ├── Module.kt                # Базовый класс модуля
    │   ├── ModuleManager.kt         # Реестр модулей
    │   ├── Category.kt              # COMBAT, MOVE, VISUAL, PLAYER, WORLD, MISC
    │   ├── settings/                # Setting<*> иерархия (12 файлов)
    │   └── impl/
    │       ├── combat/              # Aura, TPAura, Criticals, AutoTotem...
    │       ├── move/                # Speed, Flight, Velocity, AutoSprint
    │       ├── render/              # Hud, PlayerESP, NameTags, NoRender, WorldRenderer...
    │       ├── player/              # ElytraHelper, NoPush
    │       ├── world/               # MiddleClick
    │       └── misc/                # ClickGui, DiscordRPC, Disabler, PacketDebug...
    │
    └── utils/
        ├── assets/AssetManager.kt   # Синхронизация удаленных ассетов
        ├── events/                  # EventBus, Event, EventHandler, StateMachine, 11 impl
        ├── files/                   # FilesManager, AprParser, ConfigFile, ChatFile
        ├── imports/QuickImports.kt  # Глобальные алиасы (r, mc, files, os, bus...)
        ├── math/                    # Angle, Prediction, Speed
        ├── packets/                 # PacketInterceptor, PacketUtil
        └── user/
            ├── command/             # Command, CommandManager, 7 impl
            ├── friend/FriendManager.kt
            ├── input/               # KeybindManager, KeyboardLayout, KeyCodeMap
            ├── locale/LocaleManager.kt
            ├── logger/Logger.kt     # ANSI-цветной вывод
            ├── player/              # inventory/, movement/, rotation/ (8 режимов)
            └── render/              # ⭐ Система рендеринга
                ├── animation/       # Animator, Easing, SpringSimulator...
                ├── color/ColorUtil.kt
                ├── core/            # AporiaRenderer, BlurRenderer, PipelineSnippet...
                ├── font/            # FontAtlas, FontRenderer, FontPipeline, Glyph
                ├── render3d/AporiaRenderer3D.java
                └── ui/              # ChatScreen, ClickGuiScreen (1112 строк), QuestManager, ThemeManagerModule
```

### Module Implementations (40+ модулей)

| Категория | Модули |
|-----------|--------|
| **Combat** | Aura, TPAura (529 строк), AutoGapple, AutoTotem, Criticals, ElytraTarget, God, NoFriendDamage, SpearTarget |
| **Move** | AutoSprint, Flight (Default/Spear/DragonFly), Speed (Packet/Grim), Velocity |
| **Render** | Hud, Beautifully (Kawase blur, пост-обработка), PlayerESP (598 строк), NameTags, NoRender, WorldRenderer, DynamicIsland, TargetHud |
| **Player** | ElytraHelper, NoPush |
| **World** | MiddleClick |
| **Misc** | ClickGui, DiscordRPC (APP_ID: 1471901603287142421), Disabler, PacketDebug, ServerHelper, AutoConfig, AutoEZ, TestModule |

---

## 🔧 Key Components Deep Dive

### OsManager (`src/aporia/cc/OsManager.kt`, 911 строк)

Singleton-абстракция над ОС. Полностью написана на Kotlin.

- **Platform detection:** WINDOWS / LINUX / MAC / UNKNOWN
- **CPU info:** имя, ядра, частота (через WMIC / proc/cpuinfo / sysctl)
- **GPU info:** через GL properties + fallback на WMIC/lspci/system_profiler
- **RAM info:** через Runtime + WMIC / proc/meminfo / sysctl
- **Disk info:** total/free/usable space
- **VM detection:** BIOS vendor, DMI, hypervisor `/sys/hypervisor/type`
- **Container detection:** `/.dockerenv`, `/proc/1/cgroup`, env vars
- **Location services:** ipwho.is API (город/регион/страна/timezone)
- **Weather:** wttr.in API (температура, влажность, ветер)
- **Directories:** `~/.apr/` с подпапками: cache, data, logs, temp, backup, modules, themes, scripts
- **System info:** ASCII-баннер, полный дамп системы

### FilesManager (`src/so/aporia/utils/files/FilesManager.kt`, 300 строк)

Singleton, центральный файловый менеджер.

- `ROOT = OsManager.mainDirectory` = `~/.apr/`
- `init()` — создаёт ROOT, на Windows делает `attrib +s +h`
- `resolve(path)` / `resolveTilde(path)` — резольвинг путей
- `.apr` формат: ZIP с `content.dat` + `time.json`

**FileType enum:** APR, ZIP, JAVA, LUA, CBM, CFG, UNKNOWN

**Методы:** `writeApr`, `readApr`, `readBytes`, `readText`, `writeBytes`, `writeText`, `delete`, `exists`, `size`, `copy`, `move`, `readCfg`, `writeCfg`, `listFiles`, `append`, `writeJson`, `readJson`

**Impl:**
- `ConfigFile.java` — автосохранение конфигов (30s interval)
- `ChatFile.kt` — сохранение/загрузка чата
- `AprParser.kt` — парсер `.apr` конфиг-формата

### StateMachine (`src/so/aporia/utils/events/StateMachine.kt`, 80 строк)

Generic engine для стейт-машин на Kotlin.

- `@StateMachine` — аннотация для класса
- `@StateMachineRegister(from, to, on)` — аннотация для переходов
- `StateMachineEngine<S : Enum<S>>` — сам движок:
  - Рефлексией находит аннотированные переходы
  - `transition(event)` — переключение состояния
  - `isIn(vararg states)` — проверка состояния
  - `on<T>(from, to, handler)` — inline DSL для переходов
  - Постит `StateTransitionEvent` в EventBus при смене состояния

### QuickImports (`src/so/aporia/utils/imports/QuickImports.kt`, 27 строк)

Набор глобальных get-only val-алиасов для быстрого доступа:

| Алиас | Что даёт |
|-------|----------|
| `r` | `AporiaRenderer.INSTANCE` |
| `mc` | `Minecraft.getInstance()` |
| `logger` | `Logger` |
| `fonts` | `Aporia.FONTS` |
| `locale` | `LocaleManager.INSTANCE` |
| `theme` | `ThemeManagerModule.activeTheme()` |
| `files` | `FilesManager` |
| `os` | `OsManager` |
| `bus` | `EventBus` |
| `mm` | `ModuleManager` |
| `colorUtil` | `ColorUtil` |
| `fm` | `FriendManager` |

---

## 🔗 Dependencies

- **Discord IPC:** `com/ferra13671/discordipc/`
- **Protocol libs:** `ViaLoadingBase`, `ViaMCP` (`de/florianmichael/`)
- **Webview:** CEF (`org/cef/`), MCEF (`com/cinemamod/mcef/`)
- **Obfuscation:** ChaosObfuscator (`com/chaos/annotation/`)
- **Native:** `mediaplayerinfo/` DLL

---

## ⚙️ Build

Сборка через `completion.ps1`:

1. **Компиляция:** `kotlin-compiler.jar` (K2) для `.kt` + `javac` для `.java` (инкрементально)
2. **Мерж:** распаковка всех `libs/*.jar` в общую директорию
3. **Пакет:** создание fat-jar `Aporia.jar` + `Aporia.zip` + `Aporia_RELEASE.zip`

```powershell
.\completion.ps1 build     # Build + Zip
.\completion.ps1 run       # Run (dev)
.\completion.ps1 menu      # Интерактивное меню
```

**Output:** `sborka/Aporia.jar` + `sborka/Aporia.mcp.jar` + `sborka/Aporia.zip`

---

## 📄 License

Aporia.cc Software License Agreement v1.0 — проприетарное ПО.
