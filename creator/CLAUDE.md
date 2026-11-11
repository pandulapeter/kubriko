<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# creator (Kubriko Creator)

A standalone desktop app for making Kubriko games without IntelliJ, inspired by GameMaker 6.1: Actor types and Manager types are
built from Events and Blocks and generated into plain Kotlin, Actors are placed in Scenes, and the game is played with one click.
This file holds the rules a change to Creator keeps and the why behind its less obvious designs; the code and its KDoc say what
each part does. What is planned lives in `plan/` (start with `plan/README.md`). Two packages have guides of their own: the Block
model and code generators (`core/src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/CLAUDE.md`) and the in-app assistant
(`ai/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/CLAUDE.md`).

## Running and testing

- `./gradlew :creator:run` runs against this repository's libraries however it is started: `settings.gradle.kts` turns on
  `creator.localKubriko` for any build that runs one of its run tasks (by path, or by name from the root or `creator/`, where
  `../gradlew run` starts it) and puts `:publishCreatorLibraries` first. That publishes the libraries as
  `<library.version>-creator` (so they never shadow a real release) and bundles that version and `mavenLocal()` into
  `creator.properties`, so projects use them. Packaged builds, and `-Pcreator.localKubriko=false`, bundle `library.version`
  as it is, which projects only move to once Maven Central has it.
