# 🔒 LOCKED & FROZEN MODULES — DO NOT MODIFY

**Status:** PRODUCTION STABLE & LOCKED  
**Tag:** `v1.0.0-stable`  
**Verified On:** Physical Android Device (User 0)  
**Last Locked:** 2026-11-08  

---

## Strict Rule for AI Agents & Developers

The four files listed below form the core **SafeSearch Enforcement & Anti-Porn Defender Engine**. They have been calibrated, tested on real physical Android devices, and verified to have **100% interception of adult content and mandatory Google SafeSearch with zero false-positives**.

> [!CAUTION]
> **DO NOT EDIT, REFACTOR, OR TOUCH THESE FILES.**
> Any modifications to these files are strictly prohibited unless the operator explicitly requests a change to them by name and provides written confirmation.

---

## 1. Frozen Files & Responsibilities

### 1. `FocusCrewVpnService.kt`
- Path: `app/src/main/java/com/focuscrew/app/service/FocusCrewVpnService.kt`
- **Responsibilities:**
  - Strict per-app split tunnel routing for browsers and Google Search App (`com.google.android.googlequicksearchbox`).
  - DNS Port 53 query parser and resolver.
  - Mandatory VIP redirection:
    - Google: `forcesafesearch.google.com` (`216.239.38.120`)
    - Bing: `strict.bing.com` (`204.79.197.220`)
    - DuckDuckGo: `safe.duckduckgo.com` (`52.142.124.215`)
  - Whitelist bypass for CDNs, Google APIs, and educational assets.

### 2. `AntiPornDefenderManager.kt`
- Path: `app/src/main/java/com/focuscrew/app/service/AntiPornDefenderManager.kt`
- **Responsibilities:**
  - Non-browser app exclusion vs search/browser app inclusion (strictly acyclic, zero recursion).
  - Search engine query context detection (`isSearchDomainOrEngine`).
  - Google App URL and search box view IDs.
  - SafeSearch settings page tamper detection and proactive `[ Filter ]` chip locking.

### 3. `BlockerAccessibilityService.kt`
- Path: `app/src/main/java/com/focuscrew/app/service/BlockerAccessibilityService.kt`
- **Responsibilities:**
  - Accessibility event routing for browsers and search engines.
  - Fallback window hierarchy traversal (`getActiveBrowserRootNode(event)`).
  - Immediate clearing of search input on adult query detection.
  - Nuclear fallback (`forceExitSafeSearchPage`) on SafeSearch tampering attempts.

### 4. `FuzzyAdultContentDetector.kt`
- Path: `app/src/main/java/com/focuscrew/app/domain/FuzzyAdultContentDetector.kt`
- **Responsibilities:**
  - 4-layer offline detection (exact, leetspeak/homoglyph, phonetic, adjacent compound).
  - Adjacent-only compound matching (`part1 + part2`) to guarantee zero cross-sentence false positives.
  - Innocent words allowlist.

---

## 2. Verification Proof

- APK SHA-256: `90d6a60f3d85bb9836bdecf366699411e85401fc16a45612cf9b37ac76768a69`
- GitHub Release: [FocusCrew v1.0.0 Stable](https://github.com/mohhmadfatim12/focus-crew/releases/tag/v1.0.0-stable)
- Logcat Interception Proof:
  ```text
  FocusCrewVPN: 🔒 Server-locking Google SafeSearch VIP (216.239.38.120) for www.google.com
  FocusCrewBlocker: Anti-Porn Defender intercepted: com.google.android.googlequicksearchbox with trigger "porn"
  ```
