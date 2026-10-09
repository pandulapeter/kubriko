# Split the desktop `TitleBar.kt` into one file per declaration

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop (macOS, Windows)  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/TitleBar.kt` (deleted)
- `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/ExtendedTitleBar.kt` (new)
- `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/TitleBarAppearance.kt` (new)
- `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/TitleBarInsets.kt` (new)
- `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/DesktopOperatingSystem.kt` (new)
- `app/desktop/CLAUDE.md`
- `app/desktop/build.gradle.kts` (comment only)

## Problem
`TitleBar.kt` (168 lines at 2480325f) is named after none of its declarations and holds two non-private UI
Composables, a class and the OS checks:

- `internal class ExtendedTitleBar(...)` (:46-50)
- `internal fun ComposeWindow.extendContentIntoTitleBar(): ExtendedTitleBar?` (:60-64)
- `private fun ComposeWindow.extendContentIntoCustomTitleBar(height: Dp): ExtendedTitleBar?` (:75-95)
- `@Composable internal fun TitleBarAppearance(window: ComposeWindow, titleBar: ExtendedTitleBar)` (:103-126)
- `@OptIn(InternalComposeUiApi::class) @Composable internal fun TitleBarInsets(...)` (:135-155)
- `private val isMacOs = System.getProperty("os.name").orEmpty().lowercase().contains("mac")` (:157)
- `internal val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("windows")` (:159, also used by
  `KubrikoShowcaseApp.kt`'s `setUndrawnAreaColor`)
- `private val MAC_TITLE_BAR_HEIGHT`, `private val WINDOWS_TITLE_BAR_HEIGHT` (:161-165, used only by
  `extendContentIntoTitleBar`)
- `private const val WINDOWS_DARK_CONTROLS = "controls.dark"` (:167-168, used only by `TitleBarAppearance`)

`code-style`: every non-private top-level UI Composable lives in a file named after it; one file, one thing.

## Fix
Verbatim move, each KDoc with its declaration, same package `com.pandulapeter.kubrikoShowcase`, MPL-2.0 header copied
from `TitleBar.kt`, then delete `TitleBar.kt`:

| New file | Declarations |
|---|---|
| `ExtendedTitleBar.kt` | `ExtendedTitleBar`, `extendContentIntoTitleBar`, `private extendContentIntoCustomTitleBar`, `private MAC_TITLE_BAR_HEIGHT`, `private WINDOWS_TITLE_BAR_HEIGHT` |
| `TitleBarAppearance.kt` | `TitleBarAppearance`, `private const WINDOWS_DARK_CONTROLS` |
| `TitleBarInsets.kt` | `TitleBarInsets` (with its `@OptIn(InternalComposeUiApi::class)`) |
| `DesktopOperatingSystem.kt` | `isMacOs`, `isWindows` |

`isMacOs` goes from `private` to `internal` (used by `ExtendedTitleBar.kt` and `TitleBarAppearance.kt`). No other
`isMacOs` exists in `app/desktop` (internal declarations of `app/shared` in the same package are invisible here), so
there is no clash. Nothing else changes visibility. The KDoc links `[TitleBarInsets]` and `[extendContentIntoTitleBar]`
still resolve (same package). Split the imports so each file has exactly what it uses.

References, same commit:
- `app/desktop/CLAUDE.md` → Title bar: "so the Showcase's own surface is the title bar (`TitleBar.kt`)" →
  "(`ExtendedTitleBar.kt`)".
- `app/desktop/build.gradle.kts` comment "and the window buttons follow the theme (TitleBar.kt)" →
  "(ExtendedTitleBar.kt, TitleBarAppearance.kt)".
- Grep the whole repo (docs, every `CLAUDE.md`, `.claude/skills`, build files, workflows, ProGuard rules) for
  `TitleBar.kt` and `TitleBarKt` and fix what remains. (`proguard-rules.pro` keeps `com.jetbrains.**`, not the app's
  file facades; confirm nothing names `TitleBarKt`.)

Check the move mechanically: the sorted list of top-level declarations before and after must match
(`git diff --color-moved=dimmed-zebra`).

## Behaviour
Same declarations in the same package; top-level `val`s are still initialized on first access of their (now separate)
file facade, and both read only `System.getProperty("os.name")`, so the order of initialization cannot matter.

## Public API
None (`app/desktop` is an application).

## Tests
The existing ones (none cover this file; the desktop app has no test source set).

## Verify
`./gradlew :app:desktop:compileKotlin`

## Manual check
None (a verbatim move); if convenient, `./gradlew :app:desktop:run` on macOS and see the title bar strip still drags.
