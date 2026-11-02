<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Fifth review: Kubriko Creator

Reviewed at `cbf3b7140` on `main` (2026-11-02). Scope: `creator/` plus `tools/creator-api` and `tools/creator-session`,
Creator's first sweep. Angle, from the user: Creator is no longer an MVP and needs rock-solid, scalable groundwork, so
architecture, code quality, and modularization, with the findings allowed to reshape `creator/plan/`. Eight read-only
reviewers (architecture and modularization, actors and code generation, commands, AI, workspace and session
infrastructure, UI, tests, docs), every finding verified against HEAD, simple ones fixed during the review, an
end-of-review check of the landed range, and this folder for the rest. `EXECUTION.md` is the brief for landing it.

## Headlines

- **Data loss, fixed during the review:** a Windows checkout (CRLF) marked every generated file as changed by hand, so
  Creator stopped updating them; every save batch rewrote CRLF build files to LF; a type written while an outside change
  arrived lost that change; the write loop, hot reload, and the watcher died for good on one exception, hanging every
  build; a text, number, or color field couldn't be emptied, lost keystrokes, and (in the review's own first fix) could
  write one type's text into another; generated code didn't compile for an Actor type whose only Event is "added to the
  Scene".
- **Security, fixed:** session files, the init script, and agent working folders sat in Linux's shared `/tmp`; the MCP
  token file was world-readable.
- **Still to do, data loss first:** a failed type write is dropped, not retried (K01a/K01b); a command that throws half-way
  leaves a change nobody can undo (K02a); undo can overwrite an outside change (K03); an unreadable settings file makes the
  next save wipe the saved conversations or recent projects (K11a); renaming a parent's Property leaves its children
  dangling (K04).
- **Groundwork for plans 05–07:** a Block is one descriptor and its emission (G02, G03), one Actor class emitter with
  per-Event functions (G04, G05), and the staged split of `:creator` into modules the compiler keeps layered (M01–M14).

## Fixed during the review

