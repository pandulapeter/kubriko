# Rename `SpaceSquadronMenuOverlay` to `MenuOverlay` so it matches its file.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/MenuOverlay.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGame.kt`

## Problem
`examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/MenuOverlay.kt:60` declares `internal fun SpaceSquadronMenuOverlay(`, so the file is not named after its Composable. The other three games name the same Composable `MenuOverlay` in a `MenuOverlay.kt`.

## Fix
- Rename the function to `MenuOverlay` (no other `MenuOverlay` exists in the `com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui` package — confirm with grep).
- Update the import (`import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.SpaceSquadronMenuOverlay`) and the call (`SpaceSquadronMenuOverlay(`, :109) in `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGame.kt`.
- Grep the whole repo (docs, `CLAUDE.md` files, skills) for `SpaceSquadronMenuOverlay` and fix every reference.

## Behaviour
Rename of an internal function.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-space-squadron:compileKotlinDesktop`

## Manual check
none
