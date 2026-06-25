# Aporia Client

**Aporia** — a modern cheat client for Minecraft 1.21.11+ with modular architecture, GPU-accelerated rendering, and Discord integration.

## Features

### Combat (Combat)
- **Aura** — damage to entire available area with intelligent rotation system
  - 1.8/1.9+ modes (cps and cooldown support)
  - Multi-targets (players, mobs, animals)
  - Rotations: Smooth, Snap, HVH, Matrix, Vulcan, Grim
  - Settings: Jitter, Aim Offset, Hit Chance
  - Auto Crit with jump (1.9+ mode)
  - TPS Sync synchronization with server tick cycles

- **TPAura** — instant damage with teleportation to target
- **AutoGapple** — automatic golden apple usage
- **AutoTotem** — automatic totem swap on damage
- **Criticals** — automatic critical hits
- **God** — damage protection (check if server has it enabled)

### Move (Movement)
- **Speed** — movement speed increase
  - Packet mode — position multiplier
  - Grim mode — position drift on each packet
- **AutoSprint** — automatic sprinting when moving
- **Velocity** — ignore knockback from mobs and players

### Render (Visuals)
- **HUD** — display status, time, Discord avatar, UUID
- **Beautifully** — main visual module
  - Blur (Kawase blur, 4-pass)
  - Custom chat
  - Post-processing (saturation, chromatic aberration)
- **PlayerESP** — player outlines (friend/enemy/animal colors)
- **EntityESP** — outlines for all entities
- **NameTags** — extended names with HP, distance, ping

### Player (Player)
- **NoPush** — disable push from other players

### World (World)
- **MiddleClick** — block removal and data copying via middle mouse button

### Misc (Miscellaneous)
- **ClickGui** — modern settings interface
- **Discord RPC** — status and avatar in Discord
- **ServerHelper** — server assistance
- **AutoConfig** — automatic configuration switching
- **AutoEZ** — automatic victory message

## Technical Architecture

```
src/so/aporia/
├── Aporia.java                  # Main class, entry point
├── module/
│   ├── Module.java              # Base module class
│   ├── ModuleManager.java       # Registry of all modules
│   ├── Category.java            # Categories (COMBAT, MOVE, VISUAL, PLAYER, WORLD, MISC)
│   └── impl/
│       ├── combat/              # Combat modules
│       ├── move/                # Movement modules
│       ├── render/              # Rendering modules
│       ├── player/              # Player modules
│       ├── world/               # World modules
│       └── misc/                # Miscellaneous modules
└── utils/
    ├── events/                  # Event system
    │   ├── Event.java
    │   ├── EventBus.java        # Central event bus
    │   ├── EventHandler.java    # Handler annotation
    │   └── impl/                # Specific events (TickEvent, PacketEvent, RenderHudEvent)
    ├── packets/                 # Packet interception
    │   └── PacketInterceptor.java
    ├── files/                   # File management
    │   ├── FilesManager.java    # Main file manager
    │   ├── AprParser.java       # .apr/.zip archive parser
    │   └── impl/                # ConfigFile, ChatFile, etc.
    └── user/                    # User systems
        ├── locale/              # Multi-language (LocaleManager)
        ├── rotation/            # Server rotation (RotationUtil)
        │   └── RotationUtil.java (Smooth, HVH, Matrix, Vulcan, Grim)
        ├── render/              # GPU rendering
        │   ├── core/            # AporiaRenderer (SDF, blur, shaders)
        │   ├── font/            # FontRenderer, FontAtlas, FontPipeline
        │   ├── animation/       # TypeAnim (text animation)
        │   ├── color/           # ColorUtil (RGB/RGBA/HEX generators)
        │   ├── theme/           # ThemeManager (10+ themes)
        │   └── ui/              # UI components (ClickGuiScreen)
        ├── input/               # KeybindManager (key management)
        ├── inventory/           # InventoryManager
        ├── friend/              # FriendManager
        └── logger/              # Logger (INFO/WARN/ERROR/SUCCESS)
```

## Requirements

- **Java**: 26+
- **Minecraft**: 1.21.11+
- **GPU**: OpenGL 4.5+ support (for GPU rendering)
- **Build**: Direct `javac` (no Gradle/Maven)

## Build

