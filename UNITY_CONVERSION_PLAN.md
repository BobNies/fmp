# Mission Pinball Framework (MPF) to Unity3D Conversion Plan

## Executive Summary
This document outlines the comprehensive plan for converting the Mission Pinball Framework (MPF) from Python to Unity3D/C#. MPF is a sophisticated event-driven pinball machine framework with hardware abstraction, config-driven behavior, and a robust device management system.

## Source Architecture Overview

### Core Components
1. **MachineController** - Central orchestrator managing all systems
2. **EventManager** - Asynchronous event-driven communication system
3. **DeviceManager** - Manages 50+ device types with lifecycle management
4. **ModeController** - State machine for game logic with priority-based mode stacking
5. **Clock System** - Asyncio-based tickless timing system
6. **Platform Abstraction** - Hardware abstraction layer supporting 15+ platforms
7. **Show System** - Timeline-based sequences for lights, coils, displays
8. **BCP Protocol** - Bidirectional communication for media controllers
9. **Config System** - YAML-based configuration with validation
10. **Placeholder System** - Dynamic value substitution in configs

### Technology Stack (Python)
- asyncio for concurrency
- ruamel.yaml for config parsing
- pyserial for hardware communication
- Event-driven architecture
- Dynamic typing

## Target Architecture (Unity3D)

### Unity Component Mapping

| Python Component | Unity Equivalent | Implementation Strategy |
|-----------------|------------------|------------------------|
| MachineController | GameManager (Singleton) | MonoBehaviour singleton managing all systems |
| EventManager | UnityEvent + Custom EventBus | Static event bus with type-safe delegates |
| Clock (asyncio) | Coroutines + Time.deltaTime | Unity's coroutine system for async operations |
| Modes | State Machine / SceneManagement | Hierarchical state machine with priority stacking |
| Devices | Component Architecture | MonoBehaviour components for each device type |
| Show System | Timeline + Animation | Unity Timeline with custom playables |
| Config (YAML) | ScriptableObjects + JSON | ScriptableObjects for design-time, JSON for runtime |
| Placeholder System | Expression Evaluator | C# expression parser with variable binding |
| Platform Abstraction | Interface + Abstract Classes | Hardware interfaces with platform implementations |
| BCP | WebSocket / TCP Networking | Unity networking with serialization |
| Switch Controller | Input System / Hardware Manager | Polling system with debouncing |
| Light Controller | Custom Light Manager | Priority-based light stack with batching |

## Conversion Strategy

### Phase 1: Core Foundation (Priority 1)
**Goal**: Establish the foundational systems that all other components depend on

1. **Project Setup**
   - Create Unity project structure
   - Define assembly definitions
   - Set up folder hierarchy
   - Create namespace structure

2. **Event System** (`mpf/core/events.py` → `Assets/Scripts/Core/EventManager.cs`)
   - Generic event bus with type safety
   - Priority-based handler registration
   - Async/await support using Unity coroutines
   - Event monitoring and debugging

3. **Machine Controller** (`mpf/core/machine.py` → `Assets/Scripts/Core/MachineController.cs`)
   - Singleton game manager
   - Initialization phases
   - Core module management
   - Device collection management
   - Lifecycle management

4. **Clock/Timing System** (`mpf/core/clock.py` → `Assets/Scripts/Core/ClockManager.cs`)
   - Coroutine-based scheduling
   - Periodic callbacks
   - One-shot delays
   - Delta time management

5. **Logging System** (`mpf/core/logging.py` → `Assets/Scripts/Core/LogManager.cs`)
   - Unity Debug.Log integration
   - Log levels and filtering
   - Performance monitoring

### Phase 2: Device Foundation (Priority 1)
**Goal**: Create the device abstraction layer

6. **Device Manager** (`mpf/core/device_manager.py` → `Assets/Scripts/Core/DeviceManager.cs`)
   - Device lifecycle management
   - Device collections (typed dictionaries)
   - Phase-based initialization
   - Device state monitoring

7. **Base Device Classes** (`mpf/core/device.py` → `Assets/Scripts/Devices/Device.cs`)
   - Device abstract base class
   - SystemWideDevice base class
   - ModeDevice base class
   - Device configuration

### Phase 3: Core Devices (Priority 1)
**Goal**: Implement fundamental pinball hardware devices

8. **Switch Device** (`mpf/devices/switch.py` → `Assets/Scripts/Devices/Switch.cs`)
   - State management (active/inactive)
   - Debouncing logic
   - Event posting
   - Timed handlers

