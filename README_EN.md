# Aporia Client

![Aporia Screenshot](https://raw.githubusercontent.com/aporia-cc/Aporia/main/.github/scren2ru.png)

**Aporia** — a modern cheat client for Minecraft 1.21.11+ with modular architecture, GPU-accelerated rendering, and Discord integration.

## Features

- **Modular system** — extensible framework with lifecycle management
- **GPU rendering** — SDF shaders, blur, rounded shapes, chromatic aberration
- **ClickGui** — modern UI with search, 3D model preview, scrollable settings
- **Aura** — server-side rotation, free camera, multi-targets, 1.8/1.9+ modes
- **Discord RPC** — avatar, status, profile in UI
- **Multi-language** — EN, RU, CN

## Requirements

- **Java**: 26+
- **Minecraft**: 1.21.11+
- **Build**: `javac` directly, no Gradle

## Build

```bash
git clone <repo>
cd Aporia
javac -d build -sourcepath src --release 26 -encoding UTF-8 @find src -name "*.java"
```

## Structure

```
src/so/aporia/
├── module/          # Modules (Aura, AutoSprint, ESP...)
├── utils/
│   ├── events/      # Event bus
│   ├── packets/     # Packet interceptor
│   ├── user/
│   │   ├── rotation/  # Server rotation
│   │   ├── render/    # GPU renderer
│   │   └── locale/    # i18n (EN/RU/CN)
│   └── files/       # Config
└── aporia/cc/       # OS manager, auth
```

## License

Aporia.cc Software License Agreement v1.0 — proprietary software.

---

**Version**: 0.5-dev  
**Minecraft**: 1.21.11  
**Java**: 26+  
**Status**: Active development

[Русская версия](README.md) · [中文版](README_CN.md)
