<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Hand the workspace screen's parts only the state and callbacks they use, wire them in `ui/WorkspaceScreen.kt`, and add `PackageLayeringTest.onlyEntryFilesTakeTheControllers`

**Challenged:** sound — checked at `5156af283`: the six files and their controller uses, `AssistantPanelModel` (which
lacks `acceptsImages`, so passing it separately is right), `CommandExecutor.dispatch`'s signature against both `onCommand`
types, `ProjectSettings`' `onCommand: (Invocation<*>) -> Unit`, `GitPanel`'s parameters, `folders`/`sceneFolders` types,
`WorkspaceScreen` at 115 lines (its `fun` line to its closing brace, as `check_sizes.py` counts), `PackageLayeringTest`'s
`SourceFile`/`Import`/`APP_MODULE`, and the new UI test's constructors (`PanelWidthState`, `PanelLayout`, `PanelRow`,
`GitState.Ready`, `ChangeEntry`) and texts (from `GitPanelTest`). No other caller of the moved functions exists.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all (desktop app)
**Written against:** `9bb8df2a6`. Re-locate code by the quoted snippets, not by line numbers.
**Files** (all in the app module, `:creator`, under `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless named otherwise):
- `ui/AiPanel.kt`
- `ui/WorkspaceAiSidePanel.kt`
- `ui/WorkspaceGitSidePanel.kt`
- `ui/WorkspaceDialogs.kt`
- `ui/WorkspaceUiState.kt`
- `ui/SidebarState.kt`
- `ui/WorkspaceScreen.kt` (the entry file that takes over the wiring)
- `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/PackageLayeringTest.kt` (`:creator`)
- `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/WorkspaceGitSidePanelTest.kt` (`:creator`, new)
- `creator/CLAUDE.md`, `creator/plan/README.md`

## Problem

The fifth review's plan M01 deferred a test that lets only the screens' entry files take the app, workspace, or assistant
controller, because six `ui` parts still take one:

- `ui/AiPanel.kt`: `assistant: AssistantController`, which it only hands to `AssistantPanel(model = assistant, …)`, whose
  parameter is the `AssistantPanelModel` interface (`:creator:ai`, `ai/assistant/AssistantPanelModel.kt`) that
  `AssistantController` implements.
- `ui/WorkspaceAiSidePanel.kt`: `controller: WorkspaceController` and `assistant: AssistantController`. It reads
  `controller.gameMap.gameMap` (only while the Game map view is shown), `controller.console`, `controller.selection.selectType`
  and `openScene`, `controller::captureGameScreen`, and `assistant.acceptsImages`.
- `ui/WorkspaceGitSidePanel.kt`: `controller: WorkspaceController`, for `controller.git.state`, `controller.git::refresh`,
  and `controller.commands.dispatch`.
- `ui/WorkspaceDialogs.kt`: `controller: WorkspaceController`, for `controller.project.rootDirectory`,
  `controller.commands.history`, `controller.commands::dispatch`, and `controller.git.state` (collected only while the upgrade
  dialog shows).
- `ui/WorkspaceUiState.kt`: the class is plain, but the file ends with
  ```kotlin
  @Composable
  internal fun rememberWorkspaceUiState(controller: WorkspaceController, mcp: McpController) = remember(controller) {
      WorkspaceUiState(if (mcp.settings.value.isEnabled) AiPanelView.OWN_TOOL else AiPanelView.ASSISTANT)
  }
  ```
- `ui/SidebarState.kt`: `rememberSidebarDragState(controller: WorkspaceController)` and
  `rememberFolderTreeActions(controller: WorkspaceController)` turn sidebar gestures into commands and selections (pure
  wiring), and `rememberFolderTreeGroups(controller: WorkspaceController, types, selectedTypeName, sceneNames, openSceneName)`
  collects `controller.blockTypes?.folders` and `controller.sceneFolders` before calling the pure `folderTreeGroups(…)`.

A part handed a whole controller can reach everything in it and can only be tested with a real one. `WorkspaceScreen`, the
function, is 115 lines (the limit is 120), so the wiring cannot simply be inlined into it.

## Fix

The rule: a part takes plain values, `StateFlow`s, holders such as `WorkspaceUiState`, and callbacks; the entry file
`ui/WorkspaceScreen.kt` maps the controllers to them in small **private** functions next to `WorkspaceScreen`, the pattern its
existing `WorkspaceToolbarOf` already follows. Private keeps the wiring out of reach of every other part, which is why it
goes into `WorkspaceScreen.kt` rather than into a new entry file. `McpController` and `AiClientsController` are not among the
three controllers M01 named and stay as they are (they are only passed through); `SelectionController`, `GitController`, and
`CommandExecutor` are not passed to parts either: the parts get the callbacks.

Inside the `WorkspaceScreen` function, only the names of the called functions change, never the number of lines, so it stays
at 115 lines.

### 1. `ui/AiPanel.kt`

Change `assistant: AssistantController` to `assistant: AssistantPanelModel` and the import from
`com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController` to `…ai.assistant.AssistantPanelModel`. Nothing else
changes.

### 2. `ui/WorkspaceAiSidePanel.kt`

New parameter list (KDoc: add `@param` lines for the new ones in the file's style, for example "`@param gameMap` The Game
map, collected only while that view is shown." and "`@param captureScreen` Takes a screenshot of the running game."):

```kotlin
@Composable
internal fun WorkspaceAiSidePanel(
    uiState: WorkspaceUiState,
    mcp: McpController,
    aiClients: AiClientsController,
    assistant: AssistantPanelModel,
    acceptsImages: StateFlow<Boolean>,
    gameMap: StateFlow<GameMap?>,
    console: StateFlow<List<ConsoleEntry>>,
    aiSteps: List<AiStep>,
    openSceneName: String?,
    selectedBlockType: BlockTypeDefinition?,
    sessionState: SessionState,
    onTypeSelected: (String) -> Unit,
    onSceneSelected: (String) -> Unit,
    captureScreen: suspend () -> ByteArray?,
    width: PanelWidthState,
    row: PanelRow,
)
```

In the body, replace (keeping everything else, the `remember`/`derivedStateOf` block included, as it is):
- `controller.gameMap.gameMap.collectAsState().value` → `gameMap.collectAsState().value` (still inside
  `if (uiState.aiPanelView == AiPanelView.GAME_MAP)`, so the map is still computed only while shown);
- `controller.console.collectAsState()` → `console.collectAsState()`;
- `assistant.acceptsImages.collectAsState()` → `acceptsImages.collectAsState()`;
- in `onShowStep`, `controller.selection.selectType(target.name)` → `onTypeSelected(target.name)` and
  `controller.selection.openScene(target.name)` → `onSceneSelected(target.name)`;
- `onTypeSelected = { controller.selection.selectType(it) }` → `onTypeSelected = onTypeSelected`,
  `onSceneSelected = { controller.selection.openScene(it) }` → `onSceneSelected = onSceneSelected`,
  `captureScreen = controller::captureGameScreen` → `captureScreen = captureScreen`.

Imports: drop `AssistantController`, `WorkspaceController`, and `captureGameScreen`; add
`com.pandulapeter.kubrikoCreator.ai.assistant.AssistantPanelModel`, `com.pandulapeter.kubrikoCreator.console.ConsoleEntry`,
`com.pandulapeter.kubrikoCreator.workspace.GameMap` (a data class in `:creator:workspace`'s `workspace/GameMapState.kt`), and
`kotlinx.coroutines.flow.StateFlow`.

### 3. `ui/WorkspaceGitSidePanel.kt`

```kotlin
@Composable
internal fun WorkspaceGitSidePanel(
    gitState: StateFlow<GitState>,
    onRefresh: () -> Unit,
    onCommand: (Invocation<*>, onSuccess: () -> Unit) -> Unit,
    width: PanelWidthState,
    row: PanelRow,
) {
    PanelSplitter(width, row)
    GitPanel(
        modifier = Modifier.sidePanel(width.widthIn(row)),
        state = gitState,
        onRefresh = onRefresh,
        onStartVersionControl = { onCommand(Invocation(StartVersionControl, NoInput)) {} },
        onCommit = { message, onCommitted -> onCommand(Invocation(Commit, Commit.Input(message)), onCommitted) },
        onDiscard = { change -> onCommand(Invocation(DiscardChanges, DiscardChanges.Input(change.paths))) {} },
    )
}
```

`onCommand`'s type matches `CommandExecutor.dispatch(invocation: Invocation<*>, onSuccess: () -> Unit = {})`
(`:creator:commands`), so the entry file passes `controller.commands::dispatch`. Imports: drop `WorkspaceController`; add
`com.pandulapeter.kubrikoCreator.git.GitState` and `kotlinx.coroutines.flow.StateFlow`. Update the KDoc's first sentence only
if it names the controller (it doesn't today).

### 4. `ui/WorkspaceDialogs.kt`

```kotlin
@Composable
internal fun WorkspaceDialogs(
    uiState: WorkspaceUiState,
    projectUpgrade: TemplateUpgrade.Needed?,
    rootDirectory: File,
    history: StateFlow<CommandHistory>,
    gitState: StateFlow<GitState>,
    onCommand: (Invocation<*>) -> Unit,
)
```

In the body: `controller.project.rootDirectory` → `rootDirectory`, `controller.commands.history` → `history`,
`controller.commands::dispatch` → `onCommand`, both `controller.commands.dispatch(…)` → `onCommand(…)`, and
`controller.git.state.collectAsState().value` → `gitState.collectAsState().value` (still collected only inside
`if (projectUpgrade != null && uiState.isReviewingUpgrade)`). Imports: drop `WorkspaceController`; add
`com.pandulapeter.kubrikoCreator.commands.CommandHistory`, `kotlinx.coroutines.flow.StateFlow`, and `java.io.File`. Add `@param`
lines for the new parameters in the KDoc.

### 5. `ui/WorkspaceUiState.kt`

Delete `rememberWorkspaceUiState` (and its KDoc) from this file, and the imports only it used
(`androidx.compose.runtime.Composable`, `androidx.compose.runtime.remember`, `McpController`, `WorkspaceController`). It moves
verbatim, as a `private` function, into `ui/WorkspaceScreen.kt` (step 7).

### 6. `ui/SidebarState.kt`

- Delete `rememberSidebarDragState` and `rememberFolderTreeActions` (with their KDoc); they move verbatim, as `private`
  functions, into `ui/WorkspaceScreen.kt` (step 7).
- Make `rememberFolderTreeGroups` controller-free:

  ```kotlin
  /**
   * What the sidebar's groups with folders show ([folderTreeGroups]), computed again only when one of these changes.
   *
   * @param types The project's block-built types, or null if it hasn't opted in to them.
   */
  @Composable
  internal fun rememberFolderTreeGroups(
      types: List<BlockTypeDefinition>?,
      typeFolders: Map<BlockTypeKind, Set<String>>,
      selectedTypeName: String?,
      sceneNames: List<String>,
      sceneFolders: Set<String>,
      openSceneName: String?,
  ): Map<FolderGroup, FolderTreeGroupState> = remember(types, typeFolders, selectedTypeName, sceneNames, sceneFolders, openSceneName) {
      folderTreeGroups(types, typeFolders, selectedTypeName, sceneNames, sceneFolders, openSceneName)
  }
  ```

  The old version keyed `remember` on the `blockTypesController` too and passed `types.takeIf { blockTypesController != null }`;
  the adapter in step 7 passes that same nullable list, so the result is the same function of the same values.
- Keep `folderTreeGroups` unchanged (`ProjectSidebarTest` calls it). Drop the imports nothing uses any more (`collectAsState`,
  `getValue`, `Invocation`, `CreateFolder`, `DeleteFolder`, `MoveToFolder`, `RenameFolder`, `RenameScene`, `WorkspaceController`).

### 7. `ui/WorkspaceScreen.kt` (the entry file)

Add these `private` functions after `WorkspaceToolbarOf` (each with a one-line KDoc in the file's style, for example "Hands the
AI panel what it shows and does from [controller] and [assistant]."):

- `rememberWorkspaceUiState(controller: WorkspaceController, mcp: McpController)`, moved verbatim from `WorkspaceUiState.kt`,
  now `private`.
- `rememberSidebarDragState(controller: WorkspaceController)` and `rememberFolderTreeActions(controller: WorkspaceController)`,
  moved verbatim from `SidebarState.kt`, now `private`.
- The folder groups' adapter:

  ```kotlin
  @Composable
  private fun rememberFolderTreeGroupsOf(
      controller: WorkspaceController,
      types: List<BlockTypeDefinition>,
      selectedTypeName: String?,
      sceneNames: List<String>,
      openSceneName: String?,
  ): Map<FolderGroup, FolderTreeGroupState> {
      val blockTypesController = controller.blockTypes
      val typeFolders = blockTypesController?.folders?.collectAsState()?.value.orEmpty()
      val sceneFolders by controller.sceneFolders.collectAsState()
      return rememberFolderTreeGroups(types.takeIf { blockTypesController != null }, typeFolders, selectedTypeName, sceneNames, sceneFolders, openSceneName)
  }
  ```
- `WorkspaceDialogsOf(controller: WorkspaceController, uiState: WorkspaceUiState, projectUpgrade: TemplateUpgrade.Needed?)`
  calling `WorkspaceDialogs(uiState = uiState, projectUpgrade = projectUpgrade, rootDirectory = controller.project.rootDirectory,
  history = controller.commands.history, gitState = controller.git.state, onCommand = controller.commands::dispatch)`.
- `WorkspaceGitSidePanelOf(controller: WorkspaceController, width: PanelWidthState, row: PanelRow)` calling
  `WorkspaceGitSidePanel(gitState = controller.git.state, onRefresh = controller.git::refresh, onCommand = controller.commands::dispatch, width = width, row = row)`.
- `WorkspaceAiSidePanelOf` with exactly the parameters `WorkspaceAiSidePanel` takes today (`controller`, `uiState`, `mcp`,
  `aiClients`, `assistant: AssistantController`, `aiSteps`, `openSceneName`, `selectedBlockType: BlockTypeDefinition?`,
  `sessionState`, `width`, `row`), calling the new `WorkspaceAiSidePanel` with `assistant = assistant`,
  `acceptsImages = assistant.acceptsImages`, `gameMap = controller.gameMap.gameMap`, `console = controller.console`,
  `onTypeSelected = { controller.selection.selectType(it) }`, `onSceneSelected = { controller.selection.openScene(it) }`,
  `captureScreen = controller::captureGameScreen`, and the rest passed through.

Then, inside `WorkspaceScreen`, change only the called names:
- `WorkspaceDialogs(controller = controller, uiState = uiState, projectUpgrade = projectUpgrade)` →
  `WorkspaceDialogsOf(controller = controller, uiState = uiState, projectUpgrade = projectUpgrade)`;
- `folderGroups = rememberFolderTreeGroups(controller, blockTypes, selectedBlockTypeName, sceneNames, openSceneName),` →
  `folderGroups = rememberFolderTreeGroupsOf(controller, blockTypes, selectedBlockTypeName, sceneNames, openSceneName),`;
- `WorkspaceAiSidePanel(` → `WorkspaceAiSidePanelOf(` (same named arguments);
- `if (isGitPanelShown) WorkspaceGitSidePanel(controller, rememberPanelWidth(Panel.VERSION_CONTROL, panelLayout), row)` →
  `if (isGitPanelShown) WorkspaceGitSidePanelOf(controller, rememberPanelWidth(Panel.VERSION_CONTROL, panelLayout), row)`.
`rememberWorkspaceUiState(controller, mcp)`, `rememberSidebarDragState(controller)`, and `rememberFolderTreeActions(controller)`
keep their names and now resolve to the private functions.

Add the imports the moved and new functions need: `actors.BlockTypeDefinition`, `actors.FolderPaths`, `commands.Invocation`,
`commands.folders.CreateFolder`, `DeleteFolder`, `FolderGroup`, `MoveToFolder`, `RenameFolder`, `commands.scenes.RenameScene`,
`template.TemplateUpgrade` (already imported), `ui.components.PanelWidthState`, and `workspace.captureGameScreen`. The file grows
from 210 to roughly 340 lines, within 500.

### 8. `PackageLayeringTest` (`creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/PackageLayeringTest.kt`)

Add, next to `knownExceptions`:

```kotlin
/**
 * The files of the app's `ui` packages that may take the app, workspace, or assistant controller: the screens' entry files,
 * which hand every other part the holders, state, and callbacks it uses. It never grows to admit a part.
 */
private val entryFiles = setOf("ui/AppContent.kt", "ui/WorkspaceScreen.kt")

/**
 * The controllers only [entryFiles] import, as their imports name them (relative to `com.pandulapeter.kubrikoCreator`).
 */
private val controllers = setOf("AppController", "workspace.WorkspaceController", "ai.assistant.AssistantController")
```

and the test, after `theActorsModelDoesNotImportCodegenOrStore`:

```kotlin
/**
 * A `ui` part handed a whole controller can reach everything in it and can only be tested with a real one, so only the
 * [entryFiles] import one. Star imports need no check: ktlint's no-wildcard-imports rule refuses them.
 */
@Test
fun onlyEntryFilesTakeTheControllers() {
    val violations = mainSources
        .filter { it.module == APP_MODULE && (it.unit == "ui" || it.unit.startsWith("ui.")) && it.path !in entryFiles }
        .flatMap { source -> source.imports.filter { it.name in controllers }.map { "${source.path} -> ${it.name}" } }
    assertTrue(violations.isEmpty(), "Parts that take a controller; pass them the state and callbacks they use instead:\n${violations.joinToString("\n")}")
}
```

`SourceFile.path` is relative to `com/pandulapeter/kubrikoCreator` (so `ui/AppContent.kt` matches), `Import.name` is the import
after `com.pandulapeter.kubrikoCreator.`, and `ui.components`/`ui.theme` live in `:creator:ui-components`, which the
`APP_MODULE` filter already leaves out. Update the class KDoc's first paragraph with one sentence: "Only the screens' entry
files ([entryFiles]) take the app, workspace, or assistant controller."

### 9. Docs

- `creator/CLAUDE.md` → Modules, the sentence "`PackageLayeringTest` keeps each file in its module's layer, gives every new
  package a layer, and catches an upward import inside one module" becomes "`PackageLayeringTest` keeps each file in its
  module's layer, gives every new package a layer, catches an upward import inside one module, and lets only the screens'
  entry files (`ui/AppContent.kt` and `ui/WorkspaceScreen.kt`) take the app, workspace, or assistant controller".
- `creator/plan/README.md` → Structure: delete the bullet that starts "**Six `ui` parts still take a controller**" (three
  lines). Leave the constructors bullet; later plans edit it.

## Tests

- The new `onlyEntryFilesTakeTheControllers`. Show it bites before committing: temporarily add
  `import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController` to `ui/WorkspaceGitSidePanel.kt`, run
  `./gradlew :creator:test --tests "*PackageLayeringTest*"`, see it fail naming `ui/WorkspaceGitSidePanel.kt ->
  workspace.WorkspaceController`, and remove the import again. Before step 1–7 it fails on all six files.
- New `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/WorkspaceGitSidePanelTest.kt`, the first test the refactor
  makes possible (a part rendered from plain values). Follow `ui/git/GitPanelTest.kt`: `uiTest { showing { … } }` with
  `state = MutableStateFlow<GitState>(GitState.Ready("main", listOf(ChangeEntry("Ball", ChangeKind.MODIFIED, listOf("Ball.kt"),
  isBlockType = true)), emptyList()))`, `width = PanelWidthState(Panel.VERSION_CONTROL, PanelLayout(emptyMap(), { _, _ -> }, {}))`
  (`PanelWidths.Panel` from `:creator:ui-components`), `row = PanelRow(1200.dp, panelsInRow = 2)`, and an `onCommand` that records
  each invocation and calls its `onSuccess`. Type "Make the Ball faster" into the text field
  (`onNode(hasSetTextAction()).performTextReplacement(…)`), click "Commit", `waitForIdle()`, and assert the recorded list equals
  `listOf(Invocation(Commit, Commit.Input("Make the Ball faster")))` (`Invocation` and `Commit.Input` are data classes) and that
  the message field is empty again ("What did you change?" shows), which proves `onSuccess` was handed through. No snapshot is
  needed: the panel's look is unchanged and `GitPanelTest` covers it.
- Run the whole suite, `./gradlew :creator:allTests`, then `./gradlew spotlessCheck`. After staging,
  `python3 .github/scripts/check_sizes.py` must print nothing (it checks `WorkspaceScreen` stays at most 120 lines and the files at
  most 500); `python3 .github/scripts/check_doc_names.py --staged` must pass for the `CLAUDE.md` edit.

