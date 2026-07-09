# Aporia Client — Full Project Analysis

## 📋 Project Overview

**Aporia** — современный чит-клиент для Minecraft 1.21.11+ на NeoForge.
Модульная архитектура, GPU-рендеринг через Blaze3D (OpenGL 4.5+), прямая сборка `javac` (без Gradle/Maven), Java 26+.

- **Entry point:** `src/so/aporia/Aporia.kt` — `object Aporia : ResourceManagerReloadListener`
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
├── com/cinemamod/mcef/               # Cinema mod CEF
└── org/cef/                           # Chromium Embedded Framework
```

---

## 🎨 shaders/core — Custom GLSL Shaders

32 своих шейдера (16 .fsh + 16 .vsh) в `D:\Aporia\shaders\core\`.
Резильвятся через `FilesManager.ROOT` (`~/.apr/`):

### Fragment Shaders (.fsh)

| Файл | Назначение |
|------|-----------|
| `aporia.fsh` | Основной — SDF rounded box, blur texture sampling |
| `blit.fsh` | Passthrough blit (InputTexture → fragColor) |
| `blur.fsh` | Gaussian blur (5-tap, strength, direction, saturation) |
| `entity_glow.fsh` | Glow outline для энтити (сейчас solid red) |
| `image.fsh` | Rounded rect image (SDF, border, corner radius) |
| `kawase_down.fsh` | Kawase blur down-sample (4-pixel hardware blend) |
| `kawase_up.fsh` | Kawase blur up-sample (restore resolution) |
| `logo.fsh` | Логотип с пульсацией (`sin(time * 2.0)`) |
| `mainmenu.fsh` | Главное меню — procedural noise/hash анимация |
| `msdf.fsh` | MSDF font rendering с outline |
| `player_blur.fsh` | Блюр игрока (Gaussian weights H/V) |
| `player_outline.fsh` | Простой outline (vertex color passthrough) |
| `postprocess.fsh` | Пост-обработка (saturation: grayscale → boosted) |
| `render3d.fsh` | 3D diffuse lighting (ambient + directional) |
| `render3d_mega.fsh` | Advanced 3D: SDF primitives, glow, rim, Fresnel |
| `rounded_rect.fsh` | Rounded rect SDF (circle/box, borders, multi-mode) |

### Vertex Shaders (.vsh)

| Файл | Назначение |
|------|-----------|
| `aporia.vsh` | Standart MVP + UV/color/screen pos |
| `blit.vsh` | Minimal passthrough |
| `blur.vsh` | Pass-through for blur |
| `entity_glow.vsh` | Fresnel-based edge detection |
| `image.vsh` | Standart image rendering |
| `kawase_down.vsh` | Pass-through |
| `kawase_up.vsh` | Pass-through |
| `logo.vsh` | Projection transform |
| `mainmenu.vsh` | Full-screen quad |
| `msdf.vsh` | Glyph batching + rotation + atlas coords |
| `player_blur.vsh` | Player blur + dynamic transforms + screen UV |
| `player_outline.vsh` | Color passthrough |
| `postprocess.vsh` | Full-screen quad |
| `render3d.vsh` | MVP + normal transform |
| `render3d_mega.vsh` | Draw params (mode/shape/color/panel uniforms) |
| `rounded_rect.vsh` | Standart rounded rect |

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
                ├── theme/ThemeManager.java
                └── ui/              # ChatScreen, ClickGuiScreen (1112 строк), QuestManager
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
| `theme` | `ThemeManager.INSTANCE.active()` |
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

```bash
javac -d build -sourcepath src --release 26 -encoding UTF-8 -cp "libs/*.jar" \
  -processorpath "libs/lombok.jar" \
  -J-Xmx4g $(find src -name "*.java")
```

**Output:** `sborka/Aporia.jar` (обфусцированный) + `sborka/Aporia.zip`

---

## 📄 License

Aporia.cc Software License Agreement v1.0 — проприетарное ПО.
