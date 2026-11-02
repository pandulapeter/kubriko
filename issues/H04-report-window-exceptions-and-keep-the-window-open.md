<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Install a window exception handler that reports an unexpected UI exception in the console and keeps the window open, rebuilding its content

**Challenged:** amended — spelled out that giving up goes through Compose's handler, which sends `WINDOW_CLOSING` so H03a's orderly quit saves the pending writes, and that every failure counts toward the guard (no loop); the manual check now covers a failure during composition, with a fallback split if the recomposer can't recover; K09 note.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** UI-6 (the window-handler part; its local `IOException` catch in `ProjectSettings` already landed)  ·  **Depends on:** H03a (the handler is wired in the new `main`, and giving up quits through its `shutdown()`; if H03a hasn't landed, wire it in today's `application {}` the same way). **K09:** nothing to re-locate; K09a/K09b don't touch `KubrikoCreatorApp.kt`, `AppController.reportUnexpectedProblem`, or `window/`
**Files:** new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/window/CreatorWindowExceptionHandler.kt`, new `.../window/RepeatedFailureGuard.kt`, `.../KubrikoCreatorApp.kt`, `.../AppController.kt`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/window/RepeatedFailureGuardTest.kt` (new), `.../window/CreatorWindowExceptionHandlerTest.kt` (new), `.../AppControllerTest.kt`, `creator/CLAUDE.md`

## Problem

Creator installs no window exception handler: `grep -rn "WindowExceptionHandler" creator/src` finds nothing; only coroutine scopes
have `unexpectedProblemHandler` (`foundation/UnexpectedProblemHandler.kt`). An exception thrown in composition, layout, drawing, or
an input handler (a `produceState` that hits an unexpected `IOException`, a bug in a list key) reaches Compose's
`DefaultWindowExceptionHandlerFactory`, which shows an error dialog and closes the window: the app is gone, with pending type
writes cut off. `SwingWindow` (Compose 1.12.2, `androidx.compose.ui.awt.SwingWindow`) reads `LocalWindowExceptionHandlerFactory`
and sets `ComposeWindow.exceptionHandler` from it (verified in `ui-desktop-1.12.2.jar`), so providing that local is enough.

The user decided (decisions.md, "UI exceptions"): the window exception handler logs to the console and keeps the window open,
plus local catches for I/O.

## Fix

