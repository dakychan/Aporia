# Aporia Client

**Aporia** — modern cheat client for Minecraft 1.21.11+ with modular architecture, GPU-accelerated rendering, Discord integration, and webview support.

---

## Branches

| Branch | Description | Repository |
|--------|-------------|------------|
| **MCP** | Main branch — MCP/vanilla launcher with direct `javac` build | [MCP](https://gitlab.com/protect3ed/aporia-client) |
| **Fabric** | Fabric port (experimental, development halted) | [Fabric](https://gitlab.com/protect3ed/aporia-client/-/tree/fabric?ref_type=heads) |

```bash
# Switch to MCP
git checkout mcp

# Switch to Fabric
git checkout fabric
```

---

## Features

### Combat
- **Aura** — multi-target with rotation modes (Smooth, Snap, HVH, Matrix, Vulcan, Grim), 1.8/1.9+ cooldown, jitter, hit chance, TPS sync
- **TPAura**, **AutoGapple**, **AutoTotem**, **Criticals**, **God**, **ElytraTarget**, **NoFriendDamage**, **SpearTarget**

### Movement
- **Speed** — Packet and Grim modes
- **AutoSprint**, **Velocity**, **Flight**

### Render
- **HUD** — status, time, Discord avatar, UUID
- **Beautifully** — Kawase blur (4-pass), custom chat, post-processing (saturation, chromatic aberration)
- **PlayerESP** / **EntityESP** — glow outlines with friend/enemy colors
- **NameTags** — extended nametags with HP, distance, ping
- **DynamicIsland** — media player island with seekbar, module info, bossbar
- **TargetHud**, **WorldRenderer**, **NoRender**

### Player
- **NoPush**, **ElytraHelper**

### World
- **MiddleClick**

### Misc
- **ClickGui** — modern settings UI with 10+ themes
- **Discord RPC** — status + avatar in Discord
- **ServerHelper**, **AutoConfig**, **AutoEZ**, **PacketDebug**, **Disabler**, **TestModule**

---

## Architecture

```
src/so/aporia/
├── Aporia.kt                          # Main entry point
├── module/
│   ├── Module.kt                      # Base module class
│   ├── ModuleManager.kt               # Module registry (~31 modules)
│   ├── Category.kt                    # COMBAT/MOVE/VISUAL/PLAYER/WORLD/MISC
│   ├── settings/                      # 12 setting types
│   └── impl/{combat,move,render,player,world,misc}/
├── utils/
│   ├── events/                        # EventBus + 11 event types
│   ├── files/                         # FilesManager, AprParser, ConfigFile
│   ├── packets/                       # Packet interceptor
│   ├── imports/QuickImports.kt        # Global aliases (r, mc, bus, mm, etc.)
│   └── user/
│       ├── locale/                    # EN/RU/CN
│       ├── rotation/                  # 6 rotation modes
│       ├── render/                    # GPU renderer, fonts, animations, themes, UI
│       ├── input/                     # Keybind manager
│       ├── command/                   # Command system
│       ├── friend/                    # Friend manager
│       └── logger/                    # Logger
shaders/core/                          # 31 GLSL shaders
libs/                                  # 109 dependencies
```

### Render Pipeline
- **AporiaRenderer** (2D) — SDF rounded rects, MSDF text, images, Kawase blur, post-processing
- **AporiaRenderer3D** (3D) — lines, cubes, spheres, tessellator, perspective projection
- Blaze3D API (OpenGL 4.5+), 31 custom shaders

### Rotation Modes
- **Smooth** — GCD-fixed smooth rotation
- **Snap** — instant with speed check
- **HVH** — PvP mode (no GCD fix)
- **Matrix / Vulcan / Grim** — anticheat-specific simulation

### Shaders
- `aporia.fsh` — main SDF rounded box + blur sampling
- `kawase_{down,up}.fsh` — Kawase blur (4-pass)
- `msdf.fsh` — MSDF font rendering with outline
- `rounded_rect.fsh` — per-corner rounded rect SDF
- `entity_glow.fsh` — Fresnel glow outline
- `postprocess.fsh` — saturation control
- `render3d_mega.fsh` — SDF primitives, glow, rim, Fresnel

---

## Requirements

- **Java**: 26+
- **Minecraft**: 1.21.11 (Mojang mappings)
- **GPU**: OpenGL 4.5+

## Build

```bash
# MCP branch — direct javac
javac -d build -sourcepath src --release 26 -encoding UTF-8 -cp "libs/*.jar" \
  -processorpath "libs/lombok.jar" \
  -J-Xmx4g $(find src -name "*.java")
```

**Output:** `sborka/Aporia.jar` (obfuscated), `sborka/Aporia.zip`

## Configuration

- **Config file:** `~/.apr/config.apr` (auto-save every 30s)
- **Themes:** `~/.apr/themes/*.apr` (10+ built-in, save/load)
- **Locale:** `~/.apr/.assets/aporia/locale/{en_EU,ru_RU,ch_CH}.json`

## Localization

- `en_EU` — English
- `ru_RU` — Russian
- `ch_CH` — Chinese (中文)

## Themes

DefaultAporia, Amethyst, Synthwave, Matrix, Ocean, Blood, Midnight, Forest, Sunrise, Cyberpunk

## Security

- ChaosObfuscator (`@Obfuscate`, `@ChaosNative`)
- Hidden config directory (`attrib +s +h` on Windows)
- Panic system
- No personal data collection

## License

Aporia.cc Software License Agreement v1.0 — proprietary software.

## Links

- **MCP**: https://gitlab.com/protect3ed/aporia-client
- **Fabric**: https://gitlab.com/protect3ed/aporia-client/-/tree/fabric?ref_type=heads
- **Version**: 1.0-dev
- **Status**: Active development
