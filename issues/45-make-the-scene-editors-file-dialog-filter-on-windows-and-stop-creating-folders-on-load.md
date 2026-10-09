# Make the scene editor's file dialog filter `.json` files on Windows, open at an absolute folder and stop creating folders when loading

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** Desktop (Windows for the filter; all desktop OSes for the rest)
**Challenged:** sound
**Files:** `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/InternalSceneEditor.kt`,
`tools/scene-editor/CLAUDE.md`

Ships in `tool-scene-editor`. Internal only; no public API changes. Runs after plan 40 (same file, a different function).

## Problem

The private `FileDialog` Composable in `InternalSceneEditor.kt` sets up an AWT `java.awt.FileDialog` like this:

```kotlin
object : FileDialog(parent, dialogTitle, if (isForLoading) LOAD else SAVE) {
    init {
        val scenesDirectoryFile = File(currentFolderPath)
        scenesDirectoryFile.parentFile?.mkdirs()
        if (!scenesDirectoryFile.exists()) {
            scenesDirectoryFile.mkdir()
        }
        if (isForLoading) {
            filenameFilter = FilenameFilter { _, name -> name.endsWith(".json") }
        }
        directory = currentFolderPath
        …
```

1. **The filter does nothing on Windows.** `FileDialog.setFilenameFilter` is documented as not functioning in the JDK's
   Windows implementation, so the load dialog lists every file there and a user can pick a non-scene file (which then
   fails with `LOAD_FAILED`). The usual workaround is a wildcard file name, which the native Windows dialog uses as its
   filter.
2. **The folder is relative.** The default `defaultSceneFolderPath` is `"./src/commonMain/composeResources/files/scenes"`
   (the Showcase passes `"../../examples/…"`). `setDirectory` with a relative path is resolved by the native dialog, not
   by the JVM, so whether it opens there depends on the OS dialog (it can fall back to the last-used or home folder),
   even though the path is meant relative to the process's working directory, as `File` resolves it.
3. **Loading creates folders.** `mkdirs()`/`mkdir()` run for the **load** dialog too, so merely opening "Load" from a
   game launched with a different working directory (an IDE run configuration, a packaged app) creates an empty
   `src/commonMain/composeResources/files/scenes` tree there.

## Fix

In `init`:

```kotlin
val scenesDirectoryFile = File(currentFolderPath).absoluteFile
if (!isForLoading) {
    scenesDirectoryFile.mkdirs()
}
if (isForLoading) {
    filenameFilter = FilenameFilter { _, name -> name.endsWith(".json") }
    if (System.getProperty("os.name").startsWith("Windows")) {
        file = "*.json"
    }
}
directory = scenesDirectoryFile.path
if (!isForLoading) {
    file = currentFileName
}
```

- `mkdirs()` replaces the `parentFile?.mkdirs()` + `mkdir()` pair (same result) and only runs for saving, where the
  dialog should open in the target folder even if it does not exist yet. A load dialog pointed at a missing folder just
  opens wherever the OS dialog falls back to, which is fine for loading.
- `absoluteFile` resolves the path against the JVM's working directory, the same way `File` and the subsequent
  `loadMap`/`saveScene` already resolve it, so the dialog opens where the relative default points on every OS.
- The `FilenameFilter` stays for macOS/Linux; the `*.json` file name is set on Windows only, because on macOS the file
  name field is shown and would read `*.json`.

`onCloseRequest(directory, file)` is unchanged; the native dialog returns an absolute directory either way.

`tools/scene-editor/CLAUDE.md`: next to "Default folder: …", add that relative folders resolve against the working
directory, and that the folder is created only when saving.

## Tests

None: the change is AWT dialog setup in a Composable, and showing a native dialog is not possible in the headless test
JVM.

## Manual check

- Windows: open the scene editor, press Load — only `.json` files (and folders) are listed; the dialog opens in the
  game's `scenes` folder.
- macOS/Linux: Load still filters to `.json`, opens in the scenes folder, and Save still proposes the current file name
  in that folder.
- Any desktop OS: run a game with a scene editor from a working directory where the default folder does not exist,
  press Load and cancel — no `src/commonMain/composeResources/files/scenes` folders were created; press Save — the
  folder is created and the dialog opens in it.