9. **Driver/Coil Device** (`mpf/devices/driver.py` → `Assets/Scripts/Devices/Driver.cs`)
   - Pulse control
   - Enable/disable
   - Timed enable
   - PWM support

10. **Light Device** (`mpf/devices/light.py` → `Assets/Scripts/Devices/Light.cs`)
    - RGB/RGBW color support
    - Priority-based stacking
    - Fading/transitions
    - Color correction

### Phase 4: Platform Abstraction (Priority 2)
**Goal**: Hardware abstraction layer

11. **Platform Controller** (`mpf/core/platform_controller.py` → `Assets/Scripts/Platforms/PlatformController.cs`)
    - Platform interface definitions
    - Platform loading and initialization
    - Platform feature validation

12. **Virtual Platform** (`mpf/platforms/virtual.py` → `Assets/Scripts/Platforms/VirtualPlatform.cs`)
    - Software simulation
    - Testing framework
    - Switch state management

13. **Platform Interfaces**
    - IDriverPlatform
    - ISwitchPlatform
    - ILightPlatform
    - IServoPlatform
    - IStepperPlatform

### Phase 5: Game Logic Systems (Priority 2)
**Goal**: Mode system and game flow

14. **Mode System** (`mpf/core/mode.py` → `Assets/Scripts/Modes/Mode.cs`)
    - Mode base class
    - Priority-based mode stacking
    - Mode lifecycle (start/stop)
    - Per-mode event handlers
    - Mode configuration

15. **Mode Controller** (`mpf/core/mode_controller.py` → `Assets/Scripts/Modes/ModeController.cs`)
    - Mode management
    - Mode queue
    - Active mode tracking

16. **Player & Game Management** (`mpf/modes/game/code/game.py`)
    - Player class with variables
    - Game mode
    - Ball tracking
    - Turn management
    - Scoring system

### Phase 6: Ball Management (Priority 2)
**Goal**: Ball tracking and playfield management

17. **Ball Controller** (`mpf/core/ball_controller.py`)
    - Ball inventory tracking
    - Ball search
    - Missing ball detection

18. **Ball Device** (`mpf/devices/ball_device/`)
    - Ball storage
    - Ejection logic
    - Ball counting
    - Mechanical/optical switches

19. **Playfield** (`mpf/devices/playfield.py`)
    - Special ball device
    - Ball tracking on playfield
    - Active ball count

### Phase 7: Advanced Devices (Priority 3)
**Goal**: Complex device types

20. **Shot System** (`mpf/devices/shot.py`)
    - Shot profiles
    - State machine for shots
    - Sequences and groups

21. **Flipper** (`mpf/devices/flipper.py`)
    - Enable/disable
    - Button integration
    - Power settings

22. **Multiball** (`mpf/devices/multiball.py`)
    - Multiball management
    - Ball locking
    - Multiball start/stop

23. **Additional Devices** (50+ devices total)
    - Diverter, Magnet, Motor
    - Drop targets, Pop bumpers
    - Servos, Steppers
    - Accelerometer
    - DMD, RGB DMD, Segment displays
    - Score reels
    - Tilt, Slam tilt
    - Ball save, Ball hold
    - Achievements, Extra balls
    - Logic blocks (Counter, Accrual, Sequence)
    - State machines
    - Combo switches
    - Spinners

### Phase 8: Show System (Priority 2)
**Goal**: Timeline-based sequences

24. **Show Controller** (`mpf/core/show_controller.py`)
    - Show loading
    - Show playback
    - Token substitution
    - Loop control

25. **Show Assets** (`mpf/assets/show.py`)
    - YAML show parser
    - Timeline representation
    - Action execution

26. **Unity Timeline Integration**
    - Custom Timeline tracks
    - Playable assets
    - Show player playables

### Phase 9: Config System (Priority 2)
**Goal**: Configuration management

27. **Config Loader** (`mpf/core/config_loader.py`)
    - YAML/JSON parsing
    - Config merging
    - Template processing

28. **Config Validator** (`mpf/core/config_validator.py`)
    - Schema validation
    - Type checking
    - Required field validation

29. **ScriptableObject System**
    - Device configs
    - Mode configs
    - Machine config
    - Platform config

30. **Placeholder Manager** (`mpf/core/placeholder_manager.py`)
    - Variable substitution
    - Boolean templates
    - Expression evaluation

### Phase 10: Config Players (Priority 3)
**Goal**: Config-driven action systems

