# Report the desktop maximum refresh rate of the screen the window is on, and report again when it moves to another screen

**Kind:** bug (platform edge case)  ·  **Severity:** medium  ·  **Platforms:** Desktop (multi-monitor)
**Challenged:** sound
**Files:** `engine/src/desktopMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformEffects.desktop.kt`, `engine/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:engine`. No API change: it makes the desktop value match what the public
KDoc of `MetadataManager.maximumDisplayRefreshRate` already promises; single-monitor setups see no difference.

## Problem

```kotlin
DisposableEffect(Unit) {
    // The primary screen rather than the one the window happens to sit on: AWT reports the rate per
    // screen device, and a headless environment has none at all.
    val displayMode = runCatching { GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode }.getOrNull()
    currentOnMaximumDisplayRefreshRateChanged(displayMode?.refreshRate?.takeIf { it != DisplayMode.REFRESH_RATE_UNKNOWN }?.toFloat())
    onDispose { }
}
```

The desktop reports the *primary* screen once, on first composition. The public KDoc says otherwise:
`MetadataManager.maximumDisplayRefreshRate` — "The highest refresh rate the display showing the game can present
at … Follows the display, so it updates when the device unfolds or the window moves to another screen." A laptop
(60 Hz primary) driving a 144 Hz external monitor that shows the game reports 60, so a frame rate menu built from it
(as the KDoc recommends) offers the wrong targets; moving the window between screens never updates it.

Compose Multiplatform 1.12.1 has no public `LocalWindow`, but `ui-desktop-1.12.1` exposes
`androidx.compose.ui.awt.LocalAwtWindow: ProvidableCompositionLocal<java.awt.Window?>` (`@ExperimentalComposeUiApi`),
provided by `ComposeWindow`/`ComposeDialog` and by `ComposePanel` (its parent window), `null` in tests or an
`ImageComposeScene`. Checked in the sources jar under `~/.gradle/caches/.../ui-desktop/1.12.1/`
(`desktopMain/androidx/compose/ui/awt/LocalAwtWindow.kt`). Tesselar already uses it
(`TrackpadPinch.desktop.kt`), and the engine already opts in to `ExperimentalComposeUiApi` on the web
(`PointerIconExtensions.web.kt`). AWT fires a `"graphicsConfiguration"` property change on a component when it moves
to another screen device (`java.awt.Component.updateGraphicsData` calls
`firePropertyChange("graphicsConfiguration", …)` — verified in JBR 21's `java.awt.Component` bytecode).

## Fix

```kotlin
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun PlatformMaximumDisplayRefreshRateEffect(onMaximumDisplayRefreshRateChanged: (Float?) -> Unit) {
    val currentOnMaximumDisplayRefreshRateChanged by rememberUpdatedState(onMaximumDisplayRefreshRateChanged)
    val window = LocalAwtWindow.current
    DisposableEffect(window) {
        fun update() {
            val device = window?.graphicsConfiguration?.device
                ?: runCatching { GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice }.getOrNull()
            val refreshRate = device?.displayMode?.refreshRate   // wrap in runCatching, as today
            currentOnMaximumDisplayRefreshRateChanged(refreshRate?.takeIf { it != DisplayMode.REFRESH_RATE_UNKNOWN }?.toFloat())
        }
        val listener = PropertyChangeListener { update() }
        window?.addPropertyChangeListener("graphicsConfiguration", listener)
        update()
        onDispose { window?.removePropertyChangeListener("graphicsConfiguration", listener) }
    }
}
```

- The window's own screen device, falling back to the default screen when there is no window (tests, offscreen
  scenes) and to `null` when headless — the existing `runCatching` keeps covering `HeadlessException`.
- The listener runs on the EDT; `MetadataManagerImpl.updateMaximumDisplayRefreshRateInternal` only sets a
  `MutableStateFlow`, which is thread-safe.
- Optionally also listen to `componentMoved` and recompute only when `graphicsConfiguration.device` changed, if the
  manual check shows a platform/JDK that does not fire the property change (not expected on JBR 21).
- Replace the "primary screen rather than the one the window happens to sit on" comment, and in `engine/CLAUDE.md` →
  Tick Dispatch change "desktop: the primary screen's AWT display mode" to "desktop: the AWT display mode of the
  screen the window is on, re-read when AWT moves the window to another screen device".

## Tests

None: this is AWT/Compose window plumbing with no pure logic, and test classpaths have no display.

## Manual check

macOS or Windows with two monitors at different refresh rates. Nothing in this repository displays
`maximumDisplayRefreshRate`; use Tesselar's frame rate settings (`FrameRateThrottling.offeredEntriesFor`), or
temporarily log `metadataManager.maximumDisplayRefreshRate` from a Showcase example. With the window on each screen
in turn it shows that screen's rate and changes as the window is dragged across; also start the app with the window
on the non-primary screen.
