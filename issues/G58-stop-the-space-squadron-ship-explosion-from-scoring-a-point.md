# Stop Space Squadron's own ship explosion from scoring a point, and stop the ship pushing HUD state into `UIManager`.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Files:** `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/Explosion.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/AlienShip.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/Ship.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/managers/UIManager.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/managers/GameplayManager.kt` (option), `examples/game-space-squadron/CLAUDE.md`
**Challenged:** amended — notes that after G14 the HUD values are collected at the `ShipStatusBars(...)` call in `UIManager.Composable`, which is where the new flows are collected.

## Problem
- `Explosion.onAdded` scores: `kubriko.get<ScoreManager>().incrementScore()` (`examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/Explosion.kt:35`). Explosions are added both when an alien dies (`AlienShip.kt:153`) and when the player's own ship dies (`Ship.onHit`, `Ship.kt:138`), so dying awards +1 point — and `ScoreManager` auto-saves a new high score from it. An effect actor also owns a scoring rule. `CLAUDE.md` documents "Also increments score" as intended.
- `Ship` pushes HUD values into the UI layer: `uiManager.updateShipMultiShoot(value)` / `uiManager.updateShipHealth(value)` (`Ship.kt:93, :100, :120`) into `UIManager`'s private `shipHealth` / `multiShoot` flows (`UIManager.kt:79–80, :99–101`), so an actor depends on the UI manager.

## Fix
- Move `incrementScore()` out of `Explosion` into the alien's death path (`AlienShip`, where it adds the `Explosion`).
- Expose `health` / `multiShootCount` as `StateFlow`s on `Ship` (or on `GameplayManager`, which already owns the game-over state) and let `UIManager.Composable` collect them (after G14 that is the `ShipStatusBars(healthFraction = …, multiShootFraction = …)` call; `ui/ShipStatusBars.kt` itself stays stateless); drop `updateShipHealth` / `updateShipMultiShoot` and the private `shipHealth` / `multiShoot` flows.
- Update `CLAUDE.md` (`Explosion` no longer scores; who owns ship status).

## Decision
1. Is the death point a bug? **Recommended: yes** — remove it (final scores and saved high scores become one lower than today for every game, matching the number of aliens shot). Alternative: keep it and only move the rule out of `Explosion`.
2. Who owns ship status for the HUD: **`Ship` exposing flows (recommended)**, or `GameplayManager`.

## Behaviour
With decision 1 = yes: the score no longer increases when the player's ship explodes. Everything else unchanged.

## Public API
None.

## Tests
None practical without a running particle/actor scene; a `desktopTest` with `:tools:test-fixtures`' manual-tick instance could add an `AlienShip`'s death and the ship's death and check the score, if the actors can be constructed without sprites (they use `SpriteManager`, which needs no Skia until drawing).

## Verify
`./gradlew :examples:game-space-squadron:compileKotlinDesktop`

## Manual check
Play until the ship dies: the final score equals the number of aliens destroyed; the health and multi-shot bars still update.
