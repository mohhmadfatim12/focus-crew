# 02. MVP Scope (Stage 1)

This document defines what is strictly included in **Stage 1 (MVP)** and what features are intentionally postponed for future iterations.

---

## 1. Included in Stage 1 (MVP)

### A. Core Timer & Durations
- **Preset Durations:**
  - Pomodoro: 25 minutes
  - Standard Focus: 50 minutes
  - Deep Work: 90 minutes
- **Custom Duration:**
  - Slider or number picker allowing any custom time from 1 minute up to 180 minutes.
- **Resilient Countdown Engine:**
  - Time tracking is based on timestamp deltas (`targetEndTime - currentTime`).
  - Remains accurate through screen-off / Doze mode, app process death, and device reboots.

### B. App Selection & Blocking Profiles
- **Installed App Picker:**
  - Displays installed third-party apps with app icons, friendly names, and package names.
  - Quick real-time search bar to filter apps by name.
  - *ASSUMPTION:* System apps (e.g., Phone dialer, Contacts, Settings, Messages) are excluded from the picker by default to prevent locking essential phone utilities.
- **Block Profiles:**
  - Create and save named profiles (e.g., "Study", "Coding", "Deep Work").
  - Switch active profiles before launching a focus session.

### C. Blocking & Calm Interception Screen
- **Accessibility Service Blocker:**
  - Inspects only `accessibilityEvent.packageName` to detect if the foreground app is on the active block list.
  - Launches a full-screen Compose-based calm block overlay over the restricted app.
- **Calm Block Screen Content:**
  - Friendly, non-accusatory message (e.g., "You chose to focus on your session. Let's get back.").
  - Displays remaining session time.
  - "Return to Focus" button that brings FocusCrew or the home screen to the front.

### D. Strict Mode with Friction
- **Intentional Friction Controls:**
  - When enabled, stopping an active session requires completing deliberate friction steps:
    1. A 30-second cooldown timer where the user must wait and reflect.
    2. A confirmation prompt requiring the user to type a short affirmation phrase (e.g., *"I want to stop my session"*).
- **Guaranteed Emergency Exit:**
  - The emergency exit is **never hidden or removed**.
  - Tapping "Emergency Stop" immediately stops blocking, but marks the session status as `INTERRUPTED` in history.

### E. Scheduled Breaks
- **Optional Breaks:**
  - Choose 10-minute or 15-minute break intervals during or between sessions.
  - Automatically suspends app blocking during the break.
  - Automatically re-enables app blocking the moment the break timer reaches zero.

### F. Analytics, Distraction Journal & History
- **Session History:**
  - List of past sessions with date, time, subject tag, duration, and completion status (`COMPLETED`, `INTERRUPTED`, `CANCELLED`).
  - Daily and weekly focus time totals.
- **Subject Tags:**
  - Categorize sessions (e.g., "Math", "Algorithms", "Writing", "Reading").
- **Distraction Journal (Quick Note):**
  - A quick-capture sheet where the user can jot down a wandering thought or urge without leaving focus mode, saving it for review after the session.

### G. Onboarding, Permissions & Settings
- **Permission Status Screen:**
  - Visual checklist showing the status of Accessibility Service, Notifications, and Battery Optimization.
  - Clear explanations of what each permission does and direct deep-links to Android Settings.
- **Appearance & Themes:**
  - Support for Light, Dark, and System Default themes.
- **Data Portability:**
  - Single-button local JSON export of all sessions, distraction notes, and history saved directly to device storage.

---

## 2. Postponed Features (NOT in Stage 1)

The following items are explicitly **out of scope** for Stage 1 and must **not** be worked on:

1. **Virtual Study Rooms & Multiplayer Focus:** No group timers, presence counters, or participant lists.
2. **Notes Library & Document Viewer:** No integrated markdown editors, PDF viewers, or rich text note taking.
3. **Chat & Social Messaging:** No peer-to-peer or server-based chat, messaging, or reactions.
4. **Voice Messages & Audio Rooms:** No voice calls, walkie-talkie features, or ambient audio streaming.
5. **Shorts / Reel Counter:** No algorithmic feed scraping or social media time trackers.
6. **Homescreen Widgets:** No Jetpack Glance or remote views widgets in Stage 1.
7. **NotebookLM / AI Integration:** No external LLM prompts, summary bots, or AI helpers.
8. **Cloud Accounts & Servers:** No cloud sync, Firebase, Supabase, backend databases, or OAuth logins.
