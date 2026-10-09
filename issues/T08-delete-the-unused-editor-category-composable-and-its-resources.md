# Delete the unused EditorCategory Composable, its strings and drawables, and the stale category TODO

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code and resources only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/PropertyEditorMapper.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/InstanceManagerColumn.kt, tools/scene-editor/src/desktopMain/composeResources/values/strings.xml, tools/scene-editor/src/desktopMain/composeResources/drawable/ic_collapse.xml, tools/scene-editor/src/desktopMain/composeResources/drawable/ic_expand.xml

## Problem
`PropertyEditorMapper.kt` ends with `@Composable private fun LazyItemScope.EditorCategory(title, isExpanded, onExpandedChanged, controls)`
(lines 227-274 at 2480325f), which has no caller. It is the only user of the strings `action_collapse` / `action_expand`
(`strings.xml:30-31`) and of the drawables `ic_collapse` / `ic_expand` in this module. `InstanceManagerColumn.kt:96`
still says `// TODO: Sort into categories using expandedCategories.value`, referring to state that no longer exists.
(The Showcase app has its own `ic_collapse`/`ic_expand` in `app/shared` — a different `Res`; leave those alone.)

## Fix
- Delete `EditorCategory` and the imports only it used: `AnimatedVisibility`, `background`, `clickable`, `Arrangement`,
  `Column`, `Row`, `LazyItemScope`, `MaterialTheme`, `Alignment`, `EditorIcon`, `EditorTextTitle`, `Res`,
  `action_collapse`, `action_expand`, `ic_collapse`, `ic_expand`, `stringResource` (re-check each against what remains).
- Delete `<string name="action_collapse">` and `<string name="action_expand">` from the scene-editor `strings.xml`.
- Delete `drawable/ic_collapse.xml` and `drawable/ic_expand.xml` of the scene-editor module.
- Delete the TODO line in `InstanceManagerColumn.kt`.
- Grep `tools/scene-editor` for each removed name afterwards (must be zero hits).

## Behaviour
Dead code; nothing rendered changes.

## Public API
None (the generated `Res` of this module is internal).

## Tests
The existing ones.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
None.
