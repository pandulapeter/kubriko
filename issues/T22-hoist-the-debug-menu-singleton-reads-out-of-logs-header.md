# Hoist the InternalDebugMenu and Logger reads out of LogsHeader and DebugMenuContents into DebugMenuContainer

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** tool-debug-menu (internal code only)
**Files:** tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/ui/LogsHeader.kt, tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/ui/DebugMenuContents.kt, tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/DebugMenuContainer.kt

## Problem
Two leaf Composables reach into singletons instead of taking state as parameters:
- `LogsHeader.kt` reads `InternalDebugMenu.isEditingFilter.collectAsState()` (l.69), `InternalDebugMenu.filter.collectAsState()`
  (l.83), passes `InternalDebugMenu::onFilterUpdated` (l.87) and `InternalDebugMenu::toggleIsEditingFilter` (l.101, 131), and
  `Logger.logs.collectAsState().value.isNotEmpty()` / `Logger::clearLogs` (l.134-137).
- `DebugMenuContents.kt` builds the same `LogsHeader(...)` call twice (l.96-105 and 142-150), each time collecting
  `InternalDebugMenu.isLowPriorityEnabled`, `isMediumPriorityEnabled`, `isHighPriorityEnabled` and `filter` and passing the three
  `InternalDebugMenu::on...PriorityToggled` references.
The code style allows singleton lookups only where the root wires things up; `DebugMenuContainer` is that root (it already
collects `InternalDebugMenu.metadata` and `logs`).

## Fix
Run after the OverlaySwitch and metadata-string plans (same file).
- `LogsHeader` takes plain state and callbacks: `modifier`, `isLowPriorityEnabled`, `onLowPriorityToggled`, `isMediumPriorityEnabled`,
  `onMediumPriorityToggled`, `isHighPriorityEnabled`, `onHighPriorityToggled`, `isEditingFilter: Boolean`,
  `onEditingFilterToggled: () -> Unit`, `filterText: String`, `onFilterTextChanged: (String) -> Unit`, `hasLogs: Boolean`,
  `onClearLogsClicked: () -> Unit`. `areFiltersApplied` is dropped as a parameter and computed inside as `filterText.isNotEmpty()`
  (exactly what both callers passed). `AnimatedContent(targetState = isEditingFilter, ...)`; the body otherwise unchanged, using
  the parameters in place of the singleton reads. Drop the `InternalDebugMenu`, `Logger` and `collectAsState` imports.
- `DebugMenuContents` gets a slot `logsHeader: @Composable (Modifier) -> Unit` in place of the two inline calls:
  `logsHeader(Modifier.padding(vertical = 4.dp))` in the horizontal Column and `logsHeader(Modifier)` inside
  `item("logsHeader")`. Drop its `InternalDebugMenu` and `collectAsState` imports.
- `DebugMenuContainer` passes
  `logsHeader = { modifier -> LogsHeader(modifier = modifier, isLowPriorityEnabled = InternalDebugMenu.isLowPriorityEnabled.collectAsState().value, onLowPriorityToggled = InternalDebugMenu::onLowPriorityToggled, ..., isEditingFilter = InternalDebugMenu.isEditingFilter.collectAsState().value, onEditingFilterToggled = InternalDebugMenu::toggleIsEditingFilter, filterText = InternalDebugMenu.filter.collectAsState().value, onFilterTextChanged = InternalDebugMenu::onFilterUpdated, hasLogs = Logger.logs.collectAsState().value.isNotEmpty(), onClearLogsClicked = Logger::clearLogs) }`.
  The slot is invoked at the same two places, so every `collectAsState` still runs in the same composition scope as today
  (inside the Column, or inside the lazy item).

Alternative considered: thread all thirteen values through `DebugMenuContents`' own parameters — more plumbing, and the
collections would move up into the container's scope (recomposing it on every log). The slot keeps the scopes unchanged.

## Behaviour
Same reads in the same scopes, same callbacks; rendered UI unchanged.

## Public API
None (internal).

## Tests
The existing ones.

## Verify
`./gradlew :tools:debug-menu:compileKotlinDesktop :tools:debug-menu:compileKotlinWasmJs :tools:debug-menu:desktopTest`

## Manual check
In the debug menu: toggle each priority filter, open the text filter and type, clear the logs (button disabled when empty) — in
both the horizontal and vertical layouts.
