# Correct the panel roles, snapshot fields and hex-field focus note in tools/scene-editor/CLAUDE.md

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** none (docs)
**Files:** tools/scene-editor/CLAUDE.md
**Challenged:** amended — names the concrete paths the earlier scene-editor plans move that this file mentions (`toPropertyEditorKind` → `PropertyEditorKind.kt` by T07; `exposedMutableProperties` by T17; the hex field helpers → `HexColor.kt` by T12).

## Problem
At 2480325f:
- "UI panels" swaps the two columns: it says the **Instance browser** (left) "lists actor types … click to select a type for
  placement" and the **Instance manager** (right) "shows all placed actors". In the code `InstanceBrowserColumn` (left, 150 dp) lists
  the placed instances (filter field, visible-only toggle, click to select), and `InstanceManagerColumn` (right, 220 dp) lists the
  registered types for placement when nothing is selected, or the selected actor's header and property editors.
- "Undo/redo": snapshots are described as `SceneSnapshot(serializedScene, isSceneModified)`; the class also carries `actorIds`
  (the paragraph's last sentence mentions ids, the signature does not).
- Text-input focus: "`EditorTextInput` and the HEX field in `ColorPropertyEditor` are the only call sites that forward the local
  into it [`TextInput`]" — the HEX field is a plain `BasicTextField` that reports to `LocalTextInputFocusReporter` itself (with the
  same balanced on-dispose rule); only `EditorTextInput` goes through `TextInput`.

## Fix
Run last in the lane (after the scene-editor code plans, including the property-discovery plan, which rewrites the "Property
inspector" sentence itself — check it did). Fix the three points above. Where the landed Now plans renamed files the doc names
(it names `extensions/PointBodyExtensions.kt`, `ModifierExtensions.kt`, `helpers/` for `UndoRedoHistory`, the `loadFile`/`saveFile`
helpers), make sure every named path still exists. In particular the "Property inspector" paragraph opens with
"`PropertyEditorMapper.kt` uses Kotlin reflection … (`toPropertyEditorKind` …)": after T07 `toPropertyEditorKind` and the
`KType` constants live in `PropertyEditorKind.kt`, and after T17 the discovery is `exposedMutableProperties` (still in
`PropertyEditorMapper.kt`) — name each in its file. Keep the prose terse.

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
