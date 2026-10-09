# Correct the Blocky's Journey `CLAUDE.md` description of which managers each instance holds.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-blockys-journey/CLAUDE.md`

## Problem
`examples/game-blockys-journey/CLAUDE.md`, "Architecture: two Kubriko instances": "The `SerializationManager` and `SpriteManager` are both shared between instances…". In `BlockysJourneyGameStateHolder.kt` (:139–171) the `backgroundSerializationManager` is registered only in `backgroundKubriko`; the managers shared by both instances are `sharedMusicManager`, `sharedSoundManager`, `sharedSpriteManager` and `sharedLoadingManager`. The plugin list also says `keyboard-input` / `pointer-input` without noting that their managers are registered (:122–134) but unused — no actor implements `KeyboardInputAware` or `PointerInputAware`.

## Fix
Run after this lane's other Blocky's Journey plans (G02, G06, G09, G21, G27, G29, G30), folding in any references they changed. Then:
- Replace the sentence with: "`SerializationManager` lives only in `backgroundKubriko`, which loads the level; `MusicManager`, `SoundManager`, `SpriteManager` and `LoadingManager` are registered in both instances, so resources load once and the level is prerendered before the main game starts."
- Append to the `pointer-input` and `keyboard-input` bullets: "(registered, but not used by any actor yet)". G57 decides their fate.

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
