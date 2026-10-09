# Use the desktop module's `isWindows` instead of creating a `MetadataManager` to detect Windows

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseApp.kt`

## Problem
The desktop shell answers "is this Windows?" two ways (at 2480325f):

```kotlin
// KubrikoShowcaseApp.kt:65
val isRunningOnWindows = remember { MetadataManager.newInstance().platform is MetadataManager.Platform.Desktop.Windows }
// TitleBar.kt:159 (DesktopOperatingSystem.kt after A02), also used by KubrikoShowcaseApp.kt's setUndrawnAreaColor
internal val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("windows")
```

The first builds a whole `MetadataManager` (outside any Kubriko instance) just to read its platform.

Equivalence, verified: the engine's desktop `getPlatform()` (`engine/src/desktopMain/.../PlatformUtils.desktop.kt:26`)
returns `Desktop.Windows` exactly when commons-lang3's `SystemUtils.IS_OS_WINDOWS`, which is `os.name` starting with
`"Windows"` (case-sensitive prefix). Every JVM reports Windows as `"Windows <version>"` (`"Windows 10"`, `"Windows 11"`,
`"Windows Server 2022"`), and no other OS's `os.name` contains "windows", so both give the same answer on every real
JVM; a missing `os.name` gives `false` for both.

## Fix
Delete the `isRunningOnWindows` line and use `isWindows` in its two places (`if (isRunningOnWindows) { previousWindowPosition … }`
inside the fullscreen toggle and `if (isRunningOnWindows) { key(isInFullscreenMode.value) { … } }`). Remove the now unused
`MetadataManager` import.

## Behaviour
Same boolean on every real JVM (above); one fewer `MetadataManager` allocation at startup. Unchanged.

## Public API
None.

## Tests
None (reads a system property).

## Verify
`./gradlew :app:desktop:compileKotlin`

## Manual check
On Windows, toggle fullscreen in a game and back: the window is recreated undecorated and returns to its old position, as
before. On macOS, the same toggle keeps the window.
