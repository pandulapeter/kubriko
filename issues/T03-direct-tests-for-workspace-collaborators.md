<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Test SessionController, CreatorTaskRunner, WorkspaceRunner, GameBuilds, PendingWrite, and GradleWorkspaceOpener directly, moving their cases out of WorkspaceControllerTest, and move the workspace fakes into the shared testing package

**Challenged:** amended — the read-only-folder case skipped root only and would fail on the Windows CI job (a read-only directory still takes new files there); it now probes the folder and skips when the write isn't refused.

**Kind:** tests  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** TST-10, TST-6 (workspace fakes)  ·  **Depends on:** T01, S03, S04, S05 (cross-lane). **Ordering:** this plan edits `workspace/WorkspaceControllerTest.kt`, which S03 and S04 edit in phase 1 and K and H plans (K01a, K01b, K03, H01, H02b, H03a, H03b) edit in phase 2. Run it as a serial step on `main` after lane S has merged and before the phase 2 worktrees are cut.
**Files:** (T = `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator`)
- New: `T/workspace/SessionControllerTest.kt`, `T/workspace/CreatorTaskRunnerTest.kt`, `T/workspace/WorkspaceRunnerTest.kt`, `T/workspace/GameBuildsTest.kt`, `T/workspace/PendingWriteTest.kt`, `T/workspace/GradleWorkspaceOpenerTest.kt`
- Moved to `T/testing/` (package line only): `T/workspace/FakeProjectBuild.kt`, `T/workspace/FakeSessionChannel.kt`, `T/workspace/FakeBlockTypeRepository.kt` (T01 fixed it in place)
- Rewritten: `T/workspace/WorkspaceControllerTest.kt` (the moved tests deleted, imports), and for the fakes' imports `T/AppControllerTest.kt`, `T/benchmark/CreatorBenchmarkTest.kt`, `T/workspace/BlockTypesControllerTest.kt`, `T/testing/FakeBlockTypeRepositoryContractTest.kt`, `T/commands/CommandTesting.kt`, `T/commands/ReadCommandsTest.kt`, `T/commands/actorTypes/ActorTypeCommandsTest.kt`, `T/commands/folders/FolderCommandsTest.kt`, `T/commands/git/GitCommandsTest.kt`. Check with `grep -rln "FakeProjectBuild\|FakeSessionChannel\|FakeBlockTypeRepository" creator/src/test` at execution.
- Docs: `creator/CLAUDE.md` (Running and testing: the fakes bullet names them in `testing/`)

## Problem

`T/workspace/WorkspaceControllerTest.kt` (853 lines at HEAD) is the only test of six of `WorkspaceController`'s collaborators: `SessionController`, `CreatorTaskRunner`, `WorkspaceRunner`, `GameBuilds`, `GradleWorkspaceOpener`, and `PendingWrite`'s pure helper. No test file names them. `DesktopModuleLookup` now has `DesktopModuleLookupTest`, so it is left out. Every case builds the whole workspace through `createController(...)`, which wires `FakeProjectBuild`, `FakeSessionChannel`, a test dispatcher, and every `WorkspaceController` parameter. A failure in one collaborator then shows up as a workspace failure, and the phase-2 plans that restructure `WorkspaceController` (K09: ProjectChanges router and workspace-scoped server; H03: AppGraph) have no direct tests to keep them honest.

The collaborators can be built on their own (M = `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator`):

```kotlin
// M/workspace/SessionController.kt
internal class SessionController(
    private val channel: SessionChannel,
    private val runTask: suspend (taskName: String, properties: Map<String, String>) -> Boolean,
    private val console: ConsoleLog,
    private val scope: CoroutineScope,
    private val prepareLaunch: suspend () -> GameLaunchPreparation = { GameLaunchPreparation.Unavailable },
    private val runGame: suspend (GameLaunch, Map<String, String>) -> GameProcess.Exit = { _, _ -> GameProcess.Exit(0, emptyList()) },
) : AutoCloseable

// M/workspace/CreatorTaskRunner.kt
internal class CreatorTaskRunner(
    private val rootDirectory: File,
    private val build: ProjectBuild,
    private val desktopModulePath: DesktopModuleLookup,
    private val incrementalCompilation: IncrementalCompilationPolicy,
    private val console: ConsoleLog,
    private val awaitWrites: suspend () -> Unit,
    private val currentTimeMillis: () -> Long,
)

// M/workspace/WorkspaceRunner.kt
internal class WorkspaceRunner(
    private val sessions: SessionController,
    private val startSession: (firstScene: String?) -> Unit = sessions::play,
    private val runTask: suspend (taskName: String, properties: Map<String, String>) -> Boolean,
    private val console: ConsoleLog,
    private val classpathFile: File,
) : ProjectRunner
```

