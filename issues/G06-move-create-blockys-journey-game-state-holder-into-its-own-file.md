# Move `createBlockysJourneyGameStateHolder` out of `BlockysJourneyGame.kt` into `BlockysJourneyGameStateHolderFactory.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/BlockysJourneyGame.kt`, `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/BlockysJourneyGameStateHolderFactory.kt` (new)

## Problem
`examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/BlockysJourneyGame.kt` holds two public things: the `BlockysJourneyGame` Composable and the factory `fun createBlockysJourneyGameStateHolder(` (:55-63), which builds `BlockysJourneyGameStateHolderImpl`. The code style puts every non-private UI Composable in a file of its own named after it; the factory is not part of the Composable and has nothing to do with rendering.

## Fix
- Create `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/BlockysJourneyGameStateHolderFactory.kt` (MPL-2.0 header copied from `BlockysJourneyGame.kt`, same package `com.pandulapeter.kubriko.gameBlockysJourney`).
- Move `fun createBlockysJourneyGameStateHolder(...)` verbatim into it, with the imports it needs (`BlockysJourneyGameStateHolder`, `BlockysJourneyGameStateHolderImpl`).
- Remove those imports from `BlockysJourneyGame.kt` only if nothing else there uses them (the Composable still casts `stateHolder as BlockysJourneyGameStateHolderImpl`, so that import stays).
- The function keeps its package, name and signature, so `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ExampleScreen.kt` (`import com.pandulapeter.kubriko.gameBlockysJourney.createBlockysJourneyGameStateHolder`) compiles unchanged. No Swift or JS code calls it by its JVM facade name (checked: no `BlockysJourneyGameKt` reference in the repo).
- Grep the repo (docs, `CLAUDE.md` files, skills) for `BlockysJourneyGame.kt` mentions that describe the factory, and update them in the same commit.

## Behaviour
Verbatim move within one package; the function body is unchanged.

## Public API
None that is published. The module's own JVM facade for the factory changes from `BlockysJourneyGameKt` to `BlockysJourneyGameStateHolderFactoryKt`; examples are unpublished and every caller is compiled in this repository.

## Tests
The existing ones (the module has no tests).

## Verify
`./gradlew :examples:game-blockys-journey:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none

Note: the demo and test example modules follow the same "factory in the entry file" pattern (e.g. `PhysicsDemo.kt`, `InputTest.kt`, and their `-noop` twins); they belong to other lanes, which may want to mirror this for consistency.
