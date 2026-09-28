# Report the viewport as unfocused when it leaves composition

**Challenged:** amended — "not focused" is now reported only when the *last* viewport showing the instance leaves (a per-instance viewport count, since one instance can be shown by two viewports, e.g. the debug menu's shared `internalKubriko`); the KDoc no longer claims a never-shown instance is unfocused; a lane F companion change keeps Annoyed Penguins' game viewport composed during level loads (its `UIManager` pauses on focus loss, so the plan as written would drop the player back onto the pause menu after picking a level); the manual check now exercises the fix (the Showcase disposes a game when it is left, so the old check never reached it).

**Decision needed:** when `KubrikoViewport` leaves composition while its Kubriko instance lives on, `StateManager.isFocused` (and so `isRunning`) keeps its last value, normally `true`. Should the viewport report "not focused" on leaving? — recommended: yes.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/InternalViewport.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/StateManagerImpl.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/StateManager.kt` (KDoc of `isFocused`), `CLAUDE.md` (`StateManager` bullet). Companion change: plan 99 (lane F).

Apply after plan 12 (same file; the new effect goes inside its `key(kubrikoImpl)` block).

## Problem

`LifecycleFocusEffect` (`PlatformUtils.kt`, 0008d027 ~43-58) and the web actual only unregister their observers on dispose:

```kotlin
onDispose {
    lifecycle.removeObserver(lifecycleObserver)
}
```

Nothing tells the `StateManager` that the viewport is gone. A game kept alive in a ViewModel/state holder while its screen is navigated away from therefore keeps `isFocused == true` and `isRunning == true`: `MusicManager` (which pauses on `isFocused == false`) keeps playing, `PhysicsManager`/`ParticleManager` and `Dynamic` actors keep advancing under any non-viewport `TickSource`, and game UI observing `isRunning` shows "running".

## Fix

1. `StateManagerImpl`: add `internal var attachedViewportCount = 0` (one line of comment: only touched from composition, i.e. the UI thread, on every platform).
2. In `InternalViewport`, inside the `key(kubrikoImpl)` block from plan 12, **before** `PlatformFocusEffect`, add:

   ```kotlin
   DisposableEffect(kubrikoImpl) {
       val stateManager = kubrikoImpl.stateManager
       stateManager.attachedViewportCount++
       onDispose {
           stateManager.attachedViewportCount--
           if (stateManager.attachedViewportCount == 0) stateManager.updateFocus(false)
       }
   }
   ```

   Why a count and not a plain `updateFocus(false)`: one instance can be shown by more than one viewport at a time — the debug menu composes a 0 dp `KubrikoViewport` of its global `InternalDebugMenu.internalKubriko` in every `OverlayOnly`, so the Showcase window and a scene-editor window on desktop show it twice, and consumers may do the same (a hidden logic viewport plus a visible one of the same instance). The first of them to leave must not mark an instance unfocused that is still on screen.

   Ordering: when a viewport moves within one recomposition (e.g. `DebugMenu.invoke` switching between its enabled and disabled branches, or plan 12's `key` swap), Compose runs the old effect's `onDispose` before the new composition's effects, so the count goes 1 → 0 (reports false) → 1, and the new `PlatformFocusEffect` immediately reports the real state; both writes land in the same main-thread apply, before the `isFocused` collector runs, so observers see no change. `updateFocus` only writes a `MutableStateFlow`, so it is safe on a disposed instance (the Showcase disposes a game's instance in the same apply its viewport leaves in). One place covers every platform; the per-platform actuals stay as they are.

**Alternative:** keep the behaviour and document that `isFocused` reflects the last viewport state. Not recommended.

KDoc of `StateManager.isFocused`: "Becomes false when the last `KubrikoViewport` showing this instance leaves composition, and follows the platform's focus again once one is shown. An instance that has never been shown in a viewport (headless use) stays focused." Root `CLAUDE.md` → `StateManager` → `isFocused` bullet: the same two clauses, shortened.

### Companion change — plan 99 (lane F)

Annoyed Penguins would pause on every level load once this lands (its game viewport leaves composition inside an `AnimatedVisibility`). Plan 99 keeps that viewport composed. Lane A merges first, so between the two merges `main` briefly has that regression; nothing is released in between.

## Tests

No unit test (Compose effect lifecycle; no UI test setup in the engine).

## Manual check

The Showcase disposes a game when its screen is left, so it cannot show this directly. Desktop: temporarily wrap Space Squadron's game `KubrikoViewport` in `if (isShown)` with a key binding that toggles `isShown`; while playing, hide it — the music pauses and `isRunning` goes false; show it — the game resumes on focus as usual. Then, with the debug menu enabled, open the scene editor from the Showcase and close it: the Showcase's debug menu keeps working (its internal instance stays focused). Annoyed Penguins (after plan 99): pick each level from the menu — the level starts running, the menu does not come back, the music keeps playing; repeat in the browser (`./gradlew :app:web:wasmJsBrowserDevelopmentRun`). Android: the same toggle check (350 ms debounce).
