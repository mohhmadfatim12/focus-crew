# FocusCrew (Provisional Name)

FocusCrew is a calm, offline-first Android focus timer designed for distraction-free study and work. It helps you stay focused by blocking distracting apps during your timer sessions and provides an honest, transparent experience whenever Android limits blocking.

---

## What the App Does

- **Timer Modes:** Classic Pomodoro (25 min), Standard Focus (50 min), Deep Work (90 min), and Custom session durations.
- **Resilient Countdown:** A reliable timer calculation that survives screen-off, background process kills, and phone reboots.
- **Gentle App Blocker:** Blocks selected distracting apps using an Android Accessibility Service and shows a calm, non-judgmental screen to help you return to your task.
- **Healthy Friction (Strict Mode):** Prevents impulsive session cancellations with deliberate cooldowns and confirmation steps, while always keeping an emergency exit available.
- **Privacy by Design:** 100% offline. No internet connection needed, no user account, no tracking, and zero ads.

---

## How to Build the App

1. **Install Prerequisites:**
   - Download and install [Android Studio](https://developer.android.com/studio) (Ladybug or newer recommended).
   - Ensure you have **JDK 17** or **JDK 21** installed (packaged with Android Studio).
   - Ensure the Android SDK platform (API 34 or 35) is downloaded via the SDK Manager.

2. **Open the Project:**
   - Launch Android Studio.
   - Choose **File > Open...** and select the `FocusCrew` project folder.
   - Let Gradle sync the project dependencies.

3. **Compile the APK:**
   - In Android Studio, click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   - Alternatively, open the terminal in the project directory and run:
     ```bash
     ./gradlew assembleDebug
     ```
   - The compiled debug APK will be created at:
     `app/build/outputs/apk/debug/app-debug.apk`

---

## How to Run and Test on Your Phone

1. **Enable Developer Options on Your Phone:**
   - Open **Settings > About Phone**.
   - Tap **Build Number** 7 times until you see a message saying "You are now a developer".
   - Go to **Settings > System > Developer Options** and enable **USB Debugging**.

2. **Connect Your Phone:**
   - Plug your phone into your computer using a USB cable.
   - Unlock your phone screen and tap **Allow USB debugging** when prompted.

3. **Install and Launch:**
   - In Android Studio, select your physical phone from the device dropdown at the top toolbar.
   - Click the green **Run (Play)** button, or run from the terminal:
     ```bash
     ./gradlew installDebug
     ```
   - The app will install and open automatically on your phone.

4. **Grant Required Permissions on Your Phone:**
   - Open FocusCrew and follow the onboarding screen to grant **Accessibility Service** permission (needed to detect when a blocked app is opened).
   - *(Note: On Android 13+, sideloaded APKs may require tapping "Allow restricted settings" in your phone's App Info menu before accessibility can be enabled).*

---

## Documentation Index

- [01-product.md](file:///c:/cdrive files/OneDrive/Desktop/ANORIDE APP/docs/01-product.md) — Product vision, philosophy, and principles.
- [02-mvp-scope.md](file:///c:/cdrive files/OneDrive/Desktop/ANORIDE APP/docs/02-mvp-scope.md) — Stage 1 MVP features vs. postponed features.
- [03-architecture.md](file:///c:/cdrive files/OneDrive/Desktop/ANORIDE APP/docs/03-architecture.md) — Architecture layers, resilient timer math, and folder structure.
- [04-data-model.md](file:///c:/cdrive files/OneDrive/Desktop/ANORIDE APP/docs/04-data-model.md) — Room database tables and relationships.
- [05-permissions-privacy.md](file:///c:/cdrive files/OneDrive/Desktop/ANORIDE APP/docs/05-permissions-privacy.md) — Permission rationale, denial behavior, and privacy boundaries.
- [06-design-system.md](file:///c:/cdrive files/OneDrive/Desktop/ANORIDE APP/docs/06-design-system.md) — Visual design tokens, calm themes, typography, and accessibility.
- [07-test-plan.md](file:///c:/cdrive files/OneDrive/Desktop/ANORIDE APP/docs/07-test-plan.md) — Verification checklist for device testing.
- [08-decision-log.md](file:///c:/cdrive files/OneDrive/Desktop/ANORIDE APP/docs/08-decision-log.md) — Architectural and product decisions record.
- [09-defects.md](file:///c:/cdrive files/OneDrive/Desktop/ANORIDE APP/docs/09-defects.md) — Known bugs and issue tracking table.
