# Trim the Annoyed Penguins launch-impulse comment to its constraint.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/Penguin.kt`

## Problem
`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/Penguin.kt:77–81` explains the launch impulse through the history of the bug it fixed ("…which made the old `force = impulse / deltaTime` hack frame-rate dependent… 100000 reproduces the velocity the old formula delivered at its intended scale (10000000 / 100)…"). The code style keeps such a regression guard, but only as the constraint.

## Fix
Replace the five lines with:
```kotlin
// A launch is an instantaneous velocity change, so it must be an impulse, not a force: the physics fixed timestep
// integrates forces over its sub-step, which would make the launch strength frame-rate dependent.
```
The code (`physicsBody.applyLinearImpulse(impulseOrigin.scalar(100000f))`) is unchanged.

## Behaviour
Comment only.

## Public API
None.

## Tests
None.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop`

## Manual check
none
