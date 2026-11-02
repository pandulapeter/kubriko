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

What each batch landed, in order, with the names later plans need and every deviation from a plan. Hashes before phase 3
are the lane worktrees' (main got cherry-picks with new hashes; find them by message). Every batch appends to the end.

# Phases 1 and 2 (landed on main, 2026-11-02)

## Batch 1 report (T01, T04, T06)
T01: 6ea59725c — shared testing package (testing/TestDirectories.kt testDirectory(), testing/FakeGitRepository.kt, BlockTypeRepositoryContract, GitRepositoryContract); FakeBlockTypeRepository folder behaviour fixed. creator/CLAUDE.md names the contracts by class name (check_doc_names refuses globs).
T04: 716335307 — nightly step in tests.yml compiling every generated project template against the just-published libraries; publish-check timeout 60→120 min.
T06: 5b788a9ef — ABI validation for tools/creator-api: bare `abiValidation()` (Kotlin 2.4.21 DSL), tasks checkKotlinAbi/updateKotlinAbi, committed dumps under tools/creator-api/api/ (JVM + klib for iosArm64, iosSimulatorArm64, wasmJs); tests.yml runs checkKotlinAbi; root CLAUDE.md updated.

## Batch 1 report (S03, S02, S01)
S03: 5a2c23af1 — ProjectOutput.isOwn(file, rootDirectory) / isOwnUnderCanonicalRoot(file, canonicalRoot), used by the class loader, GameProcess.snapshotOf, and GameBuilds.ownClassFolders; ProjectOutputTest added.
S02: d0e72c3c2 — ClassLoaderReleaseTest proves a released project class loader is collected (loader takes an injectable `parent`); no leak found.
S01: 306dd66c0 — refuse Scenes of projects on newer Compose/Kotlin minor; warn about other newer shared libraries (SharedLibraries, CreatorLibraries, ProjectClasspath; `library.` lines from the init script). WorkspaceController.creatorKubrikoVersion is now a plain constructor parameter (WorkspaceScreen no longer reads it). creator/CLAUDE.md: the `library.` lines clause is at the end of the init-script bullet's classpath sentence.

## Batch 1 report (G01, G02)
G01: 424cb48fb "Raise Kubriko Creator's format version for every change to what its type files persist, and pin their shape per version in PersistedShapeTest." Policy text in ProjectFormat KDoc, FormatMigration KDoc, creator/CLAUDE.md → Format versions. New: project/PersistedShapeTest.kt and src/test/resources/formats/persisted-shape-1.txt (CURRENT_VERSION 1). Deviation: sealed subclasses render as `<sealed serialName>:<subclass>` (e.g. ConditionDefinition:number.property) because condition and property subclasses share serial names; lists not counted in the visited set.
G02: bbb1a44b8 "Describe every per-kind Block fact in Kubriko Creator's BlockKind, name every Block in each when over them, and pin the every-Block project's generated code with golden files." New test files actors/EveryBlockTypes.kt, actors/GeneratedCodeGoldenTest.kt, 11 golden files in src/test/resources/generated/. BlockKind takes needsActor (no default), onlyIn, misplacedComment (init check: last two set together). BlockCodeGenerator.blockLines checks Actor-only first, then onlyIn; actorBlockLines lost its unused eventType parameter. Every listed when in BlockReferences.kt, ProjectRelations.kt, PropertyBlockParameters.kt, ActorTypeFeatures.kt names each Block, no else. Skipped manual: KUBRIKO_CREATOR_INTEGRATION run.

