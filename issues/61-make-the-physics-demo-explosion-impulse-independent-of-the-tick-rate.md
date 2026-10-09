# Make the physics demo's explosion deliver the same total impulse at any tick rate

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** all (visible on Web/Wasm and slow Android devices)
**Challenged:** sound
**Files:** `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/Bomb.kt`,
`examples/demo-physics/src/desktopTest/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/BombTest.kt` (new),
`examples/demo-physics/CLAUDE.md`

Ships in no published artifact (`examples/demo-physics` is part of the Showcase).

## Problem

`Bomb.update` fades by a delta-scaled amount but applies a fixed impulse once per tick until it has faded:

```kotlin
override fun update(deltaTimeInMilliseconds: Int) {
    body.size += SceneSize(5.sceneUnit, 5.sceneUnit) * deltaTimeInMilliseconds
    body.pivot = body.size.center
    alpha -= 0.01f * deltaTimeInMilliseconds
    if (alpha <= 0) {
        actorManager.remove(this)
    } else {
        explosion.applyBlastImpulse(25000000.sceneUnit)
    }
}
```

`ProximityExplosion.applyBlastImpulse` (plugins/physics) is an instantaneous velocity change per call —
`b.applyLinearImpulse(blastDir.normalized().scalar(blastPower / distance))`, which adds `impulse * invMass` to the
body's velocity — so the total push is proportional to the number of ticks in the bomb's 100 ms life. Counted with a
probe (same float arithmetic): 12 calls at 8 ms ticks, 6 at 16 ms, 5 at 17 ms, 3 at 33 ms, 1 at 50 ms, and **none** for
any tick of 100 ms or longer — a frame hitch right after the tap makes the explosion do nothing at all. Every Showcase
example runs at the default `TargetFrameRate.Limit(60)`, so the variation shows up where a device or browser cannot
sustain 60 ticks per second (Wasm under load, low-end Android, a 50 Hz panel), or if the target rate is ever raised.

## Fix

Spread a fixed total impulse over a fixed lifetime, in proportion to the time each tick covers:

- Replace `alpha`'s bookkeeping with `private var remainingLifetimeInMilliseconds = LIFETIME_IN_MILLISECONDS` and
  companion constants `LIFETIME_IN_MILLISECONDS = 100` and `BLAST_POWER_PER_MILLISECOND = 1_500_000f` (6 × 25 000 000
  spread over 100 ms — the strength the demo has today at 60 ticks per second).
- In `update`: grow the body as now; `val activeTimeInMilliseconds = min(deltaTimeInMilliseconds, remainingLifetimeInMilliseconds)`;
  `remainingLifetimeInMilliseconds -= deltaTimeInMilliseconds`; if `activeTimeInMilliseconds > 0` call
  `explosion.applyBlastImpulse((BLAST_POWER_PER_MILLISECOND * activeTimeInMilliseconds).sceneUnit)`; then if
  `remainingLifetimeInMilliseconds <= 0` remove the bomb.
- Draw with `alpha = max(0f, remainingLifetimeInMilliseconds / LIFETIME_IN_MILLISECONDS.toFloat())` (same fade as
  today), computed in `draw` or kept as a field updated in `update`.

The total impulse is then 100 × 1 500 000 regardless of how the 100 ms are sliced; a single 200 ms tick applies all of
it at once instead of none. Rejected alternative: one impulse on the first `update` — equivalent in total, but it drops
the brief "push over a few frames" feel and the fade/size code still needs the lifetime bookkeeping.

Update the `Bomb` paragraph in `examples/demo-physics/CLAUDE.md` ("Each subsequent frame it grows, fades, and repeatedly
calls `applyBlastImpulse(25 000 000 su)` until alpha ≤ 0") to say it applies 1 500 000 su of blast power per
millisecond of its 100 ms life, so the explosion's strength does not depend on the tick rate.

## Tests

New `BombTest` in `examples/demo-physics/src/desktopTest/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/actors/`
(the module already has a `desktopTest` source set, `DynamicChainTest`, with `:tools:test-fixtures` wired):

- A test-local `TestRigidBody : RigidBody` with `collisionMask = CircleCollisionMask(initialRadius = 10.sceneUnit,
  initialPosition = SceneOffset(100.sceneUnit, 0.sceneUnit))` and `physicsBody = PhysicsBody(collisionMask)` (density
  1, so impulses apply). It is not `Dynamic` and no `PhysicsManager` is registered, so its position stays put and its
  velocity is exactly the sum of the impulses.
- Helper `velocityAfter(deltaTimeInMilliseconds: Int, count: Int): Float` — `newManualKubriko().use { k -> }`: add the
  target, `k.actorManager.awaitProcessed()`, add `Bomb(epicenter = SceneOffset.Zero)`, `awaitProcessed()` (the bomb
  collects bodies in `onAdded` from `allActors`), `k.tick(deltaTimeInMilliseconds, count)`, return
  `target.physicsBody.velocity.x.raw`.
- `totalImpulseDoesNotDependOnTheTickRate`: `velocityAfter(17, 6)`, `velocityAfter(8, 13)` and `velocityAfter(200, 1)`
  are equal within 0.1 % and greater than zero (fails today: 5 vs 12 vs 0 calls).
- `bombRemovesItselfAfterItsLifetime`: after `tick(16, 7)` and `awaitProcessed()`, no `Bomb` is in `allActors`.

`Bomb.draw` is never called (no viewport), so no Skia is needed.

## Manual check

In the physics demo, trigger explosions next to a stack of shapes on Desktop and in a throttled browser tab (DevTools
CPU throttling 6×): the blast should push the shapes about equally hard in both, and never do nothing.
