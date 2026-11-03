<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Execution log

Follow-ups of the fifth review; earlier history is in git (PR #139).

- Challenge, F01: sound.
- Challenge, F02: amended — the report's callback is `onBatchSaved`, the KDoc has seven `@param on…` lines, and the test calls same-named parameters through `.invoke`.
- Challenge, F03: sound.
- Challenge, F04: sound.
- Challenge, F05: amended — only `store` becomes `storage.settings`; the controller's own `settings` stays the state.
- Challenge, F06: amended — the `creator/CLAUDE.md` fixture clause goes after ai's, in the Modules list's order.

## F01 — see next commit
"Hand the workspace screen's parts only the state and callbacks they use, wiring the controllers in WorkspaceScreen." — verified: `./gradlew :creator:allTests spotlessCheck` (after `spotlessApply`), `:creator:test --tests "*PackageLayeringTest*"` failing on all six files with the UI changes set aside, and the pre-commit hook's checks; skipped manual: a direct run of `check_sizes.py` and `check_doc_names.py --staged` (the CI shell refuses them outside the hook; `WorkspaceScreen.kt` is 352 lines and the `WorkspaceScreen` function kept its 115), and a window screenshot (the rendered UI is unchanged)
- `AiPanel` takes `AssistantPanelModel`; `WorkspaceAiSidePanel`, `WorkspaceGitSidePanel` (`onCommand: (Invocation<*>, onSuccess: () -> Unit) -> Unit`), and `WorkspaceDialogs` take state and callbacks; `rememberFolderTreeGroups` takes the folders as values. `rememberWorkspaceUiState`, `rememberSidebarDragState`, and `rememberFolderTreeActions` moved verbatim into `ui/WorkspaceScreen.kt` as `private`, next to the new private adapters `rememberFolderTreeGroupsOf`, `WorkspaceDialogsOf`, `WorkspaceGitSidePanelOf`, and `WorkspaceAiSidePanelOf`. New `PackageLayeringTest.onlyEntryFilesTakeTheControllers` and `ui/WorkspaceGitSidePanelTest`.
- Deviations: `WorkspaceAiSidePanel`'s locals are `shownGameMap`, `consoleEntries`, and `isAcceptingImages`, so they don't shadow the new parameters of the same names; the new test wraps the panel in a `Row`, as `WorkspaceScreen` does, since its splitter and panel are siblings.
- Extra fixes: none

## F02 — see next commit
"Report BlockTypesController's events through one BlockTypesListener, moving the workspace's console reporting of them into BlockTypesReport." — verified: `./gradlew :creator:allTests` and `spotlessCheck` (after `spotlessApply`), `:creator:workspace:test --tests "*BlockTypesReportTest*"` failing (no `BlockTypesReport`) with the main changes set aside, and the pre-commit hook's checks; skipped manual: a direct run of `check_sizes.py` and `check_doc_names.py` (the CI shell refuses them outside the hook; no `CLAUDE.md` changed, `BlockTypesController` has 7 parameters, `WorkspaceController.kt` is 460 lines), and a window screenshot (no UI changed)
- New public `actors/store/BlockTypesListener` in `:creator:core` (`onSaved` and `onSaveFailed` without defaults, the other seven no-ops); `BlockTypesController(repository, scope, ioDispatcher, listener, syncBuildFiles, writeDelayInMilliseconds, isReadOnly)`. New internal `workspace/BlockTypesReport(console: ConsoleLog, onBatchSaved)` holds the moved `hasReportedNewerKubrikoVersion` and the bodies of `reportKotlinBlockScopeChanges` and `reportBuildFileChanges`, which are gone from `WorkspaceController` with the `BlockTypeDefinition`, `KotlinBlockScopeChecks`, and `AtomicBoolean` imports. `CommandTesting.blockTypesController` passes an anonymous listener; new `BlockTypesReportTest`.
- Deviations: none (`BlockTypesReport`'s overrides are separated by blank lines, as ktlint keeps them).
- Extra fixes: none

## F03 — see next commit
"Give CommandContext the project's template upgrade as one ProjectUpgrades instead of a reading and a reporting lambda." — verified: `./gradlew spotlessApply` and `:creator:allTests`, `:creator:workspace:test --tests "*WorkspaceControllerTest.upgradingTheProjectChecksTheTemplateFilesAgain"` failing ("Expected value to be false") with `override fun onUpgraded() = Unit` in `WorkspaceController`, and the pre-commit hook's checks; skipped manual: a direct run of `check_sizes.py` (the CI shell refuses it outside the hook; `CommandContext` has 12 parameters, `WorkspaceController.kt` is 464 lines) and `check_doc_names.py` (no `CLAUDE.md` changed), and a window screenshot (no UI changed)
- New public `commands/ProjectUpgrades` in `:creator:commands` (`current(): TemplateUpgrade`, `onUpgraded()`, `ProjectUpgrades.UpToDate`); `CommandContext` takes `projectUpgrades: ProjectUpgrades = ProjectUpgrades.UpToDate` in place of `onProjectUpgraded` and `projectUpgrade`, and no longer imports `TemplateUpgrade`. `UpgradeProject` reads `context.projectUpgrades`. `WorkspaceController` passes an anonymous `ProjectUpgrades` over `_projectUpgrade` and `refreshProjectUpgrade()`; `CommandTesting.commandContext` keeps its two flat parameters and wraps them. New `WorkspaceControllerTest.upgradingTheProjectChecksTheTemplateFilesAgain`.
- Deviations: the overrides are separated by blank lines, as ktlint keeps them; the test also asserts `Game.kt` is among the changes before upgrading, so its final assertion can't pass vacuously.
- Extra fixes: none

## F04 — see next commit
"Give SceneEditingController its build, its sources, and Creator's runtime as SceneClassesBuild, SceneSources, and SceneRuntimeSetup instead of eleven parameters." — verified: `./gradlew spotlessApply` and `:creator:allTests`, and the pre-commit hook's checks; skipped manual: a failing-test demonstration (the plan adds no test: it is a behaviour-preserving refactor, and `SceneEditingControllerTest` only moves its fakes into the new types, so it can't compile against the old constructor), a direct run of `check_sizes.py` (the CI shell refuses it outside the hook; `SceneEditingController` has 8 parameters, `WorkspaceController.kt` is 455 lines) and `check_doc_names.py` (no `CLAUDE.md` changed), and a window screenshot (no UI changed)
- New public `sceneRuntime/SceneClassesBuild` (`classpathFile`, `runTask`; `requestCleanBuild`, `warmUpCompiler`, `awaitWrites` no-ops), `sceneRuntime/SceneSources` (`readScene`; `readSceneProblem`, `spriteFiles`, `actorTypeLooks` with defaults), and `sceneRuntime/SceneRuntimeSetup(creatorKubrikoVersion, creatorLibraries, entryPointLoader)` in `:creator:scene-runtime`; `SceneEditingController(runtime, classes, sources, scope, releaseScope, ioDispatcher, report, currentTimeMillis)`. New internal `workspace/WorkspaceSceneEditing.kt` holds `WorkspaceSceneClasses(tasks, incrementalCompilation, classpathFile, blockTypes)` and `WorkspaceSceneSources(scenes, resources, blockTypes)`; `WorkspaceController` builds the three and no longer imports `ActorTypeLook`. F06 can pass `SceneRuntimeSetup` through unchanged.
- Deviations: the overrides are separated by blank lines, as ktlint keeps them; the test's `runTask` and `readScene` overrides are block bodies with `return`, since they hold several statements.
- Extra fixes: none

## F05 — see next commit
"Give AssistantController its storage, its agent sources, and the open project as AssistantStorage, AgentSources, and OpenProjectAccess, moving their production wiring out of AppGraph." — verified: `./gradlew spotlessApply` and `:creator:allTests`, `:creator:ai:test --tests "*OpenProjectAccessTest*"` failing to compile (no `AssistantStorage`, `AgentSources`, or `OpenProjectAccess`) with the main changes set aside, and the pre-commit hook's checks; skipped manual: a direct run of `check_sizes.py` (the CI shell refuses it outside the hook; `AssistantController` has 11 parameters) and `check_doc_names.py` (no `CLAUDE.md` changed), and a window screenshot (no UI changed)
- New public `ai/assistant/AssistantStorage(settings, secrets, conversations, errorLogFile)` with `default(errorLogFile)`, `ai/assistant/AgentSources(registry, installer, node)` with `default()`, and the interface `ai/assistant/OpenProjectAccess` (`digest()`, `changeLog()`, `undoTurn(toolsId, sinceRevision)`; `None` and `of(executor)`), all in `:creator:ai` with `internal` properties. `AssistantController(scope, runner, storage, signInLauncher, ioDispatcher, createAssistant, openProject, sources, home, clock, isAiEnabled)`, with `errorLogFile` a body property read from `storage`. `AppGraph.assistantController` no longer imports the stores, `BinaryAgentInstaller`, `NodeRuntime`, or `ConversationStart`. New `OpenProjectAccessTest`; `creator/plan/README.md` → Structure names only `WorkspaceController` now.
- Deviations: the overrides are separated by blank lines, as ktlint keeps them.
- Extra fixes: none
