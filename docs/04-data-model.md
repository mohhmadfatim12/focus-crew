# 04. Data Model & Room Database Schema

This document defines the local Room database tables, field types, relationships, foreign keys, and indexes for FocusCrew.

---

## 1. Entity Relationship Diagram (ERD)

```
┌─────────────────────────┐           1:N          ┌─────────────────────────┐
│      BlockProfile       │ ─────────────────────► │       BlockedApp        │
│   (id, name, isDefault) │                        │ (id, profileId, pkgName)│
└─────────────────────────┘                        └─────────────────────────┘
             │
             │ 1:N
             ▼
┌─────────────────────────┐           1:N          ┌─────────────────────────┐
│         Session         │ ─────────────────────► │       BreakRecord       │
│  (id, profileId, ...)   │                        │(id, sessionId, start...)│
└─────────────────────────┘                        └─────────────────────────┘
             │
             │ 1:N
             ▼
┌─────────────────────────┐
│    DistractionEntry     │
│(id, sessionId, thought) │
└─────────────────────────┘

┌─────────────────────────┐                        ┌─────────────────────────┐
│          Goal           │                        │        EventLog         │
│  (id, type, targetMs)   │                        │ (id, eventType, meta)   │
└─────────────────────────┘                        └─────────────────────────┘
```

---

## 2. Table Specifications

### A. `Session` (Focus Session History)
Stores every started focus session and its outcome.

| Column | Type | Nullable | Description |
|---|---|---|---|
| `id` | `INTEGER` (Primary Key, Auto-increment) | No | Unique session ID. |
| `profileId` | `INTEGER` | Yes | Foreign key to `BlockProfile.id`. |
| `subject` | `TEXT` | No | Subject or tag name (e.g., "Math", "Reading", "General"). |
| `mode` | `TEXT` | No | Enum: `POMODORO`, `FOCUS_50`, `DEEP_90`, `CUSTOM`. |
| `plannedMs` | `INTEGER` | No | Planned duration in milliseconds (e.g., 1,500,000 for 25 min). |
| `startedAt` | `INTEGER` | No | Unix timestamp (epoch ms) when session started. |
| `endedAt` | `INTEGER` | Yes | Unix timestamp (epoch ms) when session ended (`NULL` if active). |
| `endReason` | `TEXT` | Yes | Enum: `COMPLETED`, `EMERGENCY_STOP`, `NATURAL_BREAK`, `CANCELLED_DURING_COOLDOWN`. |
| `strict` | `INTEGER` (Boolean) | No | `1` if strict mode was active, `0` otherwise. |
| `interruptionCount`| `INTEGER` | No | Number of times a blocked app was opened or emergency exit triggered. |

---

### B. `BlockProfile` (Block Configurations)
Named groupings of apps to block (e.g., "Study Mode", "Coding").

| Column | Type | Nullable | Description |
|---|---|---|---|
| `id` | `INTEGER` (Primary Key, Auto-increment) | No | Unique profile ID. |
| `name` | `TEXT` | No | User-facing profile name (e.g., "Deep Study"). |
| `colorHex` | `TEXT` | No | Accent color for UI representation (e.g., `#3B82F6`). |
| `isDefault` | `INTEGER` (Boolean) | No | `1` if this is the active default profile, `0` otherwise. |
| `createdAt` | `INTEGER` | No | Epoch timestamp of creation. |

---

### C. `BlockedApp` (Individual App in Profile)
Specific app package names assigned to a block profile.

| Column | Type | Nullable | Description |
|---|---|---|---|
| `id` | `INTEGER` (Primary Key, Auto-increment) | No | Unique record ID. |
| `profileId` | `INTEGER` | No | Foreign key referencing `BlockProfile(id)` (`ON DELETE CASCADE`). |
| `packageName` | `TEXT` | No | Android package identifier (e.g., `com.instagram.android`). |
| `appName` | `TEXT` | No | User-friendly app label (e.g., "Instagram"). |

*Index:* Composite index on `(profileId, packageName)` with unique constraint.

---

### D. `BreakRecord` (Session Breaks)
Tracks scheduled breaks taken during or attached to a session.

| Column | Type | Nullable | Description |
|---|---|---|---|
| `id` | `INTEGER` (Primary Key, Auto-increment) | No | Unique break ID. |
| `sessionId` | `INTEGER` | No | Foreign key referencing `Session(id)` (`ON DELETE CASCADE`). |
| `durationPlannedMs`| `INTEGER` | No | Planned break time (e.g., 600,000 ms for 10 min). |
| `startedAt` | `INTEGER` | No | Epoch timestamp when break began. |
| `endedAt` | `INTEGER` | Yes | Epoch timestamp when break ended. |
| `wasAutoRestored` | `INTEGER` (Boolean) | No | `1` if blocking auto-resumed upon break expiry, `0` if cancelled early. |

---

### E. `DistractionEntry` (Distraction Journal)
Quick thoughts or urges logged by the user during an active session to clear their mind without leaving focus mode.

| Column | Type | Nullable | Description |
|---|---|---|---|
| `id` | `INTEGER` (Primary Key, Auto-increment) | No | Unique entry ID. |
| `sessionId` | `INTEGER` | No | Foreign key referencing `Session(id)` (`ON DELETE CASCADE`). |
| `thoughtText` | `TEXT` | No | The user's note (e.g., "Check movie release date"). |
| `timestamp` | `INTEGER` | No | Epoch timestamp when logged. |
| `isResolved` | `INTEGER` (Boolean) | No | `1` if reviewed after session, `0` otherwise. |

---

### F. `Goal` (Daily / Weekly Targets)
Simple user-defined targets to help maintain consistency.

| Column | Type | Nullable | Description |
|---|---|---|---|
| `id` | `INTEGER` (Primary Key, Auto-increment) | No | Unique goal ID. |
| `targetType` | `TEXT` | No | Enum: `DAILY_MINUTES`, `WEEKLY_MINUTES`, `SESSIONS_PER_DAY`. |
| `targetValue` | `INTEGER` | No | Numerical target (e.g., 120 for 120 daily focus minutes). |
| `isActive` | `INTEGER` (Boolean) | No | `1` if currently tracked, `0` otherwise. |
| `createdAt` | `INTEGER` | No | Creation timestamp. |

---

### G. `EventLog` (Audit & Diagnostic Trail)
Tracks system events to ensure complete honesty when blocking does not work or system events occur.

| Column | Type | Nullable | Description |
|---|---|---|---|
| `id` | `INTEGER` (Primary Key, Auto-increment) | No | Unique log ID. |
| `timestamp` | `INTEGER` | No | Epoch timestamp of event. |
| `eventType` | `TEXT` | No | Enum: `ACCESSIBILITY_DISCONNECTED`, `ACCESSIBILITY_CONNECTED`, `DEVICE_BOOT_DETECTED`, `EMERGENCY_EXIT_TRIGGERED`, `BLOCK_OVERLAY_DISPLAYED`. |
| `metadata` | `TEXT` | Yes | Optional JSON details (e.g., `{"blockedPackage": "com.twitter.android"}`). |

---

## 3. Data Integrity & Migration Strategy

- **Foreign Key Constraints:** Foreign keys are strictly enforced in SQLite (`PRAGMA foreign_keys = ON;`). When a `BlockProfile` is deleted, its associated `BlockedApp` rows are automatically deleted (`CASCADE`).
- **Exporting Data:** A Room DAO query packages all tables into a structured, human-readable JSON file for backup and portability.