`GameBuilds(rootDirectory, sessions, runTask, console, scope, ioDispatcher, currentTimeMillis)` reads the classpath file that `runTask` is expected to write (`SessionInitScript.playClasspathFileOf(rootDirectory)`). `PendingWrite.withoutOverwrittenSaves(writes)` is pure. `GradleWorkspaceOpener.open` throws `ProjectInspectionException(Res.string.error_not_a_gradle_project)` for a folder that isn't a Gradle project, and `ProjectInspectionException(Res.string.error_project_format_not_upgraded_on_open)` when `ProjectFormat.prepare()` throws `IOException`. Both happen before any Gradle connection is used.

**Re-read the classes after lane S**: S04 adds `expectSession()` to `SessionChannel` and calls it from `SessionController.play`, and S05 removes Edit mode. Write the tests against the code as it is then.

## Fix

1. Create the six test files. Each builds only the class under test, using `runTest`, `UnconfinedTestDispatcher(testScheduler)`, a `ConsoleLog()`, the fakes, and literal expected values (task names from `SessionInitScript` constants, property maps written out). Then move the `WorkspaceControllerTest` cases whose subject is now covered directly. Delete each moved case from `WorkspaceControllerTest`, with its helpers if nothing else uses them, and keep the cases that exercise the wiring between collaborators (listed below). A case is moved only when its new direct version asserts at least what the old one did.

   | New file | Cases moved from `WorkspaceControllerTest` (names at HEAD) | New cases |
   |---|---|---|
   | `SessionControllerTest` | `aHelloInTheCurrentProtocolMarksTheSessionAsRunning`, `aHelloInANewerProtocolStillRunsTheSession`, `aHelloInAnInvalidProtocolStopsTheSession`, `logsOfTheSessionAppearInTheConsole`, `aSessionFailureAppearsInTheConsoleInTheSessionsWords`, `aSessionThatExitsOnItsOwnEndsQuietly`, `stoppingCancelsTheRunningSession`, `stoppingWithoutASessionDoesNothing`, `startingASessionReplacesTheRunningOne` | `restartPlaysFromTheSceneOfTheLastPlay`; `aGameThatThrowsIOExceptionSaysItDidntStart` (a `runGame` that throws `IOException("x")` appends `status_game_not_started` with argument `"x"`, and the state goes back to `Idle`); `requestsWhileNotPlayingAnswerNotPlaying`; `requestsToAnOlderProtocolAnswerUnsupported` (Hello at protocol 2, then `request(minimumProtocolVersion = 3)` gives `Unsupported`); `anUnansweredRequestTimesOut` (`FakeSessionChannel.answer = { null }`, virtual time, gives `NoAnswer`); `aRequestTheChannelCantSendAnswersNotPlaying` (`isConnected = false`); `closingCancelsTheSessionAndClosesTheChannel` |
   | `CreatorTaskRunnerTest` | `commonCodeCompilesIncrementallyUnless…`, `aProjectWithoutADesktopModuleSaysWhyItCantBuild`, `aBuildWaitsForTheChangesThatAreStillBeingWritten`, `aSessionWaitsForTheClasspathBuildBeforeIt`, `problemsAppearWithTheirLocationRelativeToTheProject`, `aFailedBuildWithoutProblemsShowsTheTailOfItsOutput`, `aFailedBuildWithProblemsShowsOnlyTheProblems`, `theSessionTaskOfANestedModuleIsAddressedByItsPath` | `aLookupFailingWithAnUnexpectedExceptionSaysItIsNotAGradleProject`; `cancellingDuringTheLookupIsNotReportedAsAFailure` (the `CancellationException` branch rethrows, and nothing is appended after `status_building`); `twoClasspathBuildsDontOverlap` (the second `FakeProjectBuild.Run` starts only after the first finishes); `onlyTheLast30OutputLinesAreShown` (35 `Output` events, failure, then exactly lines 6–35 as `BuildOutput`); `aSuccessfulClasspathBuildReportsItsTime` (`currentTimeMillis` returns 1 000, then 2 500, giving `status_built` with `"1.5"`); `warmingUpWithoutADesktopModuleDoesNothing` |
   | `WorkspaceRunnerTest` | `theBuildToolCompilesAndReturnsTheProblemsItReported`, `thePlayToolPlaysForAWhileOnceTheGameIsUpAndReturnsWhatItLogged`, `thePlayToolReportsAGameThatNeverStarted`, `sessionsOfTheFirstProtocolVersionStillRunButCantBePlayedByTools`, `theGameToolsPlayTheRunningGameAndBringItToTheFrontFirst`, `aGameThatDoesntAnswerIsReported` | `aStepWhoseBringToFrontFailedStopsThere` (`CommandFailed` for `BringToFront` gives `GameStep.Failed` and no second command is sent); `aScreenshotIsDecodedFromBase64`; `stopGameWithoutAGameReportsFailure` (`RunResult(isSuccessful = false, …)`) |
   | `GameBuildsTest` | `aGameTheBuildSaysHowToLaunchIsPlayedWithoutGradleFromTheChosenScene` and `aGameStartedWithHotReloadGetsTheChangesMadeWhileItPlays`, only if they can be expressed with `GameBuilds` alone; otherwise leave both and add direct cases | `aFailedClasspathBuildMeansFailed`; `aClasspathFileWithoutALaunchMeansUnavailable`; `onlyClassFoldersInsideTheProjectAreRead` (a classpath listing one folder under the root and one outside: `prepareLaunch` reads shapes only from the first. Paths are built with `File`, so the test works on Windows); `theRunTaskGetsTheClasspathAndHotReloadProperties` (literal map: `kubrikoCreatorMode=classpath`, `kubrikoCreatorClasspathFile=<absolute path>`, `kubrikoCreatorHotReload=true`) |
   | `PendingWriteTest` | — | `aSaveFollowedByASaveOfTheSameTypeKeepsOnlyTheLast`; `aSaveFollowedByADeleteKeepsOnlyTheDelete`; `savesOfOtherTypesAndFolderWritesKeepTheirOrder` (literal lists of `PendingWrite`s in, literal lists out); `aDeleteFollowedByASaveKeepsBoth` |
   | `GradleWorkspaceOpenerTest` | — | `aFolderThatIsNotAGradleProjectIsRefused` (a `testDirectory()` with no build files: the exception's `reason` is `Res.string.error_not_a_gradle_project`); `aProjectWhoseFormatCantBeUpgradedIsRefused` (a Gradle project whose `.kubriko/project.json` is at format 0 and whose `.kubriko` folder is read-only, so recording the new format version in `project.json` (`writeTextAtomically` creates its temporary file in that folder) fails. After `setWritable(false)`, probe it: `assumeFalse(runCatching { File(kubriko, "probe").createNewFile() }.getOrDefault(false))`, which skips the case where the folder still takes files: as root, and on Windows, whose read-only flag on a directory doesn't stop new files (`:creator:test` runs on `windows-latest` in `Tests`). Make the folder writable again in a `finally`, so the temporary directory can be deleted) |

   Kept in `WorkspaceControllerTest`: everything not in the table. That covers resources, background failures, the actor type and Scene names, undo rejection, watching, format status, closing, `playRunsTheSessionTaskOfTheRootModuleInPlayMode`, `aFailedBuildStartsNoGame`, `playingFromASceneInAProjectThatNeedsAnUpgrade…`, `stoppingAGamePlayedWithoutGradleStopsItsProcess`, `theCompilerIsWarmedUpSilently…`, `theDesktopModuleIsAskedForAgainAfterALookupThatFailed`, `closingWritesWhatIsPendingBeforeTheBuildIsReleased`, and the console cases. These test the wiring and stay as integration cases.
2. Move `FakeProjectBuild`, `FakeSessionChannel`, and `FakeBlockTypeRepository` from `T/workspace/` to `T/testing/`, changing only the package line, and update every import (see **Files**). Then run `./gradlew spotlessApply`.
3. In `creator/CLAUDE.md`, in the fakes bullet, say the fakes are in `testing/`, and add: "a collaborator of `WorkspaceController` is tested on its own, and `WorkspaceControllerTest` keeps only cases about the wiring between them."
4. No production change. If a collaborator turns out not to be constructible alone, for example because it needs a `WorkspaceController` field, stop and report it rather than widening visibility.

## Tests

Listed in the table. Every new case must fail when the line it covers is broken. For example, in `CreatorTaskRunner` change `OUTPUT_TAIL_SIZE` to 29, or remove the `classpathBuildLock` around `runBuild()`, and the matching case fails. Check this locally for each file and revert. Afterwards, `WorkspaceControllerTest` is no longer the only file naming any of the six classes, and the whole `:creator:test` suite passes.
