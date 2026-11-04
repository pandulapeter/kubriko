<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Automated testing

A game made in Creator is checked by tests the way Creator itself is: small Kotlin tests that play a Scene for a few seconds of game
time, press keys, and check what the Actors did. Most of them are written and kept up to date by the AI, since that is where a beginner
gets them for free, but they are plain Kotlin in the project's own test folders, so they run in IntelliJ, in `./gradlew`, and on CI
exactly as Creator runs them.

Tests are Kotlin, not Blocks: someone who never writes one still reads it, runs it, and sees what failed in plain words, and the
assistant does the writing. What matters is the integration: Creator finds the tests a project already has, shows them next to what
they test, runs them with one click, and tells the user and the AI what broke.

What exists to build on: `ProjectModules` and `DesktopModules` (the project's modules, from the files and from Gradle's `GradleProject`
model), `ProjectTree.walk`, the build lock and arguments every build shares (`classpathBuildLock`, `SessionInitScript.gradleArguments`),
`KotlinIdentifiers` for renames, the health checks, `WriteDescriptionButton`'s way of sending a prompt to the shown conversation,
Keep and Undo for a turn's changes (`TurnChangesCard`), and, in the library, `TickSource.manual()` and what `:tools:test-fixtures`
already does for this repository's own tests (a manual-tick instance, `awaitProcessed`).

This is the last plan: it comes after everything else, since tests are most useful once the Block set, Traits, and the screenplay
exist. Its first part, finding a project's existing tests, waits for nothing and can be pulled forward on its own.

## Finding the tests a project has

The priority, since every existing project (Tesselar has 66 test files across nine modules, in both `src/commonTest` and plain
`src/test` folders) should show its tests the moment it opens, opted in or not, before Creator writes a single one.

- **From the files first** (`tests/ProjectTests`, a new package of `:creator:core`, so `PackageLayeringTest` gives it a layer): every
  module's test source folders, found without Gradle, so opening never waits: Kotlin Multiplatform's `src/<sourceSet>Test/kotlin`
  (`commonTest`, `desktopTest`, `jvmTest`, `wasmJsTest`, `iosTest`, `androidUnitTest`, `androidInstrumentedTest`, and the rest), and
  plain JVM and Android modules' `src/test` and `src/androidTest`, Kotlin and Java alike. Every module counts, not only the ones that
  use Kubriko (`ProjectModules`), since a game's model module is often where its tests are.
- **From Gradle second:** the `GradleProject` model `DesktopModules` already fetches names each module's test tasks (`desktopTest`,
  `jvmTest`, `test`, `testDebugUnitTest`, `wasmJsBrowserTest`, `iosSimulatorArm64Test`, …), which decide what can run and where. A
  source folder a build file sets itself (`kotlin.srcDir`) comes from the session init script, which already reads the Kotlin
  extension, once a project is found to need it. Asked in the background after opening and again before the next run once a build
  file or the settings file changes, as the game's modules are; the result is kept in `.kubriko/local/tests.json`, so the next
  opening shows the same list at once.
- **The tests in a file:** a light scanner (no compiler, like `KotlinIdentifiers`): the package, the classes, and their test
  functions, for kotlin.test, JUnit 4 and 5 (`@Test`, `@ParameterizedTest`, `@Ignore`, `@Disabled`, nested classes), and Kotest's
  usual spec styles (`test("…")`, `"…" { }`, `should("…")`). Names are shown as written when they are backticked sentences and split
  into words when they are camelCase (`ballBouncesOffTheWall` → "Ball bounces off the wall"). A file the scanner can't read is still
  listed and runs as a whole; a test it misses still shows up in the results of a run, which add it to the list.
- **What each test touches:** the Actor types, Manager types, and Scenes a test file names (by the identifiers rename already
  follows, and Scene names in string literals), so a type shows its tests and a change to a type knows which tests to run.
