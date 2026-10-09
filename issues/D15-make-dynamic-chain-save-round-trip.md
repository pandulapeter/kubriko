# Make `DynamicChain.save()` the inverse of `restore()`, so each editor save/undo stops adding a link and shifting the chain.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** desktop (scene editor)  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/DynamicChain.kt`, `examples/demo-physics/src/desktopTest/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/DynamicChainTest.kt` (new)

## Problem
Traced end to end at 2480325f:
1. **restore** — `State.restore() = DynamicChain(this)`; `linkCount = state.linkCount` (N) and `generateLinks()` builds `(0..linkCount).map { linkIndex -> ChainLink(initialPosition = SceneOffset(x = state.initialCenterOffset.x + LinkDistance * (state.linkCount / 2) - (LinkDistance * linkIndex), y = state.initialCenterOffset.y)) }` (:96–103) — **N + 1** links, link 0 (the rightmost) at `C.x + LinkDistance * (N / 2)` (integer division).
2. `init { refreshBodySize() }` (:79–81, :105–112) sets `body.position = SceneOffset(left, top)` and `body.size = SceneSize(right - left, bottom - top)`, where left/top/right/bottom are the min/max link `physicsBody.position` ∓ `offset` (`SceneOffset(ChainLink.Radius * 2, ChainLink.Radius * 2)`). The chain's `BoxBody()` has a zero pivot (default `initialSize.center` of `SceneSize.Zero`, and the clamp keeps it at zero as the size grows), so `body.position` is the **top-left** of the padded bounds, not the centre.
3. **save** (:150–153):
   ```kotlin
   override fun save() = State(
       linkCount = chainLinks.size,
       initialCenterOffset = body.position,
   )
   ```
   writes `linkCount = N + 1` and the top-left as the "centre".
4. The scene editor calls `save()` on every file save **and on every undo snapshot** (`EditorController.takeSnapshot()` → `serializationManager.serializeActors(sceneActors)` → `actor.save().serialize()`; `restoreSnapshot` deserializes and re-adds), and the editor's `StateManager.newInstance(shouldAutoStart = false)` means `update()` never runs there, so nothing re-centres the chain.

So every save/undo/redo round-trip adds one link and moves the chain up-left by roughly half its extent. The shipped `examples/demo-physics/src/commonMain/composeResources/files/scenes/scene_physics_test.json` already shows the drift: its two chains have `"linkCount":23` and `"linkCount":21`, while the editor's factory default is `DynamicChain.State(linkCount = 20, …)`. The `// TODO: Something is off with the Editor preview` (:43) is this.

Note the reviewer's proposed `initialCenterOffset = body.position + body.size.center` is **not** an exact inverse: for odd N (both shipped chains), `N / 2` rounds down, so the centre of the link extent is `C.x - LinkDistance / 2` and each round-trip would still shift the chain by 18 su.

## Fix
Make `save()` invert exactly what `restore()` computes, without changing how existing JSON loads:
```kotlin
override fun save() = State(
    linkCount = linkCount,
    initialCenterOffset = SceneOffset(
        x = body.position.x + body.size.width - offset.x - LinkDistance * (linkCount / 2),
        y = body.position.y + body.size.height / 2,
    ),
)
```
(`body.position.x + body.size.width - offset.x` is the rightmost link's x, i.e. link 0 at `C.x + LinkDistance * (N / 2)`; the y is the centre of the straight chain.) Then delete the `// TODO: Something is off with the Editor preview` line. Do **not** touch `generateLinks()` or the `@Exposed` `linkCount` setter — that is planned D50, which also decides what `linkCount` counts.

## Behaviour
Only what the editor writes changes: a chain saved and reloaded now has the same link count and position. Loading the existing JSON gives the same chains as today (the two shipped chains keep their 24 and 22 links unless someone re-saves them in the editor — that is fine). The running demo never calls `save()`.

## Public API
None.

## Tests
Add `examples/demo-physics/src/desktopTest/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/DynamicChainTest.kt` (license header; same package so the `internal` class is reachable; `kotlin.test` is already on every `desktopTest` classpath via the convention plugin):
```kotlin
class DynamicChainTest {

    @Test
    fun saveReturnsTheStateTheChainWasRestoredFrom() = listOf(20, 21).forEach { linkCount ->
        val saved = DynamicChain.State(
            linkCount = linkCount,
            initialCenterOffset = SceneOffset(100f.sceneUnit, (-50f).sceneUnit),
        ).restore().save()
        assertEquals(linkCount, saved.linkCount)
        assertEquals(100f, saved.initialCenterOffset.x.raw, 0.01f)
        assertEquals(-50f, saved.initialCenterOffset.y.raw, 0.01f)
    }
}
```
It fails on the current code (21 ≠ 20 and the top-left offset) and passes with the fix. Constructing the chain needs no Kubriko instance and draws nothing (only `Path` in `draw()` touches graphics). If construction unexpectedly throws on the test classpath, keep the fix and report the test as not addable.

## Verify
`./gradlew :examples:demo-physics:desktopTest` and `./gradlew :examples:demo-physics:compileKotlinDesktop`

## Manual check
Desktop, scene editor enabled: open the Physics Scene Editor, place a chain, save, undo/redo a few times and reload — the chain keeps its length and stays put.
