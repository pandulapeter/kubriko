# Collect Space Squadron's `isFocused` once instead of twice.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGame.kt`

## Problem
The menu call in `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGame.kt` collects the same flow twice:
```kotlin
areSoundEffectsEnabled = stateHolder.stateManager.isFocused.collectAsState().value && …,
…
isMusicEnabled = stateHolder.stateManager.isFocused.collectAsState().value && …,
```
(:125 and :127). Wallbreaker, Annoyed Penguins and Blocky's Journey collect it once into a `val`.

## Fix
Run after G24. Inside the inner `AnimatedVisibility(visible = isGameLoaded, …)` content, before the `MenuOverlay(` call, add `val isFocused = stateHolder.stateManager.isFocused.collectAsState().value` and use `isFocused && …` in both arguments.

## Behaviour
Same value in the same composition scope (the argument expressions were already evaluated in that lambda); one subscription instead of two.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-space-squadron:compileKotlinDesktop`

## Manual check
none
