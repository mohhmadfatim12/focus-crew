# 06. Design System & UI Specification: Old Money Quiet Luxury

This document details the visual identity, color tokens, typography scale, spacing grid, and horological components for **FocusCrew**.

---

## 1. Design Philosophy: "Old Money Quiet Luxury"

FocusCrew rejects cheap gamification, neon purple glowing buttons, aggressive red alarms, and generic digital slop. The design is grounded in **Savile Row restraint, horological precision, and archival craftsmanship**:
- **Atmospheric Depth:** Layered Obsidian British Racing Forest (`#0D110F`) and Dark Velvet Emerald (`#151B17`), avoiding harsh pitch-black (`#000000`).
- **Horological Precision:** Concentric 1px brass hairline borders (`rgba(197, 168, 128, 0.18)`), mechanical chronometer dials, and monolithic monospace countdown digits (`JetBrains Mono`).
- **Editorial Dignity:** Headlines set in high-contrast editorial serifs (`Fraunces` / `Newsreader` / `Cormorant`), paired with clean `Satoshi` body copy and `JetBrains Mono` telemetry hallmarks.
- **Stoic Friction:** High-consequence actions (the 30s friction gate and cooldown exit) use grave, dignified Imperial Oxblood Burgundy (`#3B131A` / `#230B0F`), treating the user with respect rather than panic.
- **Accessible Contrast:** Meets WCAG 2.1 AAA contrast ratios across all screen levels. Minimum touch targets remain **48dp × 48dp**.

---

## 2. Color Tokens (Dark Luxury Palette)

| Token Name | Hex Code | Purpose / Application | Visual Role |
|---|---|---|---|
| `surface` | `#0D110F` | Main atmospheric background (Obsidian Racing Forest). | Base canvas |
| `surfaceDim` | `#080B09` | Deep shadow canvas & bottom navigation dock. | Recessed floor |
| `surfaceContainer` | `#151B17` | Standard card containers & app list rows. | Elevated emerald velvet |
| `surfaceContainerHigh` | `#1C241F` | Prominent metric containers & active intention cards. | Focused container |
| `onSurface` | `#F5EFEB` | Monolithic countdown digits, primary headings, labels. | Cream silk / warm ivory |
| `onSurfaceVariant` | `#A39B8F` | Secondary descriptions, timestamps, metadata hallmarks. | Muted parchment |
| `primary` | `#C5A880` | Brushed Champagne Gold / Antique Brass; active dials, triggers. | Singular luxury accent |
| `onPrimary` | `#1A140A` | Text and icons inside solid brass action buttons. | High-contrast bronze ink |
| `primaryContainer` | `#2A2218` | Recessed gold card backgrounds, selected interval pills. | Burnished brass |
| `onPrimaryContainer`| `#DFC7A7` | Highlights and active badge text inside gold containers. | Pale gilt |
| `secondary` | `#1B3629` | British Racing Green; subtle category badges and status fills. | Archival accent |
| `tertiary` | `#3B131A` | Imperial Oxblood Burgundy; 30s friction gate & emergency exit. | Solemn friction |
| `onTertiary` | `#FCE8E6` | High-contrast text on oxblood surfaces. | Soft rose silk |
| `outline` | `#36423A` | Structural dividers and recessed search field strokes. | Aged patina border |
| `whisperGoldBorder`| `rgba(197,168,128,0.18)` | 1px hairline border framing all luxury cards and dials. | Horological rim |

---

## 3. Typographic Architecture

| Style Name | Font Family | Size | Weight | Line Height | Usage |
|---|---|---|---|---|---|
| **Display Timer** | `JetBrains Mono` | 58sp | Medium (500) | 64sp | Monolithic countdown digits (`50:00`, `00:23`). |
| **Headline Editorial** | `Fraunces` | 32sp | SemiBold (600) | 38sp | Screen titles ("The Distraction Vault", "The Fortress Protocol"). |
| **Title Serif** | `Fraunces` | 20sp | Medium (500) | 26sp | Card headers, contemplation quotes, dialog titles. |
| **Body Sans** | `Satoshi` | 15sp | Regular (400) | 24sp | Main narrative copy, whitelist descriptions, helper text. |
| **Body Sans Medium**| `Satoshi` | 14sp | Medium (500) | 20sp | App item names, interval button labels. |
| **Label Caps Gold** | `JetBrains Mono` | 11sp | SemiBold (600) | 14sp | Micro-hallmarks (`SESSION ENGINE // NO. 01`, `SECURITY LEVEL: FORTRESS`). |
| **Data Mono** | `JetBrains Mono` | 13sp | Regular (400) | 18sp | Telemetry readouts, deflection counters, timestamps. |

