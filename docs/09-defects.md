# 09. Defect & Bug Tracking Log

This document tracks all defects, anomalies, and edge-case bugs identified during development, testing, and real-device verification.

---

## Active Defect Register

| ID | Description | Steps to Reproduce | Root Cause | Status | Retest Verification |
|---|---|---|---|---|---|
| DEF-001 | Overlay window attachment desync on relaunch | Enable and disable overlay repeatedly | Blind boolean flag `isViewAttached` out of sync with actual WindowManager state | RESOLVED | Checked `overlayView?.isAttachedToWindow == true` and added clean destroy/recreate cycle. |
| DEF-002 | JSON export syntax corruption on multi-line text | Add distraction thought with newlines or quotes, then tap Export JSON | `exportFocusData` only replaced quotes; unescaped `\`, `\n`, `\r`, `\t` | RESOLVED | Added RFC-compliant `escapeJson` utility sanitizing all escape sequences. |
| DEF-003 | Reel Counter date rollover skipped-day error | Scroll reels on day 1, skip day 2, open on day 3 | `checkAndApplyDateRollover` copied today's count to yesterday regardless of elapsed days | RESOLVED | Calculated calendar day delta via date parsing; resets yesterday count to 0 if >1 day passed. |
| DEF-004 | In-memory stats desync during date rollover | Trigger date rollover while app is running | SharedPreferences was updated on rollover but in-memory `_stats.value` was not reset | RESOLVED | `_stats.value` is reset simultaneously with SharedPreferences rollover. |
| DEF-005 | Focus subject chips horizontal overflow | Open Focus Timer on screen narrower than 380dp or with high font scaling | 5 subject FilterChips placed in non-scrollable `Row` | RESOLVED | Wrapped `Row` in `.horizontalScroll(rememberScrollState())`. |
| DEF-006 | Race condition on immediate session stop / distraction note | Start session and immediately stop or add note within 50ms | Asynchronous `scope.launch(Dispatchers.IO)` left `activeSessionDbId` null | RESOLVED | Assigned `sessionStartJob` and called `sessionStartJob?.join()` before any DB operations. |
| DEF-007 | Stale blocked app label in CalmBlockActivity | Launch blocked app A, then without closing, launch blocked app B | SingleTop activity did not override `onNewIntent` or observe dynamic state | RESOLVED | Stored label in `mutableStateOf` and updated in `onNewIntent(intent)`. |
| DEF-008 | API 33+ PackageManager query deprecation | Query installed launcher apps on Android 13+ | `queryIntentActivities(intent, 0)` deprecated on API 33+ | RESOLVED | Added SDK_INT check using `PackageManager.ResolveInfoFlags.of(0L)`. |
| DEF-009 | Daylight savings / timezone epoch day drift | Compute streak across daylight saving time transition | Integer division `timeInMillis / 86400000L` sensitive to DST 23/25h days | RESOLVED | Replaced with `java.time.LocalDate.toEpochDay()`. |
| DEF-010 | Orphaned database session on pause reset | Start session, pause, tap Reset | `TimerManager.reset()` cleared `activeSessionDbId` without updating Room | RESOLVED | Awaits `sessionStartJob` and records `CANCELLED_EARLY` with elapsed duration. |
| DEF-011 | YouTube Shorts unindexed viewport rejection | Swipe Shorts on YouTube | Custom Litho/Elements container emits `fromIndex == -1` | RESOLVED | Dual-Engine classifier evaluates vertical geometry (`absDeltaY >= 40`) and excludes non-reel view classes. |
| DEF-012 | Instagram chat bubble false positive counting | Scroll DMs / Chat in Instagram | Vertical scrolling in chat triggered scroll events | RESOLVED | Strict viewport span invariant `toIndex - fromIndex <= 1`; chat lists with multiple bubbles are rejected. |
| DEF-013 | In-session rest break unlock threshold alignment | Run 20m timer, look for rest button at 16:00 remaining | Dynamic unlock threshold was previously fixed to 5m, delaying 20m sessions past 16:00 remaining | RESOLVED | Added custom 4m threshold for 16-20m sessions; unlocks at 16:00 remaining out of 20:00. |
| DEF-014 | MainActivity tab navigation desynchronization on debug intent delivery | Send `am start --ez start_preset_timer true` while on Reels tab | Missing `android:launchMode="singleTop"` on `MainActivity` and `rememberSaveable` state did not re-trigger on intent delivery | RESOLVED | Added `launchMode="singleTop"`, `navToken` state trigger, and `TimerManager.stop()` before preset sessions. Verified across all 5 test scenarios on physical device. |

---

## Defect Severity Guidelines

- **Critical (P0):** App crashes, timer data loss, inaccessible emergency exit, or failure to launch phone dialer/settings.
- **High (P1):** Blocker fails to trigger on selected apps, or countdown drifts more than 5 seconds after screen sleep.
- **Medium (P2):** UI alignment glitch on small screens, notification shade text formatting error.
- **Low (P3):** Minor animation hitch, cosmetic color contrast tweak.
