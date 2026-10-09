# Split `ShaderAnimationsDemo`'s body into named private Composables for the tab row and the unsupported-platform message.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/ShaderAnimationsDemo.kt`

## Problem
`examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/ShaderAnimationsDemo.kt`'s `ShaderAnimationsDemo` (:50–111) inlines the `SecondaryScrollableTabRow` with its `Tab`s, the viewport crossfade with `ControlsContainer`, and the `else` fallback `Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets).padding(16.dp)) { Text(…shaders_not_supported…) }`.

## Fix
- Extract `@Composable private fun ShaderAnimationTabs(selectedDemoType: ShaderAnimationDemoType, onSelectedDemoTypeChanged: (ShaderAnimationDemoType) -> Unit)` = the `SecondaryScrollableTabRow(…) { … }` verbatim (call it with `stateHolder::onSelectedDemoTypeChanged`). It emits exactly one child, so the Column layout is unchanged.
- Extract `@Composable private fun ShadersNotSupportedMessage(windowInsets: WindowInsets)` = the fallback `Box { Text }` verbatim — **including** its `Modifier.fillMaxSize()` start (it ignores the caller's `modifier`, unlike demo-content-shaders' fallback; changing that is planned D59's decision, not this move).
- Leave the viewport `Box { AnimatedContent; ControlsContainer }` inline.

## Behaviour
Layout-transparent extraction; no wrapper added or dropped.

## Public API
None (`ShaderAnimationsDemo`'s signature is unchanged).

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-shader-animations:compileKotlinDesktop`

## Manual check
Open Shader Animations: tabs, crossfade and controls look as before.
