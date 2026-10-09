# Extract the Wallbreaker menu's two button rows into `PrimaryButtons` and `PreferenceButtons`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/ui/MenuOverlay.kt`

## Problem
`examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/ui/MenuOverlay.kt` inlines two button rows inside `FlowRow` inside `BoxWithConstraints` inside `Column` inside `Box` inside `WallbreakerAnimatedVisibility`: the play/info/exit row with the pulsing `rememberInfiniteTransition` scale (:104–139) and the sound/music/fullscreen row (:140–167).

## Fix
Run after G11. Add two `private` Composables in the same file:
- `PrimaryButtons(isActive, shouldShowResumeButton, onResumeButtonPressed, onRestartButtonPressed, onInfoButtonPressed, onExitButtonPressed, onButtonHover)` — the `Row(modifier = Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp))` of :104–139 verbatim, including `rememberInfiniteTransition()` / `animateFloat(...)` and `modifier = if (isActive) Modifier.scale(scale) else Modifier`.
- `PreferenceButtons(areSoundEffectsEnabled, onSoundEffectsToggled, isMusicEnabled, onMusicToggled, isInFullscreenMode, onFullscreenModeToggled, onButtonHover)` — the `Row` of :140–167 verbatim.
- The `FlowRow` body becomes `PrimaryButtons(...)` then `PreferenceButtons(...)`.

## Behaviour
Each function emits its `Row` as a single root, so `FlowRow` still lays out exactly two children and `maxItemsInEachRow` behaves the same. The infinite transition is still created inside the first row's composition and only while the menu is shown.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-wallbreaker:compileKotlinDesktop`

## Manual check
Open the Wallbreaker pause menu in a short and a tall window: the rows wrap the same way and the play button pulses only while no dialog is open.
