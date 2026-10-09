# Turn the Space Squadron `ShipDestination`/`Bullet` declaration comments into KDoc.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/ShipDestination.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/base/Bullet.kt`

## Problem
Declarations documented with `//` instead of KDoc:
- `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/ShipDestination.kt:45–46` above `private var shouldMoveShip = true`.
- `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/base/Bullet.kt:68` (`// Reused every frame so sweeping the bullet's path for hits stays allocation-free.`) above `private val targetMasks` / `targetCandidates`.
- `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/base/Bullet.kt:108–109` (`// Sweeps the segment the bullet actually travelled…`) above `private fun detectHit`.

## Fix
- `shouldMoveShip`: `/** On desktop the cursor is moved back to the centre after each movement; this flag filters out the event that move causes. */`
- `targetMasks`: `/** Reused every frame so sweeping the bullet's path for hits stays allocation-free (as is [targetCandidates]). */`
- `detectHit`: `/** Sweeps the segment the bullet travelled this frame rather than testing only its end position, so a fast shot (or a frame hitch) cannot tunnel through a target. */`
- Leave the statement-level comments inside function bodies (ShipDestination.kt:81–82) as they are.

## Behaviour
Comments only.

## Public API
None.

## Tests
None.

## Verify
`./gradlew :examples:game-space-squadron:compileKotlinDesktop`

## Manual check
none
