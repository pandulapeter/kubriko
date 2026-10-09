# Share the duplicated game UI helpers (hover tracking, font typography) through `examples/shared`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Files:** `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/ui/GameHover.kt` (new), `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/ui/GameTypography.kt` (new), `examples/shared/CLAUDE.md`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/AnnoyedPenguinsButton.kt`, `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/ui/BlockysJourneyButton.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/SpaceSquadronButton.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/ui/WallbreakerButton.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/ui/WallbreakerTextButton.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/AnnoyedPenguinsTheme.kt`, `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/ui/BlockysJourneyTheme.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/SpaceSquadronTheme.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/ui/WallbreakerTheme.kt`; with option B also each game's `managers/UserPreferencesManager.kt`, `ui/InfoDialog.kt`, `managers/UIManager.kt` (AP, BJ)

## Problem
- The same hover-tracking loop is pasted into five buttons (`AnnoyedPenguinsButton`, `BlockysJourneyButton`, `SpaceSquadronButton`, `WallbreakerButton`, `WallbreakerTextButton`):
  ```kotlin
  .pointerInput(Unit) {
      awaitPointerEventScope {
          while (true) {
              val event = awaitPointerEvent()
              when (event.type) {
                  PointerEventType.Enter -> { isActive.value = true; onPointerEnter() }
                  PointerEventType.Exit -> { isActive.value = false }
              }
          }
      }
  }
  ```
  (e.g. `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/ui/SpaceSquadronButton.kt:63–78`). Keyed on `Unit`, it also keeps the first `onPointerEnter` lambda forever.
- `XTypography()` is written out four times (`AnnoyedPenguinsTheme.kt:72`, `BlockysJourneyTheme.kt:72`, `SpaceSquadronTheme.kt:72`, `WallbreakerTheme.kt:73`): fifteen `copy(fontFamily = fontFamily)` lines that differ only in the font.
- Annoyed Penguins and Blocky's Journey share near-identical files: `UIManager.kt` and `UserPreferencesManager.kt` are identical apart from the package; `InfoDialog.kt` differs in two lines (the back button's alignment); `UserPreferencesManager` is identical across all four games except for the click-sound function it calls.

`tools/ui-components` is the wrong home: it is published, and these are Showcase-game styling helpers. `examples/shared` already holds `GameButton` and `gameRipple` for exactly this purpose.

## Fix
- `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/ui/GameHover.kt`: `fun Modifier.gameHover(onEnter: () -> Unit, onExit: () -> Unit = {}): Modifier` implementing the loop once; it reads the callbacks through `rememberUpdatedState` (a `composed {}`-free version: a `Modifier.Node` or `pointerInput(Unit)` over updated-state holders), so a changed lambda is honoured. Each button keeps its own `isActive` state and passes `onEnter = { isActive.value = true; onPointerEnter() }, onExit = { isActive.value = false }`.
- `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/ui/GameTypography.kt`: `fun Typography.withFontFamily(fontFamily: FontFamily): Typography` (pure, all fifteen styles). Each theme keeps its `@Composable` `XFontFamily()` (font loading needs composition) and calls `Typography().withFontFamily(XFontFamily())`.
- Update `examples/shared/CLAUDE.md` ("What lives here").

## Decision
How far to share:
- **A (recommended):** only the two helpers above — pure duplication with no game-specific behaviour.
- **B:** additionally move a `GameUserPreferencesManager(persistenceManager, onToggled: () -> Unit)` and the AP/BJ `UIManager` into `examples/shared`. Saves ~150 lines but couples four games' managers to one class, and the click sound already differs per game (`playClickSoundEffect` vs `playButtonToggleSoundEffect`), so it needs a callback seam; G52 is about to change who plays that sound.
- **C:** keep every game standalone (each game reads as a self-contained example), accept the duplication.

## Behaviour
Same hover and typography behaviour. The only difference: a button whose `onPointerEnter` lambda changes identity between compositions now calls the newest one (today it calls the first). All current callers pass method references on a remembered state holder, so nothing observable changes.

## Public API
None (examples unpublished).

## Tests
None (UI pointer handling and Compose `Typography` need a composition / no pure logic worth a unit test).

## Verify
`./gradlew :examples:shared:compileKotlinDesktop :examples:game-annoyed-penguins:compileKotlinDesktop :examples:game-blockys-journey:compileKotlinDesktop :examples:game-space-squadron:compileKotlinDesktop :examples:game-wallbreaker:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
Hover each game's buttons on desktop and web: hover sound and highlight as before; fonts unchanged.
