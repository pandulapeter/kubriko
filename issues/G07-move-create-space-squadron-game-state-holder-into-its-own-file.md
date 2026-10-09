# Move `createSpaceSquadronGameStateHolder` out of `SpaceSquadronGame.kt` into `SpaceSquadronGameStateHolderFactory.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGame.kt`, `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGameStateHolderFactory.kt` (new)

## Problem
`examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGame.kt` holds two public things: the `SpaceSquadronGame` Composable and the factory `fun createSpaceSquadronGameStateHolder(` (:40-46), which builds `SpaceSquadronGameStateHolderImpl`. The code style puts every non-private UI Composable in a file of its own named after it; the factory is not part of the Composable and has nothing to do with rendering.

## Fix
- Create `examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGameStateHolderFactory.kt` (MPL-2.0 header copied from `SpaceSquadronGame.kt`, same package `com.pandulapeter.kubriko.gameSpaceSquadron`).
- Move `fun createSpaceSquadronGameStateHolder(...)` verbatim into it, with the imports it needs (`SpaceSquadronGameStateHolder`, `SpaceSquadronGameStateHolderImpl`).
- Remove those imports from `SpaceSquadronGame.kt` only if nothing else there uses them (the Composable still casts `stateHolder as SpaceSquadronGameStateHolderImpl`, so that import stays).
- The function keeps its package, name and signature, so `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ExampleScreen.kt` (`import com.pandulapeter.kubriko.gameSpaceSquadron.createSpaceSquadronGameStateHolder`) compiles unchanged. No Swift or JS code calls it by its JVM facade name (checked: no `SpaceSquadronGameKt` reference in the repo).
- Grep the repo (docs, `CLAUDE.md` files, skills) for `SpaceSquadronGame.kt` mentions that describe the factory, and update them in the same commit.

## Behaviour
Verbatim move within one package; the function body is unchanged.

## Public API
None that is published. The module's own JVM facade for the factory changes from `SpaceSquadronGameKt` to `SpaceSquadronGameStateHolderFactoryKt`; examples are unpublished and every caller is compiled in this repository.

## Tests
The existing ones (the module has no tests).

## Verify
`./gradlew :examples:game-space-squadron:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none

Note: the demo and test example modules follow the same "factory in the entry file" pattern (e.g. `PhysicsDemo.kt`, `InputTest.kt`, and their `-noop` twins); they belong to other lanes, which may want to mirror this for consistency.
