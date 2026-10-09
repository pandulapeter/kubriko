# Decide what IS_SCENE_EDITOR_AVAILABLE means, since both the real and the noop module declare it false

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** tool-scene-editor and tool-scene-editor-noop (published)
**Files:** tools/scene-editor/src/commonMain/kotlin/com/pandulapeter/kubriko/sceneEditor/SceneEditorConstants.kt, tools/scene-editor-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/sceneEditor/SceneEditorConstants.kt, tools/scene-editor-noop/CLAUDE.md, tools/scene-editor-api/CLAUDE.md
**Rebased:** on 70de96c6 after the Now plans landed.

## Problem
Both modules declare, in `commonMain`, `const val IS_SCENE_EDITOR_AVAILABLE = false` (`SceneEditorConstants.kt:15` in each). Nothing
in this repo or in Tesselar reads it. Its real job is to give each module a common source file so it builds for iOS; its KDoc
(`SceneEditorConstants.kt:12-14` in each) says only that — "This constant is only here because completely empty Kotlin
Multiplatform modules cannot be built for iOS." — not that it cannot serve as an availability check. The module guides no longer
recommend it as a guard: T33 (landed in 041d1a13, `tools/scene-editor-api/CLAUDE.md:81`) and T34 (landed in 5a56b828,
`tools/scene-editor-noop/CLAUDE.md:17`) describe it as always `false`, unread, and "an open decision" not to be used as a guard.

## Decision
- (a) **Document it as a placeholder (recommended):** keep `false` in both, add KDoc ("Always false; exists so the module has
  common code on every target. Not a reliable availability check.") and make the docs say so. No behaviour change.
- (b) Make it a real guard: `true` in `scene-editor`. But it is a `commonMain` `const`, so it would also read `true` on Android,
  iOS and Web where `SceneEditor` does not exist (desktop-only); a correct version needs an `expect val` (no longer `const` — a
  binary change of the constant's shape), and `const` values are inlined into consumers at compile time, so the change only reaches
  code recompiled against the new artifact.
- (c) Deprecate it in both modules for removal.

## Fix
Per the decision. For (a): extend the KDoc on both declarations; in the two module guides (lines quoted above) replace "open decision" with the
resolved meaning.

## Behaviour
None for (a)/(c); (b) changes an inlined public constant.

## Public API
(a) KDoc only; (b) value/shape change; (c) deprecation.

## Tests
None.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop :tools:scene-editor-noop:compileKotlinDesktop :tools:scene-editor-noop:compileKotlinIosSimulatorArm64`

## Manual check
None.
