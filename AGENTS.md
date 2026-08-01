# AGENTS.md — Working Rules for AI Agents

Welcome to **FocusCrew** (provisional name). This document sets the operating rules that every AI agent and contributor must follow when working on this repository.

---

## 1. Core Engineering Rules & Tech Stack

- **Tech Stack:**
  - Language: Pure Kotlin
  - UI: Jetpack Compose (Material 3)
  - Database: Room (SQLite)
  - Preferences & Key-Value State: Jetpack DataStore (Preferences / Proto)
  - Target: Native Android only. No hybrid frameworks (no Flutter, React Native, etc.).
- **Dependency Policy:**
  - Do **not** add new libraries, Gradle plugins, or third-party dependencies without asking the user first and explaining why each library is strictly necessary.
- **Stage 1 (Offline Only):**
  - Strictly no network calls, no HTTP clients (no Retrofit/Ktor), no remote server code, no user accounts/logins, no third-party analytics (no Firebase/Mixpanel), and zero ads.
- **Permission Safety:**
  - Never add, edit, or remove Android Manifest permissions without informing the user and explaining the rationale in plain language first.
- **Privacy & Screen Data Boundary:**
  - **Never** read, scrape, inspect, or log text or view hierarchies from other apps' screens.
  - The accessibility service is strictly limited to reading the **package name** (`packageName`) of the current foreground app to determine if it is on the block list.
- **System Safety & Life Essentials:**
  - Never block phone calls, emergency services (e.g., 911 / 112), default phone dialers, critical System UI, or Android Settings. The user must always be able to make emergency calls and access phone settings.
- **No Unbreakable Claims:**
  - Do not claim or document that the app is "unbreakable", "unkillable", "un-bypassable", or "uninstall-proof". Android OS intentionally allows users to uninstall apps and kill services. We provide calm friction and honesty about Android OS boundaries, not fake security promises.

---

## 2. Architecture & Code Quality Directives

- **Architecture Pattern:**
  - UI (Compose Screens & Composables) → ViewModels → Domain (Pure Kotlin use-cases and state machines) → Data (Room DAOs, DataStore, Repositories).
- **Pure Kotlin Business Logic:**
  - Keep the core session timer, state transitions, and strict-mode friction state machines in pure Kotlin classes without Android framework dependencies (`android.*`).
  - All core state machines must have comprehensive, deterministic unit tests.
- **Honest Test Reporting:**
  - Never mark a test as "PASSED" unless it was genuinely executed and passed.
  - Mark unrun tests or planned test cases explicitly as `NOT RUN`.
- **Step-by-Step Delivery:**
  - Work on one small, manageable task at a time.
  - Explain every change in clear, simple English before and after making it.
- **Real-Device Verification Instructions:**
  - After completing any task, provide simple, numbered, step-by-step instructions on how the user can build, deploy, and verify the feature on their physical Android device.
- **Decision Tracking:**
  - Whenever an architectural, technical, or UX decision is made or changed, immediately log it in [08-decision-log.md](file:///c:/cdrive files/OneDrive/Desktop/ANORIDE APP/docs/08-decision-log.md).