99 commits, `050b129a4..87ddc45d5`, one finding each, not pushed. `:creator:test` passed after each (1,166 tests at the end, 8
opt-in suites skipped). The last ones are follow-ups from the end-of-review check of the landed range and from the challenge:
they fix what earlier fixes in this review got wrong (the focused-field regression of the text-field fix, the existing
`mcp.json`'s permissions, folders named `out`, Stop during context gathering) and a flaky test.

- `050b129a4` Move Description and SummarizedSchema into a schema package of their own.
- `994cfed91` Move EditorMutation into a foundation package that every layer can depend on.
- `6474c249d` Move ConsoleEntry into a console package of its own.
- `02d4e1dcc` Move LatestRequests and its test into the foundation package.
- `3875dec84` Move settingsDirectory, CreatorConfiguration, and unexpectedProblemHandler out of Kubriko Creator's root package into foundation.
- `79952d5ac` Move writeTextAtomically and its test from the project package into foundation.
- `465a564bd` Move LocalIsAiEnabled into ui/components, the only thing there that reached back into ui.
- `e1b8d5ee9` Move NumberField, ColorField, MenuIconButton, NewNameField, and ProgressScreen into ui/components, and build the toolbar's settings menu from MenuIconButton.
- `01ab9c574` Document that Kubriko Creator's command executor and workspace controllers are confined to the main thread.
- `e3961c1a7` Keep Kubriko Creator's type writing, hot reload, and project watching going after an unexpected failure, reporting it instead.
- `027f339ec` Read type definitions changed on disk while Kubriko Creator's own writes are pending once those are written, instead of losing the change.
- `b526366fb` Collect a project's class files into one map and let Kubriko Creator's class loader drop each one's bytes once it is defined.
- `59675c6dc` Keep Kubriko Creator's session files and agent working folders in its own settings folder instead of the shared temporary one.
- `bd2cd0fbc` Ignore line breaks when Kubriko Creator checks whether generated code was changed by hand, so a Windows checkout is neither reported nor rewritten.
- `4c4d79f51` Keep Windows line breaks in the build files, Gradle properties, and template upgrades Kubriko Creator edits, instead of converting every line.
- `6fdaa00f6` Share one list of skipped folders between Kubriko Creator's project walks and refuse Scene folders named like one, which would never show up.
- `cb8ff07c2` Let closing a workspace cancel Kubriko Creator's Gradle lookup of the desktop module instead of waiting for the build to configure.
- `2e79bf884` Answer a play session command that fails unexpectedly with its reason, and let a game start when Kubriko Creator isn't listening for its session.
- `728c3a06b` Say why a project failed to open unexpectedly on Kubriko Creator's welcome screen instead of calling every failure a folder that isn't a Gradle project.
- `f9d73aa78` Describe in CreatorEntryPoint's KDoc that Kubriko Creator draws Scenes with the project's classes loaded into itself and plays the game in a process of its own.
- `316b5c5e4` Keep a Scene view's Actor type looks until a load that changes them finishes, so a cancelled load can't leave Actors drawn from the old look.
- `9af5671ab` Write Kubriko Creator's MCP settings readable by their owner only, through a private option of writeTextAtomically that also replaces McpConfigFile's own writer.
- `26ac782d8` Compute the Actor types with children once per family instead of once per generated type and Block scope in Kubriko Creator.
- `1f2b9b40b` Keep Kotlin's hard keywords in one set that Kubriko Creator's Property, Timer, and package name rules share.
- `b3a63e82f` Describe the Kotlin Block's scope correctly for Manager types, which have no velocity, body, or color.
- `88d658c9e` Name the folder of a type's definition in the header of the code Kubriko Creator generates from it.
- `8bb3b70c2` Fill in a new project's template version from ProjectUpgrader instead of repeating it in Kubriko Creator's starter template.
- `63fedaa10` Share the velocity field, body center, Timer fields and countdown, and number text function between Kubriko Creator's code generators through ActorClassCode.
- `acfe2b306` Move the extensions on BlockDefinition that find and rename what a Block refers to into BlockReferences.kt.
- `e25918adc` Check without the integration flag that Kubriko Creator's integration project uses every Block, condition, and pause action.
- `2d20e92d8` Finish an AI tool call's step in Kubriko Creator even when the command throws or is cancelled.
- `fd99c08f8` Reject check_change in Kubriko Creator while the project is read-only, by moving the read-only check into CommandContext.
- `09d9ef305` Mark the game notes, game identity, and template upgrade writers of Kubriko Creator as EditorMutation, so only commands call them.
- `19b8acf1e` Validate the contents of a new Actor or Manager type in Kubriko Creator against the project, with its parent's Properties, Scenes, sounds, and sprites.
- `b8aaa38b1` Share the descriptions many of Kubriko Creator's command inputs repeat as constants in Descriptions.
- `f39537f76` Keep an outside AI client's changes out of the user's undo step in Kubriko Creator.
- `7c254dfc4` Count a redeclared Property as read wherever its family reads it in Kubriko Creator's health checks, and build their lookups once.
- `1c885eef6` Read a registry agent's remembered variables from the secret store off the main thread in Kubriko Creator.
- `cfa3d3a73` Escape every kind of single quote in the secret names Kubriko Creator puts into its Windows Credential Locker scripts.
- `bd0cef4c6` Stop Kubriko Creator's AI tool connections from waiting forever for a server that stays off.
- `730d0ceda` Gather a message's context, such as the game's screenshot, once Kubriko Creator's assistant has started the turn, so a message sent during the capture isn't lost.
- `c17a7bf62` Leave out only the agent registry entries Kubriko Creator can't read instead of the whole registry.
- `e1a413857` Discard tar's output while Kubriko Creator unpacks an agent, so its timeout applies to a tar that hangs.
- `6d97bad89` Stop the sign-in a second sign-in click replaces in Kubriko Creator's assistant instead of orphaning its process.
- `1ad0aa1a2` Keep what is typed into Kubriko Creator's number, color, and text fields until they lose focus, so a late model no longer reverts or loses keystrokes.
- `27460a85c` Build the Actor type family once per change in Kubriko Creator's type editor and look its descendants up in a set when listing parent choices.
- `73aee75a0` Derive the assistant's attention chips in Kubriko Creator's workspace so a console line no longer recomposes the whole workspace screen.
- `9001d4fc8` Show why Kubriko Creator's project settings couldn't be read instead of closing the window when reading them fails.
- `942674165` Decide which Blocks show inline parameters in Kubriko Creator with an exhaustive when, so a new Block kind can't silently get an empty row.
- `62239dec2` Name Kubriko Creator's side panel widths in PanelWidths and draw the panels through one sidePanel modifier.
- `49548ab4e` Draw Kubriko Creator's three drag previews with one shared DragPreviewChip.
- `b5e5a6118` Key Kubriko Creator's sidebar rows by folder path and item name, so toggling a folder keeps its row, and look each item's folder up in a map.
- `7d021a173` Indent the misindented blocks of Kubriko Creator's type editor and Scene editor pane.
- `84ae3d4ae` Keep the commit message in Kubriko Creator's version control panel when the commit fails, clearing it only once the commit is made.
- `756f8465e` Let a click on a switch's or checkbox's label toggle it in Kubriko Creator, and mark the selected palette tab and sidebar row as selected for accessibility.
- `c50ab9dec` Draw Kubriko Creator's Play button text in black on the dark theme's green, which white was too faint on.
- `d01d24b7b` Build Kubriko Creator's four dialogs on one CreatorDialog and both settings dialogs' section headers on one SettingsSection.
- `fe4a96564` Draw Kubriko Creator's flash highlight with a modifier that reads its animation while drawing, so a flashing row no longer recomposes every frame.
- `b7fe8e8ba` Pin the creator-session protocol's wire format with literal lines of every version, so a renamed message or field fails a test.
- `4832b5e9e` Skip Kubriko Creator's platform, permission, and opt-in tests through Assume instead of returning early, so they report as skipped rather than passed.
- `8a91e4d23` Delete the temporary directories Kubriko Creator's command tests create when the test JVM exits.
- `9377ca449` Check that cancelling a game in Kubriko Creator's GameProcessTest leaves its process dead, waiting for the game to report its PID instead of sleeping.
- `1565778ab` Fail Kubriko Creator's source-scanning tests when the folder they scan is missing, instead of passing on nothing.
- `d99c0cc34` Let a click on the AI features label in Kubriko Creator's settings toggle its switch, as the other labeled switches do.
- `6716d5f88` Remove the unused imports left across Kubriko Creator's sources and tests.
- `6915a3742` Keep Kubriko Creator's cached agent registry when a fetched one lists agents none of which can be read.
- `cf478b2dc` Say in Kubriko Creator's CLAUDE.md that Creator runs the session task only in play mode.
- `49e94f8af` Drop the history sentence about Edit mode from Kubriko Creator's CLAUDE.md.
- `000603c74` Leave the tool list's token count to TokenBudgetTest instead of quoting it in Kubriko Creator's docs.
- `a1827599e` Trim Kubriko Creator's roadmap entries for the AI assistant and settings to what is left to build.
- `00f79459e` Remove the history from Kubriko Creator's AI assistant plan.
- `242df1e93` Say in Kubriko Creator's AI assistant plan that other agents' change requests are always rejected, and number its items in order.
- `970398568` Drop environment variable sign-in from the FakeAgent variants Kubriko Creator's AI assistant plan still lists.
- `034b801ae` Name the family's base class instead of an interface in Kubriko Creator's reuse plan.
- `268a2cd00` Drop the dates and stale versions from Kubriko Creator's handover and point its checks at a scratch Breakout copy.
- `9465bb8d8` Describe Kubriko Creator's AI assistant as built in the root CLAUDE.md.
- `60eabdfbb` Note in the root CLAUDE.md that Kubriko Creator applies only the style convention plugin.
- `2b63b9097` Check that Kubriko Creator's Kubriko overview finds its sections in the root CLAUDE.md, and ask there to keep their headings.
- `9c03f3755` Name published modules by their convention plugin in the code-style skill and keep its CLAUDE.md sync rule to what the code can't show.
- `9071ff3bf` Add Kubriko Creator's review areas, tests, and plans folder to the codebase-review skill.
- `ecf76faf6` Find the root CLAUDE.md's headings for Kubriko Creator's Kubriko overview only at the start of a line.
- `ae027ca3e` Cut Kubriko Creator's CLAUDE.md to its rules and non-obvious behaviour, and move the principles, file ownership, standing decisions, and how to check work into it.
- `b7d0dad55` Move Kubriko Creator's code generation and assistant rules into guides of their own in the actors and ai/assistant packages.
- `de041fded` Keep Kubriko Creator's plans to future work, with code names in place of CLAUDE.md anchors and no grouping of the single and batch Scene tools.
- `474baad22` Import Kubriko in Kubriko Creator's generated Actor types whose onAdded only runs added-to-the-Scene Blocks without Manager lookups.
- `26d638baf` Import Kubriko in Kubriko Creator's generated Actor types exactly when they override onAdded.
- `0a41d2ad8` Close the open Scene when Kubriko Creator creates and selects a new Actor or Manager type.
- `4ab2bdf08` Keep Kubriko Creator's buffered fields from carrying one type's or row's text into another, and show undone values even while typing.
- `b363f634a` Let a long label in Kubriko Creator's labeled switches wrap instead of squeezing the switch.
- `91040c959` Let Stop in Kubriko Creator's assistant end a turn whose context is still being gathered, before anything is sent.
- `61aec7629` Correct Kubriko Creator's notes on older sessions and project class loading, and keep each rule they shared with the root CLAUDE.md in one place.
- `b557a6dff` Make Kubriko Creator's API overview test fail when the overview runs past the Performance section of the root CLAUDE.md.
- `92b8a47ea` Make Kubriko Creator's own mcp.json readable by its owner only even when an older version left it readable by others.
- `54aa67a82` Read a change made outside Kubriko Creator during its own writes even when reporting those writes throws.
- `91cd93f5f` Narrow Kubriko Creator's onboarding plan for opening failures to the unreadable project file still missing a message of its own.
- `48b22953b` Skip IDE and native build folders such as out only outside a source folder in Kubriko Creator's project walks, so a package or Scene folder called out is watched and shown again.
- `63403e6e0` Check that Kubriko Creator's project class loader drops a class's bytes once it defines the class.
- `4296e10e3` Give the workspaces of Kubriko Creator's app controller tests the test dispatcher, so closing one no longer races a real background thread.
- `87ddc45d5` Mark the project's changes as seen by Kubriko Creator's assistant only once a prompt's context is gathered, so a turn stopped before then sums them up next time.

## Index

| Plan | Fix | Severity | Lane (phase) | Challenge |
|---|---|---|---|---|
| [A01](A01-run-assistant-restarts-in-order.md) | Run the assistant's restarts, project changes, and turning off as one ordered sequence that replaces only the agent it means to, and let a tool server stopped while starting stay stopped | high | A (1) | amended |
| [A02a](A02a-split-assistant-controller.md) | Split AssistantController into AgentSelection, AssistantHost, and AssistantSignIn behind the same façade, with one AI-off gate and settings saved off the main thread | medium | A (1) | amended |
| [A02b](A02b-hoist-assistant-panel-state.md) | Give AssistantPanel a model interface instead of the controller, and take Claude-only features from the agent's profile instead of `isClaude` in the UI | low | A (1) | sound |
| [A03](A03-tell-agents-how-to-recover-read-deltas.md) | Tell the agent to pass `full: true` whenever it lacks an earlier read's result, and forget a conversation's reads when a turn is cancelled | medium | A (1) | amended |
| [A04](A04-check-tool-kind-before-auto-allowing-and-pin-mcp-remote.md) | Auto-allow a Creator tool call only when its kind doesn't change files or run commands, and pin mcp-remote to one exact version | low | A (1) | sound |
| [D01](D01-update-creator-plans-for-this-sweeps-architecture.md) | Bring Kubriko Creator's plan files in line with what this sweep built: remove the costs it fixed, record the ones it left, and tie Functions, Traits, new Blocks, and sprite writes to the new code generation and write pipeline | low | D (4) | amended |
| [G01](G01-pin-the-persisted-format-and-raise-it-for-every-addition.md) | Raise the format version for every new persisted field, Block, Event, or enum value, and pin the persisted shape so a change without a raise fails a test | medium | G (1) | amended |
| [G02](G02-describe-each-block-kind-once-with-exhaustive-whens.md) | Make BlockKind the descriptor of every per-kind Block fact, replace every `else` over Blocks with an exhaustive `when`, and pin the generated code of the every-Block project | medium | G (1) | amended |
| [G03](G03-derive-generated-declarations-from-structured-block-code.md) | Have each Block's emission return a structured BlockCode (lines, needs, imports, Managers) and derive every feature and import from it instead of searching the generated text | medium | G (1) | amended |
| [G04](G04-generate-standalone-and-family-actor-classes-through-one-emitter.md) | Generate standalone Actor types and the base class of a family through one Actor class emitter, with imports per file | medium | G (1) | amended |
| [G05](G05-generate-each-event-kind-as-a-function-of-its-own.md) | Generate each kind of Event of standalone Actor types and Manager types as a private function that update, onAdded, onKeyPressed, and onCollisionDetected only call | low | G (1) | amended |
| [G06](G06-leave-out-type-files-with-unusable-property-names.md) | Leave a type file out as unreadable when one of its own Properties has a name the generated code can't use | medium | G (1) | amended |
| [G07](G07-compare-events-by-their-trigger.md) | Compare and group Events by one EventTrigger that ignores the fields their type doesn't use | low | G (1) | sound |
| [G08](G08-describe-template-upgrades-as-per-version-steps.md) | Describe each template upgrade as a list of per-version TemplateSteps that check() folds over | low | G (1) | amended |
| [G09](G09-let-kotlin-blocks-import.md) | Let a Kotlin Block start with import lines that become imports of the file it is generated into | low | G (1) | amended |
| [H01](H01-one-workspace-selection-for-types-and-scenes.md) | Give the workspace one selection (a type, a Scene, or nothing) that every caller goes through, instead of four places pairing `select` with `open`/`close` by hand | medium | H (3) | amended |
| [H02a](H02a-compute-the-game-map-once-off-the-main-thread.md) | Compute the AI panel's Game map (digest and health findings) once per burst of changes, off the main thread, in a workspace-layer `GameMapState` | medium | H (3) | amended |
| [H02b](H02b-move-workspace-screen-state-into-a-tested-holder-and-split-the-screen.md) | Move `WorkspaceScreen`'s seven pieces of screen state and their transitions into a tested `WorkspaceUiState`, and split the 270-line composable into its parts | medium | H (3) | amended |
| [H03a](H03a-build-the-app-graph-before-compose-and-quit-off-the-ui-thread.md) | Build the app's controllers in an `AppGraph` before `application {}`, and quit through one `shutdown()` that runs off the UI thread under one timeout | medium | H (3) | amended |
| [H03b](H03b-close-a-workspace-only-once-its-game-is-gone.md) | Close a workspace only once its game process (and that process's children) is gone, kill leftovers at JVM exit, and open a project only after the previous one closed | medium | H (3) | amended |
| [H04](H04-report-window-exceptions-and-keep-the-window-open.md) | Install a window exception handler that reports an unexpected UI exception in the console and keeps the window open, rebuilding its content | medium | H (3) | amended |
| [H05](H05-searchable-choice-button-for-long-lists.md) | Add a `SearchableChoiceButton` (lazy list, filter field once there are more than eight options, Enter picks the first match) and use it for every project-content chooser | medium | H (3) | amended |
| [H06a](H06a-filter-the-sidebar.md) | Add a filter field at the top of the sidebar that narrows types, Scenes, and files to those whose names contain what is typed, opening the folders that hold them | low | H (3) | amended |
| [H06b](H06b-resizable-side-panels-with-remembered-widths.md) | Let the user drag the edges of the five side panels to resize them, within limits, remembering the widths in Creator's settings | low | H (3) | amended |
| [H07a](H07a-move-block-type-editor-commands-into-block-type-editor-actions.md) | Move the type editor's 25 inline `Invocation`s into a tested `BlockTypeEditorActions`, and the screen text default into `ScreenTextDefinition.defaultFor` | medium | H (3) | amended |
| [H07b](H07b-track-the-selected-event-by-its-list-key.md) | Track the type editor's selected Event by its `ListItemKeys` key instead of its index, so undo, redo, rejected commands, and the AI's Event changes keep the same Event selected | low | H (3) | amended |
| [K01a](K01a-keep-failed-type-writes-queued-retry-and-name-them.md) | Keep a type write that fails queued with everything after it, retry it with backoff, report what failed by name, and reload from disk only when no type change was queued during the read. | high | K (2) | amended |
| [K01b](K01b-refuse-commit-and-discard-while-types-are-unsaved-and-warn-builds.md) | Make awaitWrites say whether everything is on disk, refuse Commit, Discard, and Start version control while it isn't, and let builds, Play, and Scenes go ahead with a console warning naming the unsaved types. | high | K (2) | amended |
| [K01c](K01c-index-definition-files-by-name-instead-of-walking-per-save.md) | Find a type's definition files through an index that load builds and save and delete keep, instead of walking the definitions folder on every write. | low | K (2) | amended |
| [K02a](K02a-reject-throwing-commands-and-write-scenes-all-or-nothing.md) | Turn a command that throws into a rejection, write the Scenes a command changes all or nothing, and put the in-memory type change back when they can't be written. | high | K (2) | amended |
| [K02b](K02b-add-internal-composite-command-and-restore-dropped-property-values.md) | Add an internal CompositeCommand that applies parts in order and rolls them back on failure, and make undoing add_property bring back the Scene values it dropped. | medium | K (2) | amended |
| [K03](K03-drop-undo-steps-whose-type-or-scene-changed-outside.md) | Remember with each undo step a hash of the types and Scenes it touches, and drop the step instead of applying it when one of them changed outside Kubriko Creator since. | high | K (2) | amended |
| [K04](K04-make-property-commands-follow-the-actor-type-family.md) | Rename an inherited Property only on the ancestor that declares it, through every descendant, and check add_property and set_parent against descendants too. | medium | K (2) | amended |
| [K05](K05-run-read-tools-under-the-change-lock.md) | Run read tools under the executor's change lock, so an AI never reads a project half-way through another change. | medium | K (2) | amended |
| [K06](K06-validate-without-file-reads-on-the-main-thread.md) | Validate commands on the main thread from an in-memory Scene name index and warm Scene cache, and move the project-file reads of upgrade_project and set_project_settings into apply. | medium | K (2) | amended |
| [K07](K07-check-for-changes-to-commit-after-the-writes.md) | Decide whether there is anything to commit from the repository's status after the writes, inside the git operation, instead of from the panel's state when the commit is asked for. | medium | K (2) | sound |
| [K08](K08-make-single-actor-scene-tools-delegate-to-their-batch-forms.md) | Keep the single-Actor Scene tools, but make each one check and apply through the same functions as its batch form. | low | K (2) | sound |
| [K09a](K09a-route-every-project-change-through-one-place.md) | Route Creator's own writes and the watcher's changes through one ProjectChanges router, fed by typed changes SceneStore records itself, so a Scene edit no longer rescans the project and git refreshes after Creator's own writes. | medium | K (2) | sound |
| [K09b](K09b-build-the-workspace-in-dependency-order-and-close-it-if-opening-fails.md) | Build the workspace's parts in dependency order without forward references, let commands see the block-built types through an interface, and run the session server and watcher in the workspace scope, closing everything when opening fails. | medium | K (2) | sound |
| [K10](K10-walk-the-project-tree-one-way-and-reuse-the-watchers-snapshot.md) | Walk a project's files through one ProjectTree.walk that enters source folders whatever their names, and build the resource tree after a watcher change from the snapshot the watcher just took. | medium | K (2) | amended |
| [K11a](K11a-share-one-json-file-store-that-never-overwrites-an-unreadable-file.md) | Share one JsonFileStore between Kubriko Creator's settings stores that sets an unreadable file aside as .bak before writing, instead of six copies that replace it. | medium | K (2) | amended |
| [K11b](K11b-share-one-http-download-helper-and-client.md) | Download through one shared HTTP helper and client instead of four copies that build a new client per request. | low | K (2) | sound |
| [K12](K12-retry-atomic-moves-windows-refuses-for-a-moment.md) | Retry the move of writeTextAtomically about five times with a short backoff when Windows refuses it for a moment. | medium | K (2) | amended |
| [M01](M01-test-the-package-layering-of-creator.md) | Add a test that holds Kubriko Creator's packages to the layers of the planned Gradle modules, with a shrinking list of the known exceptions | medium | M (4) | amended |
| [M02a](M02a-move-ui-labels-out-of-the-model-and-commands.md) | Move the labels the UI shows for model enums and commands into the UI, and tell the AI about changes by their tool names instead of UI strings | medium | M (4) | amended |
| [M02b](M02b-type-domain-errors-and-console-texts.md) | Give every error and console text of the model, commands, and workspace a typed reason with fixed English for the AI, and word it for the user only in the UI | medium | M (4) | amended |
| [M03](M03-extract-the-creator-foundation-module.md) | Extract Kubriko Creator's `foundation` and `console` packages into `:creator:foundation`, with the `kubriko-creator-module` convention plugin, an aggregate `:creator:allTests` task that CI runs, and a test-fixtures home for shared test helpers | medium | M (4) | amended |
| [M04](M04-extract-the-creator-ui-components-module.md) | Extract Kubriko Creator's shared UI components and theme into `:creator:ui-components`, with its own resources and the offscreen UI test helpers as its test fixtures | medium | M (4) | amended |
| [M05](M05-split-the-actors-package-into-model-codegen-and-store.md) | Split the `actors` package into the Block model (`actors`), code generation (`actors.codegen`), and storage (`actors.store`), with the generated file names in the model, and hold the three to that order in the layering test | low | M (4) | sound |
| [M06](M06-move-the-types-that-cross-the-core-boundary.md) | Move the types that cross the planned `core` boundary: the Scene editor's runtime into `sceneRuntime`, the classpath into `session`, the desktop-module lookup into `gradle`, and the type store's controller into `actors.store` | medium | M (4) | amended |
| [M07](M07-extract-the-creator-core-module.md) | Extract Kubriko Creator's Block model, code generation, type store, project files, schema annotations, and Scene files into `:creator:core` | medium | M (4) | amended |
| [M08](M08-extract-the-creator-gradle-session-module.md) | Extract Kubriko Creator's Gradle connection and game sessions (`gradle`, `session`) into `:creator:gradle-session` | medium | M (4) | sound |
| [M09](M09-extract-the-creator-git-module.md) | Extract Kubriko Creator's version control (`git`) into `:creator:git`, with the fake repository as its test fixture | low | M (4) | sound |
| [M10](M10-extract-the-creator-template-module.md) | Extract Kubriko Creator's project templates and upgrades (`template`, with their resources) into `:creator:template`, and point the integration test's commands at it | low | M (4) | amended |
| [M11](M11-extract-the-creator-commands-module.md) | Extract Kubriko Creator's commands and AI context (`commands`, `context`, with the Kubriko overview resource) into `:creator:commands`, with the command test helpers as its test fixtures | medium | M (4) | amended |
| [M12](M12-extract-the-creator-ai-module.md) | Extract Kubriko Creator's MCP server and in-app assistant (`mcp`, `ai`) into `:creator:ai` | low | M (4) | amended |
| [M13](M13-extract-the-creator-scene-runtime-module.md) | Extract the Scene editor's runtime (`sceneRuntime`: project class loading, the live preview, sprites) into `:creator:scene-runtime`, with the engine and plugin dependencies | medium | M (4) | amended |
| [M14](M14-extract-the-creator-workspace-module.md) | Extract Kubriko Creator's open-project wiring (`workspace`) into `:creator:workspace`, leave only the app's UI and settings in `:creator`, and finish the docs of the split | medium | M (4) | sound |
| [P01](P01-measure-member-functions-in-the-size-check.md) | Make the size check measure every function, members of classes and objects included, under a name qualified by its class, and treat test fixtures as tests | medium | T (1) | amended |
| [P02](P02-review-each-plan-file-before-the-next.md) | Review the commits of every finished plan item before the next one starts, and fix what the review finds | medium | T (1) | sound |
| [P03](P03-refuse-growth-past-the-size-limits-and-limit-constructor-parameters.md) | Refuse a change that grows a file, function, or constructor already past its size limit, and limit primary constructors to 12 parameters | medium | D (4, last) | added after the challenge |
| [S01](S01-refuse-scenes-of-projects-on-newer-compose-or-kotlin.md) | Refuse to draw the Scenes of a project whose Compose or Kotlin is a newer major or minor version than Creator's, and warn about any other newer shared library | medium | S (1) | sound |
| [S02](S02-prove-released-project-classes-can-be-collected.md) | Prove with a test that a project's released class loader, and every class it defined, can be garbage collected | medium | S (1) | sound |
| [S03](S03-one-project-output-rule-under-the-project-root.md) | Decide what the project's own build output is in one place: a classpath entry canonically under the project's root | medium | S (1) | sound |
| [S04](S04-accept-one-session-connection-per-launch-after-its-hello.md) | Let the session server take messages and send commands only on a connection that said Hello, and only on one per launched game | low | S (1) | amended |
| [S05](S05-remove-edit-mode-from-creator-session.md) | Remove Edit mode and its `tool-scene-editor` dependency from `tool-creator-session`, keeping `SessionMode.EDIT` in the protocol | low | S (1) | amended |
| [T01](T01-shared-testing-package-and-fake-contract-tests.md) | Start a shared `testing` package for Kubriko Creator's tests with `testDirectory()` and `FakeGitRepository`, and run one contract test against each fake and its real implementation so the fakes behave like what they replace | medium | T (1) | amended |
| [T02a](T02a-assistant-tests-wait-for-states-not-sleeps.md) | Make the assistant tests wait for an idle state instead of sleeping before negative assertions, share one blocking-test helper, and give FakeAgent's delayed notification a structured scope | medium | A (1) | amended |
| [T02b](T02b-mcp-tests-without-port-races-or-sleeps.md) | Give the MCP tests ports outside the system's ephemeral range instead of a just-released free port, replace their fixed sleeps with waits for a state, and move FakeProcessRunner into the shared testing package | medium | A (1) | amended |
| [T03](T03-direct-tests-for-workspace-collaborators.md) | Test SessionController, CreatorTaskRunner, WorkspaceRunner, GameBuilds, PendingWrite, and GradleWorkspaceOpener directly, moving their cases out of WorkspaceControllerTest, and move the workspace fakes into the shared testing package | medium | serial (1→2) | amended |
| [T04](T04-compile-generated-projects-nightly-in-ci.md) | Compile every generated project template, with every Block, against the just-published libraries in a nightly CI step | high | T (1) | sound |
| [T05](T05-golden-fixtures-for-the-kubriko-file-format.md) | Pin the `.kubriko` file format with golden fixture projects, one frozen folder per format version, that decode to known values and that the current format re-encodes byte for byte | high | serial (1→2) | amended |
| [T06](T06-validate-creator-api-binary-compatibility.md) | Check tool-creator-api's binary API against a committed ABI dump in every Tests run, and make the docs say creator-api keeps its binary API | low | T (1) | amended |

## Lanes and merge order

| Lane | Area | Plans in order | Owns |
|---|---|---|---|
| T | test infrastructure, CI, ABI, process | T01, T04, T06, P01, P02 | `creator/src/test/.../testing/`, contract tests, `.github/workflows/tests.yml`, `tools/creator-api/api/`, `.github/scripts/check_sizes.py` |
| S | Scene class loading, session, creator-session | S03, S02, S01, S04, S05 | `scenes/` loaders, `session/`, `tools/creator-session/`, the init script |
| G | model and code generation, templates | G01, G02, G03, G04, G05, G07, G09, G06, G08 | `actors/`, `template/`, generator tests, `src/test/resources/generated/` |
| A | assistant, MCP | A01, A02a, A02b, A03, A04, then T02a, T02b | `ai/assistant/`, `mcp/`, `ui/assistant/`, `ui/mcp/`, their tests and fakes |
| serial | between phases 1 and 2, on main | T03 (after S), T05 (after G) | `WorkspaceControllerTest` split, workspace fakes; `src/test/resources/formats/` |
| K | workspace, commands, persistence | K12, K01a, K01b, K01c, K02a, K02b, K03, K04, K05, K06, K07, K08, K11a, K11b, K09a, K09b, K10 | `workspace/`, `commands/`, `context/`, `scenes/SceneStore`, `project/` watcher and tree, `git/`, the JSON stores |
| H | app shell and UI | H01, H02a, H02b, H03a, H03b, H04, H05, H06a, H06b, H07a, H07b | `AppController`, `KubrikoCreatorApp`, `ui/` (except assistant and mcp), `window/` |
| M | modules | M01, M02a, M02b, M03–M14 | `settings.gradle.kts`, every `creator/**/build.gradle.kts`, `gradle/build-logic` |
| D | plan folder, then the size check | D01, P03 | `creator/plan/`; then P03's `check_sizes.py`, its test, and the doc phrases it names |

Phase 1 runs T, S, G, and A in parallel and merges them in that order (T first: the others' tests use its `testing`
package; A last: it shares the most test files). T03 and T05 then land on main one after the other. K, H, and M each run
alone, in that order, because each rewrites what the next builds on: K splits `WorkspaceController` (K09) that H's state
holders move into, and M moves every package. D01 goes last, since it removes from `creator/plan/` what the others made done. P03 follows it: the stricter size check would stop plans written under today's rule.

**Shared files:** `creator/CLAUDE.md`, the package guides (`actors/CLAUDE.md`, `ai/assistant/CLAUDE.md`), and
`creator/src/main/composeResources/values/strings.xml` are touched by most lanes; each plan edits only the sentence or key it
names, re-located by topic, and a cherry-pick conflict there is merged word by word keeping both sides. `tests.yml` belongs
to lane T in phase 1 (M03 and M10 edit it later). Plans name their files; a lane commit touching a file outside its plan's
**Files** stops the merge.

## Decisions

Taken by the user on 2026-11-02, every one the recommended option; plans implement them and nothing is re-asked.

- Modularization: full staged split (~10 modules, app stays :creator), layering test first, typed domain errors (ARCH-7); runs last.
- creator/CLAUDE.md: rules + short non-obvious behaviour per feature, Creator budget ~4,000 words, small actors/ and ai/assistant/ guides; plan/README reworded so finished work adds only rules.
- creator-session Edit mode: remove EditSession + scene-editor dependency in the next library release, keep SessionMode.EDIT in the protocol.
- Type file format: raise formatVersion for any new persisted field; golden JSON fixtures pin field names.
- Family Property rename: only on the declaring ancestor, carried through all descendants (Blocks, placeholders, Kotlin, overrides, Scene values); rejected on a child with a pointer to the ancestor.
- Failed type saves: Build/Play proceed with a console warning naming unsaved types; Commit/Discard refused until on disk.
- UI exceptions: window exception handler logs to the console and keeps the window open, plus local catches for I/O.
- Session channel: Creator-only fix now (accept a connection only after its Hello, one per launch); token with the next protocol bump.
- Classloader gate: refuse a project whose Compose/Kotlin is a newer major/minor than Creator's; warn otherwise.
- Project output = under the project root.
- CompositeCommand now, internal only.
- Keep single and batch Scene tools; singles delegate internally.
- Keep the actorType wire name (package rename optional).
- AI read memory: reword Unchanged to tell the agent to pass full when it lacks the content; forget reads on cancel.
- AI permissions: auto-allow Creator tools only for non-changing kinds; pin mcp-remote exactly (check how registry agents label MCP calls first).
- ABI validation for creator-api; session protocol guarded by the literal test (TST-2).
- One type for disk format, model, and AI schema, guarded by fixtures.
- No .gitattributes in projects.
- Imports in Kotlin Blocks: yes.
- Nightly CI job compiling generated code: yes.
- Windows move retry: about 5 tries with backoff.
- Searchable menus: build.

Taken during the challenge, also the recommended option:

- G05 (Events as functions): Creator reports, as health checks and once in the console at opening, every Kotlin Block that
  uses `return` or one of the locals that leave scope, so nothing breaks silently.
- G09 (imports in Kotlin Blocks): raises the format version with a migration that changes no file, and the format policy
  covers a new meaning for an existing field too.

One more for execution time: **P01 edits `.claude/settings.json`** (a phrase in the size-check hook's text), which configures
Claude Code itself; the executing session asks before that commit.

## Checked and found solid

- The command layer's contract: one registry generates the tool list with derived schemas; tests enforce registration,
  described schemas, and that only commands opt in to `@EditorMutation`; every sampled UI call site dispatches a command.
- Inverses record deltas, not snapshots; history, change log, and AI steps are capped; one mutex serializes user and AI
  writes; `assertRoundTrip` covers every undoable command.
- MCP server: localhost only, constant-time token checks, per-conversation tokens, DNS rebinding protection on.
- Gradle Tooling API runs: cancellable, wait for Gradle to stop; classpath builds never overlap; `@argfile` for Windows.
- Session protocol negotiation by the lower version; game log forwarding never blocks the game.
- Generated hot paths allocate nothing; output is deterministic; string escaping and float literals are safe.
- Secrets only on stdin, API key only in the agent's environment; the binary installer guards zip-slip and checks hashes.
- Ports and adapters (`ProjectRunner`, `BlockTypeRepository`, `GitRepository`, `SecretStore`, `AgentLauncher`) keep
  infrastructure behind fakes; no mutable global singletons.

## Dropped after verification

- Saving settings on the main thread (AI-5, second half): the writes are trivially small.
- A leak of Managers when `SceneView`'s constructor throws (INF-5, second half): what was created is collected with the view.
- `HashMap` as a reserved type name (ACT-10): `GeneratedAudio` runs it once at start, and the clash can't happen.
- The Scene names `remember` key (EXTRA-1): it is already keyed on the resources the names come from.
- The `folder` part of creating a type (CMD-14): `folder` is transient and never comes from input.
- Splitting `InlineParameters` by category (UI-10) and `strings.xml` by area (UI-24): both are under their limits.
- The diff colors and the list joiner (UI-22), and keys on the git lists (UI-20): no real problem.
- A shared drag-state base class (UI-13, second half): low value. One cancellation idiom (ARCH-11): style only.
- `.gitattributes` in generated projects (CRLF): the user decided against it; line breaks are now preserved instead.
- Direct tests of `DesktopModuleLookup` (TST-10): they exist.

## Challenge

Four fresh read-only challengers (T, S, P, D · G, M · K · A, H) tried to break every plan against HEAD, the documented usages,
the other lanes, and the threading and lifecycle each fix introduces: **17 sound, 55 amended, none dropped** (each amended
plan says what changed under its title). What the challenge caught, besides stale quotes after the review's own fixes:

- K01a's reload could start in the middle of a batch and put an older type over a newer one, and `awaitWrites` returned too
  early; the batch being written now counts as unwritten, which K01b and K03 build on.
- K02a's "write Scenes first" would have put every keystroke in a name field behind a disk write; the in-memory change stays
  first and is rolled back when the Scene writes fail. K04 and K02b follow the same order.
- K05 would have held the change lock while `get_kubriko_api` downloads from Maven Central; K06's stale Scene name index
  could let `create_scene` overwrite a Scene made outside Creator; K10 would have walked into a root `out/` again; K11a
  would have set aside a settings file that was only locked for a moment.
- A01 could hang forever on an agent that never answers `session/close`; H02a's replayed triggers would have delayed every
  opening by the debounce; H03b could skip waiting for the previous project to close; one shutdown order now spans H03a,
  H03b, A01, and A02a, and agent processes join the game's shutdown hook.
- Golden files compare ignoring line breaks, since the Windows CI job checks them out with CRLF; T05's `.gitattributes`
  stays inside its fixture folder (the user's "no `.gitattributes`" decision is about generated projects).
- G04 and G05 build on the landed `overridesOnAdded`; two generator tests that could never fail are repaired; M03 keeps T06's
  ABI check when CI switches to the aggregate task; M06 always moves `BlockTypesController` into core.
- The challenge also found one more bug in the review's own follow-ups (Stop during context gathering dropped the summary
  of others' changes), fixed as `87ddc45d5`.

Accepted, not planned: if an undo lands on a value a focused field is still waiting to see echoed and Compose collapses the
two updates, the undone value shows only when the field loses focus.