- **Watched** like every other source: a test file changed outside Creator shows within a second.
- **Unit tests** over real temporary folders laid out like Tesselar, the Showcase's `examples/`, a plain Android app, and the template;
  the scanner over files in every supported style, each with what it must not take for a test (a `@Test` in a comment or a string,
  a helper function in a test class); `GradleProject` models recorded from those projects.

## Running them

Anything with a JVM test task runs from the start (`desktopTest`, `jvmTest`, `test`, and Android's `testDebugUnitTest`), which is
nearly every test a game has; the web's and iOS's test tasks and Android's instrumented tests are listed, marked as not run by Creator
yet, and come later (below).

- **Gradle's own test launcher** (the Tooling API's `TestLauncher`), not a task run with its output parsed: one test, a class, a file,
  a module, or everything, with per-test events (started, passed, failed, skipped), durations, what each test printed, and failures
  with their expected and actual values (`TestAssertionFailure`) and stack traces. It runs in `:creator:gradle-session` with every
  other build's arguments and holds `classpathBuildLock`, so it never overlaps the Scene editor's builds or a play session's; the pending
  writes land first, as before a build.
- **One run at a time:** asking for another while one runs queues a single run of both sets after it, rather than cancelling the first,
  as the Scene editor's builds do. Stop cancels through the launcher's token and returns once Gradle has stopped. Tests that never end
  are the user's to stop, but the game test kit (below) gives each game test a timeout of its own.
- **Tests that don't compile** are a result of their own ("The tests don't compile"), with the compiler's messages at their file and line,
  never mistaken for failing tests, and a game that doesn't compile says so before any test runs.
- **Results** (`TestResults`) are kept per test in `.kubriko/local/test-results.json` (`JsonFileStore`), so the last run shows after
  reopening; a result whose file, or anything its test touches, changed since is marked as out of date rather than dropped.
- **Unhappy paths, tested against `FakeProjectBuild`:** a test task that disappears from the build, a filter that matches nothing
  (Gradle fails that run; Creator says no test matched), a daemon that dies midway, Windows paths in stack traces, and output that floods
  (each test keeps its last lines, as the console does). One run of the starter's tests for real in `ProjectTemplateIntegrationTest`.

## Where they show

- **The sidebar** gains a Tests group per module that has tests, after its Scenes: files in their folders, each test a row under its file
  with its last result (passed, failed, out of date, or not run). Running is `hoverReveal`'s button on any row and its `RightClickMenu`
  (Run, Run the file, Ask the assistant to fix it, Open in IntelliJ, Delete). A module with no tests shows no group.
- **The toolbar** gains Run tests beside Play, which runs every test that can run, and shows the counts once it has (a quiet "24 passed",
  a red "2 failed"); its menu runs only the tests of what changed since the last run.
