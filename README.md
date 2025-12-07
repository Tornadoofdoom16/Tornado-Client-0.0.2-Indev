# Tornado Client v1.0.0

A modular Minecraft Fabric 1.21 client with advanced PvP and utility features.

## Build Requirements

- Java 21+
- Gradle (wrapper included)

## Building

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/tornadoclient-1.0.0.jar`

## Project Structure

```
src/main/java/net/example/infiniteaura/
├── TornadoClient.java              # Main entry point
├── InfiniteAuraSettings.java        # Global settings singleton
├── InfiniteAuraScreen.java          # GUI screen
├── client/
│   ├── Module.java                  # Abstract module base
│   └── ModuleManager.java           # Module manager & registry
├── modules/                         # All 20 modules
└── mixin/                           # Mixin injections
```

## Features

- **20 Total Modules** across 5 categories
- **Rage Combat**: InfiniteAura, AnchorAura, CrystalAura, Trapper, CartPVP, Nuker
- **Legit Combat**: LegitCrystal, LegitKillAura, ElytraMace, PVPBot
- **Defense**: AutoTotem, AntiMace, LowTotemAlert
- **Movement**: Flight, Speed, NoFall, AirJump, Jesus
- **Utility**: PearlBot, BaritoneBot, Target/Friends List

## Keybindings

- **R**: Toggle mod on/off
- **U**: Open GUI
- **M**: Toggle Mace mode
- **K**: Toggle Auto Sword
- **V**: Single strike attack

## Configuration

All settings managed via `InfiniteAuraSettings.java` singleton.

## License

MIT
