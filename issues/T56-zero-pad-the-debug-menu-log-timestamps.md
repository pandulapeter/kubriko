# Zero-pad the time in debug menu log entries

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** tool-debug-menu (internal code only; visible output changes)
**Files:** tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/ui/LogEntry.kt, tools/debug-menu/src/commonTest/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/ui/LogEntryTextTest.kt
**Rebased:** on 70de96c6 after the Now plans landed.

## Problem
`logEntryText` (`LogEntry.kt:49-56`, extracted by T19, landed in 68f6d5c7) formats the time at :51 as `"${it.hour}:${it.minute}:${it.second}.${it.nanosecond / 1_000_000}"`, so 09:05:03.007
shows as `[9:5:3.7]` — ambiguous (`.7` reads as 700 ms) and the columns jump as the time changes.

## Decision
- (a) **Pad to `HH:mm:ss.SSS` (recommended):** `hour/minute/second` with `padStart(2, '0')`, milliseconds with `padStart(3, '0')`.
- (b) Keep the current output.

## Fix
Change the timestamp expression in `logEntryText` and update the three expectations in `LogEntryTextTest` (:34, :39, :45; the
fixture is `3_723_004L` UTC): `"[1:2:3.4] m"` → `"[01:02:03.004] m"`, `"[1:2:3.4] Src: m"` → `"[01:02:03.004] Src: m"`.

## Behaviour
The debug menu's log lines show padded times; nothing else changes. The string is not public API.

## Public API
None.

## Tests
The updated `LogEntryTextTest`.

## Verify
`./gradlew :tools:debug-menu:desktopTest :tools:debug-menu:compileKotlinWasmJs`

## Manual check
Open the debug menu: timestamps line up.
