# Drop the redundant canvas wrapper and pointer check in the Annoyed Penguins `Slingshot`, and move its last property up.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/slingshot/Slingshot.kt`

## Problem
In `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/slingshot/Slingshot.kt`:
- `draw()` wraps a plain `DrawScope` call in an unused canvas lambda (:146–148):
  ```kotlin
  drawIntoCanvas { canvas ->
      drawImage(background)
  }
  ```
- In `update()`, the `else` branch re-checks `if (pressedPointerPositions.isNotEmpty())` (:201) inside the outer `if (pressedPointerPositions.isNotEmpty())` (:193), where it is always true.
- `private var isPointerPressedInPreviousStep = false` (:189) is declared between two functions instead of with the other properties.

## Fix
- Replace the `drawIntoCanvas { … }` block with `drawImage(background)`; remove the `drawIntoCanvas` import if nothing else uses it.
- Unwrap the inner `if (pressedPointerPositions.isNotEmpty()) { … }`, keeping its body and the `// Detect if the initial press was on the slingshot` comment.
- Move `private var isPointerPressedInPreviousStep = false` up next to the other private state (after `private val activePenguin: Penguin? get() = …`, :126).

## Behaviour
`drawIntoCanvas` is an inline call that just invokes the lambda; `drawImage` is already resolved on the outer `DrawScope`. The removed condition is always true where it stands. Moving a property declaration with a constant initialiser does not change initialisation (nothing reads it during construction).

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop`

## Manual check
none
