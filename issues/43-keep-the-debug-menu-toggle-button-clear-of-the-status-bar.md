# Keep the debug menu's toggle button clear of the status bar and notch when it is placed by `DebugMenu(...)`

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** Android, iOS (edge-to-edge)
**Challenged:** sound
**Files:** `tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/DebugMenu.kt`,
`tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/DebugMenuOverlay.kt` (new),
`tools/debug-menu/CLAUDE.md`; only under option B of the decision also
`tools/debug-menu-api/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/DebugMenuContract.kt`,
`tools/debug-menu-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/DebugMenu.kt`,
`tools/debug-menu-api/CLAUDE.md`, `tools/debug-menu-noop/CLAUDE.md`, `tools/debug-menu/README.md`

Ships in `tool-debug-menu` (and, under option B, `tool-debug-menu-api` and `tool-debug-menu-noop`).

## Problem

The toggle button of `DebugMenu.OverlayOnly` is placed with a fixed margin and no insets:

```kotlin
if (buttonAlignment != null) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        …
        FloatingActionButton(
            modifier = Modifier.size(40.dp).align(buttonAlignment),
```

`OverlayOnly` has no `windowInsets` parameter, and `DebugMenu.invoke` — the documented "recommended way", which does
take `windowInsets` (default `WindowInsets.safeDrawing`) — calls `OverlayOnly(...)` without passing them on. With the
default `buttonAlignment = Alignment.TopStart`, an edge-to-edge Android app or any iPhone with a notch / Dynamic Island
gets the 40 dp button 16 dp from the screen's top edge: under the status bar icons (Android, where taps on the status
bar area can be intercepted by the system) or partly behind the camera cutout (iOS in landscape, left edge). The panels
next to it do honour the same `windowInsets`.

Neither the Showcase (`buttonAlignment = null`) nor the scene editor (desktop, zero insets) shows this; it hits external
games that use `DebugMenu(kubriko, isEnabled) { … }` on mobile.

## Fix

1. Move `OverlayOnly`'s body verbatim into a new `internal` Composable
   `DebugMenuOverlay(modifier, kubriko, kubrikoViewport, buttonAlignment, windowInsets: WindowInsets)` in
   `implementation/DebugMenuOverlay.kt` (one file per Composable), changing only the button container to:

   ```kotlin
   Box(
       modifier = Modifier
           .fillMaxSize()
           .windowInsetsPadding(windowInsets)
           .padding(16.dp),
   )
   ```

   (`windowInsetsPadding` here only affects the button's box; the game viewport and overlay viewport above it stay
   full-size.)

2. `DebugMenu.invoke` calls `DebugMenuOverlay(..., windowInsets = windowInsets)` instead of `OverlayOnly(...)`. The full
   `windowInsets` are applied, not just the sides that touch the screen edge: when a panel is open on the right or at the
   bottom, a button aligned to that side sits one inset further from the panel than strictly necessary, but it never
   jumps while the panels animate in and out, and the default `TopStart` alignment only uses the top and left sides,
   which always touch the screen edge.

3. The public `OverlayOnly` override calls `DebugMenuOverlay(..., windowInsets = WindowInsets(0, 0, 0, 0))`, so its
   behaviour is unchanged under the recommended option.

4. `tools/debug-menu/CLAUDE.md`: note that `invoke` keeps the toggle button inside its `windowInsets` and that
   `OverlayOnly` uses no insets (or, under option B, its new parameter).

## Decision

Whether the public `OverlayOnly` should also keep its button clear of the insets. Changing it is a behaviour change of a
published API (`tool-debug-menu-api`'s `DebugMenuContract`, implemented by `tool-debug-menu` and `tool-debug-menu-noop`),
so it is not part of the fix above.

- **A (recommended for this sweep): leave `OverlayOnly` as it is.** Only `invoke` changes, which is a fix of its
  documented `windowInsets` parameter. A consumer calling `OverlayOnly` directly keeps today's placement.
- **B: add a `windowInsets: WindowInsets` parameter to `OverlayOnly`.** Add a new detailed overload
  `OverlayOnly(modifier, kubriko, kubrikoViewport, buttonAlignment, windowInsets)` to `DebugMenuContract` with KDoc,
  implement it in `debug-menu` (calls `DebugMenuOverlay`) and `debug-menu-noop` (renders `kubrikoViewport()` in a
  `Box(modifier)` like the existing one), and keep the existing four-parameter overloads delegating with
  `WindowInsets(0, 0, 0, 0)` so source and binary callers keep their behaviour. Additive, but a new interface member:
  any third-party implementation of `DebugMenuContract` (none known) would have to implement it unless it gets a default
  body in the interface (it can: `= OverlayOnly(modifier, kubriko, kubrikoViewport, buttonAlignment)`).
- **C: make the existing `OverlayOnly` apply `WindowInsets.safeDrawing` internally.** Smallest diff, but moves the
  button for every existing consumer and double-pads one that already insets its own content; not recommended.

## Tests

None: placement inside a Composable; library test classpaths cannot lay out or draw.

## Manual check

Android phone (edge-to-edge) and an iPhone with a notch/Dynamic Island: a game wrapped in
`DebugMenu(kubriko = kubriko, isEnabled = true) { KubrikoViewport(...) }` (for instance temporarily in an example, not
committed) shows the toggle button below the status bar in portrait and clear of the cutout in landscape; toggling the
menu opens the panels and the button does not move. On Desktop the button sits 16 dp from the corner as before.
