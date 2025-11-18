using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace MPF.Core
{
    /// <summary>
    /// Scheduled event handle that can be cancelled
    /// </summary>
    public class ScheduledEvent
    {
        public Coroutine Coroutine { get; set; }
        public bool IsCancelled { get; set; }

        public void Cancel()
        {
            IsCancelled = true;
        }
    }

    /// <summary>
    /// Periodic task handle
    /// </summary>
    public class PeriodicTask
    {
        public Coroutine Coroutine { get; set; }
        public bool IsCancelled { get; set; }
        public float Interval { get; set; }
        public float NextCallTime { get; set; }

        public void Cancel()
        {
            IsCancelled = true;
        }

        public float GetNextCallTime()
        {
            return NextCallTime;
        }
    }

    /// <summary>
    /// Clock manager for MPF - handles timing and scheduling
    /// Converted from Python mpf/core/clock.py
    /// Uses Unity's coroutine system instead of asyncio
    /// </summary>
    public class ClockManager : MonoBehaviour
    {
        private static ClockManager _instance;
        public static ClockManager Instance
        {
            get
            {
                if (_instance == null)
                {
                    var go = new GameObject("ClockManager");
                    _instance = go.AddComponent<ClockManager>();
                    DontDestroyOnLoad(go);
                }
                return _instance;
            }
        }

        private float startTime;
        private List<PeriodicTask> periodicTasks = new List<PeriodicTask>();
        public bool DebugMode { get; set; } = false;

        private void Awake()
        {
            if (_instance != null && _instance != this)
            {
                Destroy(gameObject);
                return;
            }
            _instance = this;
            DontDestroyOnLoad(gameObject);
            startTime = Time.time;

            if (DebugMode)
            {
                Debug.Log("[ClockManager] Starting tickless clock");
            }
        }

        /// <summary>
        /// Get current time in seconds since clock started
        /// </summary>
        public float GetTime()
        {
            return Time.time - startTime;
        }

        /// <summary>
        /// Get current DateTime
        /// </summary>
        public DateTime GetDateTime()
        {
            return DateTime.Now;
        }

        /// <summary>
        /// Schedule a callback to run once after a delay
        /// </summary>
        /// <param name="callback">The callback to invoke</param>
        /// <param name="timeout">Delay in seconds (0 = next frame)</param>
        /// <returns>ScheduledEvent handle</returns>
        public ScheduledEvent ScheduleOnce(Action callback, float timeout = 0)
        {
            if (callback == null)
            {
                throw new ArgumentException("Callback must not be null");
            }

            var scheduledEvent = new ScheduledEvent();
            scheduledEvent.Coroutine = StartCoroutine(ScheduleOnceCoroutine(callback, timeout, scheduledEvent));

            if (DebugMode)
            {
                Debug.Log($"[ClockManager] Scheduled one-time callback: {callback.Method.Name}, timeout: {timeout}s");
            }

            return scheduledEvent;
        }

        private IEnumerator ScheduleOnceCoroutine(Action callback, float timeout, ScheduledEvent scheduledEvent)
        {
            if (timeout > 0)
            {
                yield return new WaitForSeconds(timeout);
            }
            else
            {
                yield return null; // Wait one frame
            }

            if (!scheduledEvent.IsCancelled)
            {
                try
                {
                    callback();
                }
                catch (Exception ex)
                {
                    Debug.LogError($"[ClockManager] Error in scheduled callback: {ex}");
                }
            }
        }

        /// <summary>
        /// Schedule a callback to run repeatedly at an interval
        /// </summary>
        /// <param name="callback">The callback to invoke</param>
        /// <param name="interval">Interval in seconds</param>
        /// <returns>PeriodicTask handle</returns>
        public PeriodicTask ScheduleInterval(Action callback, float interval)
        {
            if (callback == null)
            {
                throw new ArgumentException("Callback must not be null");
            }

            if (interval <= 0)
            {
                throw new ArgumentException("Interval must be greater than 0");
            }

            var periodicTask = new PeriodicTask
            {
                Interval = interval,
                NextCallTime = Time.time + interval
            };

            periodicTask.Coroutine = StartCoroutine(ScheduleIntervalCoroutine(callback, interval, periodicTask));
            periodicTasks.Add(periodicTask);

            if (DebugMode)
            {
                Debug.Log($"[ClockManager] Scheduled recurring callback: {callback.Method.Name}, interval: {interval}s");
            }

            return periodicTask;
        }

        private IEnumerator ScheduleIntervalCoroutine(Action callback, float interval, PeriodicTask periodicTask)
        {
            while (!periodicTask.IsCancelled)
            {
                yield return new WaitForSeconds(interval);

                if (periodicTask.IsCancelled)
                {
                    break;
                }

                periodicTask.NextCallTime = Time.time + interval;

                try
                {
                    callback();
                }
                catch (Exception ex)
                {
                    Debug.LogError($"[ClockManager] Error in periodic callback: {ex}");
                }
            }

            // Remove from list when cancelled
            periodicTasks.Remove(periodicTask);
        }

        /// <summary>
        /// Cancel all periodic tasks
        /// </summary>
        public void CancelAllPeriodicTasks()
        {
            foreach (var task in periodicTasks)
            {
                task.Cancel();
            }
            periodicTasks.Clear();
        }

        /// <summary>
        /// Get count of active periodic tasks
        /// </summary>
        public int GetPeriodicTaskCount()
        {
            return periodicTasks.Count;
        }

        private void OnDestroy()
        {
            CancelAllPeriodicTasks();
        }
    }
}
