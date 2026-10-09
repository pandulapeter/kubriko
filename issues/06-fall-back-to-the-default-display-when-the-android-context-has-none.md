# Fall back to the default display instead of crashing when the viewport's Android context is not associated with one

**Kind:** bug (crash, platform edge case)  ·  **Severity:** low  ·  **Platforms:** Android 11+ (API 30+)
**Challenged:** sound
**Files:** `engine/src/androidMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformEffects.android.kt`

Ships in `io.github.pandulapeter.kubriko:engine`. No API change.

## Problem

```kotlin
private fun Context.findDisplay(): Display? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else @Suppress("DEPRECATION") findActivity()?.windowManager?.defaultDisplay
```

`PlatformMaximumDisplayRefreshRateEffect` calls it on `LocalContext.current` on first composition (and from its
`DisplayListener`). On API 30+ `Context.getDisplay()` **throws `UnsupportedOperationException`** for a context that
is not associated with a display — a `Service` context, `Application` context, or a `ContextWrapper` around one. A
`KubrikoViewport` hosted in a `ComposeView` whose context is such a context — a `DreamService` (screensaver), a
live-wallpaper/overlay window added through `WindowManager` from a `Service`, an Android Auto/Presentation-like host
built on a non-UI context — crashes on first composition. (Before API 30 the `findActivity()` branch just returns
`null`.) `PlatformFrameRateHint` is not affected: it returns early when `findActivity()` is `null`, and
`Window.findDisplayMode` reads `Window.context`, which is the Activity.

## Fix

```kotlin
private fun Context.findDisplay(): Display? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            display
        } catch (_: UnsupportedOperationException) {
            (getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)?.getDisplay(Display.DEFAULT_DISPLAY)
        }
    } else {
        @Suppress("DEPRECATION") findActivity()?.windowManager?.defaultDisplay
    }
```

Catch only `UnsupportedOperationException` (what `Context.getDisplay()` documents), not everything. The default
display is the right fallback: such hosts draw on it in practice, and the effect only reports the panel's ceiling.
(An alternative is `findActivity()?.display ?: DisplayManager default`, which skips the exception path for Activity
contexts but misses a `WindowContext`, which does have a display; the try/catch keeps both.)

## Tests

None: Android framework behaviour; the engine's tests run on the desktop JVM.

## Manual check

On an API 30+ device or emulator, host a `KubrikoViewport` in a `ComposeView` attached to a non-Activity context —
the quickest is a `DreamService` (Settings → Display → Screen saver) or an overlay window from a foreground
`Service` (`TYPE_APPLICATION_OVERLAY`, with the overlay permission granted): it renders instead of crashing with
`UnsupportedOperationException: Tried to obtain display from a Context not associated with one`. The Showcase itself
is unaffected either way.
