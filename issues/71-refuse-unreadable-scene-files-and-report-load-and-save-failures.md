# Refuse unreadable scene files, run scene file I/O off the main thread with results applied on it, and report load and save failures

**Challenged:** amended — in Connected mode a blank `sceneJson` is an empty scene with no error (the desktop examples' `sceneJson` starts as `""` until the demo has loaded its scene, and HEAD opens an empty editor silently there), and the Connected-mode failure report no longer names the unrelated current file.

**Kind:** bug (data loss)  ·  **Severity:** high  ·  **Platforms:** desktop
**Artifact:** `tool-scene-editor`
**Files:** `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/EditorController.kt`, `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/PlatformHelpers.kt`, `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/SceneParsing.kt` (new), `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/InternalSceneEditor.kt`, `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/EditorUserInterface.kt`, `tools/scene-editor/src/desktopMain/composeResources/values/strings.xml`, `tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/SceneParsingTest.kt` (new), `tools/scene-editor/CLAUDE.md`

## Problem

**Loading garbage wipes the scene.** `EditorController.loadMap` (~378-393):

```kotlin
fun loadMap(path: String) {
    _shouldShowLoadingIndicator.update { true }
    launch {
        loadFile(path)?.let { json ->
            parseJson(json)
            onSceneReplaced()
            updateCurrentFolderPathAndFileName(path)
        }
        _shouldShowLoadingIndicator.update { false }
    }
}

private fun parseJson(json: String) {
    replaceSceneActors(serializationManager.deserializeActors(json))
    _selectedActor.update { null }
}
```

`SerializationManagerImpl.deserializeActors` returns `emptyList()` on a `SerializationException` and silently drops entries whose `typeId` it does not know. So opening any file that is not a scene of this game — a PNG, another game's scene, a truncated file — clears the current scene, **clears the undo history** (`onSceneReplaced()`), marks the scene unmodified and makes the bad file the current file. The next Save (the dialog pre-fills that file name) overwrites it with whatever is in the editor. Nothing tells the user. The `.json` filter on the open dialog is commented out (`InternalSceneEditor.kt` ~197-199), which makes picking a wrong file easy. A deserializer that throws something other than `SerializationException` escapes the `launch` instead, leaving the loading indicator on forever.

**Threading and unreported I/O errors.** The controller scope is `SupervisorJob() + Dispatchers.Default`, so `loadMap` and `saveScene` (~412-422) run their whole bodies on a background thread:

```kotlin
fun saveScene(path: String) {
    launch {
        saveFile(
            path = path,
            content = serializationManager.serializeActors(allEditableActors.value),
        )
        updateCurrentFolderPathAndFileName(path)
        _isSceneModified.update { false }
        pendingPropertyEditKey = null
    }
}
```

`onSceneReplaced()` resets `UndoRedoHistory`, whose stacks are plain `ArrayDeque`s, while the UI thread may be recording an action into them; `pendingPropertyEditKey` is a plain `var` also written from the UI. `saveFile` (`PlatformHelpers.kt`) throws `IOException` on a read-only folder or full disk; nothing catches it, so the coroutine fails into the default handler (a stack trace on stderr at best), the user gets no feedback and the file name keeps its "modified" marker with no explanation. `loadFile` swallows every exception into `null`, so a failed read is silent too.

The plugins lane owns `deserializeActors` and may change its failure behaviour; this plan must work whether it keeps returning an empty list or starts throwing.

## Fix

1. New `helpers/SceneParsing.kt` (MPL header):
   ```kotlin
   internal fun <T> deserializeSceneOrNull(json: String, deserialize: (String) -> List<T>): List<T>?
   ```
   Returns `null` if `deserialize` throws (catch `Exception`, rethrow `CancellationException`), or if it returns an empty list while `json` is not an empty array (`json.filterNot(Char::isWhitespace) == "[]"`; `serializeActors(emptyList())` writes `[]`). A blank file is a failure. Partially known scenes (some entries dropped) are out of scope — that needs the serialization plugin to report drops.
2. `PlatformHelpers.kt`: make `loadFile` return the text or throw (drop the catch-all), and have both helpers do their work in `withContext(Dispatchers.IO)`.
3. `EditorController`:
   - `loadMap(path)`: `launch(Dispatchers.Main) { try { val json = loadFile(path); val actors = withContext(Dispatchers.Default) { deserializeSceneOrNull(json, serializationManager::deserializeActors) }; if (actors == null) report(LOAD_FAILED) else { replaceSceneActors(actors); _selectedActor.update { null }; onSceneReplaced(); updateCurrentFolderPathAndFileName(path) } } catch (IOException) { report(LOAD_FAILED) } finally { _shouldShowLoadingIndicator.update { false } } }`. On failure nothing about the current scene, history, dirty flag or current file changes.
   - `saveScene(path)`: serialize on the main thread (the UI mutates actors there), then `launch(Dispatchers.Main) { try { saveFile(path, content); updateCurrentFolderPathAndFileName(path); _isSceneModified.update { false }; pendingPropertyEditKey = null } catch (e: IOException) { report(SAVE_FAILED) } }` — on failure the modified flag and current file stay as they were.
   - `syncScene()`: serialize on the main thread before launching, same reason.
   - Connected mode's `init` `parseJson(sceneEditorMode.sceneJson)` uses `deserializeSceneOrNull` too, except that a **blank** `sceneJson` is an empty scene without any report: `examples/demo-performance` and `examples/demo-physics` create their desktop `sceneJson` as `MutableStateFlow("")` and only fill it once the demo's manager has loaded its scene (their own managers skip blank values with `filter { it.isNotBlank() }`), so opening the connected editor early is normal and must not show an error. On `null` for non-blank JSON, start with an empty scene and report `CONNECTED_SCENE_INVALID` (its own string, no file-name argument — the current file name has nothing to do with the host's JSON), and leave the current file name alone.
   - Error state: `enum class FileOperationError { LOAD_FAILED, SAVE_FAILED, CONNECTED_SCENE_INVALID }`, `private val _fileOperationError = MutableStateFlow<Pair<FileOperationError, String>?>(null)` (error plus file name; empty for `CONNECTED_SCENE_INVALID`), `val fileOperationError`, `fun onFileOperationErrorShown()`.
   - Delete `parseJson` if nothing uses it any more. `Dispatchers.Main` is available on desktop through Compose Desktop's `kotlinx-coroutines-swing`.
4. `EditorUserInterface`: add a `SnackbarHostState` to the existing `Scaffold` (`snackbarHost = { SnackbarHost(it) }`) and a `LaunchedEffect(error)` that shows the message and calls `onFileOperationErrorShown()`.
5. `strings.xml`: `error_scene_load_failed` = "Could not open %1$s as a scene.", `error_scene_save_failed` = "Could not save %1$s." and `error_connected_scene_invalid` = "The connected scene could not be read."
6. `InternalSceneEditor.FileDialog`: restore the filter, for loading only: `if (isForLoading) filenameFilter = FilenameFilter { _, name -> name.endsWith(".json") }`. AWT honours `FilenameFilter` on macOS and Linux but ignores it on Windows' native dialog, which is why the parse check above is the real guard. Drop this step if on macOS the filter hides valid scene files.
7. `tools/scene-editor/CLAUDE.md` → Scene I/O: describe the refusal rule (`deserializeSceneOrNull`), that file I/O runs on `Dispatchers.IO` while state is applied on the main thread, and that failures show a snackbar and leave the scene untouched.

## Tests

`SceneParsingTest` (desktopTest, pure, with a fake `deserialize` lambda):
- valid content with two items → the two items;
- `"[]"` and `" [ ]\n"` → empty list (not `null`);
- `""` and `"not json"` with a deserializer returning empty → `null`;
- a deserializer that throws `IllegalArgumentException` → `null`;
- a deserializer that throws `CancellationException` → rethrown.

Run `./gradlew :tools:scene-editor:desktopTest`.

## Manual check

Desktop, Annoyed Penguins → Scene Editor, place two actors (the scene is now modified, undo is available):
1. Open → pick a non-scene file (on Windows type its name, since the filter is ignored there): a snackbar says it could not be opened; the two actors, the undo button, the modified marker and the file name are unchanged.
2. Save into a read-only folder (e.g. `chmod a-w` a temp folder): a snackbar reports the failure; the modified marker stays.
3. Save to a writable file, reopen it: the scene loads and the history is cleared as before.
4. Showcase → Performance demo → open its (connected) scene editor: it opens with the demo's scene and no snackbar; also open it immediately after entering the demo, before the scene has loaded: an empty editor, no snackbar.
