# Arm Space Squadron's held-Spacebar guard only when the game ends, not when it starts

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** Desktop, Web (and Android/iOS with a hardware keyboard)
**Files:** `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/managers/UIManager.kt`

## Problem

Reviewed at `0008d027`. `UIManager.onInitialize` (~line 89) is meant to swallow the Spacebar release that follows a game over, so a player who dies while holding Space (the fire key) does not instantly restart:

```kotlin
gameplayManager.isGameOver
    .filter { true }
    .onEach {
        if (keyboardInputManager.isKeyPressed(Key.Spacebar)) {
            shouldDismissNextSpacebarRelease = true
        }
    }
    .launchIn(scope)
```

`filter { true }` lets every emission through, including `isGameOver` turning `false` when a game starts. Starting a game with the mouse (Play button) while holding Space arms the guard too; if the player then pauses (Esc) still holding Space, the release that should resume the game from the pause menu (`onKeyReleased` → `playGame()`) is swallowed. Minor in effect, but the predicate is plainly a typo for `filter { it }`.

## Fix

```kotlin
gameplayManager.isGameOver
    .filter { it }
```

No other change; `filter` stays imported.

## Tests

None: examples have no test source sets; the behaviour needs keyboard input on a running game.

## Manual check

Desktop Showcase, Space Squadron:
1. Hold Space, click Play with the mouse, keep holding, press Esc to pause, then release Space: the game resumes (before the fix it stayed paused until a second press).
2. Hold Space until the ship is destroyed, release Space: the game-over menu stays up (the guard still works); a second Space press-and-release starts a new game.