31. **Base Config Player** (`mpf/config_players/`)
    - Config player interface
    - Player registration

32. **Specific Players**
    - Event player
    - Light player
    - Coil player
    - Show player
    - Sound player (integration point)
    - Flasher player
    - Variable player
    - Segment display player
    - Widget player (integration point)

### Phase 11: Communication (Priority 3)
**Goal**: External communication protocols

33. **BCP System** (`mpf/core/bcp/`)
    - BCP interface
    - Client/Server
    - Transport layer
    - Message serialization

34. **Unity Networking**
    - WebSocket implementation
    - JSON serialization
    - Device state sync
    - Event propagation

### Phase 12: Built-in Modes (Priority 3)
**Goal**: Standard game modes

35. **Core Modes** (`mpf/modes/`)
    - Attract mode
    - Game mode
    - Credits mode
    - Service mode
    - High score mode
    - Bonus mode
    - Match mode
    - Tilt mode

### Phase 13: Service & Debugging (Priority 4)
**Goal**: Development and maintenance tools

36. **Service Controller** (`mpf/core/service_controller.py`)
    - Service menu
    - Switch testing
    - Coil testing
    - Light testing

37. **Auditor** (`mpf/plugins/auditor.py`)
    - Audit logging
    - Statistics tracking
    - Data persistence

38. **Debug Tools**
    - Inspector integration
    - Event monitoring
    - Device state visualization

### Phase 14: Hardware Platforms (Priority 4)
**Goal**: Real hardware support

39. **Platform Implementations** (`mpf/platforms/`)
    - P-ROC / P3-ROC
    - FAST Pinball
    - Open Pinball Project (OPP)
    - SPIKE
    - LISY
    - Other platforms (15+ total)

### Phase 15: Testing & Documentation (Priority 4)
**Goal**: Quality assurance

40. **Unit Tests**
    - Core system tests
    - Device tests
    - Integration tests

41. **Documentation**
    - API documentation
    - Migration guide
    - Unity-specific notes

## Technical Considerations

### Language Differences: Python vs C#

| Aspect | Python | C# (Unity) | Conversion Strategy |
|--------|--------|-----------|-------------------|
| Type System | Dynamic | Static | Define interfaces and base classes upfront |
| Async | asyncio | Coroutines/Tasks | Use Unity coroutines, async/await for I/O |
| Events | Callback functions | Delegates/UnityEvents | EventBus with type-safe delegates |
| Collections | Lists/Dicts | List<T>/Dictionary<K,V> | Generic collections with type safety |
| Properties | @property | Properties { get; set; } | Auto-properties and computed properties |
| Inheritance | Multiple | Single + Interfaces | Use interfaces for multiple inheritance |
| Config | YAML | JSON/ScriptableObjects | ScriptableObjects for editor, JSON for runtime |
| Modules | Import system | Namespaces/Assemblies | Assembly definitions per subsystem |

### Unity-Specific Patterns

1. **Component Architecture**
   - Devices as MonoBehaviour components
   - GameObject hierarchy for machine structure
   - Prefabs for device templates

2. **ScriptableObjects**
   - Config data storage
   - Shared game data
   - Mode definitions

3. **Coroutines**
   - Replace asyncio patterns
   - Timed delays and sequences
   - Frame-based updates

4. **Unity Events**
   - Integration with UI
   - Cross-component communication
   - Inspector-assignable callbacks

5. **Serialization**
   - UnityEngine.JsonUtility
   - Custom serialization for complex types
   - EditorPrefs for persistent data

### Performance Optimizations

1. **Object Pooling**
   - Ball objects
   - Light effect objects
   - Event objects

2. **Batching**
   - Light updates (similar to Python sortedcontainers)
   - Event dispatch
   - Hardware commands

3. **Caching**
   - Device lookups
   - Config data
   - Platform interfaces

4. **Update Optimization**
   - Fixed update for physics
   - Late update for cameras
   - Custom update loops for high-frequency systems

## Project Structure

