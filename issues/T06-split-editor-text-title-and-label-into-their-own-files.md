# Split EditorTextTitle and EditorTextLabel out of EditorText.kt into files of their own

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/components/EditorText.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/components/EditorTextTitle.kt (new), tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/components/EditorTextLabel.kt (new)

## Problem
`components/EditorText.kt` holds three non-private UI Composables: `EditorText`, `EditorTextTitle`
(`Text(modifier = modifier.padding(bottom = 4.dp), style = MaterialTheme.typography.titleSmall, text = text)`) and
`EditorTextLabel` (`style = MaterialTheme.typography.labelSmall`). The code style puts every non-private UI Composable
in a file named after it.

## Fix
Move `EditorTextTitle` verbatim to `components/EditorTextTitle.kt` and `EditorTextLabel` verbatim to
`components/EditorTextLabel.kt` (same package, MPL-2.0 header, only the imports each needs). Trim the imports
`EditorText.kt` no longer uses (`padding`, `dp` if unused). Callers are in the same package or import by name.

## Behaviour
Verbatim move.

## Public API
None (internal).

## Tests
The existing ones.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
None.
