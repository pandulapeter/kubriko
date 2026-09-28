# Keep the desktop window's content and its title bar listener alive across full screen toggles

**Challenged:** sound

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** Desktop (macOS; Windows marginally, see below)
**Files:** `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/TitleBar.kt`,
`app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseApp.kt`, `app/desktop/CLAUDE.md`

## Problem

`TitleBarInsets` (`TitleBar.kt`, ~line 137) calls its `content` from two different places in the composition:

```kotlin
    if (titleBar == null || isFullscreen) return content()
    ...
    CompositionLocalProvider(LocalPlatformWindowInsets provides insets, content = content)
```

Compose identifies a subtree by where it is called, so whenever `isFullscreen`
(`windowState.placement == WindowPlacement.Fullscreen`, passed from `KubrikoShowcaseApp.kt`) flips on a window that has
an `ExtendedTitleBar` (macOS and windowed Windows with the JetBrains Runtime), the whole `content` subtree is disposed
and composed again from scratch. That subtree (`KubrikoShowcaseApp.kt`, ~line 92) holds both
`TitleBarAppearance` and the entire `KubrikoShowcase` UI:

```kotlin
                TitleBarInsets(
                    titleBar = titleBar,
                    isFullscreen = windowState.placement == WindowPlacement.Fullscreen,
                ) {
                    titleBar?.let { TitleBarAppearance(window = window, titleBar = it) }
                    KubrikoShowcase(
```

Two consequences on macOS, every time the user enters full screen (the Showcase's own full screen button in a game's
menu overlay, the green window button, or ⌃⌘F):

1. `TitleBarAppearance` leaves the composition and its effect removes the toolkit-wide mouse listener:

   ```kotlin
       DisposableEffect(titleBar) {
           onDispose { Toolkit.getDefaultToolkit().removeAWTEventListener(titleBar.mouseListener) }
       }
   ```

   The listener is only ever added once, in `extendContentIntoCustomTitleBar` from the `SwingWindow`'s `init`
   (`KubrikoShowcaseApp.kt`, ~line 78). After leaving full screen the re-composed `TitleBarAppearance` does not add it
   back, so the title strip can no longer be dragged and double-clicking it no longer zooms — undoing commit 65bd769e for
   the rest of the session.
2. Every `remember`ed thing below `KubrikoShowcase` is reset on each toggle in either direction: the side menu's scroll
   position, `KubrikoViewport`s (recreated, which re-runs their first-composition setup), dialogs and animation state.

On Windows the window itself is recreated by `key(isInFullscreenMode.value)`, so the content reset is inherent there
and the listener is re-added by the new window's `init`; the bug is effectively macOS-only. Linux has no
`ExtendedTitleBar`, so it always takes the first branch and never switches.

## Fix

1. In `TitleBarInsets`, always provide the composition local so `content` has a single call site; pass the platform
   insets through unchanged when there is no strip. Roughly:

   ```kotlin
   val platformInsets = LocalPlatformWindowInsets.current
   val titleBarHeight = titleBar?.takeUnless { isFullscreen }?.let { with(LocalDensity.current) { it.height.roundToPx() } }
   val insets = remember(platformInsets, titleBarHeight) {
       if (titleBarHeight == null) platformInsets else object : PlatformWindowInsets by platformInsets {
           override val captionBar = PlatformInsets(top = titleBarHeight)
           override val systemBars = PlatformInsets(top = titleBarHeight)

           // A dialog or a popup asks for the insets without the ones it has already kept clear of.
           override fun excluding(safeInsets: Boolean, ime: Boolean) = if (safeInsets) platformInsets.excluding(safeInsets, ime) else this
       }
   }
   CompositionLocalProvider(LocalPlatformWindowInsets provides insets, content = content)
   ```

   (Reading `LocalDensity.current` inside `let` is fine in a Composable; hoist it into a `val density` first if the
   compiler complains.) Keep the KDoc's statement that a full screen window gets no inset — it stays true.
2. Tie the listener's removal to the window rather than to a subtree that can be recomposed: in
   `KubrikoShowcaseApp.kt`, move `titleBar?.let { TitleBarAppearance(window = window, titleBar = it) }` out of the
   `TitleBarInsets` content lambda to just before the `TitleBarInsets(...)` call (it reads no insets), so it lives
   exactly as long as the `SwingWindow` content. Its `DisposableEffect(titleBar)` then fires only when the window leaves
   the composition, which is what the KDoc of `TitleBarAppearance` already promises.

Do not re-add the listener on every composition as an alternative; one listener per window is the invariant.

`app/desktop/CLAUDE.md` (Title bar section) already says "The listener is removed when the window leaves the
composition"; it needs no change unless the wording about `TitleBarInsets` giving no inset in full screen is touched.

## Tests

None: this is Compose/AWT wiring in the desktop app module, which has no test source set, and the behaviour only exists
with a real window on the JetBrains Runtime.

## Manual check

macOS 14 or 15, `./gradlew :app:desktop:run` (it runs on the provisioned JetBrains Runtime):

1. Drag the window by the top strip, then double-click the strip — it zooms (System Settings → Desktop & Dock →
   "Double-click a window's title bar to" set to Zoom).
2. Scroll the side menu down, open Wallbreaker, open its menu overlay and press the full screen button; press it again
   to leave full screen.
3. Expected: dragging and double-clicking the strip still work; the side menu keeps its scroll position; the Wallbreaker
   game is where it was (not restarted from its loading/first-frame state).
4. Repeat step 2–3 with the green window button / ⌃⌘F and Esc to leave.

Windows 11 sanity check: toggle full screen twice from a game's menu overlay; afterwards the title strip still drags
the window and the caption buttons still follow the theme.
