# Bring demo-isometric-graphics' `CLAUDE.md` in line with the code.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-isometric-graphics/CLAUDE.md`

## Problem
- "`IsometricGraphicsDemo` … renders `IsometricGraphicsContent` plus the standard Showcase `InfoPanel`" — the `InfoPanel` is inside `IsometricGraphicsContent` (`IsometricGraphicsContent.kt:240`).
- "`MainCharacter`, `Character`, `Tree` — all extend `PlanarCuboidModelRenderer`" omits `Bush` (`implementation/logic/actor/Bush.kt`).
- "`LogicManager` (loads `character.json` + `tree.json`, scatters trees)" — it also loads `bush.json` and places up to 64 NPCs, 256 trees and 256 bushes (`LogicManager.kt:70-72, :82, :110, :134`).
- "`files/model/` — `character.json`, `tree.json`" omits `bush.json`.
- "this module used to vendor its own cut-down copy" is history the code style rules out.

## Fix
Correct these five spots in the existing terse style; drop the history clause. If D40–D46 changed anything else the file describes (e.g. D43's MiniMap sentence, `textureResolver`), make sure the text matches the code at this point.

## Behaviour
Docs only.

## Public API
None.

## Tests
None.

## Verify
none (docs only)

## Manual check
none
