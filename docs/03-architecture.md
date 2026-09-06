# 03. Architecture & System Design

This document details the architectural boundaries, system layers, resilient timer mathematics, and package organization for FocusCrew.

---

## 1. Architectural Layers

FocusCrew follows the standard Android Clean Architecture with unidirectional data flow:

```
┌─────────────────────────────────────────────────────────┐
│                     UI Layer                            │
│   Jetpack Compose Screens, Navigation, Theme, Dialogs   │
└────────────────────────────┬────────────────────────────┘
                             │ Observes StateFlow / Dispatches Events
                             ▼
┌─────────────────────────────────────────────────────────┐
│                  ViewModel Layer                        │
│   FocusTimerViewModel, AppPickerViewModel, etc.        │
└────────────────────────────┬────────────────────────────┘
                             │ Invokes UseCases
                             ▼
┌─────────────────────────────────────────────────────────┐
│                   Domain Layer                          │
│   Pure Kotlin State Machines, Time Calculators, Models   │
│   (Zero Android framework dependencies: android.* free) │
└────────────────────────────┬────────────────────────────┘
                             │ Calls Repositories
                             ▼
┌─────────────────────────────────────────────────────────┐
│                    Data Layer                           │
│   Room Database (SQLite), Jetpack DataStore, System APIs│
└─────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

1. **UI Layer (Jetpack Compose):**
   - Declarative UI with Material 3 components.
   - Observes immutable UI state classes from ViewModels.
   - Emits user actions (clicks, toggles) directly to the ViewModel.
2. **ViewModel Layer:**
   - Holds UI-related state that survives configuration changes (such as screen rotation).
   - Coordinates domain use-cases and exposes state via Kotlin `StateFlow`.
3. **Domain Layer (Pure Kotlin):**
   - Contains core business rules, the session timer state machine, break logic, and strict-mode friction rules.
   - **Crucial Rule:** Zero `android.*` imports. This allows high-speed, 100% deterministic JUnit tests that run in milliseconds on the JVM without an emulator.
4. **Data Layer:**
   - **Room Database:** Persists sessions, block profiles, blocked package names, breaks, distraction notes, and event logs.
   - **Jetpack DataStore:** Persists lightweight user preferences (active profile ID, theme choice, strict mode default).
   - **System Adapters:** Wrappers around Android system services (Accessibility, AlarmManager, Notifications).

---

## 2. Background Blocking Architecture

The blocking mechanism runs via an independent Android Accessibility Service:

```
[User taps Instagram] 
         │
         ▼
[Android OS emits TYPE_WINDOW_STATE_CHANGED]
         │
         ▼
[BlockerAccessibilityService]
         │
         ├── 1. Read event.packageName (Strict: DO NOT read screen text)
         ├── 2. Query ActiveSessionState (via in-memory cache / DataStore)
         └── 3. Check: Is session ACTIVE? && Is onBreak == FALSE? && Is package in BlockList?
                  │
                  ├── YES ──► Launch CalmBlockActivity (singleTask / flag_activity_new_task)
                  └── NO  ──► Do nothing (allow user through)
