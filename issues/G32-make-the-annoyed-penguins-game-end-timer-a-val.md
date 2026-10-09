# Make the Annoyed Penguins `gameEndTimer` a `val`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/managers/GameplayManager.kt`

## Problem
`private var gameEndTimer = Timer(` (`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/managers/GameplayManager.kt:57`) is never reassigned (grep: only read at :115 and :117).

## Fix
`private var gameEndTimer` → `private val gameEndTimer`.

## Behaviour
None.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop`

## Manual check
none
