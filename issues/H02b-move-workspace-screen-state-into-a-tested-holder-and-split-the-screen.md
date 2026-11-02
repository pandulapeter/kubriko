<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Move `WorkspaceScreen`'s seven pieces of screen state and their transitions into a tested `WorkspaceUiState`, and split the 270-line composable into its parts

**Challenged:** amended — `WorkspaceCenter` must carry 4ab2bdf08's `key(selectedBlockType.name)` around the type editor (checked in review), and its `onCommand` type is named for H07b, which changes it.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** UI-7, TST-11 (the WorkspaceScreen part; the app wiring part is H03a)  ·  **Depends on:** H01, H02a
**Files:** new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ui/WorkspaceUiState.kt`, new `.../ui/WorkspaceCenter.kt`, new `.../ui/WorkspaceSidePanels.kt`, new `.../ui/WorkspaceDialogs.kt`, `.../ui/WorkspaceScreen.kt`, `.../workspace/WorkspaceController.kt` (one delegating function), `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/WorkspaceUiStateTest.kt` (new), `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/WorkspaceCenterTest.kt` (new), `creator/CLAUDE.md`

## Problem

`ui/WorkspaceScreen.kt` `WorkspaceScreen` runs from line 87 to 358 at HEAD (over the 120-line function limit; grandfathered by
`check_sizes.py`). It holds view-model logic that no test reaches:
```kotlin
var openPanel by remember { mutableStateOf<SidePanel?>(null) }
var aiPanelView by remember { mutableStateOf(if (mcp.settings.value.isEnabled) AiPanelView.OWN_TOOL else AiPanelView.ASSISTANT) }
var revisitedAiStep by remember { mutableStateOf<RevisitedAiStep?>(null) }
var isFollowingAi by remember { mutableStateOf(true) }
var isUpgradeNoticeDismissed by remember { mutableStateOf(false) }
var isReviewingUpgrade by remember { mutableStateOf(false) }
var isShowingProjectSettings by remember { mutableStateOf(false) }
```
with transitions written inline: `onPanelToggled = { panel -> openPanel = panel.takeIf { it != openPanel }; if (openPanel == SidePanel.VERSION_CONTROL) controller.git.refresh() }`,
`onReviewUpgrade = { isShowingProjectSettings = false; isReviewingUpgrade = true }`, `onApply = { …dispatch(UpgradeProject); isReviewingUpgrade = false }`,
`AiActivityBar(onClick = { openPanel = SidePanel.AI })`, `onShowStep = { step -> revisitedAiStep = RevisitedAiStep(step, latestStepId = latestAiStep?.id); … }`.
It also reaches four objects deep from the UI: `captureScreen = { (controller.commands.context.runner.captureScreen() as? GameStep.Done)?.png }`.

## Fix

1. `ui/WorkspaceUiState.kt`: a plain class (Compose `mutableStateOf` fields, no composables, no controller) holding the seven values with
   read-only getters and one method per transition, so the rules are unit-tested:
   ```kotlin
   internal class WorkspaceUiState(initialAiPanelView: AiPanelView) {
       var openPanel: SidePanel? by mutableStateOf(null); private set
       var aiPanelView by mutableStateOf(initialAiPanelView); private set
       var revisitedAiStep: RevisitedAiStep? by mutableStateOf(null); private set
       var isFollowingAi by mutableStateOf(true); private set
       var isUpgradeNoticeDismissed by mutableStateOf(false); private set
       var isReviewingUpgrade by mutableStateOf(false); private set
       var isShowingProjectSettings by mutableStateOf(false); private set

       /** Opens [panel], or closes it if it is open. @return Whether version control just opened, which refreshes git. */
       fun togglePanel(panel: SidePanel): Boolean
       fun showAiPanel()
       fun selectAiPanelView(view: AiPanelView)
       fun setFollowingAi(isFollowing: Boolean)
       fun revisit(step: AiStep, latestStepId: Int?)
       fun dismissUpgradeNotice()
       fun reviewUpgrade()            // from the notice or from Project settings: closes the settings dialog
       fun finishReviewingUpgrade()   // applied or dismissed
       fun showProjectSettings(); fun hideProjectSettings()
   }
   @Composable internal fun rememberWorkspaceUiState(controller: WorkspaceController, mcp: McpController) =
       remember(controller) { WorkspaceUiState(if (mcp.settings.value.isEnabled) AiPanelView.OWN_TOOL else AiPanelView.ASSISTANT) }
   ```
   Keyed on `controller`, so a newly opened project starts fresh, as the plain `remember {}`s do today because `WorkspaceScreen` leaves composition.
   `RevisitedAiStep` and `shownAiStep` stay in `ui/AiFollowing.kt`. Use the actual id type of `AiStep.id` (check `commands/AiStep.kt`).

2. Split the composable (each file one Composable, as the code-style skill asks):
   - `WorkspaceDialogs.kt`: `WorkspaceDialogs(controller, uiState, projectUpgrade)` — the `ProjectSettings` and `ProjectUpgradeDialog` blocks.
   - `WorkspaceCenter.kt`: `WorkspaceCenter(modifier, selection: WorkspaceSelection, sceneEditing: SceneEditingState, …, onCommand, …)` — the `when (sceneEditing)` and the type-editor/hint branch. Take plain values and callbacks, not the controller, so it is testable offscreen (the `BlockTypeEditor` parameters it needs: the selected definition, `BlockScope`, sprites, `Inheritance`, `isChangedByHand`, `highlight`, and callbacks). Move the editor together with 4ab2bdf08's `key(selectedBlockType.name) { BlockTypeEditor(...) }` wrapper, unchanged: it is what keeps one type's buffered field text, `ScreenTextList`'s unkeyed `ListItemKeys`, and the editor's other `remember`s from carrying over to the next type. Its `onCommand` parameter has `BlockTypeEditor`'s type, `(Invocation<*>) -> Unit` here; H07b later changes both to take an `onSuccess`.
   - `WorkspaceSidePanels.kt`: `WorkspaceAiSidePanel(...)` and `WorkspaceGitSidePanel(...)` (two composables → two files, `WorkspaceAiSidePanel.kt` and `WorkspaceGitSidePanel.kt`, if the code-style skill's one-Composable-per-file rule applies to private ones too); the AI one owns `attention`, the `gameMap` collection from H02a, and `onShowStep` calling `uiState.revisit(...)` then `controller.selection.selectType/openScene` (H01).
   - `WorkspaceScreen` keeps collecting the controller's flows and lays out toolbar, notices, sidebar, center, side panels, and console; target under 120 lines.

3. `WorkspaceController.captureGameScreen(): ByteArray?` = `(commands.context.runner.captureScreen() as? GameStep.Done)?.png` (KDoc: a screenshot of the running game, null when none is running or it failed), and the AI panel passes `captureScreen = controller::captureGameScreen`. **K09:** if K09 moved the runner, put the function on whichever object owns `WorkspaceRunner` and keep `WorkspaceController.captureGameScreen` delegating.

No behavior changes, no strings change. `creator/CLAUDE.md`: in the package table's `ui` row, add that `WorkspaceUiState` holds the workspace screen's panels, dialogs, and AI-follow state.

## Tests

`WorkspaceUiStateTest` (plain JVM, no UI):
- `togglingAPanelOpensItAndTogglingAgainClosesIt`; `openingAnotherPanelReplacesTheOpenOne`.
- `onlyOpeningVersionControlAsksForAGitRefresh`: `togglePanel(VERSION_CONTROL)` → true; again (closing) → false; `togglePanel(AI)` → false.
- `theActivityBarOpensTheAiPanelEvenWhenAnotherIsOpen`.
- `reviewingTheUpgradeFromProjectSettingsClosesTheSettings`: `showProjectSettings(); reviewUpgrade()` → `isShowingProjectSettings == false`, `isReviewingUpgrade == true`; `finishReviewingUpgrade()` → false.
- `revisitingAStepRemembersTheLatestStepThen` (with `shownAiStep` from `AiFollowing.kt`: the revisited step is shown until a newer step arrives).
- `theInitialAiViewComesFromTheMcpSetting`: constructed with `OWN_TOOL` / `ASSISTANT`.

`WorkspaceCenterTest` (offscreen, `uiTest` / `showing` / `saveSnapshot` from `ui/UiSnapshots.kt`, `uiTest(width = 900, height = 600)`):
- `nothingSelectedShowsTheWorkspaceHint`: `WorkspaceSelection.None`, `SceneEditingState.Closed` → the hint's text (read it from strings.xml) exists; `saveSnapshot("workspace-center-hint")`.
- `aFailedSceneShowsItsReasonAndTryAgain`: `SceneEditingState.Failed(scene, Res.string.<an existing failure string>)` → the reason text and Try again exist; clicking Try again calls the callback; `saveSnapshot("workspace-center-scene-failed")`.
- `aPreparingSceneShowsProgress` with the `scene_preparing` text.
- No test of the `key(...)` itself (4ab2bdf08's `PropertyListTest`/`ScreenTextListTest` cover the rows; the outer key has no observable case that `SentValues` doesn't also catch); check in review that `WorkspaceCenter` still wraps `BlockTypeEditor` in it.
Look at the two PNGs under `creator/build/ui-snapshots` before calling it done.