- `./gradlew :creator:allTests` runs every module's unit tests (also on Windows and Linux in `Tests`); `:creator:<module>:test
  --tests …` runs one module's. Run the whole suite before every commit,
  not only the tests you touched: tests elsewhere catch leaks and races (a server left running, an exception escaping a scope),
  and Windows paths differ (write expected paths with `File`, not `/`). Line breaks don't: `.gitattributes` checks every file
  out with `\n` on Windows too, so a test of Windows line breaks writes `\r\n` as a literal.
- Opt-in tests, skipped without their switch:
  - `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=<version>` (`ProjectTemplateIntegrationTest`, in `:creator:template`) generates every template for real and builds it for
    desktop and web, with an Actor type and two Manager types using every Event and Block, and resolves the desktop runtime
    classpath as playing does (compiling alone misses a library whose repository is missing). For an unreleased
    `library.version`, run it after a `:creator:run` with `<library.version>-creator`. `Tests` runs it nightly (and on a
    manual dispatch) in its `publish-check` job, against that job's `0.0.0-check` publish. Its `everyTargetBuilds` turns every
    target on and builds the Android app and, through `xcodebuild` for the simulator, the iOS app; it needs the Android SDK
    and a Mac with Xcode.
  - `KUBRIKO_CREATOR_INTEGRATION=true` with Chrome or Chromium (`HeadlessChrome.find`, or `KUBRIKO_CREATOR_CHROME`) also runs
    `WebGameIntegrationTest`, which builds the starter's web distribution, serves it as Creator does, and drives a headless
    page through the DevTools protocol (no window, so it runs on any machine): the Scene its address names is drawn, and a
    change once built reloads the page. The page's focus is emulated, since Kubriko starts a game only once its page is
    focused, which a headless one never is; what it showed is left in `template/build/web-game`.
  - `KUBRIKO_CREATOR_BENCHMARK=true` times a 200-type project without Gradle (`CreatorBenchmarkTest`) and, with
    `KUBRIKO_CREATOR_VERSION`, builds it for real (`CreatorBuildBenchmarkTest`; more Gradle arguments through
    `KUBRIKO_CREATOR_BENCHMARK_ARGUMENTS`).
  - `KUBRIKO_CREATOR_E2E=true KUBRIKO_CREATOR_VERSION=<version>` (the end-to-end checks, `endToEnd/` in `:creator`'s tests;
    `./gradlew :creator:test --tests "*endToEnd*" --rerun`) needs a display nobody is using (never the user's own, whose screen
    and focus the game tools take): on Linux, `xvfb-run -s "-screen 0 1920x1080x24"`, where Skiko falls back to software rendering. Each scenario starts the real Creator in a JVM of its own (the app's runtime
    classpath, which the test task hands over, with a settings folder of its own through `kubriko.creator.settingsDirectory`
    and its MCP server on) on a fresh, committed copy of `BreakoutProject`, drives it through the MCP tools as an outside
    client would, and judges what it left: files, console, Creator's window (from the screen), and the game's (through
    `take_screenshot`), by pixels at known places rather than by eye. Every image is saved under
    `creator/build/end-to-end/<scenario>`, and whoever runs a scenario looks at them before calling a change done. The fixture
    stays at template version 7 with no format version, so every upgrade and migration runs on it; never upgrade its files.
    Synthetic input to the window is only for what no tool can do. For an unreleased `library.version`, publish to Maven
    Local first, as `:creator:run` does (`-Pcreator.localKubriko=true`). `Tests` runs them nightly (and on a manual dispatch) in
    its `creator-end-to-end` job, keeping the images as an artifact.
  - `KUBRIKO_CREATOR_CLAUDE_AGENT=true` (`KUBRIKO_CREATOR_CLAUDE_CONFIG_DIR` picks a Claude Code configuration) runs the real
    Claude Agent on an existing login: `ClaudeAgentLiveTest`, and `AssistantLiveScenariosTest`'s headless conversations on the
    Breakout fixture, which check what the project became, not the words. They cost a few requests on the user's subscription,
    so run them (`./gradlew :creator:ai:test --tests "*AssistantLiveScenariosTest*" --rerun`) when the assistant's behaviour changed.
    `EvalSetLiveTest` has the agent answer every request of the eval set (`EvalSet`), each on a fresh fixture; with
    `KUBRIKO_CREATOR_EVAL_RECORD=true` it writes the calls of each request that passed as its recording (`EvalRecordings`,
    in `:creator:commands`' test fixtures' resources), which `EvalSetTest` replays for free. Record again after a tool's input
    changes, rather than editing a recording by hand.
- Controllers are tested against fakes in `testing/` (`FakeProjectBuild`, `FakeSessionChannel`, `FakeBlockTypeRepository`, `FakeGitRepository`),
  file code against real temporary folders, git against real temporary repositories (JGit needs no installed git), and sockets,
  the MCP server, and shell commands for real on localhost. Tests on real threads use `testing/blockingTest`, and assert that
  nothing happened only after `awaitIdle` on the dispatchers they gave the code under test, never after a fixed delay.
  Each module with resources has its own `strings.xml` and `packageOfResClass`, and its own `StringResourcesTest`, which fails on a
  string or icon its sources don't use, and on escaped apostrophes. Shared fixtures live in
  `testing/` (`testDirectory()`, and the fakes above); a fake of an interface with a real implementation runs the same
  contract test as the real one (`BlockTypeRepositoryContract`, `GitRepositoryContract`). A collaborator of
  `WorkspaceController` is tested on its own, and `WorkspaceControllerTest` keeps only cases about the wiring between them.
- UI is checked offscreen: `uiTest { showing { … }; saveSnapshot("name") }` (`testing/UiSnapshots.kt` in `:creator:ui-components`'
  test fixtures) renders a composable in the app's theme and interface scale without a window, asserts its texts and clicks,
  and saves a PNG under the module's `build/ui-snapshots`.
  Every new UI gets one. Nothing is handed to the user as a check. Unit tests and snapshots come first and cover all they can
  reach; an end-to-end scenario costs a real Creator, a Gradle build, and the agent's time looking at images, so one is added
  only for what they can't reach and could really break or be built wrong (a real build of an upgraded project, a played game's
  behaviour, processes and windows working together), never for a label, a menu entry, a field's validation, or a layout, and
  a scenario checks several things in one Creator start. Only what needs hardware or an account no agent has goes to
  `plan/HANDOVER.md` → Before a release.

## Modules

Creator is split into Gradle modules, lowest first:

- `:creator:foundation` (`foundation`, `console`): files, the settings folder, typed texts, and console entries.
- `:creator:ui-components` (`ui.components`, `ui.theme`): the shared components and theme, with their own resources and the
  offscreen UI test helpers.
- `:creator:core` (`actors`, `actors.codegen`, `actors.store`, `project`, `schema`, the Scene files in `scenes`, the
  screenplay's pages in `screenplay`, and the sprites' folders in `sprites`): the Block model, code generation, the type
  store, project files, Scene files, pages, and sprite folders, with no Tooling API, JGit, Ktor, MCP, ACP, or
  engine dependency.
- `:creator:gradle-session` (`gradle`, `session`): Gradle and game sessions, with the session init script as its resource and
  the session protocol in its API.
- `:creator:git` (`git`): version control through JGit, which stays out of its API.
- `:creator:template` (`template`): project templates and upgrades, with the template files as its resources.
- `:creator:commands` (`commands`, `context`): the commands and the AI's context, with the engine overview `get_kubriko_api`
  hands the AI as its resource.
- `:creator:ai` (`ai`, `mcp`): the MCP server and the in-app assistant, with the ACP SDK in its API, since the assistant's
  state carries its model types.
- `:creator:scene-runtime` (`sceneRuntime`): the Scene editor's live preview (project class loading and sprites), with the
  engine, `tools:creator-api`, and every published plugin; the only Creator module that runs the engine.
- `:creator:workspace` (`workspace`): the open project, `WorkspaceController` and its collaborators.
- `:creator`, the app (the root package, `ui`, `settings`, `recent`, `window`): the UI, settings, and window.

A module depends only on lower ones (Gradle refuses the rest, and `PackageLayeringTest` keeps each file in its module's
layer, gives every new package a layer, catches an upward import inside one module, and lets only the screens' entry files
(`ui/AppContent.kt` and `ui/WorkspaceScreen.kt`) take the app, workspace, or assistant controller), and what another module uses is
`public` under the explicit API mode of `kubriko-creator-module`, the rest `internal`.
Test helpers other modules share live in a module's `src/testFixtures` (reached through
`testImplementation(testFixtures(projects.creator.<module>))`; foundation's hold the Creator-independent ones of `testing/`
and `creatorSourceRoots()` for tests that read the sources; core's hold `FakeBlockTypeRepository` and `EveryBlockTypes`;
gradle-session's `FakeProjectBuild`, `FakeSessionChannel`, and `GameProcessTestGame`; git's `FakeGitRepository`; commands'
`CommandTesting.kt`, `FakeProjectRunner`, and `BreakoutFixture`; ai's `FakeAgent` and `FakeAgentLauncher`; workspace's `WorkspaceTesting.kt`).

## Definition of done

Besides its tests passing, a change:

- Reuses the shared building blocks instead of writing its own: `writeTextAtomically` for every file Creator writes,
  `JsonFileStore` for every settings file it keeps (it sets a file it can't decode aside as `.bak` instead of replacing it;
  a change built from what the file holds goes through `update`, which reads it once),
  `withLineBreaksOf` (`project/LineBreaks.kt`) for every file it edits, `ProjectTree.walk` for every walk through a
  project, `LatestRequests` for background reads whose results may land out of order; in the UI `BufferedTextField`
  (`ListItemKeys` for the rows holding one, so a row keeps its text when one above it goes), `LabeledSwitch`, `CreatorDialog`, `MasterDetailDialog` (with `SettingsRow`) for every dialog of several groups, `DragPreviewChip`, `SearchableChoiceButton` for every
  choice of the project's contents (types, Scenes, sprites, audio files, Properties), which grow without bound (it filters once
  there are more than eight; `KeyChoiceButton` ranks keys by the same `matchingOptions`), `PanelWidths` with `sidePanel` and
  `PanelSplitter` for every resizable panel (`AnimatedSidePanel` for one that opens and closes), `RightClickMenu` and
  `hoverReveal` for what a row offers besides its main action, and `CreatorMotion` for every transition; in command tests
  `CommandTesting.kt` in `:creator:commands`' test fixtures (`commandContext`, `assertRoundTrip`); in every test `testing/` (`testDirectory()`, the fakes
  and their contracts).
- Has its unhappy paths designed: what happens when it throws, when it is cancelled, when it is called again while the first call
  runs, when a file has Windows line breaks, and when its file changes outside Creator.
- Does no blocking I/O on the main thread, and lets no long-lived loop (a watcher, a server, a reader of a process's output) die
  on one exception.
- Tests against literals or the real implementation, never a copy of the code under test; a test that needs a network, a tool,
  or a switch skips itself with `Assume` rather than returning early.
- Adds to a `CLAUDE.md` only the rule or the why it introduces.
- Is reviewed with `/code-review` before the next plan item starts (`plan/README.md`).

## Principles

1. **Plain files, plain Gradle.** Nothing the build needs lives in editor-private files; deleting `.kubriko/` never breaks a build, it
   only turns block-built Actor types into ordinary code.
2. **Creator never rewrites code it didn't generate,** and stops updating generated code that was changed by hand.
3. **Every change outside Creator-owned files is shown as a diff and confirmed.**
4. **No engine changes for editor convenience**; editor-only runtime needs go into opt-in modules.
5. **The AI can do exactly what the user can, through the same validated commands, and nothing else.**

## Files and ownership

| Path | Owner | When changed elsewhere |
|---|---|---|
| `.kubriko/actors/**/*.json`, `.kubriko/managers/**/*.json` | Creator | Reloaded within a second; code regenerated. |
| `.kubriko/sprites/**/*.json` (the sprites' folders and sheets) | Creator | Read again with the resource tree within a second. A directory is a folder and `<sprite>.json` puts a sprite in it; a sprite without one is in no folder. The file also holds how the sprite is cut into frames (`SpriteSheet`, through `set_sprite_sheet`), and a change to that, whoever made it, regenerates the types (`ProjectChanges.onSpriteSheets`, compared with what the first scan read). |
| `.kubriko/project.json` | Creator | Its format is checked again within a second. One that can't be read is named with what is wrong in it, in plain words rather than the parser's (`ProjectSettingsReader`, which `ProjectFormat` reads it through too, so both refuse the same files): it stops the opening, or is reported in the console while the project is open; a newer format's project opens without its types instead, since only browsing it needs none. |
| `.kubriko/scenes.json` (Scene descriptions) | Creator | Read again whenever it is used; a file that can't be read holds none. |
| `<package>/generated/*.kt` | Creator | Checksum mismatch → Creator stops updating it and offers to discard the edits. Committed, so projects build without Creator. |
| `composeResources/files/scenes/**/*.json` | Shared | Sidebar refreshes; an open Scene shows the change. |
| Sprites, sounds, and music (`composeResources/drawable/*`, the rest of `composeResources/files/**`) | Shared | Sidebar refreshes. Creator adds, renames, and deletes them only through the resource commands, which name each by its path from `composeResources` and act on every source set's copy in the game's own module (the one new Scenes go to; another module's are another `Res`), found on disk, since the last scan may be a second old, and refuse a name any image of `drawable` already makes into an accessor; their undo steps check only the structure (`validateRestoring`) for the same reason, and each raises `CommandContext.filesRevision`, which the watcher never reports, so the tree is scanned again. A sprite's pixels change only through `editSpriteImage` (`create_sprite`, `draw_pixels`, `edit_sprite`), on its frames (`SpriteFrames`), and its undo writes the image back byte for byte (`ReplaceFiles`); every write of a sprite's image raises `spriteImagesRevision`, so the Scene view and the playing game follow it as they follow one changed outside. |
| `screenplay/**/*.md` (the screenplay's pages) | Shared | Read again within a second; Creator writes them only through the page commands (`ScreenplayStore`), which check the folder again before writing, so a page made outside meanwhile is never written over. |
| A page's images next to it (`screenplay/**/<page>-<n>.<ext>`) | Shared | Written only as new files by `add_page_image`, scaled to `PageImages.MAXIMUM_WIDTH` since pages live in git; moved with their page (renamed after it), and deleted with it only when no other page shows them and the user agrees, undo bringing back the bytes (`ReplaceFiles`). |
| Every other Markdown file (the documents, outside build folders) | User | Rescanned within a second; Creator writes one only through the document commands (`DocumentStore`, which knows the paths from the last scan and its own writes), never over a file there, and undoes a delete only with the bytes it read, refusing a file that isn't UTF-8 text. |
| `AGENTS.md` (the game's notes) and the `CLAUDE.md` importing it | Shared | Read at the start of every conversation; Creator writes `AGENTS.md` through `set_game_notes`, and `CLAUDE.md` only when the project has none. |
| Hand-written Kotlin and resources | User | Sidebar refreshes; an open Scene is built again. |
| Template files (`Game.kt`, build files) | User after creation | `templateVersion` in `.kubriko/project.json`; upgrades are shown as a diff. |
| The Kubriko entries of the build files (`kubriko` version, `kubriko-*` libraries, Creator's Maven Local block) | Creator | Brought back in line with the code and Creator's version after every regeneration. |
| The `game.*` keys of `gradle.properties` (the game's identity and settings) | Creator | Edited line by line by Project settings; a value changed by hand shows there the next time it opens. |
| A target's own files (`androidApp/`, `iosApp/`, `src/androidMain`, `src/iosMain`) | User after creation | Created by turning the target on when missing, never rewritten, and kept when it is turned off. |
| The `PRODUCT_BUNDLE_IDENTIFIER` line of `iosApp/Configuration/Config.xcconfig` | Creator | Follows `game.applicationId`; the Xcode build fails, naming both, while they differ. |
| `.kubriko/palette.json` (the project's palette for drawing sprites) | Creator | Read again with the resource tree within a second; written only through `set_palette`, and absent while it holds no color. |
| `.kubriko/screenplay.json` | Creator | How the screenplay is kept (`ScreenplaySettings`, its reminders), in a file of its own rather than `project.json`, so an older Creator just keeps reminding and no format raise is needed; kept through `JsonFileStore`, and absent while it would hold only the defaults, so undoing the first change leaves no file. |
| `.kubriko/local/` | Creator | Git-ignored, by a `.gitignore` of its own (`LocalProjectStore`), since a project Creator didn't make doesn't ignore it. |

## Threads, writes, and failures

- Workspace state (the `CommandExecutor`, `BlockTypesController`, `SceneEditingController`) is confined to the main thread;
  background work hands its results back there.
- Each workspace runs in a scope of its own, a child of the app's, and so do its session server and watcher. `close()` first stops
  the game and waits until it, and every process it started, is gone (at most `CLOSE_GAME_TIMEOUT_IN_MILLISECONDS`, as long
  as `GameProcess` can take to stop it; a game outliving that is left to the shutdown hook), and opening a project waits for the previous one to close; a
  game or agent still running when Creator's JVM exits is killed by a shutdown hook (`LiveGameProcesses`). It cancels the scope
  once the pending writes are done, giving a failed one up to 10 seconds of retries (`BlockTypesController.awaitWritten`). Quitting
  (`AppGraph.shutdown`, whose `AppGraph` builds the app's parts in `main` before the window) closes the AI server, the agent, and
  the open project side by side off the UI thread, then waits for the settings' last save, all within
  `CLOSING_TIMEOUT_IN_MILLISECONDS`, the sum of the workspace's bounded waits (the game's and the writes') plus 5 seconds, so
  the workspace is closed first (derived from those bounds, never written as a literal). An opening that fails or is cancelled closes what it started (`openWorkspace`). Builds and git commands wait for the writes too (`awaitWrites`): while one
  fails, git commands are refused and builds go ahead with a console warning naming what isn't saved.
- `writeTextAtomically` writes a sibling temporary file and moves it into place, so a crash leaves the old content or the new,
  never half; a new file holding a token is readable by its owner only (`isPrivate`), as is Creator's own `mcp.json` even when an older
  version left it readable (`restrictToOwner`), and the watcher ignores the temporary files.
- `ProjectWatcher` polls file stamps every second, skipping generated code and output folders outside `src` (any name inside
  it). Files as Creator left them (`OwnWrites`) don't count as changed, so Creator's own writes reload nothing. `ProjectChanges` is the one table routing Creator's own writes (the Scene changes `SceneStore` records,
  which `CommandExecutor` hands over after each command, and saved type batches) and the watcher's changes: a Scene edit refreshes
  only the Scene view, a new or removed Scene the resource tree too, and every change the Version control panel. Nothing needs a
  refresh button: the watcher leaves `.git` out, so git is read again whenever the window gets the focus back too. Resource scans and
  git refreshes are coalesced, so at most one runs and one waits. The resource tree after an outside change comes from the
  watcher's snapshot, so its one-second walk is the only periodic one; after Creator's own writes, which a snapshot may
  predate, the project is walked, and so is it for a snapshot taken before an own write (`OwnWrites.generation`), since the
  watcher never reports that write.
- An exception nothing catches (`unexpectedProblemHandler`, on the app's scope and each workspace's) is reported in the open
  project's console (or on the welcome screen), and the rest keeps running. The window's own exceptions are reported the same way
  and its content is built again (`CreatorWindowExceptionHandler`), unless they repeat (more than three within 10 seconds), when
  Compose's dialog shows them and closes the window through the orderly quit.

## Commands

Every change to a project is a command (`commands/`), so the UI, undo and redo, and the AI share one path, and every new editor
feature lands with its command, its AI tool, and their tests (validation, apply, and the inverse round trip). The UI dispatches
commands rather than calling controllers. The AI tool list is generated from `CommandRegistry.editor`, each input schema from the
input's serial descriptor (`JsonSchema`, `@Description` on every property).

- **Enforced by `CommandRegistryTest`:** every `EditorCommand` is registered, tool names are snake_case, every part of every schema
  is described, and nothing outside `commands/` opts in to `@EditorMutation`, the marker on every controller and store function that
  changes the project. A new mutation is marked, so only a command can call it.
- **Kinds:** a `ReadCommand` is never recorded. A `WriteCommand`'s `validate` returns a `Rejection` in the user's words: one
  entry per rule broken rather than a restatement of every rule (names are told apart through `NameFlaw`), and Events and
  Blocks named by their titles in the editor, with their IDs in brackets in `english`, which the AI reads. A rejection says
  what to use instead, so the AI fixes the call without another read: a name that doesn't exist (`LookupProblem`) offers the close ones
  (`SimilarNames`, through a `_SIMILAR` reason of its own, since a reason's arguments can't carry English) or says the name
  is of the other kind of type, and an index the ones there are. Its `apply`
  returns the inverse `Invocation` (none for git operations, which can't be undone); one that rewrites files without an inverse
  (discarding changes, the template upgrade) says `isRewritingFiles`, which clears the history. An `ActionCommand` (build, play)
  runs outside the change lock (one that starts nothing and touches no file, such as `ask_user`, says
  `leavesEverythingAsItWas`, so its tool is marked read-only and an agent's read-only mode lets it run); a `ReadCommand` runs under it, so an AI never reads a change half made (one that reads only
  Creator's own knowledge says `readsProject = false`, so a download doesn't hold up edits).
- **`CommandExecutor`** (one per workspace) applies changes one at a time. Consecutive changes with the same `mergeKey` (typing into
  one field) are one undo step; steps of different conversations,
  or of an AI client and the user, never merge. A step that no longer validates, or whose type or Scene changed outside Kubriko
  Creator since (each step keeps a hash of what it touches; a Scene that can't be read right now isn't compared), is dropped. A command that throws is rejected with
  the reason (an undo step that throws is dropped), and commands that change several Scenes write them all or none
  (`SceneStore.writeAll`), putting their type change back when the Scenes can't be written. `dispatch` starts undispatched, so
  in-memory changes show before it returns, and rejections of dispatched changes and undo steps go to the console.
  `CompositeCommand` (internal, not a tool) makes several invocations one undo step and rolls back the done ones when a later one
  fails; `add_property`'s undo uses it to bring back the Scene values the add dropped.
- **Undo and redo restore:** their steps are `isRestoring` and validated by `WriteCommand.validateRestoring`, since what they bring
  back may name types, Scenes, or sounds deleted since; restoring checks only the structure (the type, Event, or Block exists, the
  name is free).
- **The AI's own rules:** `WriteCommand.validateByAi` holds the AI, and only the AI (and `check_change`), to rules the user isn't:
  the pages it writes keep the screenplay's writing rules (`ScreenplayStyle`), which the user sees only as quiet hints, since
  the design is theirs. A write's result names the pages mentioning what it changed (`mentionedIn`), looked up before the change
  too, since a delete leaves nothing to find them by.
- **AI input:** an input that doesn't decode is rejected with `InputProblem`'s one sentence rather than kotlinx.serialization's
  message, and `check_change` runs a write tool's decoding and validation without applying it. A sealed type marked
  `@SummarizedSchema` (`BlockDefinition`) lists only its subclasses' names, which `get_block_catalog` describes once.
- **Addressing:** types and Scenes by name, Events, Blocks, and texts by index, Scene Actors by ID. The Event, Block, and Property
  commands take either kind of type (`actorType` names an Actor type or a Manager type).
- **Renames follow everywhere:** Blocks, collision Events, placeholders, texts, Kotlin Blocks and Kotlin UI (`KotlinIdentifiers`,
  which knows no grammar, so a local variable of the same name is renamed too; the import lines a Kotlin Block or Kotlin UI
  starts with are left alone), the Actors of every Scene, a Property's saved values in Scenes, and the screenplay's mentions
  and images of every file a rename or move moves (`withMentionsFollowed`, which also writes a moved page's or document's own
  links again from where it is now; undo moves back, then writes the files back byte for byte with `RestoreMarkdown`, so a
  stale link isn't turned into another). Each kind follows its own rename code, and `RenamingFollowsReferencesTest` holds them
  to what `ProjectReferences` finds (every `RelationKind`), so a rename can't miss what a delete sees. Removing a Property keeps its Scene values (undo brings them back); adding one drops a leftover value of another
  kind, which wouldn't load; a Property's kind can't change. Renaming a Property renames it through the family of the Actor type that
  declares it, and is refused on one that only inherits it; adding a Property or a parent is refused when a descendant has one of
  that name of another kind.
- **Deletes clean up what refers to them** (`deleteWithUsages`): a Timer, which exists only while its Events and Blocks
  name it, is deleted by that cleanup alone, across every type sharing it (`timerSharersOf`, one subject whichever of them
  it is asked through); deleting an Actor type, Manager type, Scene, or Property
  removes its type's Actors from every Scene and the Blocks and Events that only mean something with it (an if Block with its
  section), draws a deleted sprite's Actor types as their shapes and clears a deleted music from the Scenes playing it, gives
  each page showing a deleted sprite a copy of it next to it (`KeepPageImages`), turns placeholders of its Properties and the pages' mentions of it into text (`UnlinkMentions`; documents are the user's,
  so theirs are left, as renames leave them), and
  leaves Kotlin alone (listed instead), as one undo step that brings it all back byte for byte (`ReplaceTypes`, `ReplaceScenes`,
  internal like `CompositeCommand`). Deleting a page turns the links to it into text the same way, and the AI still hears which
  pages mentioned what it deleted (`mentionedIn`), since their prose may still describe it. What it touches comes
  from `ProjectReferences`, which `check_change`, the AI's result (`Applied.details`), and the confirmation every delete from the UI waits for (`WorkspaceUiState.dispatch`) read too, so
  none sees less than the delete does. What still dangles after a change outside Creator (a file deleted in a terminal, a
  git checkout) shows where it is: on the Block's card (`isMissingChoice`), in the health checks (`MissingReferenceChecks`),
  and once in the console (`MissingReferencesReport`, only for what starts dangling within seconds of a change outside
  Creator, so neither the types loading at opening nor a placeholder typed halfway is reported as deleted). Undoing a
  creation deletes only the thing (`keepsUsages`), leaving what referred to it before as it was.
- **Batches** (`place_actors`, `remove_actors`, `restore_actors`, `set_actors_values`, `set_actors_state`) are one undo step each,
  and name no Actor twice and at least one. Paged reads (`read_scene`, and the listings through `folderPage`) keep large games readable.

## Project files

- **Format versions** (`project/ProjectFormat`): `.kubriko/project.json` records one `formatVersion` for all of a project's Creator
  files, with the `minimumCreatorVersion` that raised it. It is raised by any change to what Creator persists in `.kubriko/` type
  files: a new or renamed field, a new Block, Event, Condition, or Property kind, a new enum value, a changed default, or a new
  meaning for an existing field (any change an older Creator would read and then handle wrongly). A raise that needs no rewrite is
  an empty `FormatMigration { }` whose KDoc names what it added, so an older Creator opens the project read-only with the version
  it needs instead of dropping fields; type files are still read ignoring unknown keys. `PersistedShapeTest` pins the persisted
  shape per format version (`persisted-shape-<version>.txt`; never edit a released version's file). `FormatFixturesTest` pins the files as written, with one fixture project per version (`core/src/test/resources/formats/v<version>`): a raise copies the newest folder to a new one and edits the copy to show what it adds, a released version's folder is never edited, and an older fixture is read through `ProjectFormat.prepare()`. A name added to
  `PropertyNaming`'s reserved names makes existing files with such a Property unreadable, so it needs a raise whose migration
  renames the Property with `withPropertyReplaced` (`BlockTypeEdits`), which Blocks, placeholders, and Kotlin follow.
  Each raise is a `FormatMigration` in `ProjectFormat.MIGRATIONS`, applied at opening and when `project.json` changes, recording the
  version after each, so one that fails is carried on next time. A project whose version is newer opens read-only: every
  `WriteCommand` is rejected with the same message (`check_change` too), and nothing regenerates code, syncs the build files, or
  offers an upgrade, while browsing, building, and playing still work.
- **Block type files:** `BlockTypesController` writes changes in batches, 100 ms after the last, with one regeneration and build file
  sync per batch. A write that fails stays queued with everything after it, is retried with the next change or after 2, 10, then every
  30 seconds, and is named in the console once; nothing is regenerated meanwhile. A type saved again keeps its first save's place,
  and a delete followed by a save of the same name becomes a replacing save (`isReplacing`, the same files as both) in the delete's place. A definition file that can't be read, holds a type named other than the file or a name another file holds too
  (ignoring case), or has a Property whose name can't be one (`PropertyNaming`) is left out and reported by path; nothing is regenerated or synced until it is fixed, its name stays taken, and
  `BlockTypeStore` never writes over or deletes it.
- **Template upgrades** (`template/ProjectUpgrader`): template files belong to the user, so an upgrade is shown as a diff, offers to
  commit first, and applies only to a file that still looks like the template's; others are listed with what to add by hand, and a
  file that already has the change counts as done. A change's `after` is null for a file it deletes. A new template version is a
  `TemplateStep` per file it changes, added to `TemplateSteps.FILES`; the version number follows from them. Template version 5 sets
  `KubrikoWiring.isLoggingEnabled` from a property only the play session sets, so Log Blocks cost nothing in a released game.
- **Kubriko dependencies** (`project/KubrikoBuildFiles`): after every regeneration of an opted-in project, its build depends on
  exactly the Kubriko libraries its sources name (the engine and `tool-creator-api` always), its `kubriko` version is raised to
  Creator's (a newer one is kept and reported once), and the Maven Local block and the audio plugin's JitPack block come and go
  with what is needed. A catalog entry stays while anything else in the build file refers to it, libraries Creator doesn't know
  are never touched, and a file without the template's shape is left alone entirely. Projects that aren't opted in are never changed. A project is never moved to a Kubriko version that can't be
  downloaded from where it looks (`template/KubrikoAvailability`: Maven Local for a development build, Maven Central otherwise),
  which a Creator started without its libraries would otherwise do to every project it opens; it keeps its version, the console
  says why, and a new project isn't created at all.
- **New projects** (`template/ProjectGenerator`): the name is the game's, shown to players, so it may hold any letters
  (`Kalandjáték`); its folder, Gradle name, and suggested package are folded to a to z (`ProjectNaming`: `kalandjatek`), so
  paths on every system, the web build's file names, and the code stay plain. A creation that fails removes what it wrote
  and comes back to the form as it was filled in (`AppScreen.NewProject.form`, not the request, which drops the template behind an idea), saying why in
  the user's terms (`CreationFailure`: no network, a full disk, or a folder Creator may not write in, told from the causes
  by class name, since the Gradle daemon's reach the Tooling API as placeholders, and blaming a folder only when a path
  names it) before the root cause's own message. A location Creator may not write in (`Files.isWritable`, which reads
  Windows' access lists) is refused before anything is written, and Maven Central answering with an error is told apart
  from no network.
  Cancelling a creation interrupts its thread (`runInterruptible`, so `ProjectCreator` stays a plain blocking call): the
  wrapper download is cancelled through its token and waited for until Gradle stopped, at most 30 seconds, so a Gradle
  that never confirms can't hold everything after it, what was written is removed, and the next opening or creation
  waits for that without a time limit (the previous operation, since it may remove where this one writes), quitting
  within its own. A project finished just as it was cancelled is removed too, a folder that was there before kept.
- **Tooling API operations** report to a `ToolingOutcome`, waited for from a coroutine or a thread: a cancelled or
  interrupted wait cancels the operation and returns only once Gradle stopped it, so the daemon is free for the next.
- **Scenes** (`scenes/SceneStore`) are written with one Actor per line, so they diff and merge. Each entry's `id` is ignored by
  Kubriko's serialization (`ignoreUnknownKeys`), so games load the files unchanged; an Actor without an ID, or with an earlier one's,
  gets one from its index, the same every time, so a Scene changes only when a command writes it. Undoing a removal brings back
  the same ID. Placing works for block-built Actor types only, whose state is known without loading classes. A Scene's settings
  (bounds, background, camera start, music) are an Actor (`SceneSettings`, `Unique` and `Editable`), not a file of their own, so
  they are placed, edited, and undone like any other; a Scene without one keeps the screen's edges. `SceneStore` knows the Scene
  names from its last `refresh` (each resource scan) and its own writes, so validating on the main thread reads nothing; what
  writes acts on the folder as it is (`scanNames`), so `create_scene` and `rename_scene` don't overwrite a Scene made outside
  Creator since, and type renames reach it.
- **Folders** are real directories. Names stay unique across folders, since generated code is flat in one package; a Scene is
  named by its path (`levels/1-1`). `delete_folder` deletes what a folder holds through each thing's own delete (`contentDeletesOf`:
  an Actor type's descendants first, a page with its own images), then its folders deepest first, as one step; its
  confirmation, `check_change`, and the AI's result list the contents (a page's images too) and only the usages outside
  the folder (`RelationSubject.Folder`), and a type in it with a child outside refuses it. Its deletes share one read of
  the hand-written Kotlin (`withKotlinFilesHeld`), which no delete rewrites. A folder the sidebar shows empty is deleted
  without asking, as the folder alone (`keepsContents`), so what was put in it since is refused rather than taken; and
  restoring a folder accepts one still on disk, since a file nothing lists keeps its directory. A Scene file that can't be moved
  (Windows refuses while the game has it open) fails the rename before anything else changes, and a folder rename moves the Scenes
  it moved back.
- **Sounds' and music's folders** are the real ones of the game's own `files` folders, every source set's together
  (`AudioFolders`), named from `files` (`sounds/enemies`), since the game reads audio by its path; a folder holding only
  the other kind's files, or inside one that does, isn't the group's, and the Scenes folder is no one's. The sidebar
  hides the folder new files go to (`sounds`) while it holds everything the group shows (`FolderTreeGroupState.root`).
  Validating reads them from `AudioFolderIndex` (the last scan and Creator's own changes since), never from the disk.
  Renaming one moves its files, finding them on disk (its undo comes before the next scan), and follows each as
  `rename_resource` would; deleting one deletes each file through `delete_resource`, both kinds, and its last step
  looks for audio on disk rather than in the scan the deletes before it left behind.
- **Sprites' folders** are virtual (`SpriteFolderStore`), since Compose resources make accessors only of the images right
  in `drawable`: moving a sprite between them (`move_sprite_to_folder`) or renaming one changes only `.kubriko/sprites`,
  never the image's path, so nothing that uses it follows. A sprite's file goes with its renames and deletes (`delete_resource`
  takes it into the same `ReplaceFiles` step, so undo puts it back), and holds how the sprite is cut into frames. A sheet
  that doesn't fit the image is refused when it is set, which reads the image's size off the main thread.

## Builds and playing

- **Opening** never waits for Gradle: it reads the build files, and asks Gradle for the desktop modules in the background; the first
  build waits for that. The workspace builds the classpath and loads the project's classes right away, so the Gradle daemon is warm
  and the first Scene opens without a build, then compiles one generated file (`CreatorTaskRunner.warmUpCompiler`), since Kotlin's
  compile daemon starts only when something needs compiling and a cold one made the first change seconds slower. The warm-up is
  silent and never a failed build.
- **One build at a time:** builds of the classpath never overlap, and a play session waits for the running one (`classpathBuildLock`),
  since a build started while another runs needs a daemon of its own; a cancelled build returns only once Gradle has stopped it.
- **The init script** (`kubriko-creator-session.init.gradle.kts`) adds Creator's tasks to every Compose desktop module without
  touching the project. Every build's arguments come from `SessionInitScript.gradleArguments`, and the script and the classpath
  file keep stable paths, since Gradle reuses a configuration cache only for the same init scripts and `-P` values; the script
  works with the configuration cache. The classpath lists the folders the module's jar is packed from in place of the jar, so the
  jar isn't packed, and the versions its runtime classpath resolved (`library.` lines).
- **Incremental builds** (`IncrementalCompilationPolicy`): common code, where generated code lives, compiles incrementally only
  through Kotlin's `enableUnsafeOptimizationsForMultiplatform` (KT-62686: otherwise any change to it compiles everything), except in
  projects whose sources declare an `expect`; the build after a failed one, a Reload, or classes that couldn't be loaded runs with
  `--rerun-tasks`. Projects' own builds keep Kotlin's default.
- **Playing** builds the classpath into a file of its own (`SessionInitScript.playClasspathFileOf`), so the Scene editor's builds
  don't touch it, and Creator starts the game process itself (`GameProcess`), so no Gradle daemon is held while the game runs. The
  classpath goes through an `@argfile`, since Windows caps a command line at 32,767 characters, and the game runs from a copy of the
  class files, since the Scene editor rebuilds them meanwhile. A build without `launch.` lines falls back to the session task.
- **Hot reload** (`GameReloader`): a change while playing builds and compares the built classes' `ClassShape`s with the game's. Only
  code changes reload; a changed superclass, interface, instance field, or a class with static state (which a reload would leave
  null or reset) restarts the game from the same Scene. A game waits for the one before it to end, since Hot Reload's pid file is
  shared.
- **The game's module** (`GameModuleChoice`) is the desktop module (one with a `run` task) that every build, play session,
  and the Scene editor's classes come from: the first in Gradle's order, or the one chosen in the toolbar, which offers the
  choice only when there are several (Tesselar's game beside its model and region editors) and nothing runs. The choice is
  this computer's (`.kubriko/local/module.json`); choosing builds the Scene editor's classes again, and a change to a build file
  or the root settings file asks Gradle for the modules again before the next build rather than at once, since that configures
  the whole build.
- **Where to play** (`PlayTargetChoice`) is this computer's: the choice is kept in `.kubriko/local/play.json`, and one that
  isn't there now (a device unplugged) stays chosen while Play uses the desktop. `play` and `start_game` take the desktop
  when no `target` is given, not the user's choice, so the same call always plays in the same place. The web, Android, and
  iOS entry points read Play from this Scene and the Log Blocks switch only in debug builds (`PlaySwitchesUpgrade`); the
  web has no debug build, so it reads them only on a page served from this computer (a loopback host), as Creator's is.
- **The web** (`WebPlayer`, `WebGameServer`): playing builds the development distribution rather than running webpack's
  dev server, which would hold a Gradle daemon while the game is open, and serves it from Creator on a loopback port, with
  a script added to `index.html` that loads the page again when the event stream says so; a change while it plays builds it
  again and says so, which is the web's stand-in for hot reload. Compose for Web draws no text at all when the browser's language
  isn't a language tag, as one started under the POSIX locale reports it (`en-US@posix`), so the web `Main.kt` hands Compose
  the languages without such a modifier first (`WebLocaleUpgrade`). Every entry point calls `Game()` without sizing it
  (`GameSizeUpgrade`): a size handed to `KubrikoViewport` keeps a fixed aspect ratio from letterboxing the viewport and the
  Manager types' texts from drawing, and the viewport fills the screen by itself for the other aspect ratios. The browser's console isn't Creator's, so the console says
  where the game is and its logs are. A game played anywhere but the desktop is `SessionController.playElsewhere`, whose
  `SessionState` names the target, and the game tools refuse it (`GameStep.Elsewhere`), since only the desktop's session
  answers them. `-Dkubriko.creator.browser=none` keeps the browser closed (the end-to-end scenarios load the page in a
  headless Chrome of their own).
- **Android** (`AndroidPlayer`): devices and emulators come from the SDK the project builds with, an emulator named by its
  virtual device, since its serial changes with every start. One that isn't running is started while
  `:androidApp:assembleDebug` builds, detached and writing to a file rather than a pipe, so it outlives the game and Creator.
  The application ID is the build's (`output-metadata.json`), whatever the build file sets it from. The console shows what
  the app's process prints and its errors (`logcat --pid`), never the rest of the device's log; Stop is `am force-stop`, and
  a change while it plays reaches it the next time it is played. `adb` and `emulator` run through `ToolProcesses`, which
  tests replace with `FakeToolProcesses`, replaying what the tools printed.
- **iOS simulators** (`IosSimulatorPlayer`): listed only once the iOS target is on and the Mac has Xcode. Xcode builds the
  app (its Gradle build phase builds the game's framework) into `build/kubriko-creator/ios-simulator` while
  `simctl bootstatus -b` boots the simulator, which the Simulator app then shows (DeviceHub, which takes its place in Xcode 27,
  when it isn't there). Xcode's build waits for the writes and holds
  the classpath build lock (`CreatorTaskRunner.runOutside`), since its build phase runs Gradle on the same project. The
  switches reach the app as launch arguments, which `NSUserDefaults` reads as property lists, so a Scene's name is quoted. `simctl launch --console-pty` prints what the app prints (a pty, so its output
  isn't held back in a buffer), run inside `script`, since `simctl` itself holds back what it prints into a pipe until the
  app ends, and names the app's process once it runs, sometimes after the app's first lines, so those are held
  until then. Stop is `simctl terminate`. New simulators are made in Creator's own dialog (`IosSimulatorSetup`, through
  `simctl create`): only iPhones and iPads an installed iOS version runs are offered, the newest first by the oldest version
  they run, since `simctl`'s own order changes between Xcode versions, and the iPod touch calls itself an iPhone. A name any
  iOS simulator has is refused, since Play's menu would show two alike, and `create` runs in the workspace's scope rather
  than the dialog's, since stopping it halfway may leave a simulator that is never chosen. The new simulator is chosen before Play's places are looked for again, so whichever
  look ends last keeps it chosen.
- **Logs elsewhere:** Kubriko's `Logger` only keeps its entries, which only the desktop's session reports, so a Log Block also
  prints what it logs (on one line, as `[source] message`), and the Scene loader its failures: Creator reads them back from a
  device's output (`PrintedLogs`), and the browser shows them in its console. The print is generated into each Block rather
  than a function of `KubrikoWiring`, which a project that changed it by hand would lack.
- **The session** (`tool-creator-session`) runs at the project's Kubriko version and reports to `SessionServer` (which takes only
  the first connection that says Hello after each launch): a session of any version runs, and a command it is too old for is
  refused (`SessionController.request` takes the version a command needs and answers `GameAnswer.Unsupported`, which the game
  commands reject with `error_game_unsupported`).

## Scene editing

Creator's own Scene editor (the Scene files in core's `scenes/`, the runtime in `:creator:scene-runtime`'s `sceneRuntime/`, and `ui/scenes/`), never a window of its own and not `tools/scene-editor`.

- It works only when the project's Kubriko version equals Creator's and its Compose and Kotlin are no newer major or minor version
  (`SharedLibraries`, which gates the `androidx.compose` modules holding Compose's runtime too; another newer shared library loads with a console warning): the project's classes load into a class loader
  whose parent is Creator's (Kubriko, Compose, and Kotlin shared, so every published Kubriko module a project could use is on Creator's classpath through `scene-runtime/build.gradle.kts`; the project's jars are copied and the class files of its folders
  read before anything loads, so a rebuild can replace them while they are in use), and `SceneView` creates the Actors in a Kubriko
  instance that never runs. Another version gets a notice instead (`SceneEditingState.Unsupported`), and a failed build or entry point a
  notice with Try again (`SceneEditingState.Failed`).
- Creating a `SceneView` runs the project's code, so it happens off the main thread, and what that code throws (a
  `NoClassDefFoundError` too) fails the Scene, or leaves the older classes shown as outdated. A load cancelled midway closes what
  it loaded; released classes are closed a few seconds later on `releaseScope`, which outlives the workspace.
- A Scene file that can't be read opens with `InvalidSceneNotice`, since no command can change it; Actors that couldn't be created
  are marked, and can still be removed.
- Compose resources are read through Creator's class loader, which can't see the project's, so `SpriteLoadingEntryPoint` swaps in a
  `SpriteManager` whose `ProjectSpriteLoader` reads sprites from disk, matching resources by ID (`drawable:<accessor>`, which is what
  Compose resources are equal by).
- Every edit is a command. After each, the Scene is read again (`SceneStore` parses it in the background while the project builds,
  so the first command doesn't parse on the main thread) and `ScenePatch` applies only what changed. Dragging moves an Actor live
  and dispatches `move_actor` when let go, only for an Actor whose state saves a position (`canDragActor`); any other press pans.
- A placed Actor is selected, and so is one the user's undo or redo brings back (`CommandExecutor.broughtBack`, which an
  AI turn's undo leaves alone), since the selection is the editor's own state, which no command restores. `SceneSelection`
  selects it once the Scene shows it, and drops it if the next load doesn't (removed again meanwhile) or it is the Scene
  settings, so a stale one never takes the selection later.
- Loaded classes are kept across Scenes until a type or a source changes (`markOutdated`), Reload, or the workspace closes. A change
  while a Scene is open builds again in the background, and a change during that build builds once more after it rather than
  cancelling it; the new `SceneView` creates its Actors while the old one is still shown, then takes over its camera. A failed
  build keeps the old classes until a build works.
- Until newer classes are swapped in, Actors of block-built types whose look (`ActorTypeLook`) changed are drawn by Creator itself
  (`PreviewActor`, through the same batched look as their generated class), following the types in memory, so a change shows
  before it is even written.
- The editor's instance ticks without the focus (`shouldPauseOnFocusLoss = false`), since a Scene swapped in while Creator is in
  the background would otherwise draw nothing until it is focused.

## AI

- **One AI panel** holds both ways of working with AI (an outside client over MCP, and the in-app assistant), since nobody uses
  them at once. The assistant is the recommended way and the panel's own view, which it always opens on; the user's own tool
  and the map ("What the AI knows about your game") are in the panel's menu, both leading back to it. The map is computed
  once per burst of changes, off the main thread, and only while it is shown (`GameMapState`), its findings
  being the editors' (`ProjectHealth`), worked out once for both.
- **AI features off** (`CreatorSettingsController.isAiEnabled`) is the one check: the MCP server stays stopped, the assistant starts,
  fetches, and installs nothing (`AssistantHost` holds its check), and the UI (through `LocalIsAiEnabled`) shows no AI surface. Every new AI surface reads it and
  joins `AiFeaturesOffTest`. Commands, undo, and the game notes stay.
- **Writing descriptions:** the AI button at the end of a description (`WriteDescriptionButton`) sends its prompt to the
  conversation the assistant shows and opens it, rather than asking a hidden one, so the change is seen and reviewed like
  any other turn; it shows only while that conversation takes prompts (`LocalDescriptionWriter`).
- **Chips** (`AttentionChip`, what goes with the next message): what the center shows beyond the selection reaches the AI
  panel through `EditorAttention`, which the editors report to, since only they know it (the Event shown, and a picture of
  the open Scene, recorded from its viewport on the editor's background only for the frame after one is asked for, on the
  thread that draws, since a layer recorded every frame would cost every frame and race the capture). A picture costs far more than a link, so the
  Scene's is off until the user turns it on.
- **Following the AI** goes only to what a step changes (`AiStep.changesProject`): the Actor type, Manager type, or Scene,
  lighting up its Event or Block, or the Scene's Actors it changed (`CommandTarget.Scene.actorIds`; a placed Actor's ID
  is known only once it is placed, so a change can name what it was about through `Applied.target`). Steps are numbered
  per project, going on from the last session's (`AiStepNumbering`, in `.kubriko/local/`). Each followed step stays at least the settings' delay (`AiStepPacing`), so quick steps
  don't flash. Following, that delay, and Keep and Undo after a turn are Creator settings (`LocalAiActivitySettings`),
  and the activity bar's Follow switch writes the same setting.
- **The MCP server** (`mcp/McpToolServer`) listens on `127.0.0.1` only; every request needs the token (a header, or `?token=` for
  clients that can't send one, compared in constant time), and the SDK's DNS rebinding protection limits hosts to localhost. Tool calls run `CommandExecutor.call` on the main dispatcher, so AI
  changes enter the same undo history; rejections and unexpected exceptions are `isError` results in plain words. It stays even
  though the panel's agents speak ACP: ACP has no way of its own to hand an agent custom tools, and outside clients (Claude Code
  in a terminal, Claude Desktop, Cursor) use it too. The panel starts it on demand, whatever the switch says.
- **One-click clients** (`mcp/clients`) run through the user's shell as an interactive login shell (`ShellProcessRunner`), since
  an app started from the Finder lacks the shell's PATH and aliases such as `claude-personal` live in `.zshrc`. A client counts as
  connected only when its output names the server, since a wrapper alias can succeed without doing anything. Editing a client's
  configuration file (`McpConfigFile`) changes only Creator's entry, keeps the rest as it was, and never writes a file that isn't
  valid JSON.
- **Game tools** (`start_game`, `press_keys`, `take_screenshot`, and others) bring the game window to the front before every step,
  since Kubriko pauses unfocused games, and screenshots come from the game's own `SkiaLayer`, so they need no OS permission; `start_game` waits until the game has drawn Actors, where the session and the entry point
  can report them. What depends on the game's timing is one session command, since the game keeps running between
  commands: `press_keys` steps are played by one `PressKeySteps`, and `take_screenshot`'s `actorType` crop locates the
  Actors (the type's and its descendants' classes) in the same moment as it captures (`CaptureActors`), on the thread the
  game ticks on, since a body computes its bounds as they are read. Sessions older than protocol 4 play the steps one at a
  time, and get the whole view with a note in place of the crop.
- **Context:** every conversation starts with the fixed system prompt, then the project digest and the game's notes, so the cached
  prefix stays the same. A conversation's reads remember what they returned (`ConversationReads`, forgotten with the conversation
  and when a turn is cancelled) and answer again with what changed (`ResultDelta`; `full: true` for the whole); outside clients
  always get it whole. Every delta tells the agent to read again with `full: true` when it no longer has the earlier result, since
  a subagent (Claude Agent's game explorer) reads through the same address and compaction drops old results. Before each prompt
  the conversation hears what anyone else changed since its last one (`ChangesSummary`). What the AI reads is never a UI string:
  changes are named by their tool names (`ChangesSummary`, `writeSummary`), and the UI maps names and model enums to its strings
  (`ui/CommandTitles.kt`, `ui/actorTypes/KindTitles.kt`). The AI reads `DomainText.english`, never a UI string; a new error
  or status is an entry of its owner's enum with its English, and a string in `strings.xml` mapped in `ui/texts` (which
  `ui/DomainTexts.kt` dispatches to).
- **Keeping the project organized** is every agent's standing job, never a request of its own (the user's priority: no
  technical debt for the user to inherit): `McpToolServer.INSTRUCTIONS`, which outside clients hear too, tells them to put
  everything in folders named for its purpose once a group holds more than a handful, file new things there, split folders
  that grow, and tidy a cluttered group in the same plan. A new kind of thing with folders gets its move tool named there.
- **The writers' room** is a conversation with rules of its own (`AssistantSystemPrompt.WRITERS_ROOM`, after the project's
  context, so the other conversations' cached prefix stays the same), started with a prompt (`startWritersRoom`), not a wizard:
  the agent leads, and everything goes through the usual tools, Keep, and Undo. Its questions go through `ask_user`, an
  `ActionCommand` that waits in `UserQuestions` without holding up changes, until the user answers it or its turn is
  stopped, which takes the question away. A question shows as a card under the call that asked it in its conversation
  (`questionsOf`; an outside client's at the top of the AI panel). An agent may stop waiting without telling the server (Claude
  Code's timeout sends no cancellation), so a question whose call or turn ended (`isLate`) is answered as the user's next
  message, never into a call nobody reads.
- **Claude's names and sign-in:** Creator never offers a Claude.ai login of its own, routes requests through a user's plan, or
  touches Claude.ai credentials; Claude Code signs in by itself, through Anthropic's own flow, and runs unmodified from the registry's
  adapter. Creator may say in plain text that it runs Claude Code, but no feature carries Claude Code's or Anthropic's names or logos:
  the panel is "Assistant", and the agent "Claude Agent", as the registry names it.
- **Guards:** `EvalSetTest` replays the calls a real agent made for realistic requests (recorded by `EvalSetLiveTest`, never written
  by hand, since what models really call is what breaks), each request checked by what the project became, so a tool change that
  would break them fails without tokens, and `TokenBudgetTest` keeps the tool list, the tools loaded from the start, the digest, and the big reads under
  budgets set just above what they cost.
- **Round trips cost most:** every call re-reads the whole conversation, so the tools a session uses most are loaded from the
  start (`CommandRegistry.alwaysLoaded`, sent as `anthropic/alwaysLoad`, since a schema the model looks up stays in the
  conversation anyway), what repeats across schemas is said once (`@SummarizedSchema` on a sealed type or an enum a read tool
  describes), and a tool used in long runs takes many at once (`press_keys` steps with a screenshot, `replace_blocks`).

## Settings

- **Creator settings** (`settings/`) are this user's on this computer; an unreadable file means the defaults. The side panels'
  widths and the console's height are dragged at their edges and kept per panel (`panelWidths`, by an ID that never changes),
  saved once per drag, and clamped to the window when drawn, so the panels of a row never take more than half of it and the
  console never more than most of the column.
- **Project settings** live in the project where its build reads them, so it builds the same without Creator. The game's identity and
  settings are `game.*` keys in `gradle.properties`, edited line by line (`GradleProperties`: other lines untouched, values escaped as
  Gradle reads the ISO-8859-1 file, a key set twice read and written at its last line, as Gradle reads it), and every change to them
  is one write (`GradlePropertiesFile.update`); a project whose build doesn't read a group (`GameIdentity.isReadByBuild`,
  `GameSettings.isReadByBuild`) is offered the template upgrade.
- The game's settings reach the game through the build: template version 10's block turns them into a generated `GameSettings`
  object that `Game.kt` starts from, so `GameSettings.of` parses exactly as `upgrades/GameSettings-10.gradle.kts.template` does
  (the viewport's width and height only for the aspect ratios that use them), and the two change together. A value the build
  can't read shows its default and is written again when chosen. Renaming a Scene, or its folder, follows the first Scene;
  deleting the first Scene is refused, but undoing its creation isn't.
- **Targets** (`template/GameTargets`) are turned on and off as a confirmed diff of the build files: `set_target` makes only the
  change whose `changeId` was shown, and its undo turns the target back only when that restores every file exactly
  (`isReversible`), since turning off drops whole blocks with what the user added to them; otherwise it clears the history.
  Only the template's shape is edited, a file that lost it is left to the user, turning one off removes it from the build
  and leaves its files, and a catalog entry goes only if it is still as turning Android on added it and nothing uses it. The desktop is always on, since playing and the Scene editor run through it.
  Android is a library target of the game's module plus an `androidApp` module that packages it, since the Android Gradle
  plugin's application plugin can't share a module with Kotlin Multiplatform; turning it on needs the Android SDK and writes
  `sdk.dir` into `local.properties` when nothing else names it. iOS is a framework that the Xcode project in `iosApp/` builds
  through Gradle; the game's name and version reach the app through a plist Gradle writes and the last build phase copies in,
  but the bundle ID is a build setting Xcode reads before any phase runs, so Creator keeps the `.xcconfig` line.
  `:publishCreatorLibraries` publishes the Android libraries everywhere and the iOS ones only on a Mac, the one host that
  builds them.
- The libraries in use (`LibraryInUse`) are those the sources name, as the build sync decides them, each with what needs it
  (`PluginUse`, from which the generated Managers' plugins come too).

## UI

- Desktop-sized from the start, at Campfire's 0.85 interface scale, with the JetBrains Runtime title bar the content extends under;
  icons are vector drawables, and every string is in `strings.xml` with plain apostrophes. Only the UI reads `Res`: the model,
  commands, and workspace hand it `DomainText`s, worded by `DomainText.text`, and shared components such as `NewNameField` take
  a `FieldError` instead, since they sit below the commands.
- **Friendly, and told as a film:** Actor types are the cast, Manager types the crew, and Scenes the scenes being shot, never
  scenery. Each kind of thing keeps one `Accent` everywhere it shows (the cast, the crew, the Scenes, Events, and every Block
  category), and Play is green.
- Beginner-friendly by default: one Block category at a time, Blocks added by a click or a drag, and anything advanced (the package
  of a new project) under More options.
- **Calm and desktop-like:** soft, low-contrast colors (`CreatorColors`, never pure white or black text), compact controls
  (`CreatorTheme` turns off Material's 48dp touch targets, so a control takes only the room it draws), small corners, and
  dialogs of several groups as a list of pages beside the chosen one rather than one long scroll. A row shows its main action;
  the rest (remove, or a ⋮ `RowActionsButton` opening the same `RightClickMenu` as a right-click) fade in while it is pointed
  at or focused, never only in the right-click menu. Every menu is a `RightClickMenu` (compact rows, as tall as the window lets it, scrolling to the popover's
  edges; `MenuButton`, `MenuIconButton`, and `RowActionsButton` open one under themselves through a `MenuAnchor`), a list in a popup that its height still cuts off fades out at that edge (`scrollEdgeFades`), so it never looks
  complete when it isn't, and every menu and popup looks like one (`MenuStyle`: brighter than any panel,
  with an edge and a shadow), so it never reads as part of the panel it opened from, and is held by the button it belongs
  to (`RowActionsButton`, or a `Box` with it), since a popup counts as one more child of the row it is in and would move
  its neighbours. A panel's background reaches its divider: a `PanelSplitter` is a one-pixel line with its grip laid over
  both neighbours. Settings that rarely change stay behind a button rather than on screen (the assistant's agent, account,
  configuration, and options are in its header's setup `Popover`, which grows to its widest row and wraps a label it
  still can't fit, since an account's email or a model's name is never to be cut off; the agent and configuration are also
  on Creator settings' Assistant page, beside the AI connection's port, token, and commands, both pages listed only while
  AI features are on). A clickable row's background and press feedback span its whole panel (a padded settings page's rows reach
  past the padding with `fullBleed`); only what is drawn as a card (a Block, a chip) keeps its own shape.
- **Nothing pops in:** panels, pages, notices, list rows, and dialogs move or fade with `CreatorMotion`'s durations. What
  replaces a live Scene preview fades in (`fadeInOnChange`) rather than crossfading, which would keep two previews running.
- **Usable at any window size:** what sits side by side shrinks together and then stacks (the empty workspace's cards), never
  squeezing only the last of them; the type editor folds its details, and then its palette, into tabs beside the Events
  (`BlockTypeEditorColumns`) before the Blocks lose their room, Block parameters wrap, and labels end in an ellipsis rather
  than wrapping a header.
- The workspace keeps the focus when nothing in it is focused, so Cmd/Ctrl+Z always reaches the undo history.
- The sidebar's groups, folders, and items all work alike: the same ⋮ and right-click menu (new item, new folder, rename,
  delete, as each allows), names typed in place as a field looking like the row it becomes, and an item or a folder dropped
  on any row moves into that row's folder (onto the header or an item outside every folder, out of them all; a folder
  never into itself, and with everything in it, through `move_folder`). Headers have no + of
  their own: the one + is the New menu beside the filter field, offering every kind the project can make (a document
  always, at the project's top, so a project without bands or documents can still start one), which opens the band
  the new thing goes in and scrolls its name field into view (`revealItem`, since a lazy list knows only what it laid out).
- A field above the sidebar's groups filters everything in it by name, showing the folders that hold matches opened; the folders
  the user collapsed are left as they were and come back once the filter is cleared.
- Opening a closed folder, Kotlin container, group, or module opens it with every folder in it closed, so a deep tree opens
  a level at a time, and a row holding folders offers Expand all and Collapse all while it is pointed at. What is open is
  kept by key (`SidebarKeys`) over an outline of everything as if it were all open (`SidebarOutline`), so a folder keeps its
  state while the rows around it come and go, and what is open in a folder follows it in every module whoever moves or
  renames it (undo, redo, and an AI too), told by the command before anything moves (`FolderMoves`), since the moved folder
  shows before any later notice would arrive. It is this computer's, kept per project (`SidebarFolderExpansion`,
  `.kubriko/local/sidebar.json`); since the types are read after the files, a kept choice about what isn't shown is
  forgotten only once the whole project was read. Those buttons are laid over the end of the row's name rather than given room
  of their own (`SidebarRowTitle`), so a long module or folder name isn't cut while nothing is pointed at. Every row of the
  sidebar's list moves with `sidebarItemMotion`, since a row that jumped among sliding ones looks broken, and the list
  starts with a row that never changes, so bands inserted above the first group keep the top in view.
- A project of several Gradle modules shows each module's groups under a band of its own, nested as the folders are
  (`app/desktop` inside `app`, shown even without a build file, since Gradle makes it a project too, but a build of its
  own such as `gradle/build-logic` at the top, inside no folder above it) and named by its
  folder's own name, never by a Gradle path starting with `:`; the branch holding the game's module (the toolbar's
  choice in a project that isn't opted in) comes first and is the only one open at first, and a project of one shows no band. A module is a folder with a build file (`ResourceScanner.moduleOf`), told by the files
  rather than asked from Gradle, so the tree never waits for a build. One whose build file names a Kubriko library
  (`ProjectModules`: a catalog library or bundle, or its coordinates, outside comments) lists every group, an empty one
  as only its faded header; any other lists what it has, and is left out when that is nothing.
  Modules are only read: the types and new Scenes stay in the modules Creator writes them to (`ProjectLayout`), whose
  groups alone add items and folders, while another module's Scenes still open, move between its own folders, and are
  deleted. New Scenes go where the generated code loads them from (`BlockTypeStore.scenePath`), even in a project
  whose other modules have Scenes, or that has none yet.
- The screenplay's pages are the whole project's, so the Screenplay group (first of the groups, `FolderGroup.PAGES`) is in
  the module holding the game only, and in a project that isn't opted in only once it has pages. The page editor
  (`PageEditor`) shows a page rendered and edits one chunk (lines between blank ones, `PageChunks`) at a time in place,
  inside the text around it as it was when the chunk opened (`PageEditorState`), so typing a blank line doesn't move the
  field; it saves the whole page through `edit_page` a moment after typing pauses, and a change from elsewhere while typing
  ends the typing.
- A page's links are relative Markdown links to the thing's own file (`PageLinks`), so they work on GitHub and in IntelliJ
  too; `Mention` tells what they name from the path alone, so a mention of something deleted still says what it was. A page
  may come from anyone, so only a web or mail address opens, after it is shown, and a project file Creator has no view of opens
  in the system's app only for kinds it can't run (`canOpenOutside`). Creator's own moves are recorded (`MovedFiles`) so the
  open page follows them, keeping what is being typed (`MarkdownIdentity`).
- A page's shape (a `# Title`, a one-sentence summary as its first paragraph, headed sections, and an optional `## Planned`
  list at the end, read by `PageText`) is checked by the health checks, never forced on the user; with AI features off, pages,
  mentions, and their checks all stay, being the editor's.
