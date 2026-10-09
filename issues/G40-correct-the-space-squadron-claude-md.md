# Correct the Space Squadron `CLAUDE.md` description of `ShipAnimationWrapper`.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-space-squadron/CLAUDE.md`

## Problem
`examples/game-space-squadron/CLAUDE.md` says `Ship` "Uses `ShipAnimationWrapper` (inner class)". It is `private class ShipAnimationWrapper(` — a private nested class, not an `inner` one (`examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/Ship.kt:245`).

## Fix
Run after this lane's other Space Squadron plans (G03, G07, G10, G13, G14, G22, G24, G25, G36), folding in any references they changed (grep this file for `SpaceSquadronMenuOverlay`, `UIManager`, `Modifiers.kt`). Then change "(inner class)" to "(a private nested class)".

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
