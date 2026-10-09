# Remember the registered type id list instead of copying it on every recomposition

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/EditorUserInterface.kt

## Problem
`EditorUserInterface.kt:151` passes `registeredTypeIds = editorController.serializationManager.registeredTypeIds.toList()`
to `InstanceManagerColumn`, allocating a new list every time `EditorUserInterface` recomposes (it collects a dozen
StateFlows, including the mouse position). `registeredTypeIds` is an `ImmutableSet` fixed when the `SerializationManager` is
created (`SerializationManagerImpl`: `override val registeredTypeIds = typeIdsToDeserializers.keys` of an immutable map).

## Fix
Before the `Scaffold`, add `val registeredTypeIds = remember(editorController) { editorController.serializationManager.registeredTypeIds.toList() }`
and pass `registeredTypeIds = registeredTypeIds`.

## Behaviour
Same content in the same order; the list just keeps its identity, so `InstanceManagerColumn` can skip more often.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
None.
