# Rename PlatformHelpers.kt to SceneFileIo.kt

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/PlatformHelpers.kt (renamed), tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/SceneFileIo.kt (new name)

## Problem
`helpers/PlatformHelpers.kt` holds only the scene file I/O:
```kotlin
internal suspend fun loadFile(path: String) = withContext(Dispatchers.IO) { File(path).readBytes().decodeToString() }
internal suspend fun saveFile(path: String, content: String) = withContext(Dispatchers.IO) { ... }
```
The name says neither what it holds nor follows the "no `*Helpers.kt`" rule.

## Fix
`git mv` the file to `helpers/SceneFileIo.kt`; content unchanged. Callers import `helpers.loadFile` / `helpers.saveFile` by
name and need no change. Grep the repo for `PlatformHelpers` (none expected at 2480325f).

## Behaviour
Rename only.

## Public API
None (internal; the JVM facade `PlatformHelpersKt` becomes `SceneFileIoKt`, but both are internal to the module).

## Tests
The existing ones.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
None.
