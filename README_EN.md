# Aporia

A modern Minecraft cheat client built with Fabric, featuring a modular architecture, custom GPU-accelerated rendering, and Discord integration.

## Overview

Aporia is designed as a lightweight, extensible cheat client for Minecraft 1.21.1+. It provides a clean module system, powerful rendering capabilities, and seamless Discord Rich Presence integration. The client uses a modular approach where features are implemented as toggleable modules with configurable settings.

## Requirements

- **Java**: 26 or higher
- **Minecraft**: 1.21.11
- **Loader**: net.minecraft.client.main.Main, mcp.client.Start
- **Build Tool**: СОСАТЬ НАХУЙ БЕЗ ГРАДЛИ

## Features

### Core Architecture
- **Modular System** - Extensible module framework with lifecycle management
- **Category Organization** - Modules grouped by type (Combat, Render, Misc)
- **Settings Framework** - Flexible configuration (toggles, dropdowns, text, keybinds)
- **Event System** - Decoupled event-driven communication

### Rendering
- **GPU-Accelerated** - Blaze3D-based rendering pipeline
- **Rounded Shapes** - SDF-based rounded rectangles and circles
- **Blur Effects** - Configurable blur with saturation control
- **Image Support** - PNG rendering with optional corner radius
- **Custom Shaders** - Extensible shader system for advanced effects

### User Interface
- **ClickGui** - Modern sidebar-based interface
- **Module Search** - Quick search functionality
- **Discord Profile** - Shows username, UUID, and avatar
- **Live Activity** - Real-time module status display

### Discord Integration
- **Rich Presence** - Display activity on Discord
- **Avatar Display** - User avatar shown in GUI
- **Status Updates** - Real-time server/menu status

## Getting Started

### Build & Run

```bash
# Clone and setup
git clone <repo>
cd aporia
./gradlew build

# Run the client
./gradlew runClient
```

### Create a Module

```java
public class MyModule extends Module {
    private BooleanSetting enabled = new BooleanSetting("Enabled", true);
    
    public MyModule() {
        super("My Module", Category.MISC);
    }
    
    @Override
    protected void onEnable() {
        // Module logic
    }
    
    @Override
    protected void onDisable() {
        // Cleanup
    }
}
```

## Project Structure

```
src/so/aporia/
├── Aporia.java                 # Entry point
├── module/
│   ├── Module.java             # Base module class
│   ├── ModuleManager.java      # Module registry
│   ├── Category.java           # Module categories
│   ├── impl/                   # Module implementations
│   │   ├── combat/
│   │   ├── render/
│   │   └── misc/
│   └── settings/               # Settings framework
└── utils/
    ├── assets/                 # Asset management
    ├── events/                 # Event system
    ├── files/                  # File I/O
    └── user/
        ├── render/             # Rendering engine
        ├── logger/             # Logging
        ├── input/              # Input handling
        └── command/            # Command system
```

## Configuration

Aporia stores configuration in platform-specific directories:

- **Windows**: `%USERPROFILE%\.apr`
- **Linux**: `~/.config/apr`
- **macOS**: `~/.config/apr`

Configuration files use the `.apr` format (ZIP-based with metadata).

## Dependencies

- **Minecraft**: 1.21.11
- **Discord IPC**: For Rich Presence integration
- **Gson**: JSON serialization
- **Blaze3D**: Rendering backend

## Development

### Adding a New Module

1. Create class in `src/so/aporia/module/impl/<category>/`
2. Extend `Module` base class
3. Implement `onEnable()` and `onDisable()`
4. Add settings as needed
5. Module auto-registers via ModuleManager

### Custom Rendering

Use `AporiaRenderer.INSTANCE` for rendering:

```java
AporiaRenderer r = AporiaRenderer.INSTANCE;

// Draw shapes
r.drawRect(x, y, w, h, radius, color);
r.drawCircle(cx, cy, radius, color);
r.drawLine(x1, y1, x2, y2, thickness, color);

// Draw text
r.drawText("bold", "Hello", x, y, size, color);

// Draw images
r.drawImage(x, y, w, h, identifier, radius);
```

### Event System

Subscribe to events using `@EventHandler`:

```java
@EventHandler
public void onRender(RenderEvent event) {
    // Handle render event
}
```

## Performance

- GPU-accelerated rendering for smooth 60+ FPS
- Efficient module lifecycle management
- Cached asset loading
- Optimized event dispatching

## Troubleshooting

### Client won't start
- Ensure Java 21+ is installed
- Check Minecraft version is 1.21.1+
- Verify Fabric loader is installed

### Modules not loading
- Check module class extends `Module`
- Verify `onEnable()` and `onDisable()` are implemented
- Check logs in `~/.apr/logs/`

### Rendering issues
- Update GPU drivers
- Check shader files in `aporia/shaders/`
- Verify Blaze3D compatibility

## License

Proprietary - Aporia Cheat Client

## Support

For issues and questions, check the project documentation or create an issue.

---

**Version**: 0.5-dev
**Minecraft**: 1.21.11
**Java**: 26+  
**Status**: Active Development
