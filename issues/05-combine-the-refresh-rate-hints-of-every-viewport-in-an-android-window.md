# Combine the refresh-rate hints of every viewport in an Android window instead of letting the last one win

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** Android
**Challenged:** amended — derives an `isSized` boolean through `derivedStateOf` so the null-when-unsized request does not recompose `InternalViewport` on every resize, corrects the by-identity rationale, and records the plan 52 interaction.
**Files:** `engine/src/androidMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformEffects.android.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformEffects.kt` (expect signature), `engine/src/desktopMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformEffects.desktop.kt`, `engine/src/iosMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformEffects.ios.kt`, `engine/src/webMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformEffects.web.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/InternalViewport.kt`, new `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/FrameRateHintRequests.kt`, new `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/implementation/FrameRateHintRequestsTest.kt`, `engine/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:engine`. No public API change (all internal).

## Problem

```kotlin
@Composable
internal actual fun PlatformFrameRateHint(targetFrameRate: TargetFrameRate) {
    val window = LocalContext.current.findActivity()?.window ?: return
    DisposableEffect(window, targetFrameRate) {
        window.applyFrameRateHint(targetFrameRate)
        onDispose { window.applyFrameRateHint(TargetFrameRate.DisplayDefault) }
    }
}
```

Every `KubrikoViewport` (`InternalViewport` calls `PlatformFrameRateHint(viewportManager.targetFrameRate…)`) writes
the same `Window.attributes` (`preferredDisplayModeId` / `preferredRefreshRate`). With several viewports in one
Activity — which the root `CLAUDE.md` documents as valid ("Multiple Kubriko instances … a background layer and a game
layer"), and which `DebugMenu` always does (a `Modifier.size(0.dp)` viewport for `InternalDebugMenu.internalKubriko`
at the default `Limit(60)`, plus the debug-menu instance that mirrors the game's target):

- **Last write wins.** Whichever effect ran last decides the panel mode. A game that switches to `Limit(120)` at
  runtime re-runs only its own effect, but a background instance composed later with `Limit(60)` that re-runs (or
  mounts) afterwards pins the panel at 60 Hz, and the game is capped at 60 fps.
- **One disposal clears everyone.** When any viewport leaves composition (a dialog's viewport, the debug menu
  overlay, a screen transition in the Showcase), its `onDispose` writes `DisplayDefault`, releasing the hint the
  remaining game viewport still wants — the panel goes back to its maximum while the game runs `Limit(30)`, burning
  the battery the hint was meant to save.

## Fix

1. **A pure chooser** in commonMain (`FrameRateHintRequests.kt`, internal):

   ```kotlin
   /** The single hint that satisfies every viewport sharing a window; [TargetFrameRate.DisplayDefault] (release) when none requests one. */
   internal fun combineFrameRateHints(requests: List<TargetFrameRate>): TargetFrameRate
   ```

   - empty → `DisplayDefault` (release);
   - any `DisplayDefault` or `DisplayDivider` → `DisplayDefault` (they need the panel's native rate; see
     `toPreferredRefreshRate`'s comment);
   - otherwise the `Limit` with the highest `framesPerSecond` (the slowest mode covering it covers every other
     request).

   Indexed loop, no allocation beyond the list itself (this runs on composition changes, not per frame).
2. **A per-window registry** in `PlatformEffects.android.kt`: `private val frameRateHintRequests =
   WeakHashMap<Window, ArrayList<TargetFrameRate>>()` (touched only from the main thread, inside composition
   effects). `PlatformFrameRateHint`'s `DisposableEffect(window, targetFrameRate)` adds its request, applies
   `combineFrameRateHints(list)` to the window, and on dispose removes **its own** entry (by identity, `removeAt`
   of the index holding that exact instance — equal `Limit`s from two viewports are interchangeable, so this is only
   for clarity; `remove(element)` would be equally correct) and applies the combined
   result of the rest (`DisplayDefault` when none remain, and the map entry is dropped).
3. **Viewports that cannot tick don't vote.** Change the internal expect to
   `PlatformFrameRateHint(targetFrameRate: TargetFrameRate?)`, with `InternalViewport` passing `null` while
   `viewportManager.size` is empty (the frame loop is gated off then, so it needs no panel rate). Read the size so
   that only the empty/non-empty flip recomposes `InternalViewport`, not every size change (a desktop window drag or
   a split-screen resize would otherwise re-run its whole body, manager modifier fold included, once per layout):
   `val sizeState = viewportManager.size.collectAsState()` and
   `val isSized by remember { derivedStateOf { !sizeState.value.isEmpty() } }`, reading `sizeState` nowhere else in
   composition, then `PlatformFrameRateHint(if (isSized) targetFrameRate else null)`. This stops
   `DebugMenu`'s 0 dp persistence viewport from holding every game's `Limit(30)` up at 60 Hz once requests are
   combined. On Android a `null` request simply isn't registered; the other actuals stay no-ops (update their
   signatures).
4. `engine/CLAUDE.md` → Tick Dispatch: after the `PlatformFrameRateHint` description add that the requests of all
   viewports in one Activity window are combined (any `DisplayDefault`/`DisplayDivider` releases the hint, otherwise
   the fastest `Limit` wins; an unsized viewport makes no request).

Option not taken: making the hint opt-in per viewport (would need a public parameter on `KubrikoViewport`).

Interaction with plan 52 (Android `configChanges`): the registry lives as long as the Activity's `Window`, which plan
52 keeps across rotation/resize instead of recreating it, so requests are no longer reset by recreation — the
registry must therefore stay consistent through in-place changes, which it does (only composition adds/removes). The
foldable follow-up named in plan 52 (re-applying the hint when the display mode changes) must re-apply
`combineFrameRateHints` of the window's registry list, not the calling viewport's own target.

## Tests

`FrameRateHintRequestsTest` (desktopTest, pure):

- `[]` → `DisplayDefault`; `[Limit(30)]` → `Limit(30)`; `[Limit(30), Limit(120), Limit(60)]` → `Limit(120)`;
- `[Limit(30), DisplayDivider(2)]` → `DisplayDefault`; `[Limit(30), DisplayDefault]` → `DisplayDefault`.

The registry and the window writes are Android framework code and are covered by the manual check.

## Manual check

Android device with a variable-refresh panel (e.g. 60/120 Hz), Developer options → "Show refresh rate": in the Showcase
with the debug menu enabled, open a game that uses a `Limit` below the panel maximum (or temporarily set one), then
open and close the debug menu overlay and navigate between examples — the indicator stays at the mode for the game's
target instead of jumping back to 120 Hz after a viewport leaves.
