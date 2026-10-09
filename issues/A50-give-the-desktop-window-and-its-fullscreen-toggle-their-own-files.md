# Give the desktop window and its fullscreen toggle their own files instead of a local Composable over six loose states in `main()`

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** desktop (Windows, macOS, Linux)  ·  **Class:** Planned
**Artifact:** unpublished (app)
**Files:**
- `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseApp.kt`
- `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseWindow.kt` (new)
- `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/DesktopFullscreenState.kt` (new)
- `app/desktop/CLAUDE.md`

**Rebased:** on 70de96c6 after the Now plans landed.

A02 (d6cffee4, title bar split into `ExtendedTitleBar.kt` / `TitleBarAppearance.kt` / `TitleBarInsets.kt`), A13
(1c5de146, title from `Res.string.kubriko_showcase`), A14 (2d4d2306, `isWindows` from `DesktopOperatingSystem.kt`) and A19
(70de96c6, trailing commas) have landed; the quotes below are at 70de96c6.

## Problem
`fun main()` (`KubrikoShowcaseApp.kt:51-165` at 70de96c6) is one 115-line `application { }` block:
- six loose states (:59-64) — `previousBounds`, `previousWindowPlacement`, `previousWindowLocation`,
  `previousWindowPosition`, `windowSize`, `isInFullscreenMode` — that together are one thing, the fullscreen toggle's
  saved window geometry (`windowState` itself is the `com.pandulapeter.kubriko.implementation.windowState` global, assigned
  at :55);
- a **local** `@Composable fun KubrikoShowcaseWindow(undecorated, resizable)` (:67-137) closing over all of them,
  `windowState`, `coroutineScope` and `isRunningOnWindows`;
- the 30-line toggle inline in the `onFullscreenModeToggled` argument (:102-132), with the platform branch and the
  `delay(100)` before restoring `window.bounds` on macOS / Linux;
- `window.minimumSize = Dimension(400, 400)` (:93) executed on every recomposition of the window content.

None of it can be read or tested on its own, and the window cannot be found by name (`code-style`: a non-private UI
Composable lives in a file named after it; a screenful holding several distinct groups is split).

## Fix
1. `DesktopFullscreenState.kt`: `internal class DesktopFullscreenState` holding `isInFullscreenMode` (a
   `mutableStateOf(false)`) and the five saved values as private state, with `fun toggle(window: ComposeWindow, windowState: WindowState, coroutineScope: CoroutineScope)`
   whose body is today's lambda verbatim (enter: save size, bounds, placement, location, position, then
   `placement = Fullscreen`; exit: restore placement and size, then on Windows `windowState.position`, elsewhere
   `window.setLocation` and, after `delay(100)`, `window.bounds`), and `fun onWindowStateChanged(windowState)` for the
   `WindowStateListener` body. Plus `@Composable internal fun rememberDesktopFullscreenState()`.
2. `KubrikoShowcaseWindow.kt`: `@Composable internal fun ApplicationScope.KubrikoShowcaseWindow(fullscreenState, undecorated, resizable)`
   — today's local function, with the toggle replaced by `fullscreenState.toggle(window, windowState, coroutineScope)`.
3. `window.minimumSize = Dimension(400, 400)`: options — (a) move it into the `init = { window -> … }` lambda next to
   `if (!undecorated) titleBar = window.extendContentIntoTitleBar()` (:80), so it is set once per window before it is shown (the Windows toggle recreates the
   window, which runs `init` again); (b) keep it in the content but in a `DisposableEffect(window)` / `SideEffect`.
   **Recommended: (a).** Confirm on all three OSes that the minimum still applies after a fullscreen round trip.
4. `main()` keeps `System.setProperty`, `windowState = rememberWindowState(...)`, the scene editors and the
   `if (isWindows) key(isInFullscreenMode) { … } else { … }` choice.

`app/desktop/CLAUDE.md` → Fullscreen handling: name `DesktopFullscreenState` and `KubrikoShowcaseWindow.kt`; Entry point:
the minimum size line says where it is set.

## Behaviour
Intended unchanged: same states, same order of saves and restores, same `delay(100)`, same recreation via `key` on
Windows. The risks are timing ones — when `minimumSize` is set, and that the `WindowStateListener` and the toggle see
the same state instance after the window is recreated — hence Planned.

## Public API
None.

## Tests
`DesktopFullscreenState`'s save/restore needs a real `ComposeWindow` (AWT), which the desktop app has no test setup for;
none planned. Optionally extract the pure "what to restore" decision if it grows.

## Verify
`./gradlew :app:desktop:compileKotlin`

## Manual check
On Windows, macOS and Linux: toggle fullscreen in a game (button and, on macOS, the green button / Escape), return —
the window comes back at the same size and position, with the title bar strip; resize below 400×400 is refused before and
after a round trip; maximize, then fullscreen, then back → maximized again.
