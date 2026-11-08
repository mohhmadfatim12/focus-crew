# 🔒 LOCKED & FROZEN MODULES — DO NOT MODIFY

**Status:** PRODUCTION STABLE & LOCKED  
**Tags:** `v1.0.1` / `v1.0.0-stable`  
**Verified On:** Physical Android Device (User 0)  
**Binary SHA-256:** `969d609cc380b8e2936a6b00a90b8882e5b566e0b4d9e19e46f52253de933d16`  
**Last Updated:** 2026-11-08  

---

## Strict Rule for AI Agents & Developers (Claude Code, Antigravity, Cursor, Copilot)

The core security, SafeSearch enforcement, and DNS filtering modules listed below are **100% PRODUCTION VERIFIED & STRICTLY LOCKED**. They have been calibrated on real physical Android devices to guarantee:
1. **Mandatory SafeSearch VIP Locking**: Google (`216.239.38.120`), Bing (`204.79.197.220`), DuckDuckGo (`52.142.124.215`).
2. **1000+ Adult Domain Blackholing**: Instant DNS blackhole to `0.0.0.0` in 0ms.
3. **High-Speed Split Tunnel (`allowBypass`)**: Full physical speed on Wi-Fi and 4G/5G for all normal websites (GitHub, Wikipedia, Google, YouTube, news, apps).
4. **Chrome History Exemption (`isBrowserHistoryScreen`)**: User can freely review Chrome history and clear browsing data without getting kicked out. Adult sites are strictly blocked at click/navigation time.
5. **DoH Whitelisting**: Resolves `dns.google` and `cloudflare-dns.com` so Chrome never throws `DNS_PROBE_FINISHED_BAD_SECURE_CONFIG`.

> [!CAUTION]
> **DO NOT EDIT, REFACTOR, REORGANIZE, OR TOUCH THESE FILES.**
> Any modifications to these files are strictly prohibited unless the operator explicitly requests a change to them by name and provides written confirmation.

---

## 1. Frozen Files & Core Responsibilities

### 1. `FocusCrewVpnService.kt`
- **Path:** `app/src/main/java/com/focuscrew/app/service/FocusCrewVpnService.kt`
- **Responsibilities:**
  - Strict browser split-tunnel routing with `builder.allowBypass()`.
  - DNS Port 53 query interceptor routing exclusively to local virtual endpoint (`10.245.0.2` / `fd00:fc::2`).
  - Google SafeSearch VIP routing (`forcesafesearch.google.com` / `216.239.38.120`).
  - Bing VIP routing (`strict.bing.com` / `204.79.197.220`).
  - DuckDuckGo VIP routing (`safe.duckduckgo.com` / `52.142.124.215`).
  - Whitelist bypass for Tier-1 Anycast DoH resolvers and educational CDNs.

### 2. `AntiPornDefenderManager.kt`
- **Path:** `app/src/main/java/com/focuscrew/app/service/AntiPornDefenderManager.kt`
- **Responsibilities:**
  - Acyclic non-browser vs search/browser package resolution (strictly acyclic, zero recursion).
  - Search query context detection in URL bars and omniboxes.
  - SafeSearch settings page tamper detection and proactive `[ Filter ]` chip locking.
  - `isBrowserHistoryScreen(currentUrl, rootNode)`: History list exemption for Chrome and Chromium browsers.
  - `isLocalOrExemptUrl`: Exemption for internal browser schemes (`chrome://`, `chrome-native://`, `about:`, `edge://`, `brave://`).

### 3. `BlockerAccessibilityService.kt`
- **Path:** `app/src/main/java/com/focuscrew/app/service/BlockerAccessibilityService.kt`
- **Responsibilities:**
  - Accessibility event processing for browsers and search engines.
  - Fallback window hierarchy traversal (`getActiveBrowserRootNode(event)`).
  - Nuclear fallback (`forceExitSafeSearchPage`) on SafeSearch tampering attempts.
  - Immediate return on `isBrowserHistoryScreen` to protect history management and clearing browsing data.

### 4. `FuzzyAdultContentDetector.kt`
- **Path:** `app/src/main/java/com/focuscrew/app/domain/FuzzyAdultContentDetector.kt`
- **Responsibilities:**
  - 4-layer offline detection (exact, leetspeak/homoglyph, phonetic, adjacent compound).
  - Adjacent-only compound matching (`part1 + part2`) to guarantee zero cross-sentence false positives.
  - Innocent words allowlist.

### 5. `AdultDomainBlocklist.kt`
- **Path:** `app/src/main/java/com/focuscrew/app/domain/AdultDomainBlocklist.kt`
- **Responsibilities:**
  - Fast-lookup hash set of 1,000+ verified adult domains.

---

## 2. Release & Verification Proof
- **Official GitHub Releases:**
  - [FocusCrew v1.0.1](https://github.com/mohhmadfatim12/focus-crew/releases/tag/v1.0.1)
  - [FocusCrew v1.0.0 Stable](https://github.com/mohhmadfatim12/focus-crew/releases/tag/v1.0.0-stable)
- **Production Binary SHA-256:**
  `969d609cc380b8e2936a6b00a90b8882e5b566e0b4d9e19e46f52253de933d16`
