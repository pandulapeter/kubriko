# Share the "info panel + expandable bottom-end controls panel + brush button" overlay through `examples/shared`.

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Files:** `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/ui/ContentShadersOverlay.kt` (after D02), `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/ContentShadersDemoStateHolder.kt`, `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/implementation/managers/ContentShadersDemoManager.kt`, `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ui/ParticlesDemoOverlay.kt` (after D07), `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ParticlesDemoStateHolder.kt`, `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/managers/ParticlesDemoManager.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/ui/ControlsContainer.kt`, the three modules' `composeResources/values/strings.xml` and `composeResources/drawable/ic_brush.xml`, `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/ui/ExpandableControlsOverlay.kt` (new), `examples/shared/build.gradle.kts`, `examples/shared/CLAUDE.md`

## Problem
After D02/D07, `ContentShadersOverlay` and `ParticlesDemoOverlay` are the same tree: Column(fillMaxSize, windowInsetsPadding, padding 16) > InfoPanel > Spacer(weight) > Box(fillMaxWidth) > `this@Column.AnimatedVisibility`(fadeIn + scaleIn(TransformOrigin(1f, 1f)) / scaleOut + fadeOut) > Box(fillMaxSize) > panel at BottomEnd with `padding(bottom/end = 16.dp)`, plus a `FloatingButton(icon = ic_brush, …collapse_controls/expand_controls)`. Both Managers repeat `_areControlsExpanded` / `toggleControlsExpanded()` and both state holders repeat the same `navigateBack` collapse logic. `expand_controls`, `collapse_controls` and `ic_brush.xml` are copied into three modules (demo-shader-animations uses them in `ControlsContainer` too).

## Fix
`@Composable fun ExpandableControlsOverlay(windowInsets, description: StringResource, isExpanded: Boolean, onToggle: () -> Unit, panelModifier: Modifier, content: @Composable () -> Unit)` in `examples/shared`, the two overlays delegate to it, and the brush/expand/collapse resources move to shared. examples/shared is lane G's module, and it has no compose resources today (`examples/shared/src/commonMain` holds only `kotlin/`), hence Planned.

## Decision
Where the shared resources live: (a) give `examples/shared` compose resources (build-file change; every example then also preloads shared strings) — **recommended**; (b) put the brush icon/strings in `tools/ui-components` (a published artifact — adds public resources, so a public-API decision); (c) share only the layout and keep resources per module, passing `icon`/content descriptions in.

## Behaviour
Same tree per demo.

## Public API
None for (a)/(c); (b) adds public resources to `tool-ui-components`.

## Tests
None (UI).

## Verify
`./gradlew :examples:shared:compileKotlinDesktop :examples:demo-content-shaders:compileKotlinDesktop :examples:demo-particles:compileKotlinDesktop :examples:demo-shader-animations:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
Content Shaders and Particles: controls expand/collapse identically, back gesture collapses them first.
