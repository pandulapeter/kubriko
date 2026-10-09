# Handle orientation, size, input-device and density changes in the Android Activity instead of recreating it

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** Android
**Challenged:** sound
**Files:** `app/android/src/main/AndroidManifest.xml`, `app/android/CLAUDE.md`

Ships in no published artifact (`app/android` is the Showcase).

## Problem

`KubrikoShowcaseActivity` declares no `android:configChanges`:

```xml
<activity
    android:name=".KubrikoShowcaseActivity"
    android:exported="true"
    android:resizeableActivity="true">
```

so every configuration change destroys and recreates it: rotating the device, resizing the window (split screen,
freeform / ChromeOS windows, folding a foldable), and — the common one for a game — connecting or disconnecting a
Bluetooth gamepad or keyboard, which changes `keyboard` / `keyboardHidden` / `navigation`.

The Showcase state survives (the `ShowcaseSession` is a process-level `internal val showcaseSession` in
`KubrikoShowcase.kt`, so the Kubriko instances outlive the Activity), but the whole Compose tree is torn down and
rebuilt: every `KubrikoViewport` leaves composition, which takes `StateManager.isFocused` to `false`, debounced by
the Android default of 350 ms (`getDefaultFocusDebounce() = 350L` in `Platform.android.kt`). On a device slow enough
for the new Activity's first frame to take longer than that, the instance goes unfocused, and games react to it:
Space Squadron's `UIManager` pauses the game on `isFocused` going `false`
(`stateManager.isFocused.filterNot { it }.onEach { gameplayManager.pauseGame() }`), and its `AudioManager` stops the
music. A player who connects a controller mid-game gets a hitch at best and a paused game at worst.

## Fix

Add to the `<activity>` element:

```xml
android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboard|keyboardHidden|navigation|density"
```

The activity overrides no `onConfigurationChanged`; none is needed. Checked that nothing relies on recreation for
these changes:

- `orientation|screenSize|screenLayout|smallestScreenSize`: `AndroidComposeView` resizes and updates
  `LocalConfiguration`; the layout decisions in `app/shared` are size-based (`BoxWithConstraints` in
  `KubrikoShowcase.kt` → `shouldUseCompactUi = maxWidth < 640.dp`), as are the examples' menus (`BoxWithConstraints` in
  the Space Squadron, Wallbreaker and Annoyed Penguins `MenuOverlay`s). The viewport size reaches `ViewportManager`
  through layout. Nothing in `app/` or `examples/` reads `LocalConfiguration`, `resources.configuration` or
  `displayMetrics` once.
- `keyboard|keyboardHidden|navigation`: nothing reads them; the gamepad plugin tracks devices through
  `InputManager.InputDeviceListener` (`GamepadEventHandler.android.kt`), independent of recreation, and is already
  rebound by `isValid()` when the Activity does change.
- `density`: `AndroidComposeView.onConfigurationChanged` replaces `LocalDensity`, and Compose resources re-resolve
  density-qualified drawables from the composition's environment.
- The splash screen and `enableEdgeToEdge()` live in `onCreate`; they are needed once, not per change. The
  fullscreen `MutableStateFlow` is file-level and its collector is bound to the Activity's `lifecycleScope`, which now
  simply lives on.

Deliberately left out, so they still recreate: `uiMode` (dark mode — `KubrikoTheme`/`isSystemInDarkTheme()` would
follow it, but recreation also refreshes the splash and window background drawables, which are theme resources),
`locale`/`layoutDirection` (the Compose string resources and `supportsRtl="false"` window setup are simplest to
refresh by recreation), `fontScale`, `colorMode`.

Update `app/android/CLAUDE.md` → "Entry point": add a bullet that the Activity handles the listed changes itself, so
rotating, resizing, or connecting a controller or keyboard does not recreate it (which would take every viewport out
of composition and can pause a running game), and that dark mode and locale changes still recreate it.

## Decision

Which changes the Activity absorbs (`app/` is unpublished, but this is a behaviour choice):
- **Recommended:** the list above — `orientation|screenSize|screenLayout|smallestScreenSize|keyboard|keyboardHidden|navigation|density`.
- Narrower: only `keyboard|keyboardHidden|navigation` (the controller case), keeping recreation on rotation/resize.
  Avoids the foldable risk below, but leaves rotation and window resizing with the hitch.
- Wider: also `uiMode`, so a dark-mode switch does not pause games either; needs a check that the window background
  and system bar appearance follow the theme without recreation.

Risk to check, and an engine follow-up rather than part of this plan: `PlatformFrameRateHint` in
`engine/src/androidMain/.../PlatformEffects.android.kt` keys its `DisposableEffect` on `(window, targetFrameRate)`
and picks `preferredDisplayModeId` from the modes matching the **current** resolution. On a foldable whose panel
switches resolution on fold/unfold, recreation used to re-pick the mode; with `screenSize` handled in place the old
mode id stays applied. If the manual check below shows the frame rate limit lost or the resolution wrong after
folding, report it as an engine finding (re-apply the hint on display change, as
`PlatformMaximumDisplayRefreshRateEffect` already listens for) — every game that declares `configChanges` would hit
it, not only the Showcase.

## Tests

None: a manifest attribute; nothing to unit test.

## Manual check

On an Android phone, a release build (`./gradlew :app:android:installRelease` or the debug build):
1. Open Space Squadron and start a game; connect (and then disconnect) a Bluetooth controller → the game keeps
   running, no pause menu, music continues; the controller works immediately.
2. Rotate the device mid-game → the layout reflows (compact / wide menu switch where the width crosses 640 dp), the
   game keeps running.
3. Split screen / freeform window resize (or a ChromeOS window) → same.
4. Change display size (Settings → Display size) → UI rescales correctly.
5. Toggle dark mode → the Activity still recreates and the theme switches as before.
6. On a foldable, with a `Limit` frame rate selected in the debug menu, fold/unfold → FPS stays at the limit; see the
   risk note above if not.
