# Correct the Annoyed Penguins `CLAUDE.md` description of `Star` and of the keyboard plugin.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/CLAUDE.md`

## Problem
`examples/game-annoyed-penguins/CLAUDE.md`:
- :42 `| `Star` | `Visible`, `Editable` | Non-physics collectible; picked up via collision mask overlap checked in `GameplayManager.onUpdate`. |` — `Star` (`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/Star.kt:40`) is `Visible, Editable<Star>, Dynamic, CollisionDetector`; its `onCollisionDetected` (with `Penguin` / `DestructiblePhysicsObject`) calls `gameplayManager.onStarCollected()`, then it shrinks in `update()` and removes itself; `GameplayManager.onUpdate` only compares `collectedStarCount` with `totalStarCount` to run the level-end timer.
- :20 `keyboard-input — KeyboardInputManager (available but minimal use in this game).` — it is registered (`AnnoyedPenguinsGameStateHolder.kt:166`) but no actor or manager uses it.

## Fix
Run after the code plans of this lane that touch Annoyed Penguins (G01, G05, G12, G15–G17, G20, G26, G28, G31–G34, G37), and fold in any doc change they need (grep this `CLAUDE.md` for names they moved). Then:
- Star row: `| `Star` | `Visible`, `Dynamic`, `CollisionDetector`, `Editable` | Collectible; when a penguin or a destructible block hits it, it calls `GameplayManager.onStarCollected()`, shrinks and removes itself. `GameplayManager.onUpdate` ends the level once `collectedStarCount` reaches `totalStarCount`. |`
- keyboard line: `- `keyboard-input` — `KeyboardInputManager` is registered, but no actor uses it yet.` (G57 decides whether to remove it; update this line again there.)

## Behaviour
Docs only.

## Public API
None.

## Tests
None.

## Verify
none (docs)

## Manual check
none
