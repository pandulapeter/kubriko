# Decide what IS_SCENE_EDITOR_AVAILABLE means, since both the real and the noop module declare it false

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** tool-scene-editor and tool-scene-editor-noop (published)
**Files:** tools/scene-editor/src/commonMain/kotlin/com/pandulapeter/kubriko/sceneEditor/SceneEditorConstants.kt, tools/scene-editor-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/sceneEditor/SceneEditorConstants.kt, tools/scene-editor-noop/CLAUDE.md, tools/scene-editor-api/CLAUDE.md

## Problem
Both modules declare, in `commonMain`, `const val IS_SCENE_EDITOR_AVAILABLE = false` (`SceneEditorConstants.kt:15` in each). Nothing
in this repo or in Tesselar reads it. The noop's CLAUDE.md recommends it as a guard for "Launch Editor" buttons, which can never be
true. Its real job is to give each module a common source file so it builds for iOS. It has no KDoc.

## Decision
- (a) **Document it as a placeholder (recommended):** keep `false` in both, add KDoc ("Always false; exists so the module has
  common code on every target. Not a reliable availability check.") and make the docs say so. No behaviour change.
- (b) Make it a real guard: `true` in `scene-editor`. But it is a `commonMain` `const`, so it would also read `true` on Android,
  iOS and Web where `SceneEditor` does not exist (desktop-only); a correct version needs an `expect val` (no longer `const` — a
  binary change of the constant's shape), and `const` values are inlined into consumers at compile time, so the change only reaches
  code recompiled against the new artifact.
- (c) Deprecate it in both modules for removal.

## Fix
Per the decision. For (a): KDoc on both declarations; the docs plans T33/T34 already describe it as a placeholder.

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