## Batch 2 report (G03, G04)
G03: 2cddf910b — each Block's emission returns a `BlockCode` (lines, needs, imports, Managers) from `blockCode`/`eventCode`; `templateLiteral` returns a `TemplateLiteral`; every lookup/import/Trait derived from that, each generator declaring needs with its own exhaustive when. Removed usesScenes, usesPause, movesTheActor, needsActorManager, managersUsedBy; `usesAudio` stays for PluginManagers; `BlockScope.isOwn` non-private. A family's `asText` decision is computed per member scope through eventCode. New tests: BlockCodeTest, ManagerTypeCodeGeneratorTest.aTextShowingANumberGetsTheNumberTextFunction.
G04: 36ec7a89c — new actors/ActorClassEmitter.kt with Role (STANDALONE / FAMILY_ROOT) and EventPlacement (INLINE / HOOK_CALLS); Hooks moved to actors/FamilyHooks.kt as internal `FamilyHooks`. appendTimerFields takes a visibility; appendPropertyFields and maskClassOf live in ActorClassCode. Per-file imports; ActorTypeFeatures takes `eventScopes`, its `imports` replaced by `lookupImports` and `codeImports`, no packageName parameter; legacyNeedsOfComments deleted. Kotlin Blocks import SceneOffset in Actor scope. BlockTypeNamingTest lists class names literally (BASE_IMPORTS etc. removed). appendKeyHandling lost its `guard` parameter; drawFunction and SPRITE_DRAW_FUNCTION private. Suite: 1179 tests, 8 skipped.

