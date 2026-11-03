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
