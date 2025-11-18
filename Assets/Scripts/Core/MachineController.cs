using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace MPF.Core
{
    /// <summary>
    /// Main machine controller - the central orchestrator for the MPF framework
    /// Converted from Python mpf/core/machine.py
    /// Singleton MonoBehaviour that manages all core systems and devices
    /// </summary>
    public class MachineController : MonoBehaviour
    {
        private static MachineController _instance;
        public static MachineController Instance
        {
            get
            {
                if (_instance == null)
                {
                    var go = new GameObject("MachineController");
                    _instance = go.AddComponent<MachineController>();
                    DontDestroyOnLoad(go);
                }
                return _instance;
            }
        }

        // Core managers (automatically initialized)
        public EventManager Events { get; private set; }
        public ClockManager Clock { get; private set; }
        public DeviceManager DeviceManager { get; private set; }
        public ModeController ModeController { get; private set; }
        // public SwitchController SwitchController { get; private set; }
        // public BallController BallController { get; private set; }
        // public ShowController ShowController { get; private set; }
        // public LightController LightController { get; private set; }
        // public PlatformController PlatformController { get; private set; }
        // public PlaceholderManager PlaceholderManager { get; private set; }
        // public ServiceController ServiceController { get; private set; }
        // public BcpInterface Bcp { get; private set; }

        // Machine state
        public bool IsInitDone { get; private set; }
        public bool IsShuttingDown { get; private set; }
        public string MachinePath { get; set; }
        public string Version { get; set; } = "0.80.0-unity";

        // Configuration
        public Dictionary<string, object> Config { get; private set; }

        // Device collections (will be populated by DeviceManager)
        public Dictionary<string, object> Switches { get; private set; }
        public Dictionary<string, object> Coils { get; private set; }
        public Dictionary<string, object> Lights { get; private set; }
        public Dictionary<string, object> BallDevices { get; private set; }
        public Dictionary<string, object> Flippers { get; private set; }
        public Dictionary<string, object> Shots { get; private set; }
        public Dictionary<string, object> ShotGroups { get; private set; }
        public Dictionary<string, object> Diverters { get; private set; }
        public Dictionary<string, object> DropTargets { get; private set; }
        public Dictionary<string, object> DropTargetBanks { get; private set; }
        public Dictionary<string, object> Servos { get; private set; }
        public Dictionary<string, object> Steppers { get; private set; }
        public Dictionary<string, object> Motors { get; private set; }
        public Dictionary<string, object> Magnets { get; private set; }
        public Dictionary<string, object> Multiballs { get; private set; }
        public Dictionary<string, object> MultiballLocks { get; private set; }
        public Dictionary<string, object> BallHolds { get; private set; }
        public Dictionary<string, object> BallSaves { get; private set; }
        public Dictionary<string, object> Achievements { get; private set; }
        public Dictionary<string, object> AchievementGroups { get; private set; }
        public Dictionary<string, object> ExtraBalls { get; private set; }
        public Dictionary<string, object> ExtraBallGroups { get; private set; }
        public Dictionary<string, object> StateMachines { get; private set; }
        public Dictionary<string, object> Counters { get; private set; }
        public Dictionary<string, object> Sequences { get; private set; }
        public Dictionary<string, object> Accruals { get; private set; }

        // Current game reference
        public object Game { get; set; }
        public object Player { get; set; }
        public object Playfield { get; set; }

        // Modes
        public Dictionary<string, object> Modes { get; private set; }

        // Boot holds for initialization
        private HashSet<string> bootHolds = new HashSet<string>();

        // Debug mode
        public bool DebugMode { get; set; } = true;

        private void Awake()
        {
            if (_instance != null && _instance != this)
            {
                Destroy(gameObject);
                return;
            }
            _instance = this;
            DontDestroyOnLoad(gameObject);

            Debug.Log($"Mission Pinball Framework Core Engine v{Version}");
            Debug.Log($"Unity version: {Application.unityVersion}");

            // Initialize collections
            InitializeCollections();
        }

        private void Start()
        {
            // Auto-initialize on start
            StartCoroutine(InitializeMachine());
        }

        private void InitializeCollections()
        {
            Config = new Dictionary<string, object>();
            Switches = new Dictionary<string, object>();
            Coils = new Dictionary<string, object>();
            Lights = new Dictionary<string, object>();
            BallDevices = new Dictionary<string, object>();
            Flippers = new Dictionary<string, object>();
            Shots = new Dictionary<string, object>();
            ShotGroups = new Dictionary<string, object>();
            Diverters = new Dictionary<string, object>();
            DropTargets = new Dictionary<string, object>();
            DropTargetBanks = new Dictionary<string, object>();
            Servos = new Dictionary<string, object>();
            Steppers = new Dictionary<string, object>();
            Motors = new Dictionary<string, object>();
            Magnets = new Dictionary<string, object>();
            Multiballs = new Dictionary<string, object>();
            MultiballLocks = new Dictionary<string, object>();
            BallHolds = new Dictionary<string, object>();
            BallSaves = new Dictionary<string, object>();
            Achievements = new Dictionary<string, object>();
            AchievementGroups = new Dictionary<string, object>();
            ExtraBalls = new Dictionary<string, object>();
            ExtraBallGroups = new Dictionary<string, object>();
            StateMachines = new Dictionary<string, object>();
            Counters = new Dictionary<string, object>();
            Sequences = new Dictionary<string, object>();
            Accruals = new Dictionary<string, object>();
            Modes = new Dictionary<string, object>();
        }

        /// <summary>
        /// Initialize the machine asynchronously
        /// </summary>
        public IEnumerator InitializeMachine()
        {
            Debug.Log("[MachineController] Initializing machine...");

            RegisterBootHold("init");

            // Initialize core managers
            yield return StartCoroutine(LoadCoreModules());

            // Initialize hardware platforms (when implemented)
            // yield return StartCoroutine(LoadHardwarePlatforms());

            // Initialize devices (when device manager is ready)
            // yield return StartCoroutine(InitializeDevices());

            // Run initialization phases
            yield return StartCoroutine(RunInitPhases());

            ClearBootHold("init");

            IsInitDone = true;
            Debug.Log("[MachineController] Machine initialization complete!");

            // Post init_done event
            Events.Post("init_done");
        }

        /// <summary>
        /// Load core modules in order
        /// </summary>
        private IEnumerator LoadCoreModules()
        {
            Debug.Log("[MachineController] Loading core modules...");

            // EventManager
            if (EventManager.Instance != null)
            {
                Events = EventManager.Instance;
                Debug.Log("[MachineController] EventManager loaded");
            }

            // ClockManager
            if (ClockManager.Instance != null)
            {
                Clock = ClockManager.Instance;
                Debug.Log("[MachineController] ClockManager loaded");
            }

            // DeviceManager (when ready)
            // DeviceManager = DeviceManager.Instance;

            // ModeController (when ready)
            // ModeController = ModeController.Instance;

            // SwitchController (when ready)
            // SwitchController = SwitchController.Instance;

            // Additional controllers...

            yield return null;
        }

        /// <summary>
        /// Run initialization phases
        /// </summary>
        private IEnumerator RunInitPhases()
        {
            Debug.Log("[MachineController] Running init phases...");

            // Phase 1: Device modules loaded
            Events.Post("init_phase_1");
            yield return new WaitForSeconds(0.1f);

            // Phase 2: Switches initialized
            Events.Post("init_phase_2");
            yield return new WaitForSeconds(0.1f);

            // Phase 3: Plugins loaded
            Events.Post("init_phase_3");
            yield return new WaitForSeconds(0.1f);

            // Phase 4: Final initialization
            Events.Post("init_phase_4");
            yield return new WaitForSeconds(0.1f);

            Debug.Log("[MachineController] Init phases complete");
        }

        /// <summary>
        /// Register a boot hold to prevent machine from being ready
        /// </summary>
        public void RegisterBootHold(string hold)
        {
            bootHolds.Add(hold);
            if (DebugMode)
            {
                Debug.Log($"[MachineController] Registered boot hold: {hold}");
            }
        }

        /// <summary>
        /// Clear a boot hold
        /// </summary>
        public void ClearBootHold(string hold)
        {
            bootHolds.Remove(hold);
            if (DebugMode)
            {
                Debug.Log($"[MachineController] Cleared boot hold: {hold}");
            }

            if (bootHolds.Count == 0)
            {
                Events.Post("machine_ready");
            }
        }

        /// <summary>
        /// Check if machine has boot holds
        /// </summary>
        public bool HasBootHolds()
        {
            return bootHolds.Count > 0;
        }

        /// <summary>
        /// Reset the machine
        /// </summary>
        public void Reset()
        {
            Debug.Log("[MachineController] Resetting machine...");
            Events.Post("machine_reset_phase_1");
            Events.Post("machine_reset_phase_2");
            Events.Post("machine_reset_phase_3");
        }

        /// <summary>
        /// Shutdown the machine
        /// </summary>
        public IEnumerator Shutdown()
        {
            if (IsShuttingDown)
            {
                yield break;
            }

            IsShuttingDown = true;
            Debug.Log("[MachineController] Shutting down machine...");

            Events.Post("shutdown");
            yield return new WaitForSeconds(0.5f);

            // Stop all periodic tasks
            if (Clock != null)
            {
                Clock.CancelAllPeriodicTasks();
            }

            Debug.Log("[MachineController] Shutdown complete");
        }

        /// <summary>
        /// Stop the machine (called when application quits)
        /// </summary>
        public void Stop()
        {
            StartCoroutine(Shutdown());
        }

        private void OnApplicationQuit()
        {
            Stop();
        }

        private void OnDestroy()
        {
            if (_instance == this)
            {
                _instance = null;
            }
        }
    }
}