## Batch 3 report (G05, G07, G09, extra fix)
G05: 1935deffd — each Event kind of standalone Actor and Manager types runs in its own function that lifecycle methods only call. actors/FamilyHooks.kt renamed to actors/EventFunctions.kt (object with Function(signature, call, imports), of(event, scope) returning null for an Event the type kind lacks, appendFunction). EventPlacement, appendEvents, appendTimer, appendKeyHandling, eventComment, isCommented removed; Event comments dropped. Reporting: new health-check kind KOTLIN_BLOCK_SCOPE_CHANGED with a `usage` field ("return" or the local's name); KotlinIdentifiers.Occurrence has `depth`; console line per type on first load via BlockTypesController `onFirstLoaded`.
G07: 71a1187d7 — Events compared and grouped by trigger; `replaces` compares triggers; generators read Event details through the trigger.
G09: 318a01491 — Kotlin Blocks may start with import lines; format version 2 via empty KotlinBlockImportsMigration; persisted-shape-2.txt; ManagerFieldMigrationTest expects CURRENT_VERSION.
Extra: 3b9d2cd2d — CollisionMask reserved; BlockTypeNamingTest.everyClassTheEveryBlockProjectsGeneratedCodeImportsIsReserved.
Suite: 1197 tests, 8 skipped.

## Batch 1 report (A01, A02a)
A01: a5eaf1685 — assistant restarts, project changes, and turning off run as one ordered sequence; McpToolServer stays stopped while starting stopped (start after stop() throws IOException). New shared helper `runIgnoringFailure` (file IgnoringFailures.kt, added in A02a). The ordered-sequence sentence is in ai/assistant/CLAUDE.md.
A02a: 716ad42e7 — AssistantController split into AgentSelection, AssistantHost, AssistantSignIn (private field `signIns`), one AI-off gate in AssistantHost, settings saved off the main thread. AssistantSignIn.setVariables takes the agent ID. AssistantControllerTest: three tests that read the settings file back now wrap that in `awaitSavedSettings { … }` (accepted by the orchestrator). ai/assistant/CLAUDE.md has a "Panel state" bullet saying the controller is "created once in `KubrikoCreatorApp`"; creator/CLAUDE.md's AI features bullet names AssistantHost. AssistantController.kt is at 499 lines (limit 500).

## Batch 2 report (A02b, A03, A04)
A02b: 227ad66ff — assistant panel takes an `AssistantPanelModel` (AssistantController implements it); agent capabilities from its profile (`usesClaudeAccount`). `hasApiKey` and `configurations` are derived flows (combine + stateIn), so tests wait with `.first { … }`. AssistantPanelTest builds its AssistantChat on an AcpAgent over FakeAgentLauncher. New AgentProfileTest.
A03: f61687338 — ResultDelta tells the agent to read with full: true; a cancelled turn forgets the conversation's reads (`forgetReads`). Known (not fixed): AssistantChat.prompt starts its turn as a LAZY coroutine; a stop() before it first runs leaves isBusy true. Tests avoid sending and stopping in one main-thread block.
A04: 573fa47a0 — auto-allow only tool kinds that change no files and run nothing; mcp-remote pinned to 0.15.2.

# Lane K reports
Base: main cbc82e795, which holds all of phase 1 (lanes T, S, G, A), T03 (workspace collaborators tested alone; fakes FakeProjectBuild, FakeSessionChannel, FakeBlockTypeRepository, FakeGitRepository, FakeProcessRunner live in creator/src/test/.../testing/; testing/ also has testDirectory(), blockingTest, awaitIdle/awaitTrue, IdleTrackingDispatcher, TestPorts, and the contract tests), T05 (format fixtures in src/test/resources/formats/v0..v2), and format version 2 (G09).

## Batch 1 report (K12, K01a, K01b)
K12: 4d4d73a19 — atomic writes retry the move with short backoff on Windows sharing violations (injectable mover, `moveReplacing`); a failed write deletes its temporary file.
K01a: 7e108951a — failed block type writes stay queued with everything after them, retried at 2/10/30 s; new workspace/FailedWrite.kt, workspace/FailedWriteEntry.kt; PendingWrite.subject, in-flight batch, generation counters, `unwrittenCount`, `failure`, `unsavedNames()`; reload reads again when a type changed during it. Taking a batch also drains the pending write request. Strings: removed status_actor_type_not_saved; added status_block_type_not_saved, status_type_folder_not_saved, status_generated_code_not_written, status_block_types_saved_again. FakeBlockTypeRepository has `failingNames`, `onLoad`, `loadCount`.
K01b: 39d2b2b0c — `awaitWrites(): Boolean` (written generation), `awaitWritten()`; close() waits up to CLOSE_WRITE_TIMEOUT_IN_MILLISECONDS (10 s). CommandContext.awaitWrites() / unsavedWrite(), Applied.failed(Rejection); Commit, DiscardChanges, StartVersionControl refused while a write fails. CreatorTaskRunner takes `awaitWrites: suspend () -> UnsavedWrites?` (workspace/UnsavedWrites.kt). Batch disk side moved to workspace/BlockTypeBatchWriter.kt (BlockTypeBatchWriter, BatchResult, BatchFailure). Strings added: error_command_changes_not_written, error_command_generated_code_not_written, status_building_with_unsaved_types, status_building_with_unwritten_generated_code.

## Batch 2 report (K01c, K02a, K02b)
K01c: 32c9f2c81 — BlockTypeStore keeps a name→files index (`filesByName`/`isIndexed`) built by load, kept by save/delete (all @Synchronized); a file copied in after load is left for the next load to report as a duplicate.
K02a: fda8cb223 — CommandExecutor turns a throwing command into a rejection (`error_command_failed`; step drops its history entry; CancellationException still thrown). New SceneStore.writeAll (all or nothing; `writeText` hook); commandContext has `ioDispatcher` and `sceneWriteText` params. RenameActorType, AddProperty, EditProperty roll back their in-memory type change when Scene writes throw.
K02b: 5310e4f30 — internal commands/CompositeCommand.kt (not registered, serializer throws); WriteCommand has `title(input)` and `targets(context, input)`; SceneStore.changeStates / changeSceneStates return the replaced states; AddProperty's undo is RemoveProperty + SetActorsState composite (parts `isRestoring`). Suite: 1396 tests.

## Batch 3 report (K03, K04, K05)
K03: b1725ef87 — HistoryEntry.contentHashes: Map<CommandTarget, Int>; CommandContext.contentHashesOf(targets) (type on calling thread, Scenes in one IO hop; 0 deleted, null unreadable); executor hashes after execute/step, merged step keeps previous + new; step checks after rejectChanges, before validate; string error_command_changed_outside. SceneStore has a `readText` hook; test helper commandContext has `sceneReadText`; blockTypesController helper takes any BlockTypeRepository.
K04: a4dcf6211 — ActorTypeFamily.declarerOf; BlockTypeEdits.withPropertyUsesRenamed split from withPropertyReplaced; EditProperty renames on the declarer across the family, rolls back on Scene failure; AddProperty checks descendants (error_command_property_kind_in_descendant); SetParent checks descendants; SceneStore.changeStates and changeSceneStates take Set<String> type ids; string error_command_property_inherited; tests in commands/actorTypes/FamilyPropertyCommandsTest.kt.
K05: 2e60cb6c5 — ReadCommand.readsProject (default true; GetKubrikoApi, GetBlockCatalog, ReadConsole false); reads that read the project run under the change lock. Suite: 1413 tests.

## Batch 4 report (K06, K07, K08)
K06: 3aeb0ca7c — SceneStore keeps a @Volatile sorted name index (null until first refresh()); refresh() walks the Scenes folder, warms the cache, keeps names written/removed during the walk; `exists` answers from the index; `scanNames()` reads the folder now (changeActors, moveFolder on IO, CreateScene/RenameScene apply re-checks); `beforeIndexing` test hook. WorkspaceController.refreshResources() calls scenes.refresh() in a second IO hop after resources are set. contentHashesOf checks scenes.fileOf(name).isFile. CommandContext.projectUpgrade wired to _projectUpgrade.value; UpgradeProject re-checks on IO; SetProjectSettings reads identity in apply.
K07: 73ab80388 — git/CommitResult.kt; GitController.perform<T> returns the operation's result with the state; commit reads status inside the operation; FakeGitRepository.statusCalls.
K08: ecd2c9da2 — commands/scenes/ActorBatches.kt holds the batch bodies; single-Actor Scene tools call them with one-element lists (names, inputs, results unchanged). Suite: 1427 tests.

## Batch 5 report (K11a, K11b, K09a)
K11a: e92516bbb — foundation/JsonFileStore.kt (`JsonFileStore<T>(file, serializer, isPrivate, json, readText)`, read(): Missing/Found/Unreadable, write(value): Boolean, sets undecodable content aside as .bak, never replaces an unreadable file); used by CreatorSettingsStore, AssistantSettingsStore, McpSettingsStore, SavedConversationStore, RecentProjectsStore (AgentRegistryStore unchanged).
K11b: 84337970f — foundation/HttpDownloads.kt (one shared lazily built client, `get(url, bodyHandler, timeout?)`, `text(url, timeout)`); used by KubrikoSources, AgentRegistryStore, NodeRuntime, BinaryAgentInstaller.
K09a: 30785ef07 — scenes/SceneChange.kt (Written, Deleted, Moved, FoldersChanged); SceneStore records changes (`takeChanges()`), writeAll only after all succeed; CommandExecutor takes `onSceneChanges` and hands changes over in a finally inside the lock (work in applyChange / stepHistory); CommandContext.onScenesChanged/onSceneRenamed removed. New workspace/ProjectChanges.kt: `ProjectChanges(scope, scanResources, onScenesChanged, onSceneRenamed, onScenesChangedOnDisk, markSceneOutdated, regenerate, onCodeChanged, refreshGit, refreshFormatStatus, reloadTypes, refreshProjectUpgrade)` — 12 parameters, P03's limit, so K10 must pass its snapshot through `scanResources`, not a new parameter; methods refreshResources(), onSceneChanges(changes), onTypesSaved(haveTypesChanged), onProjectChanged(changes); conflated scan loop that survives exceptions, exposes no flow. WorkspaceController: `private val changes = ProjectChanges(...)` after `scenes`; scanning in `private suspend fun scanResourcesNow()`; onSaved no longer rescans. GitController.refresh() sends on a conflated channel to one guarded loop. createController test helper has a `gitRepository` parameter. Suite: 1460 tests.

## Batch 6 report (K09b, K10)
K09b: 1139900e7 — construction in dependency order; commands/BlockTypes.kt interface (BlockTypesController implements it; CommandContext.blockTypes is BlockTypes?); UnsavedWrites moved to commands/; blockTypes reports saves through an unlimited Channel ProjectChanges reads; SessionController.play(firstScene, prepareLaunch), restart() reuses it; workspace/WorkspaceOpening.kt (workspaceScopeOf, WorkspaceOpening track/abandon, openWorkspace) closes what a failed opening started; WorkspaceController has a `workspaceScope` parameter. Not done: the optional factory step (constructor still ~23 parameters).
K10: 11d11e98f — ProjectTree.walk(root, skipped, hiddenDirectory) + ProjectTree.isWithinSources; project/ProjectSnapshot.kt; ProjectWatcher.start callback (changes, snapshot); ResourceScanner.scan(root, hiddenDirectory, snapshot); ProjectChanges.scanResources takes ProjectSnapshot?; WorkspaceController.onProjectChanged(changes, snapshot = null).
Review fixes: 212578617 (merged Save keeps its first place), 04cd99656 (JsonFileStore.update, one read), 07525d70b (CLOSING_TIMEOUT = CLOSE_WRITE_TIMEOUT + margin), 9374f7c45 (OwnWrites.generation; ProjectSnapshot.ownWriteGeneration; stale snapshot → walk).

## Serial and extra commits on main
- T03: workspace collaborators (SessionController, CreatorTaskRunner, WorkspaceRunner, GameBuilds, PendingWrite, GradleWorkspaceOpener) tested on their own; FakeProjectBuild, FakeSessionChannel, FakeBlockTypeRepository moved to testing/; WorkspaceControllerTest keeps wiring cases only.
- T05: golden fixture project per format version in creator/src/test/resources/formats/v0..v2 (FormatFixturesTest); a format raise needs a new folder.
- Extra: the pre-commit hook unsets GIT_DIR, GIT_INDEX_FILE, and GIT_WORK_TREE before Gradle (JGit honoured them in linked worktrees); check_sizes.py names functions with nullable/generic receivers and bounded type parameters correctly; SharedLibraries also refuses newer androidx.compose minors; AssistantChat clears isBusy in invokeOnCompletion so a turn stopped before it starts ends; CollisionMask reserved as a type name.
- Lane K was reviewed before merging; the review's fixes are the last seven lane K commits on main.

# Phase 3 (CI, branch fifth-review-execution)

## H01 — see next commit
"Give Kubriko Creator's workspace one selection, a type, a Scene, or nothing, that every caller changes through SelectionController." — verified: ./gradlew :creator:test (1,509 tests, none failed), ./gradlew :creator:spotlessApply, SelectionControllerTest fails to compile without the fix; skipped manual: the :creator:run check, no display in CI
- New workspace/WorkspaceSelection.kt (`WorkspaceSelection` None / BlockType(name) / Scene(name), and `SceneEditingState.sceneName` for every non-closed state) and workspace/SelectionController.kt (`current: StateFlow<WorkspaceSelection>`, `selectType(name)`, `openScene(name)`, `clear()`). `WorkspaceController.selection` is built right after `sceneEditing`; createActorType/createManagerType select through it. Nothing under ui/ calls `BlockTypesController.select` or `SceneEditingController.open/close` any more; the sidebar highlights a preparing or failed Scene too.
- Deviation: the type editor renders on `selection is BlockType && sceneEditing == Closed` (not on the selection alone), since `current` is a `stateIn` that follows the Scene state one dispatch later and would otherwise show the editor beside a Scene for a frame.
- Deviation: creator/CLAUDE.md no longer has the sidebar paragraph or the package table the plan names (cut by ae027ca3e), so the rule is one bullet in its UI section.
- Deviation: `$RUNNER_TEMP` is outside what this session may touch, so the changes set aside for the failing-test check went to the git-ignored `build/scratch/`.
- Extra fixes: none
