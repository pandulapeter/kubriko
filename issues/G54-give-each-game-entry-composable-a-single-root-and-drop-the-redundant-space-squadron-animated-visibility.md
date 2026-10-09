# Give each game's entry Composable a single root that takes `modifier`, and drop Space Squadron's redundant nested `AnimatedVisibility`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Rebased:** on 70de96c6 after the Now plans landed.
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt`, `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/BlockysJourneyGame.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGame.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/WallbreakerGame.kt`

## Problem
- The entry Composables emit several siblings straight into the caller's layout and apply the caller's `modifier` to more than one of them: Wallbreaker to the background `KubrikoViewport` (`modifier = modifier.fillMaxSize().background(…)`, `WallbreakerGame.kt:53`) and to both `AnimatedVisibility`s (:59, :71); Space Squadron (`SpaceSquadronGame.kt:50` viewport, :57 loading `AnimatedVisibility`), Annoyed Penguins (`AnnoyedPenguinsGame.kt:59` viewport, :151 loading `AnimatedVisibility`) and Blocky's Journey (`BlockysJourneyGame.kt:66` viewport, :153 loading `AnimatedVisibility`) twice each. Any padding, size or click a caller passes is applied two or three times, each to a different sibling, and siblings that do not get it ignore it. The Showcase passes no `modifier` (`app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ExampleScreen.kt:40–65`), which is the only reason it works.
- `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGame.kt:92` wraps the menu (`MenuOverlay(...)`, :98) in `AnimatedVisibility(visible = isGameLoaded, enter = fadeIn() + scaleIn(), exit = scaleOut() + fadeOut())` inside the outer `AnimatedVisibility(visible = isGameLoaded, …)` (:72). Its content is composed only when the outer one is visible, so its `visible` is already `true` on first composition (no enter animation); its exit could only run if `isGameLoaded` went back to `false`, which the loading managers never do.

## Fix
- Wrap each entry Composable's content in one `Box(modifier = modifier)`; children use `Modifier` (the background viewport keeps `.fillMaxSize().background(…)`; the `AnimatedVisibility`s lose `modifier =`). Use `Box(modifier.fillMaxSize())` if the caller should not need to size it.
- Space Squadron: replace the inner `AnimatedVisibility(visible = isGameLoaded, …) { MenuOverlay(...) }` with the bare `MenuOverlay(...)` call.

## Decision
The `Box` changes the layout tree the Showcase sees (one child instead of several). The Showcase's `ExampleScreen` parent is the `AnimatedContent(contentAlignment = Alignment.Center)` in `ShowcaseContent.kt:316–334` (a `Box`-like container), so stacking stays the same, but this is a layout change, not a pure refactor:
- **A (recommended):** add the root `Box`; manually compare each game in the Showcase on desktop and Android before/after.
- **B:** keep the siblings and only stop reusing `modifier` (apply it to the background viewport alone, document that it positions the whole game).

## Behaviour
With option A and the Showcase's current call (no `modifier`): same rendering. With a non-default `modifier`, it is now applied once to the whole game, which is what the parameter promises. Removing the inner Space Squadron `AnimatedVisibility` changes nothing visible (see Problem).

## Public API
None published; the entry Composables' signatures stay the same.

## Tests
None (layout).

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop :examples:game-blockys-journey:compileKotlinDesktop :examples:game-space-squadron:compileKotlinDesktop :examples:game-wallbreaker:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
Open each game in the desktop and Android Showcase, switch between games (crossfade), enter/leave fullscreen, rotate on Android: loading spinner, background, game and menus are placed exactly as before.
