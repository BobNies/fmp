# Mission Pinball Framework - Unity3D Conversion

## Overview

This is a Unity3D conversion of the Mission Pinball Framework (MPF), originally written in Python. MPF is a comprehensive framework for controlling pinball machines, featuring hardware abstraction, event-driven architecture, and extensive device support.

**Original Python Framework:** https://github.com/missionpinball/mpf
**This Fork:** https://github.com/BobNies/fmp

## Conversion Status

### ✅ Completed Components

#### Core Systems
- **EventManager** (`Assets/Scripts/Core/EventManager.cs`)
  - Type-safe event system with priority-based handlers
  - Support for standard, boolean, and queue events
  - Event condition parsing and priority modifiers
  - Fully functional with Unity patterns

- **ClockManager** (`Assets/Scripts/Core/ClockManager.cs`)
  - Coroutine-based scheduling system (replaces Python asyncio)
  - One-shot scheduled callbacks
  - Periodic interval callbacks
  - Compatible with Unity's time system

- **MachineController** (`Assets/Scripts/Core/MachineController.cs`)
  - Central singleton managing all MPF systems
  - Initialization phases (init_phase_1 through init_phase_4)
  - Boot hold management
  - Device collection management
  - Lifecycle event handling

- **DeviceManager** (`Assets/Scripts/Core/DeviceManager.cs`)
  - Generic device creation and lifecycle management
  - Typed device collections
  - Device monitoring for BCP integration
  - Phase-based device initialization

#### Device Framework
- **Device Base Classes** (`Assets/Scripts/Devices/Device.cs`)
  - `Device` - Abstract base class for all devices
  - `SystemWideDevice` - For machine-wide devices
  - `ModeDevice` - For mode-specific devices
  - Configuration management
  - Tag system

#### Core Devices
- **Switch** (`Assets/Scripts/Devices/Switch.cs`)
  - State management (active/inactive)
  - NC/NO support with inversion
  - Debouncing
  - Recycle protection
  - Event posting on state changes
  - Manual activation for testing

- **Driver (Coil)** (`Assets/Scripts/Devices/Driver.cs`)
  - Pulse control with configurable duration and power
  - Enable/disable for hold coils
  - Timed enable with auto-disable
  - Pulse-with-hold functionality
  - Max hold duration safety
  - Event posting for coil actions

- **Light** (`Assets/Scripts/Devices/Light.cs`)
  - RGB/RGBW color support
  - Priority-based color stacking
  - Smooth color fading
  - Color correction
  - Multiple light types support
  - Event posting on color changes

## Architecture Differences: Python vs Unity

