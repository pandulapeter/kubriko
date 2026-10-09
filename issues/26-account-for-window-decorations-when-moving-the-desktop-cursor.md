# Account for window decorations when moving the desktop cursor

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** Desktop (macOS, Windows; Linux skips cursor moves already)
**Challenged:** sound
**Files:** `plugins/pointer-input/src/commonMain/kotlin/com/pandulapeter/kubriko/pointerInput/PointerInputManagerImpl.kt`, `plugins/pointer-input/src/commonMain/kotlin/com/pandulapeter/kubriko/pointerInput/implementation/PlatformExtensions.kt`, `plugins/pointer-input/src/desktopMain/kotlin/com/pandulapeter/kubriko/pointerInput/implementation/PlatformExtensions.desktop.kt`, `plugins/pointer-input/src/androidMain/kotlin/com/pandulapeter/kubriko/pointerInput/implementation/PlatformExtensions.android.kt`, `plugins/pointer-input/src/iosMain/kotlin/com/pandulapeter/kubriko/pointerInput/implementation/PlatformExtensions.ios.kt`, `plugins/pointer-input/src/webMain/kotlin/com/pandulapeter/kubriko/pointerInput/implementation/PlatformExtensions.web.kt`, `plugins/pointer-input/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:plugin-pointer-input`. Internal route; no public API change (the engine's
`windowState` stays as it is, no window is exposed).

## Problem

`tryToMoveHoveringPointer` adds a root-relative offset to the window's **outer** position:

```kotlin
// PlatformExtensions.desktop.kt
val windowPosition = windowState.position          // the frame's top-left, title bar and borders included
...
val x = (windowPosition.x.value + offset.x * densityMultiplier).roundToInt()
val y = (windowPosition.y.value + offset.y * densityMultiplier).roundToInt()
```

while `offset` comes from `PointerInputManagerImpl` as `offset + viewportOffset.value`, where `viewportOffset` is
`coordinates.positionInRoot()` — relative to the **content** area. On a window with system decorations the cursor
lands one title bar too high (~28 pt on macOS, ~31 px on Windows) and, on Windows 10/11, ~7 px too far left (the
invisible resize border is part of the AWT frame bounds). Wallbreaker and Space Squadron re-centre the cursor every
time they start, so the paddle/ship jumps by that amount on any consumer's decorated window. The Showcase does not
show it because it extends its content into the title bar (`window.extendContentIntoTitleBar()` in
`KubrikoShowcaseWindow.kt`), which puts the Compose root at the frame's top-left.

## Fix

Measure the content's offset inside the frame at layout time, where Compose can convert to screen coordinates, and
keep adding the live `windowState.position` at call time (layout does not rerun when the window is merely moved):

1. `PointerInputManagerImpl.processModifier` (the layer-container branch that already records `viewportOffset` in
   `onGloballyPositioned`): also call `coordinates.positionOnScreen()` there and store
   `windowContentOffset = positionOnScreen - positionInRoot - windowOuterPositionInPixels()` in a
   `MutableStateFlow<Offset>` (or a `@Volatile var`) when every term is specified; leave the previous value otherwise.
   `windowOuterPositionInPixels(densityMultiplier)` is a new `internal expect fun` (desktop: `windowState.position`
   in Dp — AWT's logical units — divided by the manager's existing `densityMultiplier` (= 1 / density), with the
   same `UninitializedPropertyAccessException`/`isSpecified` guards `setPointerPosition` has; other platforms:
   `Offset.Unspecified`, which skips the store). On desktop `positionOnScreen()` is Compose's
   `container.locationOnScreen` × density (`PlatformWindowContext.desktop.kt`), and `onGloballyPositioned` runs on the
   AWT event thread that call requires.
2. Pass it through: `setPointerPosition(platform, offset + windowContentOffset, densityMultiplier)` in
   `tryToMoveHoveringPointer` (Android/iOS/Web actuals ignore it as today). The desktop formula itself stays
   `windowState.position + offset * densityMultiplier`.
3. The fullscreen/maximize transitions resize the content, so `onGloballyPositioned` re-measures; a window moved
   between monitors with different scales also relayouts. `windowContentOffset` starts at `Offset.Zero`, i.e. today's
   behaviour until the first layout.
4. `CLAUDE.md` "Cursor control": one sentence that the target is content-relative and the content's offset inside the
   frame is measured at layout time (`positionOnScreen` minus the window's outer position), so decorations and
   Windows' invisible borders are accounted for.

If `positionOnScreen()` returns `Offset.Unspecified` on desktop in practice (the window not showing yet), the store is
skipped and the next layout fills it.

## Tests

None: the inputs are AWT screen coordinates, which do not exist headlessly, and the arithmetic is a single subtraction.

## Manual check

A desktop app with a **system-decorated** window (e.g. temporarily run the Showcase with `extendContentIntoTitleBar`
removed, or any consumer `Window { … }`) on macOS and Windows: start Wallbreaker; the cursor must land exactly on the
paddle's centre, not a title bar above it. Move the window and restart the level: still exact. Repeat in fullscreen
and with the Showcase's own extended title bar (must stay exact there too).
