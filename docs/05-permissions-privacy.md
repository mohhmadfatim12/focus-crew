# 05. Permissions & Privacy Architecture

This document explains the Android permissions required by FocusCrew, why each is necessary, when it is requested, what happens if denied or revoked, and our strict privacy boundaries.

---

## 1. Privacy Guarantee & Data Boundaries

- **Zero Network Traffic:** FocusCrew contains no internet permission (`android.permission.INTERNET`). The app physically cannot send data over the internet or communicate with an external server.
- **Local Storage Only:** All session history, distraction thoughts, and block profiles stay on your personal device in the encrypted SQLite app sandbox.
- **Strictly No Screen Scraping:** 
  - The accessibility service is strictly configured with `android:accessibilityFeedbackType="feedbackGeneric"` and flags that **do not retrieve window contents**.
  - We read only `accessibilityEvent.packageName` to match against your blocked app list.
  - We **never** read user text, passwords, chat messages, form inputs, or search queries.

---

## 2. Permission Breakdown

### 1. Accessibility Service (`BIND_ACCESSIBILITY_SERVICE`)
- **Why Needed:** 
  Allows FocusCrew to receive an immediate system callback whenever a new window appears on the screen, reading only the `packageName` of the app entering the foreground. If that package is in your active block list, FocusCrew displays the calm block screen.
- **When Asked:** 
  During initial onboarding, or before launching the first focus session with app blocking enabled.
- **What Happens If Denied or Revoked:** 
  The timer functions normally as a visual countdown, but FocusCrew cannot detect or intercept other apps. The app displays an honest banner: *"App blocking is inactive because Accessibility permission is turned off."*
- **ASSUMPTION (Sideloaded APKs on Android 13+):** 
  When installing FocusCrew via a direct APK file (sideloading) rather than Google Play, Android 13 and newer automatically greys out accessibility settings with the warning *"Restricted setting"*. 
  *Device workaround:* The user must go to **Settings > Apps > FocusCrew > tap three dots (top right) > tap "Allow restricted settings"** and authenticate with their phone PIN/fingerprint before enabling the Accessibility Service. This assumption will be verified directly on a physical test device.

---

### 2. Post Notifications (`POST_NOTIFICATIONS` - Android 13+)
- **Why Needed:** 
  Required to show a persistent, low-priority foreground notification while a focus session or break is active. This ensures the user can see remaining time in their notification shade and helps prevent Android from killing the timer process.
- **When Asked:** 
  On Android 13+ (API level 33+), prompted during initial setup when the user configures their first session.
- **What Happens If Denied or Revoked:** 
  The app still works, but remaining time will not appear in the notification drawer. Session completion chimes may be missed if the app is in the background.

---

### 3. Exact Alarm (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`)
- **Why Needed (Optional / Stage 1 Best-Effort):** 
  Allows scheduling a high-precision wake alarm at the exact minute a 25/50/90-minute session concludes, so the phone can play a chime or gentle vibration even if the device is in deep Doze mode.
- **When Asked:** 
  Only when the user toggles on "Play Sound on Completion" in Settings.
- **What Happens If Denied or Revoked:** 
  FocusCrew falls back to standard inexact alarms or UI-based timer checks. The UI countdown and accessibility blocker remain 100% accurate because they calculate time from `SystemClock.elapsedRealtime()`, not the alarm.

---

### 4. Usage Access (`PACKAGE_USAGE_STATS`)
- **Why Needed (Optional Secondary Fallback):** 
  On some heavily customized Android skins (e.g., MIUI/HyperOS, ColorOS), manufacturers aggressively kill accessibility services. Usage Access provides an alternate way to query which app is in the foreground.
- **When Asked:** 
  Only if the accessibility service repeatedly gets killed by the OS or if the user opts for app-usage insights.
- **What Happens If Denied or Revoked:** 
  FocusCrew relies exclusively on the primary Accessibility Service. No functionality is lost on standard Android devices.

---

## 3. Summary Matrix

| Permission | Category | Crucial for Blocking? | User Recourse If Revoked |
|---|---|---|---|
| **Accessibility Service** | System Setting | Yes | Clear prompt directing to Settings; timer remains usable as standalone clock. |
| **Notifications** | Runtime (`POST_NOTIFICATIONS`) | No | App operates normally; notification shade tracker is hidden. |
| **Exact Alarm** | Special App Access | No | Sound alert may delay slightly in deep sleep; visual timer and blocker remain exact. |
| **Usage Stats** | Special App Access | Optional | Falls back completely to Accessibility Service. |