- While a turn offered for review (Keep and Undo after a turn) changed the open page, the page editor shows the page as a diff
  of its Markdown from before the turn, with the turn's Keep and Undo, instead of the page (`PageReview`); what it was is
  read back from the undo history (`pageBeforeTurn`), following the turn's moves, so nothing is copied while a turn runs. It
  counts only what the turn's Undo would undo (the conversation's changes newer than anyone else's), and none while a later
  turn of the conversation runs, so the diff is always what Undo brings back.
- The health checks (`ProjectHealth`, kept up to date only while the workspace collects them) show quietly where their
  subject is edited (`HealthHintsList` in the type, Scene, and page editors, a Property's in its type's), besides the
  Game map's list; only the screenplay's also mark the sidebar row they are about with a dot (`HealthHints`), since a
  dot on every row nothing goes to would be noise. Where a finding is shown and fixed is `HealthFindingPlace`, so the
  map and the editors never route one differently; a first Scene that doesn't exist keeps the game from starting, so it
  is a notice under the toolbar (`GameSettingsNotice`) rather than a hint. The unreached-Scene check leaves out the
  Scene the game starts with when its settings name one that exists, and says so; without one, the editors leave the
  check's findings to the map, since one of those Scenes is the first. The project's Markdown is
  read once per change to a page or document (`screenplayTexts`, keyed by their revisions, which every write Creator makes
  among the pages raises, images included), while what the pages link to is looked at afresh every time.
