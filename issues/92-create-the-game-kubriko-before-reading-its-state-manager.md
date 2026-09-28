# Create the game's Kubriko instance before reading its StateManager in Blocky's Journey and Annoyed Penguins

**Challenged:** amended — the manual check's scratch host now wraps the state holder in `remember`, since creating it inline re-creates it (and its Kubriko instances) on every recomposition; the fix itself is unchanged.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Files:** `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/BlockysJourneyGame.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt`

## Problem

Reviewed at `0008d027`. In both games the state holder builds its main `Kubriko` lazily — `BlockysJourneyGameStateHolderImpl._kubriko by lazy { MutableStateFlow(Kubriko.newInstance(..., stateManager, ...)) }` (~line 152) and the same in `AnnoyedPenguinsGameStateHolderImpl` (~line 204). The laziness is deliberate: the desktop scene-editor wrappers build the same state holder only for its serialization manager and custom managers and must not spin up the game instance.

`StateManager.isRunning` / `isFocused` are `autoInitializingLazy` properties backed by `asStateFlowOnMainThread`, which needs the Manager's scope; the scope is only set when a `Kubriko` instance containing the Manager is constructed (`KubrikoImpl`'s `init` calls `stateManager.initializeInternal(this)`). Yet the public composables read them before anything touches `stateHolder.kubriko`:

`BlockysJourneyGame.kt` (~line 84):

```kotlin
val isGameLoaded = stateHolder.sharedLoadingManager.isGameLoaded()
val isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value
```

`AnnoyedPenguinsGame.kt` (~line 87):

```kotlin
val isGameLoaded = stateHolder.backgroundLoadingManager.isGameLoaded()
val isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value
```

The main instance is only created later, inside `AnimatedVisibility`, by `kubriko = stateHolder.kubriko.value`. Any host that composes these games without first touching `stateHolder.kubriko` crashes on first composition with `IllegalStateException: Cannot use the scope of StateManagerImpl until the Manager has been initialized`. The Showcase happens not to, only because `KubrikoShowcase.kt` (~line 110) collects `activeStateHolder?.kubriko` for the debug menu before `ExampleScreen` composes the game. These composables are the repo's canonical "how to embed a Kubriko game" references, so the ordering dependency is a trap.

## Fix

In each composable, obtain the main instance first, and reuse it for the viewport:

- `BlockysJourneyGame`: right after `stateHolder as BlockysJourneyGameStateHolderImpl`, add

  ```kotlin
  val kubriko = stateHolder.kubriko.collectAsState().value
  ```

  (it must come before the first `stateHolder.stateManager` read), and change the game viewport's `kubriko = stateHolder.kubriko.value` (~line 97) to `kubriko = kubriko`.
- `AnnoyedPenguinsGame`: the same two changes (declaration after `stateHolder as AnnoyedPenguinsGameStateHolderImpl`; ~line 106 `kubriko = stateHolder.kubriko.value` → `kubriko = kubriko`).

Add one short comment on the new line, since the ordering is not obvious from the code: `// Touching kubriko first creates the instance that initializes stateManager.`

Keep the state holders' laziness: the desktop `*GameSceneEditor.kt` wrappers build the state holder only for `serializationManager` / `customManagersForSceneEditor` and never touch `kubriko`, so making `_kubriko` eager would start a second, unused game instance whose Managers the scene editor also registers. `navigateBack()` also reads `stateManager.isRunning`, but only after `isLoadingDone`, which is set by `isGameLoaded()` during a composition that — after this fix — has already created the instance.

No `CLAUDE.md` change needed.

## Tests

None: the examples have no test source sets; the failure is a composition-order crash that needs a Compose host.

## Manual check

On Desktop, in a scratch `main()` (not committed) that opens a `Window { BlockysJourneyGame(stateHolder = remember { createBlockysJourneyGameStateHolder(webRootPathName = "", isSceneEditorEnabled = false, isLoggingEnabled = false) }) }` without anything else touching `stateHolder.kubriko` (the `remember` matters: a state holder created inline in a composable is re-created on every recomposition — the trap plan 93 removes): before the fix it throws the "Cannot use the scope of StateManagerImpl" exception on first frame; after it, the game loads and the menu appears. Repeat with `AnnoyedPenguinsGame`. Then in the Showcase (Desktop and Android) open both games, start a level, pause and resume — behaviour unchanged.
