# Mission Pinball Framework - Python to Kotlin Conversion

## Overview

This document describes the conversion of the Mission Pinball Framework (MPF) from Python to Kotlin.

## Project Scope

The original Python project contains **515 Python files** across the following major components:
- Core framework (~50 files)
- Device implementations
- Platform interfaces (hardware communication)
- Configuration system
- Mode system
- Event system
- Tests

## Conversion Strategy

### Language Mapping

| Python Concept | Kotlin Equivalent |
|----------------|-------------------|
| `asyncio` | Kotlin Coroutines (`kotlinx.coroutines`) |
| `ruamel.yaml` | Kaml (Kotlin YAML library) |
| `pyserial` | jSerialComm |
| `argparse` | Clikt (Kotlin command-line parser) |
| `dict` | `Map` / `MutableMap` |
| Case-insensitive dict | Custom `CaseInsensitiveMap` class |
| `@property` | Kotlin properties with getters |
| `__slots__` | Regular class properties (memory optimization less critical on JVM) |
| Type hints | Kotlin's native type system |

### Architectural Changes

1. **Async/Await**: Python's `asyncio` event loop replaced with Kotlin Coroutines
2. **Package Structure**: `mpf.` → `org.missionpinball.mpf.`
3. **Build System**: `setuptools` → Gradle (Kotlin DSL)
4. **Logging**: Python logging → kotlin-logging with SLF4J/Logback
5. **Entry Point**: Python's `__main__.py` → Kotlin's `Main.kt`

## Converted Components

### ✅ Completed

1. **Build Configuration**
   - `build.gradle.kts` - Gradle build with all dependencies
   - `settings.gradle.kts` - Project settings

2. **Version Information**
   - `src/main/kotlin/org/missionpinball/mpf/Version.kt` (from `mpf/_version.py`)

3. **Exceptions Module**
   - `BaseError.kt` (from `mpf/exceptions/base_error.py`)
   - `ConfigFileError.kt` (from `mpf/exceptions/config_file_error.py`)
   - `MpfRuntimeError.kt` (from `mpf/exceptions/runtime_error.py`)
   - `DriverLimitsError.kt` (from `mpf/exceptions/driver_limits_error.py`)

4. **Core Utilities**
   - `CaseInsensitiveMap.kt` (from `mpf/core/case_insensitive_dict.py`)
   - `Clock.kt` (from `mpf/core/clock.py`)
   - `DelayManager.kt` (from `mpf/core/delays.py`)

5. **Command Line Interface**
   - `Main.kt` - Main entry point
   - `CommandLineUtility.kt` - CLI command dispatcher (from `mpf/commands/__init__.py`)

## Remaining Components to Convert

### Core Framework (High Priority)
- `machine.py` - Main machine controller (~800 lines)
- `events.py` - Event system (~800 lines)
- `mode.py` - Mode system (~600 lines)
- `platform.py` - Platform base classes (~600 lines)
- `switch_controller.py` - Switch handling (~700 lines)
- `device.py` - Base device class
- `device_manager.py` - Device management
- `config_loader.py` - Configuration loading
- `config_validator.py` - Configuration validation
- `placeholder_manager.py` - Template/placeholder system

### Devices (~30 files)
- Switches, Lights, Coils, Flippers, etc.
- Each device type needs conversion

### Platforms (~20 files)
- Hardware interface implementations
- FAST, P-ROC, OPP, Virtual, etc.

### Configuration System
- YAML parsing and validation
- Config spec processing
- Config players

### Mode System (~13 files)
- Game modes
- Mode controllers
- Mode devices

### Additional Components
- Command implementations (game, hardware, service, etc.)
- Tests (~150+ test files)
- Plugins
- File interfaces

## Build and Run

### Prerequisites
- JDK 17 or higher
- Gradle 8.x (wrapper included)

### Building

```bash
./gradlew build
```

### Running

```bash
./gradlew run --args="--version"
./gradlew run --args="game /path/to/machine"
```

### Testing

```bash
./gradlew test
```

## Key Conversion Patterns

### Pattern 1: Async Functions

**Python:**
```python
async def some_function():
    await some_async_call()
```

**Kotlin:**
```kotlin
suspend fun someFunction() {
    someAsyncCall()
}
```

### Pattern 2: Properties

**Python:**
```python
@property
def value(self):
    return self._value
```

**Kotlin:**
```kotlin
val value: String
    get() = _value
```

### Pattern 3: Event Loop Scheduling

**Python:**
```python
self.machine.clock.schedule_once(callback, 1.0)
```

**Kotlin:**
```kotlin
clock.scheduleOnce(callback, 1.0)
```

### Pattern 4: Error Handling

**Python:**
```python
raise ConfigFileError("Error message", 1, "logger_name")
```

**Kotlin:**
```kotlin
throw ConfigFileError("Error message", 1, "logger_name")
```

## Next Steps

1. **Convert Core Machine Controller** - The main machine class is central to the framework
2. **Convert Event System** - Required by almost all other components
3. **Convert Device Base Classes** - Foundation for all hardware devices
4. **Convert Platform Base Classes** - Hardware communication layer
5. **Add Comprehensive Tests** - Port unit tests from Python
6. **Convert Specific Devices** - Switches, lights, coils, etc.
7. **Convert Platform Implementations** - Hardware-specific code
8. **Convert Mode System** - Game mode framework
9. **Documentation** - Update all docs for Kotlin API

## Dependencies

All Python dependencies have Kotlin/JVM equivalents specified in `build.gradle.kts`:

- **Coroutines**: For async operations
- **Kaml**: YAML parsing (replaces ruamel.yaml)
- **jSerialComm**: Serial communication (replaces pyserial)
- **Clikt**: Command-line parsing (replaces argparse)
- **kotlin-logging**: Logging facade
- **OSHI**: System monitoring (replaces psutil)

## Configuration Files

Python MPF configuration files (YAML) remain compatible. The Kaml library handles YAML parsing in Kotlin.

## Testing Strategy

1. Port existing Python unit tests to Kotlin using JUnit 5
2. Use MockK for mocking (equivalent to Python's unittest.mock)
3. Use kotlinx-coroutines-test for testing coroutines
4. Maintain test coverage similar to Python version

## Performance Considerations

- **JVM Startup**: Slower cold start compared to Python, but faster steady-state performance
- **Memory**: JVM has higher base memory footprint
- **Concurrency**: Kotlin coroutines are lightweight and efficient
- **Hardware I/O**: jSerialComm performance is comparable to pyserial

## Compatibility Notes

- Config files remain YAML-based and compatible with Python version
- Hardware interfaces need platform-specific testing
- Timing-critical operations may need tuning
- Serial communication timing should be validated against hardware

## Contributing

To contribute to the conversion:

1. Pick a module from the "Remaining Components" section
2. Follow the conversion patterns documented above
3. Add corresponding unit tests
4. Update this document with your progress
5. Submit a pull request

## Resources

- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Original MPF Python Documentation](https://missionpinball.org)
- [Gradle User Manual](https://docs.gradle.org/)

## License

This Kotlin conversion maintains the original MIT license of the Mission Pinball Framework.

---

**Conversion Status**: In Progress
**Last Updated**: 2025-11-04
**Python Files Remaining**: ~510 of 515