```
Assets/
├── Scripts/
│   ├── Core/
│   │   ├── MachineController.cs
│   │   ├── EventManager.cs
│   │   ├── ClockManager.cs
│   │   ├── DeviceManager.cs
│   │   ├── ModeController.cs
│   │   ├── PlatformController.cs
│   │   ├── LightController.cs
│   │   ├── SwitchController.cs
│   │   ├── BallController.cs
│   │   ├── ShowController.cs
│   │   ├── ServiceController.cs
│   │   ├── ConfigLoader.cs
│   │   ├── ConfigValidator.cs
│   │   ├── PlaceholderManager.cs
│   │   ├── LogManager.cs
│   │   ├── DataManager.cs
│   │   └── DelayManager.cs
│   ├── Devices/
│   │   ├── Device.cs (base)
│   │   ├── SystemWideDevice.cs
│   │   ├── ModeDevice.cs
│   │   ├── Switch.cs
│   │   ├── Driver.cs (Coil)
│   │   ├── Light.cs
│   │   ├── Flipper.cs
│   │   ├── BallDevice/
│   │   │   ├── BallDevice.cs
│   │   │   ├── Playfield.cs
│   │   │   └── ...
│   │   ├── Shot.cs
│   │   ├── ShotGroup.cs
│   │   ├── Multiball.cs
│   │   ├── MultiballLock.cs
│   │   ├── Diverter.cs
│   │   ├── DropTarget.cs
│   │   ├── DropTargetBank.cs
│   │   ├── Servo.cs
│   │   ├── Stepper.cs
│   │   ├── Motor.cs
│   │   ├── Magnet.cs
│   │   ├── Accelerometer.cs
│   │   ├── AutofireCoil.cs
│   │   ├── BallHold.cs
│   │   ├── BallSave.cs
│   │   ├── Spinner.cs
│   │   ├── ComboSwitch.cs
│   │   ├── Achievement.cs
│   │   ├── AchievementGroup.cs
│   │   ├── ExtraBall.cs
│   │   ├── ExtraBallGroup.cs
│   │   ├── StateMachine.cs
│   │   ├── ScoreQueue.cs
│   │   ├── LogicBlocks/
│   │   │   ├── Counter.cs
│   │   │   ├── Accrual.cs
│   │   │   └── Sequence.cs
│   │   ├── Displays/
│   │   │   ├── Dmd.cs
│   │   │   ├── RgbDmd.cs
│   │   │   ├── SegmentDisplay.cs
│   │   │   └── ...
│   │   └── ...
│   ├── Modes/
│   │   ├── Mode.cs (base)
│   │   ├── AttractMode.cs
│   │   ├── GameMode.cs
│   │   ├── CreditsMode.cs
│   │   ├── ServiceMode.cs
│   │   ├── HighScoreMode.cs
│   │   ├── BonusMode.cs
│   │   ├── MatchMode.cs
│   │   ├── TiltMode.cs
│   │   └── CarouselMode.cs
│   ├── Platforms/
│   │   ├── IPlatform.cs (interface)
│   │   ├── PlatformController.cs
│   │   ├── Interfaces/
│   │   │   ├── IDriverPlatform.cs
│   │   │   ├── ISwitchPlatform.cs
│   │   │   ├── ILightPlatform.cs
│   │   │   ├── IServoPlatform.cs
│   │   │   ├── IStepperPlatform.cs
│   │   │   └── ...
│   │   ├── Virtual/
│   │   │   ├── VirtualPlatform.cs
│   │   │   ├── VirtualDriver.cs
│   │   │   ├── VirtualSwitch.cs
│   │   │   └── VirtualLight.cs
│   │   ├── PROC/
│   │   ├── FAST/
│   │   ├── OPP/
│   │   └── ...
│   ├── ConfigPlayers/
│   │   ├── IConfigPlayer.cs
│   │   ├── EventPlayer.cs
│   │   ├── LightPlayer.cs
│   │   ├── CoilPlayer.cs
│   │   ├── ShowPlayer.cs
│   │   ├── FlasherPlayer.cs
│   │   ├── VariablePlayer.cs
│   │   └── ...
│   ├── Shows/
│   │   ├── Show.cs
│   │   ├── ShowController.cs
│   │   ├── ShowPlayer.cs
│   │   └── ShowConfig.cs
│   ├── BCP/
│   │   ├── BcpInterface.cs
│   │   ├── BcpServer.cs
│   │   ├── BcpClient.cs
│   │   ├── BcpTransport.cs
│   │   └── BcpMessages.cs
│   ├── Game/
│   │   ├── Player.cs
│   │   ├── GameController.cs
│   │   ├── BallTracking.cs
│   │   └── Scoring.cs
│   ├── Plugins/
│   │   ├── Auditor.cs
│   │   ├── SwitchPlayer.cs
│   │   └── ...
│   └── Utility/
│       ├── MpfExtensions.cs
│       ├── ColorUtility.cs
│       ├── ConfigUtility.cs
│       └── SerializationUtility.cs
├── Config/
│   ├── ScriptableObjects/
│   │   ├── MachineConfig.asset
│   │   ├── DeviceConfigs/
│   │   ├── ModeConfigs/
│   │   └── PlatformConfigs/
│   └── JSON/
│       └── (runtime configs)
├── Prefabs/
│   ├── Devices/
│   │   ├── Switch.prefab
│   │   ├── Coil.prefab
│   │   ├── Light.prefab
│   │   └── ...
│   └── Modes/
├── Scenes/
│   ├── MainMachine.unity
│   ├── ServiceMode.unity
│   └── ...
└── Tests/
    ├── Core/
    ├── Devices/
    └── Integration/
```