```

- **Separation of Concerns:** The `BlockerAccessibilityService` does not run timer tick loops. It only evaluates the current timestamp against the active session's precomputed end time whenever a window change occurs.

---

## 3. Resilient Timer Design & Math

### The Anti-Pattern: Never Decrement a Counter
Many beginner timer apps decrement a variable every second (e.g., `remainingSeconds--`). This is **fragile** because:
- Android suspends CPU cycles when the screen turns off (Doze mode).
- The operating system can kill the app process at any time to free RAM.
- A 1-second delay can drift or pause entirely, making the timer inaccurate.

### The Correct Pattern: Absolute End Time Math
FocusCrew stores absolute hardware and wall-clock reference points at session start:

| Field | Description | Source |
|---|---|---|
| `startWallClock` | Unix timestamp of when the session started (in milliseconds). | `System.currentTimeMillis()` |
| `startElapsedRealtime` | Time in ms since device boot, including deep sleep. | `SystemClock.elapsedRealtime()` |
| `plannedDurationMs` | Total duration chosen for this session in ms (e.g., 25 min = 1,500,000 ms). | User input |
| `pausedDurationMs` | Total accumulated break/pause time in ms. | Domain calculation |

### Calculating Remaining Time
At any moment $T_{\text{now}}$, the remaining time is calculated as:

$$\text{Target End Time} = \text{startElapsedRealtime} + \text{plannedDurationMs} + \text{pausedDurationMs}$$

$$\text{Remaining Time} = \text{Target End Time} - \text{SystemClock.elapsedRealtime()}$$

If $\text{Remaining Time} \le 0$, the session is completed.

### Surviving Edge Cases

1. **Screen Off / Doze Mode:**
   - Because `SystemClock.elapsedRealtime()` counts milliseconds spent in deep sleep, time calculation is instantly correct the millisecond the screen wakes up.
2. **Process Death:**
   - Both `startWallClock` and `startElapsedRealtime` along with the boot timestamp are saved to DataStore/Room.
   - When the app is relaunched, the state machine loads the stored target end time and recalculates remaining time immediately.
3. **Device Reboot:**
   - On reboot, `elapsedRealtime` resets to zero.
   - When this happens, FocusCrew detects the boot cycle (boot timestamp changed) and falls back to:
     $$\text{Target End Wall Clock} = \text{startWallClock} + \text{plannedDurationMs} + \text{pausedDurationMs}$$
     $$\text{Remaining Time} = \text{Target End Wall Clock} - \text{System.currentTimeMillis()}$$
   - *ASSUMPTION:* If the device was powered off during the entire remaining duration, on reboot the session is marked as `COMPLETED` if target time passed, or `INTERRUPTED` if strict mode was active during reboot.
4. **Manual Clock Manipulation:**
   - If the user manually changes system time, `SystemClock.elapsedRealtime()` remains unaffected (monotonic clock cannot be set manually).
5. **Completion Alarms (`AlarmManager`):**
   - The completion alarm is strictly **best-effort**. If the OS delays or drops the alarm, the UI and the blocker remain 100% accurate because they rely on delta math, not alarm firing.

---

## 4. Proposed Package & Folder Structure

```
com.focuscrew.app/
│
├── core/
│   ├── model/                  # Core shared data classes & enums
│   │   ├── SessionMode.kt
│   │   ├── SessionStatus.kt
│   │   └── EndReason.kt
│   └── time/                   # Pure Kotlin time utilities
│       └── TimeProvider.kt     # Monotonic time interface for easy unit testing
│
├── domain/                     # Pure Kotlin business logic (no android.*)
│   ├── model/
│   │   ├── FocusSession.kt
│   │   └── TimerState.kt
│   ├── statemachine/
│   │   ├── SessionStateMachine.kt
│   │   └── StrictFrictionMachine.kt
│   └── usecase/
│       ├── StartSessionUseCase.kt
│       ├── StopSessionUseCase.kt
│       ├── CalculateRemainingTimeUseCase.kt
│       └── CheckIsAppBlockedUseCase.kt
│
├── data/                       # Data layer (Room, DataStore, Repositories)
│   ├── local/
│   │   ├── database/
│   │   │   ├── FocusDatabase.kt
│   │   │   └── dao/
│   │   │       ├── SessionDao.kt
│   │   │       ├── BlockProfileDao.kt
│   │   │       └── DistractionDao.kt
│   │   ├── entity/
│   │   │   ├── SessionEntity.kt
│   │   │   ├── BlockProfileEntity.kt
│   │   │   ├── BlockedAppEntity.kt
│   │   │   └── DistractionEntryEntity.kt
│   │   └── datastore/
│   │       └── UserPreferencesDataStore.kt
│   └── repository/
│       ├── SessionRepositoryImpl.kt
│       └── BlockProfileRepositoryImpl.kt
│
├── service/                    # Android background services
│   ├── BlockerAccessibilityService.kt   # Intercepts foreground app changes
│   └── alarm/
│       └── SessionCompletionReceiver.kt # Broadcast receiver for completion alarm
│
└── ui/                         # Presentation Layer (Jetpack Compose)
    ├── navigation/
    │   └── NavGraph.kt
    ├── theme/
    │   ├── Color.kt
    │   ├── Type.kt
    │   └── Theme.kt
    ├── components/             # Reusable UI widgets (cards, buttons, sliders)
    ├── timer/                  # Active timer and countdown screen
    │   ├── TimerScreen.kt
    │   └── TimerViewModel.kt
    ├── blocker/                # Calm block overlay screen
    │   └── CalmBlockActivity.kt
    ├── apps/                   # App picker & block list management
    │   ├── AppPickerScreen.kt
    │   └── AppPickerViewModel.kt
    ├── history/                # History, analytics & distraction journal
    │   ├── HistoryScreen.kt
    │   └── HistoryViewModel.kt
    └── onboarding/             # Permission checklist & onboarding guide
        └── PermissionStatusScreen.kt
```
