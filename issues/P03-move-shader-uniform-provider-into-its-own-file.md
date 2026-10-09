# Move `ShaderUniformProvider` out of `ModifierExtensions.kt` into a file of its own

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-shaders
**Files:**
- `plugins/shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/shaders/extensions/ModifierExtensions.kt`
- new `plugins/shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/shaders/extensions/ShaderUniformProvider.kt`

## Problem
The public `interface ShaderUniformProvider` (ModifierExtensions.kt:97–140 at 2480325f, KDoc included) sits in a
file named after the `Modifier.shader` extension:

```kotlin
/**
 * A provider for setting uniform values on a [Shader].
 ...
interface ShaderUniformProvider {
    fun uniform(name: String, value: Int)
```

The code-style skill wants every top-level type in a file of its own, named after it.

## Fix
Move the interface with its KDoc, verbatim, into `ShaderUniformProvider.kt` in the same package
`com.pandulapeter.kubriko.shaders.extensions` (MPL-2.0 header copied from a sibling). It needs the imports
`androidx.compose.ui.graphics.ImageBitmap` and `com.pandulapeter.kubriko.shaders.Shader` (for the `[Shader]` KDoc link).
Remove the `ImageBitmap` import from `ModifierExtensions.kt` if nothing else there uses it (nothing does at 2480325f).
Nothing else changes. Grep the repo (docs, CLAUDE.md, skills) for a statement that the interface lives in
`ModifierExtensions.kt` (none at 2480325f).

Drop this plan if lane E's Skia-source-set plan has already moved the common `ModifierExtensions.kt` (it only targets
the platform actuals at the time of writing).

## Behaviour
Unchanged.

## Public API
None. An interface's JVM name is its FQN (`com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider`), which
does not depend on the file; the package stays the same, so every implementation (`Shader` states in the plugin,
the examples, Tesselar) compiles and links unchanged.

## Tests
The existing ones.

## Verify
`./gradlew :plugins:shaders:compileKotlinDesktop :plugins:shaders:compileAndroidMain`

## Manual check
none
