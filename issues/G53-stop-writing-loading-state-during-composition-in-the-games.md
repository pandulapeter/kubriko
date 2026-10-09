# Stop writing loading state during composition in the games' `LoadingManager`s.

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/managers/LoadingManager.kt`, `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/managers/LoadingManager.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/managers/LoadingManager.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/managers/LoadingManager.kt`, the four entry Composables (`*Game.kt`), the four `*GameStateHolderImpl.kt` (`navigateBack` reads `isLoadingDone`); optionally `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/…` for a shared helper, `examples/shared/CLAUDE.md`

## Problem
Each `LoadingManager` mutates state from inside composition:
```kotlin
@Composable
fun isGameLoaded() = (isInitialized.collectAsState().value
        && areMenuResourcesLoaded()
        && areGameResourcesLoaded.collectAsState().value).also {
    isLoadingDone = it
}
…
override fun Composable(windowInsets: WindowInsets) {
    if (!isFontLoaded.value) {
        isFontLoaded.update { preloadedFont(Res.font.kanit_regular).value != null }
    }
}
```
(`examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/managers/LoadingManager.kt:82–93`; the same at AP :95–105, BJ :121–131, SS :95–105). `isLoadingDone` is a plain `var` read by `navigateBack` (from the back-press dispatcher), written as a side effect of a composition that may be discarded or run again; `isFontLoaded.update {}` writes a `StateFlow` that the same composition reads. Compose rules call for `SideEffect`/effects for both.

## Fix
- `isGameLoaded()` computes the value without side effects; the write moves to `SideEffect { isLoadingDone = isGameLoaded }` at its call site, or `isLoadingDone` becomes a `MutableStateFlow<Boolean>` updated in a `LaunchedEffect(isGameLoaded)`.
- Font preloading: `val font = preloadedFont(Res.font.…)`, then `LaunchedEffect(font.value) { if (font.value != null) isFontLoaded.value = true }` (or `SideEffect`).
- Optional: a shared `@Composable fun rememberPreloaded(...)`-style helper in `examples/shared` if the four copies stay identical (they are, apart from the font and resource lists).

## Decision
- **SideEffect-based minimal fix (recommended)** vs reworking each `LoadingManager` to expose a single `StateFlow<Boolean>` (menu resources are `@Composable` preloads, so a pure-flow version would need the composable part to feed a flow anyway).
- Share a helper in `examples/shared` now, or after G50.

## Behaviour
Same moment the loading overlay disappears; `navigateBack` sees `isLoadingDone` one frame later at most (after the composition commits), which is when the menu actually appears.

## Public API
None.

## Tests
None (composition timing).

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop :examples:game-blockys-journey:compileKotlinDesktop :examples:game-space-squadron:compileKotlinDesktop :examples:game-wallbreaker:compileKotlinDesktop`

## Manual check
Cold-start each game on web (slowest loading): the spinner shows, the game appears once fonts/audio/sprites load, and pressing back during loading does nothing (as today).
