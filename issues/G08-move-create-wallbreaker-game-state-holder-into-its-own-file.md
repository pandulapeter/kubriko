# Move `createWallbreakerGameStateHolder` out of `WallbreakerGame.kt` into `WallbreakerGameStateHolderFactory.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/WallbreakerGame.kt`, `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/WallbreakerGameStateHolderFactory.kt` (new)

## Problem
`examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/WallbreakerGame.kt` holds two public things: the `WallbreakerGame` Composable and the factory `fun createWallbreakerGameStateHolder(` (:42-48), which builds `WallbreakerGameStateHolderImpl`. The code style puts every non-private UI Composable in a file of its own named after it; the factory is not part of the Composable and has nothing to do with rendering.

## Fix
- Create `examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/WallbreakerGameStateHolderFactory.kt` (MPL-2.0 header copied from `WallbreakerGame.kt`, same package `com.pandulapeter.kubriko.gameWallbreaker`).
- Move `fun createWallbreakerGameStateHolder(...)` verbatim into it, with the imports it needs (`WallbreakerGameStateHolder`, `WallbreakerGameStateHolderImpl`).
- Remove those imports from `WallbreakerGame.kt` only if nothing else there uses them (the Composable still casts `stateHolder as WallbreakerGameStateHolderImpl`, so that import stays).
- The function keeps its package, name and signature, so `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ExampleScreen.kt` (`import com.pandulapeter.kubriko.gameWallbreaker.createWallbreakerGameStateHolder`) compiles unchanged. No Swift or JS code calls it by its JVM facade name (checked: no `WallbreakerGameKt` reference in the repo).
- Grep the repo (docs, `CLAUDE.md` files, skills) for `WallbreakerGame.kt` mentions that describe the factory, and update them in the same commit.

## Behaviour
Verbatim move within one package; the function body is unchanged.

## Public API
None that is published. The module's own JVM facade for the factory changes from `WallbreakerGameKt` to `WallbreakerGameStateHolderFactoryKt`; examples are unpublished and every caller is compiled in this repository.

## Tests
The existing ones (the module has no tests).

## Verify
`./gradlew :examples:game-wallbreaker:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none

Note: the demo and test example modules follow the same "factory in the entry file" pattern (e.g. `PhysicsDemo.kt`, `InputTest.kt`, and their `-noop` twins); they belong to other lanes, which may want to mirror this for consistency.
