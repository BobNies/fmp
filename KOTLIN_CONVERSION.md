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

3. **Exceptions Module (4 files)**
   - `BaseError.kt` (from `mpf/exceptions/base_error.py`)
   - `ConfigFileError.kt` (from `mpf/exceptions/config_file_error.py`)
   - `MpfRuntimeError.kt` (from `mpf/exceptions/runtime_error.py`)
   - `DriverLimitsError.kt` (from `mpf/exceptions/driver_limits_error.py`)

4. **Core Framework (19 files, ~4,020 lines)**
   - `CaseInsensitiveMap.kt` (from `mpf/core/case_insensitive_dict.py`)
   - `Clock.kt` (from `mpf/core/clock.py`)
   - `DelayManager.kt` (from `mpf/core/delays.py`)
   - `LogMixin.kt` (from `mpf/core/logging.py`) - Logging with delegation pattern
   - `MpfController.kt` (from `mpf/core/mpf_controller.py`) - Base controller class
   - `EventManager.kt` (from `mpf/core/events.py`) - Complete event system ~350 lines
   - `UtilityFunctions.kt` (from `mpf/core/utility_functions.py`) - Common utilities
   - `RGBColor.kt` (from `mpf/core/rgb_color.py`) - RGB color with 140+ named colors
   - `Device.kt` (from `mpf/core/device.py`) - Base device class ~200 lines
   - `DeviceCollection.kt` (from `mpf/core/device_manager.py`) - Device collection ~140 lines
   - `DeviceManager.kt` (from `mpf/core/device_manager.py`) - Device manager ~260 lines
   - `Player.kt` (from `mpf/core/player.py`) - Player management ~240 lines
   - `Mode.kt` (from `mpf/core/mode.py`) - Mode system base class ~400 lines
   - `SystemWideDevice.kt` (from `mpf/core/system_wide_device.py`) - System-wide device base ~22 lines
   - `ServiceController.kt` (from `mpf/core/service_controller.py`) - Service mode controller ~210 lines
   - `Randomizer.kt` (from `mpf/core/randomizer.py`) - Weighted random selection ~283 lines
   - `FileManager.kt` (from `mpf/core/file_manager.py`) - File loading/saving ~260 lines
   - `DataManager.kt` (from `mpf/core/data_manager.py`) - Key-value data persistence ~212 lines
   - `SettingsController.kt` (from `mpf/core/settings_controller.py`) - Operator settings ~158 lines
   - `LightController.kt` (from `mpf/core/light_controller.py`) - Light updates and monitoring ~207 lines
   - `CustomCode.kt` (from `mpf/core/custom_code.py`) - User custom code base class ~38 lines
   - `MpfPlugin.kt` (from `mpf/core/plugin.py`) - Plugin module base class ~59 lines
   - `RGBAColor.kt` (from `mpf/core/rgba_color.py`) - RGB color with alpha channel ~108 lines
   - `AsyncMode.kt` (from `mpf/core/async_mode.py`) - Coroutine-based mode base class ~99 lines
   - `ModeDevice.kt` (from `mpf/core/mode_device.py`) - Mode-specific device base class ~110 lines

5. **Platform Interfaces (1 file, ~246 lines)**
   - `PlatformInterfaces.kt` - Platform abstraction layer
     - BasePlatform abstract class
     - DriverPlatform abstract class
     - DriverPlatformInterface abstract class
     - DriverConfig, PulseSettings, HoldSettings data classes
     - RepulseSettings data class

6. **Config System Stubs (1 file, ~150 lines)**
   - `ConfigStubs.kt` - Minimal config infrastructure for integration
     - ConfigValidator stub
     - MpfConfig stub
     - YamlMultifileConfigLoader stub