- Documents (every Markdown file outside `screenplay/`) have no group: each is a row in the band of the folder it is
  in (`SidebarModules`' `bandOf`: the nearest module, or folder holding only modules, above it), after the band's
  groups, a plain folder of them a folder row closed at first, so where a document lies stays visible. Every band,
  document folder, and document offers New document in its own place. Deleting one or adding it to the screenplay waits for its own
  confirmation (`DocumentChangeConfirmation`), since the file is the user's and has no usages to list.
- The sidebar's Kotlin files are read-only and grouped by what the code is, not where it lies: the shared source sets
  (`commonMain`, `main`) at the top, each platform's in a container with its Play menu icon (closed at first), and a folder
  for each package below the one all files share, merged with its only child folder while it holds nothing else (`ui.menu`).
  The package is the one each file declares (`KotlinPackages`), since Kotlin lets folders leave out the shared root package.
- What the center shows is one `WorkspaceSelection` (a type, a Scene being prepared, open, or failed, a page, a document, a sprite, or nothing),
  changed only through `SelectionController`, the one place that pairs selecting a type with closing the Scene. The workspace screen's
  own panels, dialogs, and the AI step the user went back to live in `WorkspaceUiState`, a plain class its tests reach without a UI.
- The sprite editor (`ui/sprites`) changes a sprite only through commands, so its steps are undone like any other and
  the AI can make each: a stroke, a shape, or a fill is one `draw_pixels` of the pixels it changed when the pointer is
  let go, and pixels lifted or pasted float until they are put down, as one step that clears where they came from.
  What it draws shows over the frame until the image is read again (`pendingPixels`), so nothing flickers back while
  the write is under way. Its `SpriteEditorState` lives as long as the workspace, so the colors, the tool, and what was
  copied go from one sprite to the next, while the view and the selection start over.
- The type editor's selected Event is its `ListItemKeys` key, not its index (`EventSelectionState`), so it stays on the same
  Event through undo, redo, and the AI's Event changes; an added Event is selected only once its add succeeded, found
  by its key in the type as it is at that moment (an add may wait behind an AI tool call that changes the Events).

## Library side

What Creator needs from the libraries reaches projects only through a Kubriko release, since projects resolve Kubriko from Maven
Central and opted-in ones use Creator's own version, so a Creator release follows each Kubriko release that changes its library side.
Nothing goes into a
published artifact before the Creator feature that uses it exists, and library changes Creator needs follow the public API rules in
the root `CLAUDE.md`.
