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
