# Correct the Wallbreaker `CLAUDE.md` description of the background instance.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-wallbreaker/CLAUDE.md`

## Problem
`examples/game-wallbreaker/CLAUDE.md` says: "`backgroundKubriko`: contains `FogShader` and shared audio managers; has no `StateManager` so it always runs." `Kubriko.newInstance` always adds the default Managers, so `backgroundKubriko` (`WallbreakerGameStateHolder.kt:63–71`) gets a default `StateManager` (`shouldAutoStart = true`); it also holds its own `ShaderManager` and the `LoadingManager`.

## Fix
Run after this lane's other Wallbreaker plans (G04, G08, G11, G18, G19, G23, G35). Replace the bullet with: "`backgroundKubriko`: contains `FogShader`, its own `ShaderManager`, the `LoadingManager` and the shared audio managers. It uses the engine's default `StateManager` (`shouldAutoStart = true`), so it runs whenever it is shown, independently of the game's pause state."

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
