# Move `createAnnoyedPenguinsGameStateHolder` out of `AnnoyedPenguinsGame.kt` into `AnnoyedPenguinsGameStateHolderFactory.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGameStateHolderFactory.kt` (new)

## Problem
`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt` holds two public things: the `AnnoyedPenguinsGame` Composable and the factory `fun createAnnoyedPenguinsGameStateHolder(` (:57-66), which builds `AnnoyedPenguinsGameStateHolderImpl`. The code style puts every non-private UI Composable in a file of its own named after it; the factory is not part of the Composable and has nothing to do with rendering.

## Fix
- Create `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGameStateHolderFactory.kt` (MPL-2.0 header copied from `AnnoyedPenguinsGame.kt`, same package `com.pandulapeter.kubriko.gameAnnoyedPenguins`).
- Move `fun createAnnoyedPenguinsGameStateHolder(...)` verbatim into it, with the imports it needs (`AnnoyedPenguinsGameStateHolder`, `AnnoyedPenguinsGameStateHolderImpl`).
- Remove those imports from `AnnoyedPenguinsGame.kt` only if nothing else there uses them (the Composable still casts `stateHolder as AnnoyedPenguinsGameStateHolderImpl`, so that import stays).
- The function keeps its package, name and signature, so `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ExampleScreen.kt` (`import com.pandulapeter.kubriko.gameAnnoyedPenguins.createAnnoyedPenguinsGameStateHolder`) compiles unchanged. No Swift or JS code calls it by its JVM facade name (checked: no `AnnoyedPenguinsGameKt` reference in the repo).
- Grep the repo (docs, `CLAUDE.md` files, skills) for `AnnoyedPenguinsGame.kt` mentions that describe the factory, and update them in the same commit.

## Behaviour
Verbatim move within one package; the function body is unchanged.

## Public API
None that is published. The module's own JVM facade for the factory changes from `AnnoyedPenguinsGameKt` to `AnnoyedPenguinsGameStateHolderFactoryKt`; examples are unpublished and every caller is compiled in this repository.

## Tests
The existing ones (the module has no tests).

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none

Note: the demo and test example modules follow the same "factory in the entry file" pattern (e.g. `PhysicsDemo.kt`, `InputTest.kt`, and their `-noop` twins); they belong to other lanes, which may want to mirror this for consistency.
