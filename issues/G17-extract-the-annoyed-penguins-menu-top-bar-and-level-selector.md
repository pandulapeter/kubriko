# Extract `MenuTopBar` and `LevelSelector` out of the Annoyed Penguins `MenuOverlay`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/MenuOverlay.kt`

## Problem
`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/MenuOverlay.kt` nests about eight layout levels: the button bar (`Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp))`, :135–192, with two inner `Row`s and a `Spacer(weight(1f))`) and the level picker (`Row(modifier = Modifier.padding(horizontal = 16.dp).background(…CircleShape…).border(…).clip(CircleShape).horizontalScroll(levelSelectorScrollState).padding(…))`, :204–232) are inlined deep in the tree.

## Fix
Add two `private` Composables in the same file and call them where the blocks were:
- `MenuTopBar(onCloseButtonPressed, onInfoButtonPressed, isSceneEditorEnabled, areSoundEffectsEnabled, onSoundEffectsToggled, isMusicEnabled, onMusicToggled, isInFullscreenMode, onFullscreenModeToggled, playHoverSoundEffect, playToggleSoundEffect)` — the outer `Row` of :135–192 verbatim as its root (its `Spacer(Modifier.defaultMinSize(minWidth = 6.dp).weight(1f))` stays inside, in its own `RowScope`).
- `LevelSelector(allLevels: ImmutableList<String>, currentLevel: String?, onLevelSelected: (String) -> Unit, playHoverSoundEffect: () -> Unit, scrollState: ScrollState)` — the `Row` of :204–232 verbatim as its root, using `scrollState` for `horizontalScroll`.
- `MenuOverlay` passes its parameters through (`levelSelectorScrollState` as `scrollState`). Neither block takes a parent-scope modifier: the top bar is a child of a `Box` and only uses its own modifiers, the level row is a child of a `Column` without `weight`/`align`.

## Behaviour
Each extracted function emits exactly the `Row` it replaces as its single root, so the parent `Box` / `Column` see the same children with the same modifiers. `levelSelectorScrollState` is still created by `MenuOverlay`'s default parameter, so scroll position survives as before.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop`

## Manual check
Open the Annoyed Penguins menu (with and without the scene editor flag, and in fullscreen on desktop): the top bar and the level picker look and scroll as before.
