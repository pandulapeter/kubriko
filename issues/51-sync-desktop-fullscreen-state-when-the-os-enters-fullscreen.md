# Keep the desktop fullscreen state in sync when the system puts the window into fullscreen

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** Desktop (macOS, Linux)
**Challenged:** amended — following the OS into fullscreen now has a guard against the native transition re-reporting the old placement after an in-app toggle, a manual check for it, and Linux is marked unverified.
**Files:** `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/DesktopFullscreenState.kt`,
`app/desktop/CLAUDE.md`

Ships in no published artifact (`app/desktop` is the Showcase).

## Problem

`DesktopFullscreenState` only follows the system out of fullscreen, never into it:

```kotlin
/** Leaves fullscreen mode when the system takes the window out of it (e.g. Escape on macOS). */
fun onWindowStateChanged(windowState: WindowState) {
    if (isInFullscreenMode) {
        isInFullscreenMode = windowState.placement == WindowPlacement.Fullscreen
    }
}
```

and `toggle()` saves whatever placement the window has when the in-app mode is entered:

```kotlin
} else {
    windowSize.value = windowState.size
    previousBounds.value = window.bounds
    previousWindowPlacement.value = windowState.placement
    ...
    windowState.placement = WindowPlacement.Fullscreen
}
```

Scenario on macOS: the user clicks the green window button. The window goes fullscreen, `windowState.placement`
becomes `Fullscreen`, but `isInFullscreenMode` stays `false`, so the Showcase still shows its top bar and offers
"enter fullscreen". Pressing it saves `previousWindowPlacement = Fullscreen` and the screen-sized bounds and flips the
state to `true` with no visible effect. Pressing it again "restores" `Fullscreen` — the window stays fullscreen,
`isInFullscreenMode` is now `false`, and the in-app toggle can never leave; only the system's own controls can.
Linux window managers that put a window into fullscreen (e.g. a keyboard shortcut) behave the same way.

Windows is not affected: there is no system fullscreen for a decorated window there, the in-app mode recreates the
window undecorated (`key(fullscreenState.isInFullscreenMode)` in `KubrikoShowcaseApp.kt`), and the placement only
becomes `Fullscreen` through `toggle()`.

## Fix

In `DesktopFullscreenState`:

1. **Follow the system both ways off Windows.** `onWindowStateChanged` becomes:
   - on Windows: unchanged (only leaving is followed);
   - elsewhere: `isInFullscreenMode = windowState.placement == WindowPlacement.Fullscreen`. When this turns it **on**
     (it was `false`), clear the saved geometry (`previousWindowPlacement`, `previousBounds`,
     `previousWindowLocation`, `previousWindowPosition` to `null`, `windowSize` to `DpSize.Unspecified`): the window
     went fullscreen without `toggle()`, so anything saved belongs to an earlier, already-finished in-app session.
2. **Never save `Fullscreen` as the placement to return to.** In `toggle()`'s enter branch, if
   `windowState.placement` is already `Fullscreen`, only flip the state and save nothing.
3. **Leave without saved geometry.** In `toggle()`'s exit branch, when `previousWindowPlacement` is `null`, set
   `windowState.placement = WindowPlacement.Floating` and nothing else: the system remembers the frame the window had
   before it entered its own fullscreen and puts it back (macOS does this for native fullscreen), whereas the saved
   bounds would be the screen's. Keep the existing restore path unchanged when geometry was saved.

The combined effect: the green button makes the Showcase hide its top bar and offer "exit fullscreen", which works;
the in-app button and the system controls can be mixed in any order.

Execution-time check: the existing leave-sync relies on Compose having updated `windowState.placement` by the time
the AWT `WindowStateListener` in `KubrikoShowcaseWindow.kt` runs. If entering turns out not to be picked up (the
listener runs before Compose's own), observe the placement from Compose instead —
`LaunchedEffect(Unit) { snapshotFlow { windowState.placement }.collect { fullscreenState.onWindowStateChanged(windowState) } }`
in `KubrikoShowcaseWindow` in place of the AWT listener — which sees the value only after Compose has written it.

**Guard against transition echoes.** Following entry is new exposure: macOS native fullscreen enters and leaves
with an animation, and Compose re-derives `windowState.placement` from the window while it runs. If, after the
in-app exit sets `Floating`, a report of `Fullscreen` arrives before the animation ends, the both-ways sync would
flip `isInFullscreenMode` back to `true` (the leave-only sync never had this problem in that direction). Add a
private `requestedPlacement: WindowPlacement?` set by `toggle()` to the placement it writes, and in
`onWindowStateChanged` ignore any report while `requestedPlacement` is non-null and differs from the reported value,
clearing it once a report matches. With no request pending (OS-initiated changes) the sync follows immediately.

Linux is unverified: whether a window manager's own fullscreen (`_NET_WM_STATE_FULLSCREEN`) reaches
`windowState.placement` depends on Compose's X11 window adapter. If it does not, nothing changes on Linux and the
original scenario does not occur there either; the change is safe both ways.

Update `app/desktop/CLAUDE.md` → "Fullscreen handling": the last paragraph says the listener syncs the state when the
user *exits* via the OS; make it say that on macOS / Linux it follows the OS both ways (green button included), that
a fullscreen the OS entered is left by returning to `Floating` and letting the OS restore the frame, and that on
Windows only leaving is followed.

## Tests

None. `app/desktop` has no test source set (it applies `kotlin.jvm` with no `src/test`, and `:tools:test-fixtures`
is not wired into it), and `DesktopFullscreenState.toggle()` needs a real `ComposeWindow` (`window.bounds`,
`window.location`, `window.setLocation`), which cannot be created on a headless test JVM. Splitting the placement
decisions into a pure function would leave three one-line `if`s to test; not worth a new test source set.

## Manual check

On macOS (`./gradlew :app:desktop:run`):
1. Green button → window goes fullscreen; the Showcase's own fullscreen control now reads "exit". Press it → the
   window returns to its previous size and position, and the control reads "enter".
2. Green button, then the system's exit (Escape / green button / menu) → state follows, as before.
3. In-app enter, then in-app exit → the saved frame is restored exactly as before (regression check).
4. In-app enter, system exit, green button, in-app exit → no stale frame is applied; the window lands where macOS
   puts it.
5. In-app enter, in-app exit, repeated quickly several times → after each animation settles the control matches the
   window (no flip back to "exit" while the window is floating).
On Windows: in-app enter/exit behaves as before.