```bash
# Clone repository
git clone <repo>
cd Aporia

# Use build.bat (recommended)
build.bat

# Or manually
javac -d build -sourcepath src --release 26 -encoding UTF-8 -cp "libs/*.jar" \
  -processorpath "libs/lombok.jar" \
  -J-Xmx4g $(find src -name "*.java")
```

**Build output:**
- `sborka/Aporia.jar` — obfuscated jar file
- `sborka/Aporia.zip` — archive with jar and version JSON

## Rendering System

Aporia uses modern GPU rendering via **Blaze3D API**:

**AporiaRenderer (2D):**
- Rectangles (fill, circle, rounded rect)
- Text (bold, regular with MSDF fonts)
- Images (loading from InputStream)
- Kawase blur (4-pass down/up sample)
- Post-processing (saturation, chromatic aberration)

**AporiaRenderer3D (3D):**
- 3D lines, cubes, spheres
- Tesselator for complex geometry
- Perspective projection

**Shaders (GLSL):**
- `core/aporia` — main shader (SDF, shapes, blur)
- `core/image` — texture rendering
- `pipeline/aporia` — render pipeline

## Event System

```java
// Register handlers
@EventHandler
public void onTick(TickEvent event) {
    // Code on each tick
}

// Post events
EventBus.INSTANCE.post(new TickEvent());
```

**Main events:**
- `TickEvent` — game tick (20 ticks/sec)
- `PacketEvent` — packet send/receive
- `RenderHudEvent` — HUD rendering
- `MouseMoveEvent`, `KeyChangeEvent`, `MouseClickEvent`

## Rotation System

**Rotation modes (RotationUtil):**
- **Smooth** — smooth rotation with GCD fix
- **Snap** — instant rotation (with speed check)
- **HVH** — PvP mode (no GCD fix)
- **Matrix** — Matrix cheat imitation
- **Vulcan** — Vulcan cheat imitation
- **Grim** — Grim cheat imitation

**Global synchronization:**
```java
RotationUtil.sync();          // Sync with client
RotationUtil.update(target, speed);  // Update to target
RotationUtil.getServerYaw();  // Get server-side yaw
RotationUtil.getServerPitch(); // Get server-side pitch
```

## Multi-language

**Supported languages:**
- `en_EU` — English (EU)
- `ru_RU` — Russian
- `ch_CH` — Chinese

**Usage:**
```java
LocaleManager lm = LocaleManager.getInstance();
lm.get("module.aura.range"); // Returns localized string
```

**Locale files:**
- `~/.apr/.assets/aporia/locale/en_EU.json`
- `~/.apr/.assets/aporia/locale/ru_RU.json`
- `~/.apr/.assets/aporia/locale/ch_CH.json`

## Themes

**Built-in themes:**
1. DefaultAporia — classic design
2. Amethyst — purple
3. Synthwave — retro-futurism
4. Matrix — green terminal
5. Ocean — blue ocean
6. Blood — red bloody
7. Midnight — dark blue
8. Forest — green forest
9. Sunrise — orange sunrise
10. Cyberpunk — neon cyberpunk

**Theme files:**
- `~/.apr/themes/DefaultAporia.apr`
- `~/.apr/themes/_selected.apr`

## Module Settings

**Setting types:**
- `BooleanSetting` — checkbox (true/false)
- `SelectSetting` — dropdown (single choice)
- `MultiSelectSetting` — multi-select
- `NumberSetting` — number (min/max/step)
- `RangeSetting` — range
- `TextSetting` — text
- `ButtonSetting` — button
- `BindSetting` — key binding

**Example:**
```java
public NumberSetting range = new NumberSetting(
    "Range", "Damage range",
    3.5, 1.0, 6.0, 0.1
);
```

## Security

- Obfuscation via ChaosObfuscator
- Hidden files on Windows (attrib +s +h for ~/.apr)
- Built-in panic system (PanicSystem)
- No personal data collection

## License

Aporia.cc Software License Agreement v1.0 — proprietary software.

**Allowed:**
- Installation and use on one device
- Learning (viewing source code)
- Creating modifications for personal use

**Prohibited:**
- Distribution
- Commercial use
- Creating competitors (reverse engineering)
- Removing copyright notices

---

**Version**: 1.0-dev  
**Minecraft**: 1.21.11  
**Java**: 26+  
**Status**: Active development