---

## 4. Key Component Archetypes (0 to 100 App Sections)

### 0. Brand Heritage & App Logo System
- Centered circular crest with mechanical pocket watch escapement, laurel wreath, and hourglass on pebbled leather.
- Carved brass banner with `FOCUSCREW` serif typography.
- 3 Archival App Icon Editions: Master Racing Green, Obsidian Midnight (`FC` Monogram), and Imperial Oxblood Friction.

### 0.5. Onboarding & Dopamine Neuro-Audit
- 5-stage sovereign focus deficit assessment with segmented progress bar.
- Real-time 10-year projection ledger (`14,600 Hours Lost`, `$182,500 Valuation`).
- Daily quota specification selector (`30m`, `45m`, `60m`).

### 1. The Executive Chronometer Dial (Home / Timer)
- Concentric circular gauge with 1px brass hairline ring (`rgba(197, 168, 128, 0.25)`).
- 60 perimeter minute ticks simulating bespoke pocket watch escapements.
- Ivory countdown digits (`50:00`) flanked by `IMMERSIVE FLOW ACTIVE` badge.

### 2. The Distraction Vault Cards (Blocker Screen)
- Dark velvet emerald cards with 12dp rounded corners and 1px whisper borders.
- Cameo icons in brass frames with real-time deflection counters (`42 Deflections`).
- Vintage knurled brass sliding pins for toggle switches.

### 3. The 30-Second Friction Interceptor (Friction Gate)
- Velvet oxblood & espresso gradient overlay (`#230B0F` to `#0D110F`).
- Stoic contemplation prompt: *"Is this momentary distraction worthy of the hour you cannot retrieve?"*
- Circular 30-second breathing dial in aged champagne gold.

### 4. The Focus Ledger (Analytics Screen)
- Archival leatherbound ledger layout.
- Horological weekly chronograph bar chart in champagne gold hairlines.
- Gold sealed wax marks (`PURE FOCUS`) next to completed sessions.

### 5. The Strict Mode Fortress (Settings & Safeguard Screen)
- Segmented brass pill selectors for friction cooldown (`15s`, `30s`, `60s`).
- Permanent Life Essentials Whitelist: Emergency 911/112, Phone Dialer, and System Settings marked with an unbreachable `IMMUTABLE` laurel badge.

### 5.5. Emergency Cooldown Confirmation Sheet (Friction Modal)
- Elevated dark velvet oxblood bottom sheet modal.
- Active flow invested and focus purity penalty calculation (`-3.8% Impact`).
- 5-second tactile contemplation slider in brushed brass track.

---

## 5. Google Stitch Project & Generated Screen Inventory
- **Stitch Project ID:** `7273476458734246792`
- **Project Title:** `FocusCrew - Old Money Luxury Design System`
- **Design System Asset:** `assets/55f3a2ee960342a7a500d5adc9d7cc5f`
- **Specification File:** [DESIGN.md](file:///c:/cdrive%20files/OneDrive/Desktop/ANORIDE%20APP/DESIGN.md)

| Screen Section | Google Stitch Screen ID | Screen Title |
|---|---|---|
| **Section 0** | `a6968252e98f445e9fdbf7c86ca7f67f` | FocusCrew Brand Heritage & Insignia |
| **Section 0.5** | `48b60185906e48b682195eab43c6d421` | Onboarding & Dopamine Neuro-Audit |
| **Section 1** | `0754c86a5ca248bf81cb5182c32d34fa` | The Executive Chronometer Dashboard |
| **Section 2** | `90574922ddb44070be06b7ddab0a74e9` | The Distraction Vault (App Blocker) |
| **Section 3** | `33d72fb3a5ac43eea567a4330972b599` | The Interceptor & Friction Gate |
| **Section 4** | `9fb0b68880c54aa8996620c752333a8a` | The Focus Ledger (Historical Chronology) |
| **Section 5** | `b9fe049a60ce4b06b98ba05d9bfdb65c` | The Fortress Protocol (Settings & Whitelist) |
| **Section 5.5** | `bbcdca00daf54b19a1ef7c0b5b41940b` | Emergency Cooldown Confirmation Sheet |
