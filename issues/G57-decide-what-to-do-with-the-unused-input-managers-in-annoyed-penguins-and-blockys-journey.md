# Decide what to do with the registered-but-unused input managers in Annoyed Penguins and Blocky's Journey.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/AnnoyedPenguinsGameStateHolderImpl.kt`, `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/BlockysJourneyGameStateHolderImpl.kt`, `examples/game-annoyed-penguins/build.gradle.kts`, `examples/game-blockys-journey/build.gradle.kts`, `examples/game-blockys-journey/src/commonMain/composeResources/values/strings.xml`, both modules' `CLAUDE.md`
**Rebased:** on 70de96c6 after the Now plans landed.

## Problem
- Annoyed Penguins registers `KeyboardInputManager` (`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/AnnoyedPenguinsGameStateHolderImpl.kt:163–168`, import at `:28`; dependency at `examples/game-annoyed-penguins/build.gradle.kts:25`), but no actor or manager implements `KeyboardInputAware` or reads it (grep of the module).
- Blocky's Journey registers `KeyboardInputManager` and `PointerInputManager` (`examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/BlockysJourneyGameStateHolderImpl.kt:119–131`, imports at `:24, :28`; dependencies at `examples/game-blockys-journey/build.gradle.kts:24, :26`), but nothing uses them: `Blocky` walks on its own timer.
- Blocky's Journey's info text promises controls that do not exist: `information_contents` (`examples/game-blockys-journey/src/commonMain/composeResources/values/strings.xml:22–23`) — "To move Blocky drag the pointer (mouse or touch) anywhere on the screen to bring up the virtual joystick or use the Up / Left / Down / Right or W / A / S / D keys." The game is marked unfinished.

Unused managers still run every tick and capture input (`PointerInputManager(isActiveAboveViewport = true)` in Blocky's Journey sits above the viewport), and the examples are meant to teach which plugins a game needs.

## Fix / Decision
- **Annoyed Penguins:** remove `KeyboardInputManager` from the state holder and `implementation(projects.plugins.keyboardInput)` from the build file (recommended), unless keyboard controls are planned.
- **Blocky's Journey:**
  - **A (recommended):** keep both managers (the unfinished game is meant to get the joystick/WASD controls), and change the info text to say movement is automatic for now.
  - **B:** remove both managers and their dependencies, and the controls paragraph, until controls are implemented.
  - **C:** implement the promised controls (out of scope for a refactor sweep).
Update both `CLAUDE.md` plugin lists (G38/G39, landed in 64572764/78f1efd3, now mark them as unused: `examples/game-annoyed-penguins/CLAUDE.md:20`, `examples/game-blockys-journey/CLAUDE.md:17–18`).

## Behaviour
Removing an unused manager changes nothing a player sees, except that Blocky's Journey's above-viewport pointer manager no longer receives events (verify the menu buttons still work — they are Compose buttons, not actors). Changing the info text changes on-screen copy.

## Public API
None.

## Tests
None.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop :examples:game-blockys-journey:compileKotlinDesktop`

## Manual check
Play both games with mouse, touch and keyboard: everything that worked still works; the Blocky's Journey info text matches the game.
