# Restore the Windows window's position through the window state when leaving full screen

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** Desktop (Windows)
**Files:** `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseApp.kt`, `app/desktop/CLAUDE.md`

## Problem

On Windows the Showcase window is recreated when full screen is toggled (`KubrikoShowcaseApp.kt`, ~line 144):

```kotlin
        if (isRunningOnWindows) {
            key(isInFullscreenMode.value) {
                KubrikoShowcaseWindow(
                    undecorated = isInFullscreenMode.value,
                    resizable = !isInFullscreenMode.value,
                )
            }
```

The restore path in `onFullscreenModeToggled` (~line 100) runs synchronously inside the click handler of the *old*
(full screen) window and restores the location through that window object:

```kotlin
                                if (currentValue) {
                                    previousWindowPlacement.value?.let { previousWindowPlacement ->
                                        windowState.placement = previousWindowPlacement
                                        windowState.size = windowSize.value
                                        previousWindowLocation.value?.let {
                                            window.setLocation(it.x, it.y)
                                        }
                                        previousBounds.value?.let {
                                            coroutineScope.launch {
                                                delay(100)
                                                window.bounds = it
                                            }
                                        }
                                    }
```

`isInFullscreenMode.value = !currentValue` has already flipped the `key`, so on the next recomposition that `window` is
disposed and a new `ComposeWindow` is created from `windowState`. `setLocation` and the delayed `bounds` land on the
disposed window and are lost. The new window takes its position from `windowState.position`, which Compose's
`componentMoved` listener (`SwingWindow.desktop.kt` in Compose 1.12.1) overwrote with the full screen window's
origin while it was on screen. Size is restored (it goes through `windowState.size`), position is not: after leaving
full screen the window jumps to the top-left corner of the monitor it was full screen on (on a multi-monitor setup,
possibly not the monitor it started on).

macOS and Linux keep the same window, so their `setLocation`/`bounds` restore works and is out of scope.

Drop this plan if, on Windows 11 at the reviewed commit, the window already returns to its previous position.

## Fix

Restore the position the same way the size already is — through `windowState`, which the recreated window reads:

1. Add `val previousWindowPosition = remember { mutableStateOf<WindowPosition?>(null) }` next to the other
   `previous…` states.
2. When entering full screen (the `else` branch), also save `previousWindowPosition.value = windowState.position`.
3. When leaving, inside `previousWindowPlacement.value?.let { … }`: on Windows set
   `previousWindowPosition.value?.let { windowState.position = it }` (before or after `windowState.size`, both happen
   before the recomposition that creates the new window) and skip the `window.setLocation` / delayed `window.bounds`
   calls, which only make sense for a window that survives the toggle. Keep those two calls for macOS/Linux
   (`if (isRunningOnWindows) … else …`), since they were added for macOS's native full screen exit and changing them is
   not needed for this bug.

Remove any import this leaves unused. Update the "Fullscreen handling" paragraph of `app/desktop/CLAUDE.md` that
says the previous location and `window.bounds` are saved: on Windows the position is restored through
`windowState.position`, because the window is recreated; the `setLocation`/`bounds` + 100 ms delay restore applies to
macOS/Linux only.

## Tests

None: window placement is AWT behaviour in the desktop app module, which has no test source set.

## Manual check

Windows 11 (ideally with two monitors), `./gradlew :app:desktop:run`:

1. Move the window away from the top-left corner (e.g. the middle of the secondary monitor) and resize it.
2. Open Wallbreaker, open its menu overlay, press the full screen button, then press it again.
3. Expected: the window comes back at the same position, on the same monitor, with the same size. Before the fix it
   reappears at the monitor's top-left corner.
4. Repeat starting from a maximized window: it returns maximized.
