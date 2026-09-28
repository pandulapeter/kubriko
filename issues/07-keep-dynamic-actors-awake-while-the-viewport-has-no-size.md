# Keep Dynamic actors awake while the viewport has no size

**Challenged:** amended — the no-viewport publication goes in plan 06's pre-loop refresh instead of the post-loop cull branch, so the documented headless `start(); tick()` updates the initial actors on its first tick (it did not with the fix as written), and the tests assert exactly one tick.

**Decision needed:** with no measured viewport (headless use, or before the first layout) the far-away-sleep cull never runs, so with the default `shouldPutFarAwayActorsToSleep = true` no `Dynamic` actor is ever updated. Treat "no viewport size" as "nothing is far away"? — recommended: yes, every `Dynamic` actor is active while the size is empty; `visibleActorsWithinViewport` stays empty (there is no viewport to be within).

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all (headless/TickSource users)  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManagerImpl.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManager.kt` (KDoc of `shouldPutFarAwayActorsToSleep`, `activeDynamicActors`, `visibleActorsWithinViewport`), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/HeadlessActorUpdateTest.kt` (new), `documentation/TICK_SOURCE.md` (Headless usage), `engine/CLAUDE.md` (Culling allocation model), `CLAUDE.md` (ActorManager bullet on `shouldPutFarAwayActorsToSleep`)

Apply after plan 06 (both edit `onUpdate`).

## Problem

`ActorManagerImpl.onUpdate` (0008d027 ~372-386) only culls when the viewport has a size:

```kotlin
if (shouldPutFarAwayActorsToSleep) {
    ...
    if (!viewportSize.isEmpty()) {
        if (dynamicActorsList !== lastDynamicActors || ...) {
            updateActiveDynamicActors(cameraPosition, scaleFactor)
        }
    }
} else if (_activeDynamicActors.value !== dynamicActors.value) {
    _activeDynamicActors.value = dynamicActors.value
}
```

`ViewportManager.size` starts at `Size.Zero` and is only written by `KubrikoViewport`'s `onSizeChanged` (the setter `updateSize` is internal). Without a mounted viewport it stays empty forever, so `activeDynamicActors` stays empty and, with the default `shouldPutFarAwayActorsToSleep = true`, no `Dynamic` actor ever receives `update()`. `documentation/TICK_SOURCE.md` sells `fixedRate`/`fixedFrequency`/`manual` for "automated tests, deterministic simulations, server-side or command-line simulations" — none of them updates a single actor unless the user knows to also pass `shouldPutFarAwayActorsToSleep = false`. The same happens to a viewport-backed game driven by a non-viewport tick source before the first layout.

## Fix

In `onUpdate`, in the **pre-loop** refresh block plan 06 added (the one that runs when the dynamic list reference changed, before the update loop), add the branch plan 06 left a slot for: sleeping is on and `viewportSize.isEmpty()` → publish the full dynamic list as active (`if (_activeDynamicActors.value !== list) _activeDynamicActors.value = list`), set `lastDynamicActors = list`, and set `lastViewportSizeForDynamic = null` so that the first non-empty size triggers a real cull through the existing post-loop `viewportSize != lastViewportSizeForDynamic` check. No allocation: it republishes an existing list reference.

It must be the pre-loop block, not the post-loop `shouldPutFarAwayActorsToSleep` branch: there the list would only become active *after* the first tick's update loop, so the documented headless flow (`documentation/TICK_SOURCE.md`: `start(); tick(16); tick(16)` with a default `ActorManager`) would still lose its first tick — exactly what plans 05 and 06 were amended to guarantee.

Leave `visibleActorsWithinViewport` as it is (empty without a viewport) and say so in its KDoc.

**Alternative:** keep the behaviour and document that headless instances must pass `shouldPutFarAwayActorsToSleep = false`. Not recommended: the default configuration silently does nothing.

KDoc:
- `ActorManager.newInstance(shouldPutFarAwayActorsToSleep)`: "Has no effect while the viewport has no size (e.g. no `KubrikoViewport` is mounted): every Dynamic actor is updated then."
- `activeDynamicActors`: same clause.
- `visibleActorsWithinViewport`: "Empty while the viewport has no size."

Docs: `documentation/TICK_SOURCE.md` → *Headless usage*: add "Dynamic actors are all updated while no viewport size is known; far-away sleeping starts once a viewport is measured." `engine/CLAUDE.md` → *Culling allocation model*: one bullet with the same rule. Root `CLAUDE.md` → the `shouldPutFarAwayActorsToSleep` bullet: add "(not applied while the viewport has no size)".

## Tests

`HeadlessActorUpdateTest` (desktopTest; build the instance by hand without `updateSize`):
- `dynamicActorsUpdateWithoutAViewport` — `Kubriko.newInstance(tickSource = manual)`, start, `add(a)`, await presence, exactly one `tick(16)`, assert `a.updates == 1`; assert `visibleActorsWithinViewport.value.isEmpty()`.
- `documentedManualExampleUpdatesOnTheFirstTick` — the `documentation/TICK_SOURCE.md` flow with a default `ActorManager` (sleeping on, no viewport): `ActorManager.newInstance(initialActors = listOf(a))`, `TickSource.manual()`, `start()`, one `tick(16)` with no waiting in between; assert `a.updates == 1`. Repeat 200 times with fresh instances.
- `sleepStartsOnceAViewportIsMeasured` — continue: place a second actor at `SceneOffset(100_000f, 100_000f)`, `viewportManager.updateSize(Size(1920f, 1080f))`, tick twice; assert the far actor is not in `activeDynamicActors` and stops receiving updates.

## Manual check

None.
