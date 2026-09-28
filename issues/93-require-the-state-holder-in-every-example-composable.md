# Require the state holder in every example's entry composable instead of creating one as a default argument

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all
**Files:**
`examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/WallbreakerGame.kt`,
`examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/SpaceSquadronGame.kt`,
`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGame.kt`,
`examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/BlockysJourneyGame.kt`,
`examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/ContentShadersDemo.kt`,
`examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/IsometricGraphicsDemo.kt`,
`examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/ParticlesDemo.kt`,
`examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/PerformanceDemo.kt`,
`examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/PhysicsDemo.kt`,
`examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/ShaderAnimationsDemo.kt`,
`examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/AudioTest.kt`,
`examples/test-audio-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/AudioTest.kt`,
`examples/test-collision/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/CollisionTest.kt`,
`examples/test-collision-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/CollisionTest.kt`,
`examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/InputTest.kt`,
`examples/test-input-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/InputTest.kt`

## Problem

Reviewed at `0008d027`. Every example's public entry composable declares its state holder with a factory call as the default, e.g. `WallbreakerGame.kt` (~line 53):

```kotlin
@Composable
fun WallbreakerGame(
    modifier: Modifier = Modifier,
    stateHolder: WallbreakerGameStateHolder = createWallbreakerGameStateHolder(
        webRootPathName = "",
        isLoggingEnabled = false,
    ),
    ...
```

and likewise `SpaceSquadronGame`, `AnnoyedPenguinsGame`, `BlockysJourneyGame`, `ContentShadersDemo`, `IsometricGraphicsDemo`, `ParticlesDemo`, `PerformanceDemo`, `PhysicsDemo`, `ShaderAnimationsDemo`, `AudioTest`, `CollisionTest`, `InputTest` and the three `-noop` twins (16 files, listed above).

A composable's default-argument expression is not remembered: it is evaluated every time the function body executes. A caller that relies on the default — which is exactly what the signature invites a reader of these "canonical how-to-use-Kubriko" references to do — gets a brand-new state holder, with new `Kubriko` instances, on every recomposition of the call site (a changed `modifier`, `windowInsets`, `isInFullscreenMode`…). The previous instances are never `dispose()`d: their managers, coroutine scopes, persistence writers and music stay alive, and the game restarts from its menu each time. Several state holders (e.g. `IsometricGraphicsDemoStateHolderImpl`, `WallbreakerGameStateHolderImpl`) build their `Kubriko` instances eagerly in the constructor, so every re-evaluation starts real, running instances.

The Showcase itself always passes `stateHolder = getOrCreateState(...)` by name (`app/shared/.../ExampleScreen.kt`) and never uses the defaults; no other in-repo caller does either.

## Fix

**Decision needed:** how to remove the trap — recommended: **(A) make `stateHolder` required.**

- **(A) Required parameter (recommended).** In all 16 composables, delete the default (`= createXxxStateHolder(...)`) and move `stateHolder` to be the **first** parameter, before `modifier`, following the Compose API guideline that required parameters precede `modifier`. Every caller (only `ExampleScreen.kt`) already passes it by name, so no call site changes. The `-noop` twins must change identically so the Showcase compiles against either variant. The state holder's lifetime then visibly belongs to the host, which is the contract `examples/README.md` already describes ("a StateHolder instance that can be used to persist game state across configuration changes").
- **(B) Remembered default.** Keep the parameter optional and replace each default with a new `@Composable fun rememberXxxStateHolder(...)` that does `remember { createXxxStateHolder(...) }.also { DisposableEffect(it) { onDispose { it.dispose() } } }`. Keeps "zero-argument" embedding, but adds 13 more public functions (plus noop twins) and hides a disposal policy inside a default, which is the thing the Showcase deliberately does not want (it keeps state holders alive across screen switches).

With (A), check with `grep -rn "createXxxStateHolder" examples` that each factory is still used (they are — by `ExampleScreen.kt` and the scene-editor wrappers), so nothing becomes unused.

No `CLAUDE.md` or README change: none documents the defaults.

If plan 92 has landed first, its edits to `BlockysJourneyGame.kt` and `AnnoyedPenguinsGame.kt` are in the function body and do not conflict with this signature change.

## Tests

None: examples have no test source sets; the change is a signature change verified by compilation.

## Manual check

Compile the Showcase with both `showcase.areTestExamplesEnabled=true` and `=false` in `gradle.properties` (the latter swaps in the `-noop` modules) — `./gradlew :app:desktop:compileKotlin` for each. Then run the Showcase on Desktop and open every example once: each loads and behaves as before.
