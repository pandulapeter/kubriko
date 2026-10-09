# Let the horizontal debug menu's left column scroll when its content is taller than the panel

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** all (mostly Android/iOS with a large font scale)
**Challenged:** sound
**Files:** `tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/ui/DebugMenuContents.kt`

Ships in `tool-debug-menu`. Internal only; no public API changes. Runs after plan 42 (same file).

## Problem

In the horizontal layout the left column of `DebugMenuContents` is a plain `Column` that cannot scroll:

```kotlin
if (!shouldUseVerticalLayout) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Bottom)),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        if (debugMenuMetadata != null) {
            Metadata(…)          // five lines of labelSmall (debug_metadata has four \n), padding 8 + 4 + 2 dp
            OverlaySwitch(…)     // 24 dp
            OverlaySwitch(…)     // 24 dp
        }
        logsHeader(Modifier.padding(vertical = 4.dp))   // 24 dp icons + 8 dp
    }
}
```

With Material 3's default `labelSmall` (16 sp line height) that is about 5 × 16 + 14 + 24 + 24 + 32 ≈ 174 dp at font scale
1.0. The panel is 160 dp tall in `DebugMenu.invoke` (`horizontalDebugMenuHeight = 160.dp`) and 180 dp in the simple
`Horizontal` overload the Showcase uses. So `invoke` already overflows at the default font scale, and at the 1.3–2.0
font scales Android and iOS offer for accessibility both do: `Column` gives the last children whatever height is left,
so the logs header — the priority filters, the filter field and the clear button — is squeezed or cut off and cannot be
reached. The vertical layout is not affected (its content is in the scrolling `LazyColumn`).

## Fix

Make the column scroll, while keeping the `SpaceEvenly` spread when everything fits:

```kotlin
if (!shouldUseVerticalLayout) {
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Bottom)),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            … unchanged children …
        }
    }
}
```

Inside `verticalScroll` the column has an unbounded maximum height, so `SpaceEvenly` alone would collapse to packed
content; `heightIn(min = maxHeight)` makes the column at least the viewport's height, which is what `SpaceEvenly`
distributes when the content fits, and lets it grow (and scroll) when it does not. Keep the bottom inset padding on the
outer box so the scroll viewport ends above the home indicator.

Not part of this plan: enlarging the 24 dp `LogsHeader` icons to 48 dp touch targets
(`minimumInteractiveComponentSize`). It would make the row twice as tall and worsen the overflow this plan fixes; it is a
separate design choice for the debug menu.

## Tests

None: layout inside a Composable; library test classpaths cannot lay out or draw.

## Manual check

Android phone in portrait with the system font size at the largest setting (and an iPhone with Larger Text), Showcase
with the debug menu enabled: open the debug menu — the bottom panel's left column scrolls, and the logs header (priority
toggles, filter, clear) can be scrolled into view and used. At the default font size the column looks as before (items
spread evenly). Repeat with a game using `DebugMenu(kubriko, isEnabled) { … }` (160 dp panel) at the default font size —
the logs header is fully visible after scrolling instead of being clipped.
