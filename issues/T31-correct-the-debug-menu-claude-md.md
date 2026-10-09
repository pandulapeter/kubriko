# Correct the panel sizes, DebugMenuContainer's role and the log-viewer data flow in tools/debug-menu/CLAUDE.md

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** none (docs)
**Files:** tools/debug-menu/CLAUDE.md

## Problem
At 2480325f:
- "Vertical panel width: 192 dp; Horizontal panel height: 160 dp" holds only for `invoke`. The simple `Horizontal(...)` overload
  defaults to 180 dp (`DebugMenuContract.kt`: `height = 180.dp`), `Vertical(...)` to 192 dp.
- "`DebugMenuContainer.kt` — Composable layout switching between Horizontal/Vertical panels": the switching happens in
  `DebugMenu.invoke` (`BoxWithConstraints`, `maxWidth < maxHeight`); `DebugMenuContainer` registers the game Kubriko with
  `InternalDebugMenu` and draws the panel `Surface` around `ui/DebugMenuContents`.
- "Log Viewer: Reads directly from `Logger.logs`": the list shows `InternalDebugMenu.logs`, a `combine` of `Logger.logs` with the three
  importance toggles and the text filter.
- The `ui/` files (`DebugMenuContents`, `LogsHeader`, `LogEntry`) are not listed.

## Fix
Run after the debug-menu code plans (OverlaySwitch merge, metadata string, singleton hoist, LogEntry extraction) so the text
describes the landed structure. Update the four points above: list both size sets; describe `DebugMenuContainer` as the panel
root that collects `InternalDebugMenu` state and passes it down (including the `logsHeader` slot, if the hoist plan landed);
describe the log data flow (`InternalDebugMenu.logs`; `LogEntry` formats via `logEntryText`/`sourceHue`, if that plan landed); add
one Key Files line for `ui/`. Keep it terse.

## Behaviour
Docs only.

## Public API
None.

## Tests
None.

## Verify
None (docs).

## Manual check
None.
