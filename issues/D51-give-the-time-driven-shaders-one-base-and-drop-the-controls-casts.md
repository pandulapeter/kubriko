# Give the five time-driven animation shaders one shared base and map each demo type to its code and controls without unchecked casts.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Rebased:** on 70de96c6 after the Now plans landed.
**Files:** `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/shaders/CloudShader.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/shaders/EtherShader.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/shaders/GradientShader.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/shaders/NoodleShader.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/shaders/WarpShader.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/shaders/TimeDrivenShader.kt` (new), `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ShaderAnimationsDemoStateHolderImpl.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ShaderAnimationDemoHolder.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/managers/ShaderAnimationsDemoManager.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ShaderAnimationDemoType.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ui/ControlsContainer.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/ShaderAnimationsDemo.kt`, `examples/demo-shader-animations/CLAUDE.md`
**Challenged:** amended — rebased on D26: `ControlsContainer` gets a per-type `getControls`/`getCode` lookup in place of D26's `getManager` (still resolved inside `AnimatedContent`), and `ShaderAnimationsDemo.kt` joins Files.

## Problem
- Each shader repeats `private lateinit var metadataManager: MetadataManager`, `onAdded { metadataManager = kubriko.get() }`, `update() { shaderState = shaderState.copy(time = (metadataManager.activeRuntimeInMilliseconds.value % 100000L) / 1000f) }` and `fun updateState(state: State) { this.shaderState = state.copy(time = this.shaderState.time) }` (e.g. `GradientShader.kt:27-39`).
- The Impl passes `updater = { shader, state -> shader.updateState(state) }` five times (`ShaderAnimationsDemoStateHolderImpl.kt:28-65`, e.g. :32 `updater = { shader, state -> shader.updateState(state) },`).
- `ControlsContainer.kt`'s private `Code` (:157–178) switches on the type for `CODE` (:171–177), and private `Controls(manager: ShaderAnimationsDemoManager<*, *>, demoType)` (:180–237) switches again under one `@Suppress("UNCHECKED_CAST")` (:194) and does five `manager as ShaderAnimationsDemoManager<XShader, XShader.State>` casts (e.g. :197) before calling the per-type `ui/controls/XControls.kt` Composables with `manager.shaderState.collectAsState().value` and `manager::setState`.

## Fix
New abstraction (hence Planned): `internal abstract class TimeDrivenShader<S : Shader.State>(initialState: S) : Shader<S>, Dynamic` owning `shaderState`, `metadataManager`, `update()` and `updateState()`, with `protected abstract fun S.withTime(time: Float): S` (each `State` data class implements it as `copy(time = time)`). `ShaderAnimationsDemoManager` then needs no `updater` (`shader.updateState(it)`). For the UI, give each `ShaderAnimationDemoHolder` (or a sealed per-type holder) its `code: String` and a `@Composable Controls()` lambda typed at construction, so `ControlsContainer` calls `holder.Controls()` without a cast. Since D26 (landed in f7fb6e7c), `ControlsContainer` no longer receives the holder map: it takes `getManager: (ShaderAnimationDemoType) -> ShaderAnimationsDemoManager<*, *>` (`ControlsContainer.kt:75`, called as `getManager(targetState.first)` at :116), wired in `ShaderAnimationsDemo.kt:85` as `getManager = stateHolder::getManager` to the Impl's `fun getManager(demoType: ShaderAnimationDemoType)` (`ShaderAnimationsDemoStateHolderImpl.kt:73`). Replace that parameter with a per-type lookup of the typed binding (e.g. `getControls: (ShaderAnimationDemoType) -> @Composable () -> Unit` / `getCode`), still called with `targetState.first` **inside** the `AnimatedContent` content so the outgoing branch keeps its own type's controls during the crossfade (see D26), and drop the Impl's `getManager`.

## Decision
Where the per-type UI binding lives: (a) a typed `controls: @Composable () -> Unit` built in the Impl next to each holder — **recommended**, keeps `ShaderAnimationDemoType` a plain enum of names; (b) an abstract member on a sealed holder hierarchy (five subclasses).

## Behaviour
Same time formula and per-tick `copy` (one allocation per tick already exists — do not add more; `withTime` must be the same single `copy`). Same controls and code text.

## Public API
None.

## Tests
None (shader state is data; could add a `withTime` round-trip test per state if wanted).

## Verify
`./gradlew :examples:demo-shader-animations:compileKotlinDesktop`

## Manual check
Every tab animates, controls change the shader live, code view shows the right SKSL, switching tabs with controls open does not crash.
