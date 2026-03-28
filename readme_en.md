<div align="center">

![Aporia](src/main/resources/assets/Aporia.png)

# ✨ Aporia Client

**Advanced Minecraft Client for version 1.21.11**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-red?style=for-the-badge&logo=minecraft)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.18.4-yellow?style=for-the-badge&logo=fabric)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-26-green?style=for-the-badge&logo=openjdk)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/Version-0.4.1-blue?style=for-the-badge)](https://github.com/aporia/client/releases)

</div>

> 🕐 Last build: **28.03.2026 21:37:03 UTC** — v0.4.1


---

## 🚀 Quick Start

### Installation

1. Install [Fabric Loader 0.18.4+](https://fabricmc.net/use/installer/)
2. Download the latest version from [Releases](https://github.com/aporia/client/releases) or [beta/latest/](beta/latest/)
3. Place the `.jar` file in the `mods` folder
4. Launch Minecraft with the Fabric profile

### Building from Source

```bash
git clone https://github.com/aporia/client.git
cd client

# Build
./gradlew build

# Run client
./gradlew runClient

# Obfuscated build
./gradlew obfuscateRemappedJar
```

**Requirements:**
- JDK 26
- Gradle 8.x
- Git

---

## 📦 Features

### 🔥 95+ Modules

| Category | Count | Examples |
|----------|-------|----------|
| ⚔️ **Combat** | 19 | Aura, TpAura, AutoCrystal, Criticals, Velocity |
| 🏃 **Movement** | 15 | Fly, Speed, Jesus, Strafe, ElytraMotion |
| 👤 **Player** | 12 | AutoTool, ChestStealer, FreeCam, NoFall |
| 🎨 **Render** | 21 | ESP, BlockESP, FullBright, ChinaHat, Particles |
| 🔧 **Misc** | 13 | AutoBuy, DiscordRPC, ServerHelper, WindJump |

### 🎯 Key Features

<details>
<summary><b>⚔️ Combat System</b></summary>

- **Aura** — Advanced aiming system with multiple rotation modes
- **TpAura** — Teleportation aura with anti-cheat bypasses
- **AutoCrystal** — Automatic crystal placement and detonation
- **MaceTarget** — Auto-attack with mace and movement prediction
- **Velocity** — Full knockback protection

</details>

<details>
<summary><b>🏃 Movement</b></summary>

- **Fly** — Multiple flight modes
- **Speed** — Increased movement speed
- **Jesus** — Walking on water and lava
- **ElytraMotion** — Improved elytra control
- **Spider** — Wall climbing

</details>

<details>
<summary><b>🎨 Visuals</b></summary>

- **ESP** — Highlighting players, chests, resources
- **BlockESP** — Specific block highlighting
- **ChinaHat** — Cosmetic Chinese hat
- **Particles** — Beautiful particles around the player
- **FullBright** — Full brightness without gamma

</details>

<details>
<summary><b>🔧 Utilities</b></summary>

- **AutoBuy** — Automatic item purchasing at auction
- **DiscordRPC** — Discord status
- **Macro System** — Macro system
- **Waypoints** — Waypoints
- **Proxy Support** — SOCKS4/5 and HTTP proxy support

</details>

---

## 🎨 ClickGUI

Intuitive interface for managing modules:

- 📁 **Categories** — Convenient grouping by sections
- ⚙️ **Settings** — Sliders, colors, key bindings
- 🎯 **Drag & Drop** — Window dragging
- 💾 **Profiles** — Save and load configurations
- 🔍 **Search** — Quick module search

**Open:** `Right Shift` (configurable)

---

## 💬 Commands

| Command | Description |
|---------|-------------|
| `.help` | Show all commands |
| `.toggle <module>` | Enable/disable module |
| `.bind <module> <key>` | Bind module to a key |
| `.friend add/remove <nick>` | Manage friends list |
| `.macro create/delete <name>` | Manage macros |
| `.way add/remove <name>` | Manage waypoints |
| `.prefix <symbol>` | Change command prefix |
| `.saveconfig` | Save current configuration |
| `.reload` | Reload configuration |
| `.staff` | Show server staff |

---

## 🛡️ Unicode Chaos Obfuscation

Aporia uses an advanced code obfuscation system:

### Protection Levels

| Level | Characters | Example |
|-------|------------|---------|
| 🟢 LIGHT | ASCII | `C/x/eU` |
| 🟡 MEDIUM | Greek + Cyrillic | `Т/θ/а_У` |
| 🟠 HEAVY | Asian languages | `ड/し/ぬ大 f` |
| 🔴 EXTREME | All languages + emojis | `›/ჩ/ཽ/Л可ძĐ"🔥` |

### Features

- 🔄 **Daily rotation** of mappings at 00:00 UTC
- 🌍 **30+ languages** supported (Chinese, Japanese, runes, emojis...)
- 🔐 **RuntimeMapper** for log deobfuscation
- 🚫 **Mixins are not obfuscated** for stability

More details: [UNICODE_CHAOS_GUIDE.md](src/main/java/anidumpproject/api/README.md)

---

## 📊 HUD Elements

Customizable interface elements:

- 🛡️ **ArmorHud** — Armor display
- 📍 **Coordinates** — XYZ coordinates
- 📊 **FPS Counter** — Frame rate counter
- 📋 **ModuleList** — List of active modules
- 📶 **Ping** — Connection latency
- ⚗️ **Potions** — Active effects
- 🎯 **TargetHud** — Target information
- ©️ **Watermark** — Client logo

All elements can be dragged and customized!

---

## 🔧 Technologies

- **Fabric API** — Next-generation mod loader
- **Mixin** — Minecraft code injection system
- **LWJGL3** — Low-level graphics
- **JOML** — Math library
- **Netty** — Networking library
- **Gson** — JSON parsing
- **Lombok** — Code simplification

---

## 📁 Project Structure

```
Aporia/
├── src/main/
│   ├── java/aporia/su/
│   │   ├── Initialization.java      # Entry point
│   │   ├── mixin/                   # Mixin injections (50+)
│   │   ├── modules/                 # Modules (95+)
│   │   │   ├── combat/              # Combat
│   │   │   ├── movement/            # Movement
│   │   │   ├── player/              # Player
│   │   │   ├── render/              # Render
│   │   │   └── misc/                # Misc
│   │   └── util/                    # Utilities
│   │       ├── events/              # Event system
│   │       ├── files/               # Configs
│   │       ├── render/              # Rendering
│   │       ├── chat/                # Chat and commands
│   │       └── network/             # Network
│   │
│   └── resources/
│       ├── assets/Aporia/           # Textures, fonts
│       ├── fabric.mod.json          # Metadata
│       ├── mixins.json              # Mixin config
│       └── accesswidener            # Access Widener
│
├── buildSrc/src/main/kotlin/        # Build scripts
│   ├── ChaosObfuscator.kt           # Unicode obfuscation
│   ├── StringEncryptor.kt           # String encryption
│   └── ...
│
└── beta/                            # Beta builds
    ├── 0.4.1/
    └── latest/
```

Full documentation: [`context.md`](context.md)

---

## 🤝 Contributing

We welcome contributions to Aporia!

### How to Help

1. **Fork** the project
2. Create a **feature branch** (`git checkout -b feature/AmazingFeature`)
3. Make your **changes**
4. **Commit** (`git commit -m 'Add AmazingFeature'`)
5. Push to **remote** (`git push origin feature/AmazingFeature`)
6. Open a **Pull Request**

### Code Requirements

- ✅ Follow the project's code style
- ✅ Add tests for new features
- ✅ Document complex sections
- ✅ Verify the build before submitting

---

## 📝 License

This project is distributed under the **MIT** license.
Details in the [`LICENSE`](LICENSE) file.

---

## ⚠️ Disclaimer

> This project is created for educational purposes.
> Using it on public servers may violate their rules.
> The authors are not responsible for possible consequences.

---

## 💎 Authors

Created with ❤️ by the **Aporia** team

- **protect3ed** — Lead Developer
- Aporia Team — Contributors

---

## 📞 Contacts

- 🌐 **Website:** [aporia.su](https://aporia.su)
- 💬 **Discord:** [Join](https://discord.gg/aporia)
- 📧 **Email:** support@aporia.su
- 🐙 **GitHub:** [@aporia](https://github.com/aporia)

---

<div align="center">

### ⭐ If you like this project, give it a star! ⭐

**Aporia Client** — *Your path to perfection in Minecraft*

[![Downloads](https://img.shields.io/github/downloads/aporia/client/total?style=for-the-badge&color=green)](https://github.com/aporia/client/releases)
[![Stars](https://img.shields.io/github/stars/aporia/client?style=for-the-badge&color=yellow)](https://github.com/aporia/client/stargazers)
[![Issues](https://img.shields.io/github/issues/aporia/client?style=for-the-badge&color=red)](https://github.com/aporia/client/issues)

---

*Version: 0.4.1 | Minecraft: 1.21.11 | Updated: March 2026*

</div>