7. **Device Implementations (30 files, ~9,011 lines)**
   - `Switch.kt` (from `mpf/devices/switch.py`) - Switch device with state tracking ~280 lines
   - `SwitchController.kt` (from `mpf/core/switch_controller.py`) - Switch controller ~250 lines
   - `Driver.kt` (from `mpf/devices/driver.py`) - Driver (coil) device ~485 lines
   - `DevicePositionMixin.kt` (from `mpf/devices/device_mixins.py`) - Position mixin ~42 lines
   - `DigitalOutput.kt` (from `mpf/devices/digital_output.py`) - Digital output (driver/light) ~238 lines
   - `ShotProfile.kt` (from `mpf/devices/shot_profile.py`) - Shot profile configuration ~36 lines
   - `PowerSupplyUnit.kt` (from `mpf/devices/power_supply_unit.py`) - PSU power management ~76 lines
   - `Dmd.kt` (from `mpf/devices/dmd.py`) - Monochrome DMD display ~88 lines
   - `RgbDmd.kt` (from `mpf/devices/rgb_dmd.py`) - RGB DMD display with brightness ~105 lines
   - `Servo.kt` (from `mpf/devices/servo.py`) - Servo motor control with ball search ~195 lines
   - `Accelerometer.kt` (from `mpf/devices/accelerometer.py`) - Multi-axis accelerometer with tilt/level detection ~244 lines
   - `TimedSwitch.kt` (from `mpf/devices/timed_switch.py`) - Monitors switches active for specified duration ~182 lines
   - `Blinkenlight.kt` (from `mpf/devices/blinkenlight.py`) - Light that cycles through multiple colors ~207 lines
   - `HardwareSoundSystem.kt` (from `mpf/devices/hardware_sound_system.py`) - Hardware sound for EM/SS machines ~133 lines
   - `DualWoundCoil.kt` (from `mpf/devices/dual_wound_coil.py`) - Dual-wound coil with main and hold windings ~107 lines
   - `Magnet.kt` (from `mpf/devices/magnet.py`) - Playfield magnet with grab/release/fling ~326 lines
   - `AutofireCoil.kt` (from `mpf/devices/autofire.py`) - Hardware rule-based autofire (bumpers, slingshots) ~287 lines
   - `Motor.kt` (from `mpf/devices/motor.py`) - Positional motor with bi-directional control ~368 lines
   - `Kickback.kt` (from `mpf/devices/kickback.py`) - Kickback extends AutofireCoil for outlane protection ~39 lines
   - `DropTarget.kt` (from `mpf/devices/drop_target.py`) - Drop targets and banks with reset/knockdown ~732 lines
   - `Diverter.kt` (from `mpf/devices/diverter.py`) - Ball routing diverter with automatic enable/disable ~480 lines
   - `LightGroup.kt` (from `mpf/devices/light_group.py`) - Abstract light groups (strips, rings, NeoSeg displays) ~262 lines
   - `PlayfieldTransfer.kt` (from `mpf/devices/playfield_transfer.py`) - Transfer balls between playfields ~150 lines
   - `ScoreQueue.kt` (from `mpf/devices/score_queue.py`) - Sequential scoring with chimes for SS games ~118 lines
   - `Flipper.kt` (from `mpf/devices/flipper.py`) - Flipper with hardware rules and EOS support ~355 lines
   - `SequenceShot.kt` (from `mpf/devices/sequence_shot.py`) - Multi-step sequence detection with timeout ~295 lines
   - `ShowQueue.kt` (from `mpf/devices/show_queue.py`) - Sequential show playback with queuing ~92 lines
   - `Speedometer.kt` (from `mpf/devices/speedometer.py`) - Ball speed measurement between switches ~90 lines
   - `Spinner.kt` (from `mpf/devices/spinner.py`) - Spinner with active/idle states and event buffering ~298 lines
   - `Stepper.kt` (from `mpf/devices/stepper.py`) - Stepper motor with homing and named positions ~412 lines

8. **Command Line Interface**
   - `Main.kt` - Main entry point
   - `CommandLineUtility.kt` - CLI command dispatcher (from `mpf/commands/__init__.py`)

## Remaining Components to Convert

### Core Framework (High Priority)
- `machine.py` - Main machine controller (~800 lines) - **TODO**
- `mode_controller.py` - Mode controller (~400 lines) - **TODO**
- `platform.py` - Platform base classes (~600 lines) - **TODO**
- `config_loader.py` - Configuration loading - **TODO**
- `config_validator.py` - Configuration validation - **TODO**
- `placeholder_manager.py` - Template/placeholder system - **TODO**
- `ball_controller.py` - Ball tracking (~400 lines) - **TODO**

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
**Total Kotlin Files**: 64 (~13,444 lines of code)
**Python Files Converted**: ~65 of 515 (~13%)
**Key Systems Complete**: ✓ Event System, ✓ Logging, ✓ Utilities, ✓ Colors, ✓ Devices, ✓ Player, ✓ Mode, ✓ Config Stubs, ✓ Switch Infrastructure, ✓ Platform Interfaces, ✓ Driver Device, ✓ Service Controller, ✓ File Management, ✓ Data Persistence, ✓ Randomizer, ✓ Settings, ✓ Light Controller, ✓ Custom Code, ✓ Plugins, ✓ Async Modes, ✓ Mode Devices, ✓ Digital Output, ✓ PSU Management, ✓ DMD Displays, ✓ Servo Control, ✓ Accelerometer, ✓ Timed Switches, ✓ Blinkenlight, ✓ Hardware Sound, ✓ Dual Wound Coils, ✓ Magnets, ✓ Autofire Coils, ✓ Motors, ✓ Kickbacks, ✓ Drop Targets, ✓ Diverters, ✓ Light Groups, ✓ Playfield Transfers, ✓ Score Queues, ✓ Flippers, ✓ Sequence Shots, ✓ Show Queues, ✓ Speedometers, ✓ Spinners, ✓ Steppers
**Based On**: Branch 0.80.x