| Python (Original) | Unity (Converted) | Notes |
|------------------|-------------------|-------|
| asyncio event loop | Unity MonoBehaviour + Coroutines | Coroutines replace async/await |
| YAML config files | JSON + ScriptableObjects | More Unity-native approach |
| Dynamic typing | Static typing (C#) | Stronger type safety |
| Callback functions | Delegates + UnityEvents | Type-safe event system |
| Multiple inheritance | Single inheritance + interfaces | C# limitation |
| Global machine instance | Singleton MonoBehaviours | Unity pattern |
| Module imports | Namespaces + Assembly Definitions | Unity organization |

## Project Structure

```
Assets/
├── Scripts/
│   ├── Core/
│   │   ├── EventManager.cs         - Event system
│   │   ├── ClockManager.cs         - Timing and scheduling
│   │   ├── MachineController.cs    - Main machine controller
│   │   └── DeviceManager.cs        - Device lifecycle management
│   ├── Devices/
│   │   ├── Device.cs               - Base device classes
│   │   ├── Switch.cs               - Switch device
│   │   ├── Driver.cs               - Coil/driver device
│   │   └── Light.cs                - RGB light device
│   ├── MPF.Core.asmdef             - Core assembly definition
│   └── MPF.Devices.asmdef          - Devices assembly definition
├── Config/
│   └── ScriptableObjects/          - Runtime configuration
├── Prefabs/
│   └── Devices/                    - Device prefabs
└── Scenes/
    └── (Unity scenes)
```

## Quick Start

### 1. Open Project in Unity
- Requires Unity 2021.3 LTS or newer
- Open the `fmp` directory as a Unity project

### 2. Scene Setup
Create a new scene with the core managers:

```csharp
// MachineController will auto-create EventManager and ClockManager as singletons
var machine = MachineController.Instance;

// Machine will auto-initialize on Start()
// Or manually trigger: StartCoroutine(machine.InitializeMachine());
```

### 3. Create Devices

```csharp
// Using DeviceManager
var switchConfig = new Dictionary<string, object>
{
    { "type", "NO" },  // Normally Open
    { "debounce_open", 30f },
    { "debounce_close", 30f }
};

var switches = DeviceManager.Instance.CreateDeviceCollection<Switch>("switches");
// Add switches via config or manually...
```

### 4. Working with Events

```csharp
// Register event handler
machine.Events.AddHandler("ball_started", OnBallStarted, priority: 10);

void OnBallStarted(Dictionary<string, object> kwargs)
{
    Debug.Log("Ball started!");
}

// Post event
machine.Events.Post("ball_started", null, new Dictionary<string, object>
{
    { "player", currentPlayer },
    { "ball", currentBall }
});
```

### 5. Working with Devices

```csharp
// Switch
var leftSlingshot = GetComponent<Switch>();
leftSlingshot.Initialize(machine, "left_slingshot");
leftSlingshot.Configure(config);

// When switch activates (from hardware or manual)
leftSlingshot.Activate();  // Posts "left_slingshot_active" event

// Driver/Coil
var leftFlipperMain = GetComponent<Driver>();
leftFlipperMain.Pulse(30f);              // 30ms pulse
leftFlipperMain.Enable(0.375f);          // Hold at 37.5% power
leftFlipperMain.TimedEnable(2000f);      // Enable for 2 seconds

// Light
var shootAgainLight = GetComponent<Light>();
shootAgainLight.On(Color.red, priority: 100);
shootAgainLight.SetColor(Color.blue, priority: 50, fadeMs: 500f);
shootAgainLight.Off();
```

## Event System Usage

The event system is the heart of MPF. Everything communicates through events.

```csharp
// Standard event (fire and forget)
machine.Events.Post("my_event");

// Event with data
machine.Events.Post("score", null, new Dictionary<string, object>
{
    { "points", 1000 },
    { "player", playerIndex }
});

// Boolean event (can be cancelled by handlers returning false)
machine.Events.PostBoolean("ball_ending");

// Queue event (waits for all handlers)
machine.Events.PostQueue("mode_game_started", OnGameStartComplete);

// Event with priority modifier
machine.Events.AddHandler("ball_started.100", MyHighPriorityHandler);

// Event with condition (requires PlaceholderManager)
machine.Events.AddHandler("switch_active{device.state==1}", ConditionalHandler);
```

## Clock/Timing Usage

```csharp
// Schedule one-time callback
machine.Clock.ScheduleOnce(() => {
    Debug.Log("Delayed action!");
}, timeout: 2.0f);  // 2 seconds

// Schedule recurring callback
var periodicTask = machine.Clock.ScheduleInterval(() => {
    Debug.Log("Every second!");
}, interval: 1.0f);

// Cancel periodic task
periodicTask.Cancel();
```

## Configuration

Currently, configuration is done programmatically via dictionaries. In a full implementation, this would be loaded from JSON or ScriptableObjects.

```csharp
var deviceConfig = new Dictionary<string, object>
{
    { "label", "Left Flipper" },
    { "tags", new List<string> { "flipper", "player_controlled" } },
    { "type", "NO" },
    { "platform", "virtual" }
};
```

## Next Steps / Roadmap

### High Priority
- [ ] **SwitchController** - Hardware switch polling and management
- [ ] **ModeController** - Game mode state machine
- [ ] **Mode base class** - Mode lifecycle and priority stacking
- [ ] **BallController** - Ball tracking and management
- [ ] **Playfield device** - Special ball device for playfield
- [ ] **BallDevice** - Ball storage and ejection

### Medium Priority
- [ ] **Platform abstraction** - Hardware interface layer
- [ ] **Virtual Platform** - Software simulation for testing
- [ ] **ShowController** - Timeline-based show system
- [ ] **Config loading** - JSON/YAML to ScriptableObject pipeline
- [ ] **PlaceholderManager** - Dynamic value substitution
- [ ] **Additional devices** - Flipper, Shot, ShotGroup, etc.

### Lower Priority
- [ ] **BCP implementation** - External communication protocol
- [ ] **Config players** - Light player, coil player, etc.
- [ ] **Built-in modes** - Attract, game, credits, service
- [ ] **Hardware platform implementations** - P-ROC, FAST, OPP, etc.
- [ ] **Service mode** - Diagnostics and testing UI
- [ ] **Auditor** - Statistics tracking

## Design Decisions

### Singletons
Core managers (MachineController, EventManager, etc.) use the singleton pattern for global access. This aligns with Unity best practices and MPF's design where there's one machine instance.

### Coroutines vs async/await
Unity's coroutines are used instead of Python's asyncio. While C# supports async/await, Unity's MonoBehaviour lifecycle works better with coroutines for frame-based operations.

### Event System
The event system uses `Dictionary<string, object>` for kwargs to maintain flexibility similar to Python's `**kwargs`. Type-safe alternatives could be implemented using generics or UnityEvents.

### Device Components
Devices are MonoBehaviour components that can be added to GameObjects. This allows them to use Unity's lifecycle, inspector integration, and scene hierarchy.

## Testing

Manual testing:
```csharp
// In Unity Editor, attach scripts to GameObjects and test in Play mode

// Create test switch
var testSwitch = new GameObject("TestSwitch").AddComponent<Switch>();
testSwitch.Initialize(MachineController.Instance, "test");
testSwitch.Configure(new Dictionary<string, object> { { "type", "NO" } });

// Manually toggle
testSwitch.Activate();   // Should post "test_active" event
testSwitch.Deactivate(); // Should post "test_inactive" event
```

## Contributing

This conversion is a work in progress. The core foundation is in place, but many components from the original MPF still need to be converted.

### Areas needing help:
1. Mode system implementation
2. Hardware platform interfaces
3. Config loading system
4. Additional device types
5. Show/timeline system
6. BCP networking

## License

Original MPF: MIT License
This Unity conversion: MIT License (maintaining compatibility)

## Credits

**Original MPF Framework:**
- Mission Pinball Framework Team
- https://missionpinball.org
- https://github.com/missionpinball/mpf

**Unity Conversion:**
- Based on MPF 0.80.x
- Converted to Unity3D / C#
- Fork maintained at: https://github.com/BobNies/fmp

## References

- [MPF Documentation](https://docs.missionpinball.org)
- [MPF GitHub](https://github.com/missionpinball/mpf)
- [Unity Documentation](https://docs.unity3d.com)

## Version History

### v0.80.0-unity-alpha1 (Current)
- Initial Unity conversion
- Core event system
- Clock/timing system
- Machine controller
- Device manager and base classes
- Core devices: Switch, Driver, Light
- Assembly definitions
- Documentation

---

**Note:** This is an early alpha conversion. Many features from the original MPF are not yet implemented. See the UNITY_CONVERSION_PLAN.md for the comprehensive conversion roadmap.