1. `window/CreatorWindowExceptionHandler.kt`:
   ```kotlin
   /**
    * Reports what the window's UI threw and nothing caught ([report]) instead of closing the window, and asks for its content
    * to be built again ([onRecover]). Failures that keep coming ([RepeatedFailureGuard]) fall back to Compose's own handler,
    * which shows them and closes the window, rather than looping.
    */
   @OptIn(ExperimentalComposeUiApi::class)
   internal fun creatorWindowExceptionHandlerFactory(
       report: (Throwable) -> Unit,
       onRecover: () -> Unit,
       guard: RepeatedFailureGuard = RepeatedFailureGuard(),
       fallback: (Window, Throwable) -> Unit = { window, throwable -> DefaultWindowExceptionHandlerFactory.exceptionHandler(window).onException(throwable) },
   ) = WindowExceptionHandlerFactory { window ->
       WindowExceptionHandler { throwable ->
           throwable.printStackTrace()
           if (guard.shouldGiveUp()) {
               fallback(window, throwable)
           } else {
               report(throwable)
               onRecover()
           }
       }
   }
   ```
   `report` and `onRecover` must not throw; the handler runs on the AWT event thread, so `report` only appends to a `StateFlow` (it does).
   **Giving up loses no work:** Compose's own handler (checked in `ui-desktop-1.12.2.jar`: `DefaultWindowExceptionHandlerFactory`) shows the error dialog and then dispatches `WINDOW_CLOSING` to the window, which runs `onCloseRequest`, that is H03a's `shutdown()` (the game stopped, pending type writes and the last settings save finished), and then rethrows. So the fallback must stay that handler (no `window.dispose()` of our own, no `exitProcess`), and H03a's `isQuitting` guard makes a second `WINDOW_CLOSING` from a repeated failure harmless. Say so in the factory's KDoc.
   **No loop:** every failure, reported or not, is counted by the guard before anything else, so a subtree that throws on every recomposition is rebuilt at most three times within 10 s and then goes to the fallback; a `report` that itself makes the UI throw (a console row that can't be drawn) counts the same way.
2. `window/RepeatedFailureGuard.kt`: `internal class RepeatedFailureGuard(private val maximumFailures: Int = 3, private val windowInMilliseconds: Long = 10_000, private val currentTimeMillis: () -> Long = System::currentTimeMillis)` with `fun shouldGiveUp(): Boolean` recording the failure time and returning true once more than `maximumFailures` happened within the window (a ring of timestamps, `LongArray`).
3. `KubrikoCreatorApp.kt`: `var recoveries by remember { mutableIntStateOf(0) }`; wrap the `SwingWindow` call in `CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides remember { creatorWindowExceptionHandlerFactory(report = graph.controller::reportUnexpectedProblem, onRecover = { recoveries++ }) })`, and wrap the window's content (inside `TitleBarInsets`) in `key(recoveries) { … }`, so a subtree that threw is thrown away and composed afresh instead of being left half-built. Remembered app state above the key (controllers, window state, title bar) survives; state remembered inside it (open panel, dialogs) resets, which is acceptable after a crash.
4. `AppController.reportUnexpectedProblem`: today it reports only into an open workspace's console:
   ```kotlin
   fun reportUnexpectedProblem(exception: Throwable) {
       (_screen.value as? AppScreen.Workspace)?.controller?.reportUnexpectedProblem(exception)
   }
   ```
   Add: on `AppScreen.Welcome`, replace the screen with `AppScreen.Welcome(error = Res.string.status_unexpected_problem, errorArguments = listOf(exception.toString()))` (the Welcome screen already shows `error` with arguments); on the other screens it stays printed only (by the handler). KDoc updated.

No new strings (`status_unexpected_problem` exists: "Kubriko Creator ran into a problem: %1$s"). `creator/CLAUDE.md`: next to where `unexpectedProblemHandler` / unexpected problems are described (grep "unexpected"), add: the window's own exceptions are reported the same way and the window's content is built again, unless they repeat (more than three within 10 s), when Compose's dialog closes it.

## Tests

- `RepeatedFailureGuardTest`: three failures within 10 s → never gives up; the fourth within 10 s → gives up; four spread over more than 10 s (fake clock) → doesn't.
- `AppControllerTest.anUnexpectedProblemOnTheWelcomeScreenIsShownThere`: `createController().reportUnexpectedProblem(IllegalStateException("Broken."))` → `screen.value == AppScreen.Welcome(error = Res.string.status_unexpected_problem, errorArguments = listOf("java.lang.IllegalStateException: Broken."))`.
- `CreatorWindowExceptionHandlerTest`: the factory with a recording `report`, a counting `onRecover`, a fake-clock guard, and a recording `fallback`; get the handler with `exceptionHandler(window)` where `window` is a `java.awt.Window` only if `!GraphicsEnvironment.isHeadless()` (guarded by `org.junit.Assume.assumeTrue`), otherwise pass through an `internal fun handle(window, throwable)` the factory delegates to and that accepts a null window in tests. Assert: failures 1–3 are reported and recover, failure 4 goes to `fallback` and is not reported.

## Manual check

Temporarily add `if (screen is AppScreen.Workspace) error("test")` behind a button click in `:creator:run` (not committed): the console shows "Kubriko Creator ran into a problem: java.lang.IllegalStateException: test", the window stays open and usable, and four quick clicks show Compose's dialog. Do it twice, once with the `error("test")` in a click handler and once in a composable's body (`if (flag) error("test")` with `flag` set by the click), and confirm the content really rebuilds in both (not a blank or frozen window). If Compose's recomposer can't recover after an exception thrown during composition in this version (the window stops updating), don't keep such a window open: send a throwable whose stack trace has a `androidx.compose.runtime.ComposerImpl` or `androidx.compose.runtime.Recomposer` frame straight to the fallback (Compose's dialog, then the orderly quit above), keep recovering for the rest (input handlers, layout, drawing), add `CreatorWindowExceptionHandlerTest.aCompositionFailureGoesStraightToTheFallback` for that split, and say so in the KDoc and `creator/CLAUDE.md`. If neither case recovers, report that instead of committing.
