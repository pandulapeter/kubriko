# Keep the top safe-area inset out of the horizontal debug menu's log list

**Kind:** bug (platform edge case)  ·  **Severity:** medium  ·  **Platforms:** Android, iOS (edge-to-edge, portrait)
**Challenged:** sound
**Files:** `tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/ui/DebugMenuContents.kt`

Ships in `tool-debug-menu`. Internal only; no public API changes.

## Problem

`DebugMenuContents` pads its `LazyColumn` with the vertical window insets in both layouts:

```kotlin
LazyColumn(
    modifier = if (shouldUseVerticalLayout) Modifier else Modifier.weight(1f),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    contentPadding = windowInsets.only(WindowInsetsSides.Vertical).asPaddingValues().let {
        PaddingValues(
            start = it.calculateStartPadding(LocalLayoutDirection.current),
            top = it.calculateTopPadding() + 8.dp,
            …
            bottom = it.calculateBottomPadding() + 8.dp,
        )
    },
```

The vertical panel spans the full height on the right, so it needs both. The horizontal panel (`DebugMenu.Horizontal`,
and `invoke` in portrait) sits at the **bottom** of the screen: its `Box` is
`height(height + windowInsets.asPaddingValues().calculateBottomPadding())` and its left column already uses
`windowInsetsPadding(windowInsets.only(WindowInsetsSides.Bottom))`. Its top edge is nowhere near the status bar, yet the
log list still adds the status bar / Dynamic Island height (24–50 dp on Android edge-to-edge, ~47–59 pt on iPhones) as
top padding. In a 160 dp (`invoke`) or 180 dp (`Horizontal`) panel that is a third of the list area lost to blank space
above the first log line. The Showcase's compact UI uses `DebugMenu.Horizontal(…, windowInsets = windowInsets)` and shows
it on every phone in portrait.

## Fix

Pick the sides by layout:

```kotlin
contentPadding = windowInsets.only(
    if (shouldUseVerticalLayout) WindowInsetsSides.Vertical else WindowInsetsSides.Bottom,
).asPaddingValues().let { … unchanged … },
```

The start/end values computed from it stay 0 as before (neither side set includes horizontal sides).

## Tests

None: the change is a layout-flag ternary inside a Composable, and library test classpaths cannot lay out or draw.

## Manual check

Android or iOS phone, portrait, Showcase with the debug menu enabled (`showcase.isDebugMenuEnabled=true`): open a game
with the debug menu, open the menu — the log list in the bottom panel starts 8 dp below the panel's top edge instead of a
status-bar height lower, and the last line still clears the home indicator / gesture bar. Rotate to landscape on a tablet
or a wide layout — the vertical panel still keeps its first line below the status bar.
