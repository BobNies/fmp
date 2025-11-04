# Mission Pinball Framework - Kotlin Version

This is a Kotlin conversion of the Mission Pinball Framework (MPF), an open-source framework for controlling real pinball machines.

## Quick Start

### Prerequisites
- JDK 17 or higher
- Gradle 8.x (or use the included wrapper)

### Build

```bash
./gradlew build
```

### Run

```bash
# Show version
./gradlew run --args="--version"

# Run game mode (when implemented)
./gradlew run --args="game /path/to/machine"

# Hardware test mode (when implemented)
./gradlew run --args="hardware /path/to/machine"
```

### Test

```bash
./gradlew test
```

## Conversion Status

This is an **in-progress conversion** from Python to Kotlin. See [KOTLIN_CONVERSION.md](KOTLIN_CONVERSION.md) for detailed conversion documentation.

### What's Working

- ✅ Build system (Gradle)
- ✅ Version information
- ✅ Exception classes
- ✅ Core utilities (Clock, DelayManager, CaseInsensitiveMap)
- ✅ Command-line interface framework

### What's In Progress

- 🚧 Machine controller
- 🚧 Event system
- 🚧 Device management
- 🚧 Platform interfaces
- 🚧 Configuration system

See [KOTLIN_CONVERSION.md](KOTLIN_CONVERSION.md) for the complete conversion roadmap.

## Project Structure

```
src/
├── main/
│   ├── kotlin/
│   │   └── org/missionpinball/mpf/
│   │       ├── Main.kt                 # Entry point
│   │       ├── Version.kt              # Version info
│   │       ├── commands/               # CLI commands
│   │       ├── core/                   # Core framework
│   │       ├── exceptions/             # Exception classes
│   │       ├── devices/                # Device implementations (TBD)
│   │       ├── platforms/              # Hardware platforms (TBD)
│   │       └── modes/                  # Game modes (TBD)
│   └── resources/                      # Config files, assets
└── test/
    └── kotlin/                         # Unit tests (TBD)
```

## Architecture

### Python to Kotlin Mappings

- **asyncio** → Kotlin Coroutines
- **ruamel.yaml** → Kaml
- **pyserial** → jSerialComm
- **argparse** → Clikt
- **logging** → kotlin-logging + Logback

## Original Project

This Kotlin version is a conversion of the Python Mission Pinball Framework:
- Original Repository: https://github.com/missionpinball/mpf
- Documentation: https://missionpinball.org
- License: MIT

## Contributing

To contribute to this Kotlin conversion:

1. See [KOTLIN_CONVERSION.md](KOTLIN_CONVERSION.md) for the conversion roadmap
2. Pick an unconverted module
3. Follow the established conversion patterns
4. Add tests
5. Submit a pull request

## License

MIT License - Same as the original Mission Pinball Framework

## Support

For questions about the original Python MPF:
- https://missionpinball.org/community/

For questions about this Kotlin conversion:
- Open an issue on this repository
