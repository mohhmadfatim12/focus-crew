# CLAUDE.md — Operating Rules for Claude Code & AI Agents

> [!CAUTION]
> # 🔒 CRITICAL DIRECTIVE: PERMANENT CODE FREEZE
> **DO NOT MODIFY, REFACTOR, REORGANIZE, OR TOUCH THE CORE DEFENSE ENGINES.**
> The following core security and DNS modules are **100% PRODUCTION VERIFIED & LOCKED**:
> 
> 1. `app/src/main/java/com/focuscrew/app/service/FocusCrewVpnService.kt`
> 2. `app/src/main/java/com/focuscrew/app/service/AntiPornDefenderManager.kt`
> 3. `app/src/main/java/com/focuscrew/app/service/BlockerAccessibilityService.kt`
> 4. `app/src/main/java/com/focuscrew/app/domain/FuzzyAdultContentDetector.kt`
> 5. `app/src/main/java/com/focuscrew/app/domain/AdultDomainBlocklist.kt`
> 
> **Why are these files locked?**
> - **Mandatory SafeSearch VIP Locking**: Permanently routes Google search to `forcesafesearch.google.com` (`216.239.38.120`), Bing to `strict.bing.com` (`204.79.197.220`), and DuckDuckGo to `safe.duckduckgo.com` (`52.142.124.215`).
> - **1000+ Adult Domain Blackholing**: Blocks porn domains instantly at DNS layer (`0.0.0.0`) in 0ms without battery drain.
> - **High-Speed Split Tunnel**: Uses `builder.allowBypass()` so all normal websites (GitHub, Wikipedia, Google, YouTube, news, apps) connect directly over physical Wi-Fi and 4G/5G at 100% native speed.
> - **Chrome History Exemption (`isBrowserHistoryScreen`)**: User can freely review Chrome history and tap "Clear browsing data" without being kicked out. Adult sites are strictly blocked at click/navigation time.
> - **DoH Bootstrap Whitelisting**: Resolves `dns.google` and `cloudflare-dns.com` so Chrome never throws `DNS_PROBE_FINISHED_BAD_SECURE_CONFIG`.
> 
> **ANY MODIFICATION TO THESE FILES IS STRICTLY FORBIDDEN WITHOUT EXPLICIT OPERATOR WRITTEN CONSENT SPECIFYING THE EXACT FILE NAME.**

---

## 1. Project Overview
- **App Name**: FocusCrew
- **Package Name**: `com.focuscrew.app`
- **Platform**: Android 8.0+ (API 26 to API 35)
- **Tech Stack**: 100% Pure Kotlin, Jetpack Compose (Material 3), Room DB, Jetpack DataStore, Coroutines & Flow.
- **Stage**: Sovereign on-device offline productivity & addiction blocker. No remote servers, no cloud logins, zero tracking.

---

## 2. Standard Development Commands
- **Build Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Run Unit Tests**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Clean Project**:
  ```bash
  ./gradlew clean
  ```
- **Automated CI/CD**: Every push to `main` triggers GitHub Actions (`.github/workflows/build-apk.yml`) to run unit tests and build the official production APK artifact.

---

## 3. Working Rules for Any Agent Working on this Repo
1. **Preserve Existing Working Features**: Never break or alter already completed and tested features.
2. **Step-by-Step Execution**: Make minimal, focused, atomic changes. Explain the rationale before editing.
3. **No Unrequested Dependencies**: Never add new Gradle dependencies or system libraries without prior permission.
4. **Android Safety**: Never block emergency services, phone dialers, or Android System Settings.
5. **No False Promises**: Do not claim the app is "unkillable" or "un-bypassable" — Android OS always permits uninstalling. Focus on calm, sovereign friction.