- **The Tests panel** (beside the console, `AnimatedSidePanel`'s way): the run's tree with a summary bar, and for a failed test what a
  beginner needs first: the failure in plain words ("Expected 3, but it was 2"), expected and actual side by side when the failure has
  them, the test's own lines around the failing one, highlighted, and the stack trace shortened to the project's frames, each a link
  to its file and line. What the test printed is a fold away.
- **A test file in the center** opens read-only with Kotlin highlighting, its tests' results in the gutter, until the code editor
  (`17-later.md`) makes it editable; "Open in IntelliJ" is in its header.
- **Next to what they test:** an Actor type's, Manager type's, or Scene's header lists the tests that touch it and their state, and
  its right-click menu offers "Write a test with the assistant".
- **Committing** (`ui/git`): when tests are failing, or game code changed since the last run, the commit form says so with Run tests
  one click away. It never refuses the commit.
- **Offscreen snapshots** (`WorkspaceScreenTest`) of the sidebar group in every state, the panel with a passing run, a failing one, and
  one that doesn't compile, the toolbar button, and a test file with its gutter.

## A test kit for games

What a game's test needs that Kubriko doesn't offer yet: a game instance without a window, time that moves only when told, and input
without a keyboard. A new published library module, `tools/game-testing` (`tool-game-testing`, desktop only, since it is only for
tests), so it is a library change, released before Creator relies on it (Principle 4: opt-in, never the engine bending for the editor).

```kotlin
class BallTest {

    @Test
    fun `the ball bounces off the left wall`() = testGame(scene = "main") {
        val ball = actor<Ball>()
        ball.horizontalSpeed = -5f
        advanceBy(seconds = 2)
        assertTrue(ball.horizontalSpeed > 0)
    }

    @Test
    fun `pressing space starts the game`() = testGame {
        press(Key.Spacebar)
        advanceUntil(seconds = 1) { manager<GameStateManager>().isPlaying }
    }
}
```

- **The kit's API** (`GameTest`, `gameTest(…)`): an instance from the Managers the test passes, on `TickSource.manual()`, with a viewport
  size of its own (default 1920 by 1080), since the camera's edges are zero without a viewport and games use them;
  `advanceBy(milliseconds = …)` and `advanceFrames(count)` in fixed ticks, `advanceUntil(timeout) { condition }` in game time;
  `actor<T>()` (exactly one), `actors<T>()`, `manager<T>()`, `add`, and `remove`, which wait for the batched operation to be applied,
  so a test reads what it just did; `loadScene(name)`; `press`, `release`, `hold(key, milliseconds)`, pointer presses at a
  `SceneOffset`, and gamepad buttons; and a wall-clock timeout per test (ten seconds by default), so a test stuck in a loop fails
  rather than hangs. Audio plays nothing, sprites load as they do in the game (the game's test classpath has Skia through
  `compose.desktop.currentOs`), and nothing is drawn.
- **Library hooks it needs:** input from somewhere other than Compose's key, pointer, and gamepad events, and a viewport size without a
  viewport. Both are additions to the public API of the input plugins and `ViewportManager`, so their shape is confirmed before they land
  (the root `CLAUDE.md` → Public API stability). The kit's own tests use `:tools:test-fixtures`; whether that module then builds on the
  kit is decided once both exist.
- **Deterministic randomness:** the random Blocks draw from one generated `Random` that `testGame` seeds, so a test of a random spawn
  fails the same way every time.
- **The project's side** (a `TemplateStep`, so existing projects get it as an upgrade, and the starter from the start): `Game.kt`'s
  Manager list moves into `createGameManagers(firstScene)`, which `Game()` and the user-owned `src/desktopTest/kotlin/<package>/TestGame.kt`
  (`testGame { }`) both call, so a test runs the same game the window shows; `kotlin-test` and the kit in `desktopTest`'s dependencies;
  and one example test of the Ball. The Kubriko dependency sync (`KubrikoBuildFiles`) learns test source sets, so the kit comes and goes
  with what the tests name, in the test dependencies, never the game's.
- **The kit's documentation** is a heading of the root `CLAUDE.md`'s Common patterns, so `get_kubriko_api` hands it to the AI (`KubrikoApiTest`
  checks its headings), and a page in the library's README.
- **Tested:** the kit's own unit tests in `tools/game-testing` (time, input, timeouts, Scenes, and a test that fails reporting why), and
  the starter's example test passing in `ProjectTemplateIntegrationTest`, which also proves Scenes load from Compose resources on the
  test classpath before anything else builds on it.

## Writing them

Test files are Kotlin the user may also write by hand, so Creator writes them only through commands, which the AI shares.

- **Ownership:** test source folders become a row of `../CLAUDE.md` → Files and ownership: Shared. Creator changes them only through the
  test commands and when a rename follows into them; the user writes them anywhere. A change the assistant makes to a test file shows as a
  diff in the turn's card with Keep and Undo, which is how Principle 3 holds for them.
- **Commands** (`:creator:commands`, each with its AI tool, validation, apply, and the inverse round trip): `create_test_file`,
  `edit_test_file` (exact text replaced, as an editor's Edit does, so a small change costs few tokens), `delete_test_file`, and the
  read and action commands `list_tests` (paged), `read_test_file`, `run_tests` (a filter by module, file, test, or "what touches this
  type"; waits for the run and returns the counts and each failure in short), and `read_test_results` (one test's failure in full,
  with the compiler's messages when it doesn't compile). Writes are refused outside a test source folder, for anything but a `.kt`
  file, for a package that doesn't match its folder, and in a project that opened read-only.
- **New test without the assistant:** the Tests group's "New test" picks an Actor type, a Manager type, or a Scene and writes a file with
  one example test filled in from it (its first Property, its Scene), so a beginner edits a working test instead of starting from a blank file.
- **Renames follow** into test files as into Kotlin Blocks (`KotlinIdentifiers`), in the same undo step; a delete leaves test files alone
  and the health checks name the test that now refers to nothing.

## The AI keeps them

- **The system prompt** (`AssistantSystemPrompt`) gains a section on tests: write one after building a rule the player relies on and
  before fixing a bug (the test failing first, then passing); name tests as sentences about the game; test rules, not pixels; keep each
  to a few seconds of game time; run the tests that touch what a turn changed before ending it; and never delete, skip, or loosen a
  failing test to make it pass without saying why and asking.
- **Every write command's result** names the tests that touch what it changed ("`BallTest` uses `Ball`"), as the screenplay's pages do,
  so the agent knows what to run without listing every test; `ChangesSummary` tells it about runs the user started and their results.
- **The turn's card** marks a test that was failing and was changed in the turn, so loosening one never passes unseen.
- **The context:** the digest gains one line (how many tests, how many failing, and when they last ran), and `get_kubriko_api` the kit.
- **Asking the assistant:** "Ask the assistant to fix it" on a failed test and "Write a test with the assistant" on a type or Scene send
  their prompt to the conversation the assistant shows (`WriteDescriptionButton`'s way), so the work is seen and reviewed like any other
  turn. The fix prompt asks the agent to decide first whether the game or the test is wrong, and to ask the user when it can't tell.
- **The screenplay and the writers' room** (`04-screenplay.md`): each slice the writers' room builds ends with a test of its rule, run
  and passing; a page's rule can link to its test, and the Game map shows the tests beside the cast, the crew, and the Scenes.
- **Health checks:** failing tests, tests that don't compile, and tests that name something deleted.
- **Outside agents** get the same tools over MCP; one that writes files itself (Claude Code in a terminal) is picked up by the watcher.
  The template's `AGENTS.md` says where tests go, which kit they use, and how to run them (`./gradlew desktopTest`).
- **Guards:** `EvalSetTest` requests that write, run, and fix tests ("the ball sometimes goes through the paddle"), `TokenBudgetTest`
  budgets for the new tools and the digest line, and a live scenario (`AssistantLiveScenariosTest`) on the Breakout fixture: asked to
  make a brick take two hits, the agent changes the type, adds a test of it, runs it, and ends with every test passing.

## Later

- **The other targets' tests:** the web's in the headless Chrome Creator already finds (`HeadlessChrome`), iOS's on a simulator, and
  Android's instrumented tests on the devices and emulators Play lists (`03-playing-on-every-target.md`).
- **Running affected tests on every change,** as a Creator setting, off by default, once runs are fast enough not to compete with the
  Scene editor's builds.
- **A CI workflow** offered once in Project settings (a GitHub Actions file running the tests on every push), shown as a diff like any
  file outside Creator's own.
- **Screenshot tests:** a Scene drawn offscreen and compared with an image kept in the project, once the kit can draw; decide first whether
  they can be made stable across the three desktop systems.

## Order

Finding the tests first, then running them with the panel, the sidebar group, and the read and run tools, which together serve every
project that already has tests. Then the kit and its template step (a library release), then writing tests and the AI's side of them,
then the writers' room's and the screenplay's part.

## Open questions

1. The name: "Tests" is what IntelliJ, Gradle, and every AI call them. "Rehearsals" would keep the film (a Scene played through before
   the show) but hides what they are from anyone who meets them elsewhere.
2. Whether the assistant may change a test file the user wrote by hand without being asked to, or only propose the change.
