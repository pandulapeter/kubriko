# Give the five time-driven animation shaders one shared base and map each demo type to its code and controls without unchecked casts.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Files:** `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/shaders/CloudShader.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/shaders/EtherShader.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/shaders/GradientShader.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/shaders/NoodleShader.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/shaders/WarpShader.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/shaders/TimeDrivenShader.kt` (new), `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ShaderAnimationsDemoStateHolderImpl.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ShaderAnimationDemoHolder.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/managers/ShaderAnimationsDemoManager.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ShaderAnimationDemoType.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ui/ControlsContainer.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/ShaderAnimationsDemo.kt`, `examples/demo-shader-animations/CLAUDE.md`
**Challenged:** amended — rebased on D26: `ControlsContainer` gets a per-type `getControls`/`getCode` lookup in place of D26's `getManager` (still resolved inside `AnimatedContent`), and `ShaderAnimationsDemo.kt` joins Files.

## Problem
- Each shader repeats `private lateinit var metadataManager: MetadataManager`, `onAdded { metadataManager = kubriko.get() }`, `update() { shaderState = shaderState.copy(time = (metadataManager.activeRuntimeInMilliseconds.value % 100000L) / 1000f) }` and `fun updateState(state: State) { this.shaderState = state.copy(time = this.shaderState.time) }` (e.g. `GradientShader.kt:27-39`).
- The Impl passes `updater = { shader, state -> shader.updateState(state) }` five times (originally `ShaderAnimationsDemoStateHolder.kt:97-130`).
- `ControlsContainer.kt`'s `Code` (:161–167) switches on the type for `CODE`, and `Controls` (:184–225) switches again and does five `@Suppress("UNCHECKED_CAST") manager as ShaderAnimationsDemoManager<XShader, XShader.State>` casts.

## Fix
New abstraction (hence Planned): `internal abstract class TimeDrivenShader<S : Shader.State>(initialState: S) : Shader<S>, Dynamic` owning `shaderState`, `metadataManager`, `update()` and `updateState()`, with `protected abstract fun S.withTime(time: Float): S` (each `State` data class implements it as `copy(time = time)`). `ShaderAnimationsDemoManager` then needs no `updater` (`shader.updateState(it)`). For the UI, give each `ShaderAnimationDemoHolder` (or a sealed per-type holder) its `code: String` and a `@Composable Controls()` lambda typed at construction, so `ControlsContainer` calls `holder.Controls()` without a cast. After D26, `ControlsContainer` no longer receives the holder map: it takes `getManager: (ShaderAnimationDemoType) -> ShaderAnimationsDemoManager<*, *>`, wired in `ShaderAnimationsDemo.kt` to the Impl's `getManager`. Replace that parameter with a per-type lookup of the typed binding (e.g. `getControls: (ShaderAnimationDemoType) -> @Composable () -> Unit` / `getCode`), still called with `targetState.first` **inside** the `AnimatedContent` content so the outgoing branch keeps its own type's controls during the crossfade (see D26), and drop the Impl's `getManager`.

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
