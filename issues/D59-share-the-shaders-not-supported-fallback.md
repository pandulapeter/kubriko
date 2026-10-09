# Share the "shaders not supported" fallback and its string between the two shader demos.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Rebased:** on 70de96c6 after the Now plans landed.
**Files:** `examples/demo-content-shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/demoContentShaders/ContentShadersDemo.kt`, `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/ShaderAnimationsDemo.kt`, `examples/demo-content-shaders/src/commonMain/composeResources/values/strings.xml`, `examples/demo-shader-animations/src/commonMain/composeResources/values/strings.xml`, both modules' `…StateHolder.kt` resource gates (`ContentShadersDemoStateHolder.kt:40`, `ShaderAnimationsDemoStateHolder.kt:54`), `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/ui/ShadersNotSupportedMessage.kt` (new), `examples/shared/CLAUDE.md`

## Problem
`ContentShadersDemo.kt:55-68` (inline in the `else` branch) and `ShaderAnimationsDemo.kt:117-128` (the private `ShadersNotSupportedMessage(windowInsets)` D27 extracted, landed in 018c31cd; called at :90–92) render the same `Box(fillMaxSize, windowInsetsPadding(windowInsets), padding 16.dp) { Text(fillMaxWidth(0.75f), centred, shaders_not_supported) }`, and both modules ship the same (`strings.xml:14` in each) `<string name="shaders_not_supported">Shaders are only supported on or above Android 13</string>`. They differ in one detail: content-shaders starts from the caller's `modifier` (`modifier.fillMaxSize()`), shader-animations from `Modifier.fillMaxSize()`, dropping the caller's modifier.

## Fix
One `ShadersNotSupportedMessage(windowInsets, modifier)` in `examples/shared` (lane G's module) — depends on the same resource decision as D58.

## Decision
Same as D58 (resources in shared vs. ui-components vs. pass the string in) — recommended to follow D58's answer; and whether shader-animations' fallback should honour the caller's `modifier` (recommended yes; a visible change only if the Showcase passes a modifier there).

## Behaviour
Same text and layout (plus the modifier decision).

## Public API
None.

## Tests
None.

## Verify
`./gradlew :examples:shared:compileKotlinDesktop :examples:demo-content-shaders:compileKotlinDesktop :examples:demo-shader-animations:compileKotlinDesktop`

## Manual check
Android 12 device (or a forced `areShadersSupported = false`): both demos show the centred message.
