# Correct the property-type rendering and IS_SCENE_EDITOR_AVAILABLE notes in tools/scene-editor-api/CLAUDE.md

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** none (docs)
**Files:** tools/scene-editor-api/CLAUDE.md

## Problem
At 2480325f:
- "`Boolean` — rendered as a checkbox": `BooleanPropertyEditor` renders an `EditorSwitch`.
- "`Color` — rendered as a color picker (RGB sliders or hex, user-configurable)": `ColorPropertyEditor` always shows a hex field, a
  swatch and an alpha slider, plus RGB *or* HSV sliders per the user's setting.
- "Constant: `IS_SCENE_EDITOR_AVAILABLE` — `false` in the `-noop` module … The real `scene-editor` module does not define this
  constant": `tools/scene-editor/src/commonMain/.../SceneEditorConstants.kt:15` declares `const val IS_SCENE_EDITOR_AVAILABLE = false`
  too, and nothing in the repo reads it.

## Fix
Fix the two type lines. Replace the constant note with the current fact: both `scene-editor` and `scene-editor-noop` declare
`IS_SCENE_EDITOR_AVAILABLE = false`, so it cannot tell them apart; its meaning is an open decision (the IS_SCENE_EDITOR_AVAILABLE
plan in issues/). If that decision has landed by the time this runs, describe its outcome instead.

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