## Assembly Definitions

```
MPF.Core.asmdef
├── MPF.Devices.asmdef
├── MPF.Modes.asmdef
├── MPF.Platforms.asmdef
├── MPF.ConfigPlayers.asmdef
├── MPF.BCP.asmdef
└── MPF.Tests.asmdef
```

## Migration Priorities

### Critical Path (Must Have - Phase 1-2)
1. Event system
2. Machine controller
3. Clock/timing
4. Device manager
5. Base device classes
6. Switch, Driver, Light devices
7. Platform abstraction
8. Virtual platform

### Core Functionality (Phase 3-4)
9. Mode system
10. Player/Game management
11. Ball management
12. Basic devices (flippers, ball devices)
13. Config system

### Extended Features (Phase 5-6)
14. Show system
15. Config players
16. Advanced devices
17. BCP communication
18. Built-in modes

### Polish & Hardware (Phase 7-8)
19. Service mode
20. Hardware platform implementations
21. Testing framework
22. Documentation

## Testing Strategy

### Unit Tests
- Core system components
- Device functionality
- Platform interfaces
- Config validation

### Integration Tests
- Mode transitions
- Ball tracking
- Event flow
- Device interactions

### Hardware Tests
- Virtual platform validation
- Real hardware integration
- Performance benchmarks

## Success Criteria

1. **Functional Parity**: All core MPF features work in Unity
2. **Performance**: Maintains real-time performance for pinball timing
3. **Extensibility**: Easy to add new devices and modes
4. **Config Compatibility**: Can import existing MPF configs (with conversion)
5. **Hardware Support**: Virtual platform fully functional, real hardware possible
6. **Documentation**: Complete API docs and migration guide

## Risks & Mitigation

### Risk 1: Asyncio → Coroutine Conversion
**Impact**: High
**Mitigation**: Create abstraction layer for async operations, extensive testing

### Risk 2: YAML Config Complexity
**Impact**: Medium
**Mitigation**: Build robust parser, support JSON alternative, provide conversion tools

### Risk 3: Hardware Timing Requirements
**Impact**: High
**Mitigation**: Use FixedUpdate for critical timing, profile extensively, optimize hot paths

### Risk 4: Platform Interface Compatibility
**Impact**: Medium
**Mitigation**: Start with virtual platform, abstract hardware communication, use plugins

### Risk 5: Scope Creep
**Impact**: High
**Mitigation**: Follow phased approach, MVP first, defer advanced features

## Timeline Estimate

- **Phase 1-2**: Core Foundation & Device Framework - 2-3 weeks
- **Phase 3-4**: Core Devices & Platform Abstraction - 2-3 weeks
- **Phase 5-6**: Game Logic & Ball Management - 2-3 weeks
- **Phase 7-8**: Advanced Devices & Show System - 2-3 weeks
- **Phase 9-10**: Config System & Config Players - 2 weeks
- **Phase 11-12**: Communication & Built-in Modes - 2 weeks
- **Phase 13-15**: Service, Hardware, Testing - 2-3 weeks

**Total**: 14-19 weeks for full conversion

## Conclusion

This conversion represents a significant undertaking, translating ~30,000+ lines of Python code to C# while adapting to Unity's architecture and patterns. The phased approach ensures that foundational systems are solid before building dependent components.

The key to success is:
1. **Maintain architectural principles** - Event-driven, loosely coupled, extensible
2. **Embrace Unity patterns** - Components, ScriptableObjects, Coroutines
3. **Start simple** - Virtual platform and basic devices first
4. **Test continuously** - Unit tests and integration tests throughout
5. **Document thoroughly** - API docs and migration notes

The result will be a modern, performant pinball framework that leverages Unity's powerful game engine capabilities while preserving MPF's excellent design and flexibility.
