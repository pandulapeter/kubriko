# Correct the IS_SCENE_EDITOR_AVAILABLE guidance in tools/scene-editor-noop/CLAUDE.md

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** none (docs)
**Files:** tools/scene-editor-noop/CLAUDE.md

## Problem
Line 8 at 2480325f says the constant is a guard for "Launch Editor" buttons and that "the real `scene-editor` module does not
declare this constant; its absence signals the real implementation is linked", and the usage pattern shows
`if (IS_SCENE_EDITOR_AVAILABLE) { … }`. The real module declares the same `const val IS_SCENE_EDITOR_AVAILABLE = false`
(`tools/scene-editor/src/commonMain/.../SceneEditorConstants.kt:15`), so that guard is always false with either module.

## Fix
State the fact (declared `false` in both; nothing reads it; it exists so the module has a common source and builds for iOS) and
drop the usage example, pointing at the open IS_SCENE_EDITOR_AVAILABLE decision plan. If that decision has landed, describe its
outcome instead (e.g. restore the guard example if the real module now says `true`).

## Behaviour
Docs only.

## Public API
None.

## Tests
None.

## Verify
None (docs).

## Manual check
None.
