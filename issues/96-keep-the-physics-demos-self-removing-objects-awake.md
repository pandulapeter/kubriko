# Keep the physics demo's self-removing objects awake so they always remove themselves after leaving the view

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** Desktop, Web (window resizing); Android split-screen/foldables
**Files:** `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/BaseDynamicObject.kt`, `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/DynamicChain.kt`, `examples/demo-physics/CLAUDE.md`

## Problem

Reviewed at `0008d027`. The physics demo bounds its actor count by letting each dynamic object remove itself from its own `update()` once it has left the viewport — `BaseDynamicObject.update()` (~line 40):

```kotlin
body.position = SceneOffset(physicsBody.position.x, physicsBody.position.y)
body.rotation = physicsBody.rotation
if (!body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager)) {
    actorManager.remove(this)
} else { ... }
```

and `DynamicChain.update()` (~line 87):

```kotlin
if (chainLinks.none { it.body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager) }) {
    actorManager.remove(this)
} else {
    refreshBodySize()
}
```

The demo's `ActorManager` uses the defaults, so `shouldPutFarAwayActorsToSleep = true`: a `Dynamic` actor whose body is farther than the sleep margin (half the smaller scaled viewport dimension) outside the view stops receiving `update()`. `PhysicsManagerImpl` keeps simulating every `RigidBody` in `allActors` regardless.

In normal play an object is removed on the first frame it is outside the view, long before it reaches the sleep margin. But the active region is re-culled immediately when the **viewport size changes**: narrowing the window (FitVertical 1920: a 1920×1080 window shows ~3413 scene units across; at 480×1080 only ~853, with a sleep margin of ~427) puts objects near the old side edges beyond the new active region in one step. They are asleep and outside the view at once, so the `update()` that would remove them never runs: they stay in `allActors`, invisible, their physics bodies falling forever and still simulated and collision-checked every step. They are only reclaimed if the window is widened again enough to wake them. A chain behaves the same way (its own body is the one that goes to sleep).

(The review's original scenario — a fast chain sleeping because `ChainLink.body` lags its `physicsBody` — does not hold: the lag is at most a frame or two, while falling asleep requires travelling a full sleep margin, ~960 scene units at the default window, past the point where removal already fires.)

## Fix

Opt these self-culling actors out of sleeping; they never live off-screen for more than a frame, so sleeping buys nothing for them:

- `BaseDynamicObject`: add `override val isAlwaysActive = true`.
- `DynamicChain`: add `override val isAlwaysActive = true`. Its `ChainLink`s may keep sleeping: they are removed with the chain (they are its `Group` children), and a sleeping link's frozen body is always outside the view, which is what the chain's removal check needs.

`examples/demo-physics/CLAUDE.md`, the **Off-screen removal** paragraph (~line 69): add "Such actors set `isAlwaysActive = true`, so far-away sleeping (e.g. after the window is narrowed) cannot stop them from removing themselves."

## Tests

None: examples have no test source sets; the behaviour depends on `ActorManager` culling against a live viewport.

## Manual check

Desktop Showcase with the debug menu enabled, Physics demo: spawn a dozen shapes and a chain spread across the full width of a wide window, then quickly drag the window narrow (to roughly a quarter of its width). In the debug menu's actor count, the objects that were near the side edges disappear from the count within a second after the fix; before it they stay counted indefinitely while the window stays narrow.
