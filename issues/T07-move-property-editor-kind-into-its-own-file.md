# Move PropertyEditorKind and its KType matcher out of PropertyEditorMapper.kt into PropertyEditorKind.kt

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/PropertyEditorMapper.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/PropertyEditorKind.kt (new)

## Problem
`PropertyEditorMapper.kt` (274 lines) mixes the pure type classification with the Composable editor factory:
lines 60-100 at 2480325f hold nine `private val ...Type = X::class.createType()` constants (`booleanType`, `colorType`,
`angleDegreesType`, `angleRadiansType`, `sceneOffsetType`, `scaleType`, `floatType`, `intType`, `sceneUnitType`),
`internal enum class PropertyEditorKind` and `internal fun KType.toPropertyEditorKind(): PropertyEditorKind?` (with its KDoc
"Picks the editor for a property type..."). Its test is already `tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/PropertyEditorKindTest.kt`.

## Fix
Move the nine constants (still `private`, now private to the new file — only `toPropertyEditorKind` reads them), the enum
and `toPropertyEditorKind` with its KDoc verbatim into `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/PropertyEditorKind.kt` (same package, MPL-2.0 header,
imports: `Color`, `AngleDegrees`, `AngleRadians`, `Scale`, `SceneOffset`, `SceneUnit`, `KType`, `createType`).
`toPropertyEditor` stays in `PropertyEditorMapper.kt`; drop the imports it no longer uses (`KType`, `createType`; keep the
types it still casts to). Do not touch the private `EditorCategory` here (a later plan deletes it).

## Behaviour
Verbatim move.

## Public API
None (internal).

## Tests
The existing ones (`PropertyEditorKindTest`).

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop :tools:scene-editor:desktopTest`

## Manual check
None.
