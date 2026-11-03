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
model and code generators (`src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/CLAUDE.md`) and the in-app assistant
(`src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/CLAUDE.md`).

## Running and testing

- `./gradlew :creator:run` runs against this repository's libraries however it is started: `settings.gradle.kts` turns on
  `creator.localKubriko` for any build that runs it and puts the Maven Local publish tasks first. That publishes the libraries as
  `<library.version>-creator` (so they never shadow a real release) and bundles that version and `mavenLocal()` into
  `creator.properties`, so projects use them. Packaged builds, and `-Pcreator.localKubriko=false`, bundle the released version.
- `./gradlew :creator:allTests` runs every module's unit tests (also on Windows and Linux in `Tests`); `:creator:<module>:test
  --tests …` runs one module's. Run the whole suite before every commit,
  not only the tests you touched: tests elsewhere catch leaks and races (a server left running, an exception escaping a scope),
  and Windows paths differ (write expected paths with `File`, not `/`).
- Opt-in tests, skipped without their switch:
  - `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=<version>` generates every template for real and builds it for
    desktop and web, with an Actor type and two Manager types using every Event and Block, and resolves the desktop runtime
    classpath as playing does (compiling alone misses a library whose repository is missing). For an unreleased
    `library.version`, run it after a `:creator:run` with `<library.version>-creator`. `Tests` runs it nightly (and on a
    manual dispatch) in its `publish-check` job, against that job's `0.0.0-check` publish.
  - `KUBRIKO_CREATOR_BENCHMARK=true` times a 200-type project without Gradle (`CreatorBenchmarkTest`) and, with
    `KUBRIKO_CREATOR_VERSION`, builds it for real (`CreatorBuildBenchmarkTest`; more Gradle arguments through
    `KUBRIKO_CREATOR_BENCHMARK_ARGUMENTS`).
  - `KUBRIKO_CREATOR_CLAUDE_AGENT=true` (`KUBRIKO_CREATOR_CLAUDE_CONFIG_DIR` picks a Claude Code configuration) runs the real
    Claude Agent on an existing login: `ClaudeAgentLiveTest`, and `AssistantLiveScenariosTest`'s headless conversations on the
    Breakout fixture, which check what the project became, not the words. They cost a few requests on the user's subscription,
    so run them (`--tests "*AssistantLiveScenariosTest*" --rerun`) when the assistant's behaviour changed; `EvalSetTest`
    replays recorded tool calls for free.
- Controllers are tested against fakes in `testing/` (`FakeProjectBuild`, `FakeSessionChannel`, `FakeBlockTypeRepository`, `FakeGitRepository`),
  file code against real temporary folders, git against real temporary repositories (JGit needs no installed git), and sockets,
  the MCP server, and shell commands for real on localhost. Tests on real threads use `testing/blockingTest`, and assert that
  nothing happened only after `awaitIdle` on the dispatchers they gave the code under test, never after a fixed delay.
  Each module with resources has its own `strings.xml` and `packageOfResClass`, and its own `StringResourcesTest`, which fails on a
  string or icon its sources don't use, and on escaped apostrophes. Shared fixtures live in
  `testing/` (`testDirectory()`, and the fakes above); a fake of an interface with a real implementation runs the same
  contract test as the real one (`BlockTypeRepositoryContract`, `GitRepositoryContract`). A collaborator of
  `WorkspaceController` is tested on its own, and `WorkspaceControllerTest` keeps only cases about the wiring between them.
- `PackageLayeringTest` keeps each package in the layer of the Gradle module it belongs to (lowest first: foundation,
  ui-components, core, gradle-session, git, template, commands, ai, scene-runtime, workspace, the app); a new package gets a
  layer, and an import may only run downward.
- UI is checked offscreen: `uiTest { showing { … }; saveSnapshot("name") }` (`testing/UiSnapshots.kt` in `:creator:ui-components`'
  test fixtures) renders a composable in the app's theme and interface scale without a window, asserts its texts and clicks,
  and saves a PNG under the module's `build/ui-snapshots`.
  Every new UI gets one. What needs the real app (windows, focus, live agents) is checked with a window-only screenshot
  (`screencapture -l <window id>`) before it is called done; `plan/HANDOVER.md` → Working with the live app says how.

## Modules

Creator is being split into Gradle modules, in `PackageLayeringTest`'s layer order: foundation, ui-components, core,
gradle-session, git, template, commands, ai, scene-runtime, workspace, and the app (`:creator`, which keeps the rest until its
module is extracted). Extracted so far: `:creator:foundation` (`foundation`, `console`) and `:creator:ui-components`
(`ui.components`, `ui.theme`, with their own resources and the UI test fixtures). Each child module applies `kubriko-creator-module`, whose explicit API mode makes what another module uses `public` and leaves the rest `internal`.
Test helpers other modules share live in a module's `src/testFixtures` (reached through
`testImplementation(testFixtures(projects.creator.<module>))`; foundation's hold the Creator-independent ones of `testing/`
and `creatorSourceRoots()` for tests that read the sources), and `PackageLayeringTest` keeps every file in its module's layer.

## Definition of done

Besides its tests passing, a change:

- Reuses the shared building blocks instead of writing its own: `writeTextAtomically` for every file Creator writes,
  `JsonFileStore` for every settings file it keeps (it sets a file it can't decode aside as `.bak` instead of replacing it;
  a change built from what the file holds goes through `update`, which reads it once),
  `withLineBreaksOf` (`project/LineBreaks.kt`) for every file it edits, `ProjectTree.walk` for every walk through a
  project, `LatestRequests` for background reads whose results may land out of order; in the UI `BufferedTextField`
  (`ListItemKeys` for the rows holding one, so a row keeps its text when one above it goes), `LabeledSwitch`, `CreatorDialog`, `SettingsSection`, `DragPreviewChip`, `SearchableChoiceButton` for every
  choice of the project's contents (types, Scenes, sprites, audio files, Properties), which grow without bound (it filters once
  there are more than eight; `KeyChoiceButton` ranks keys by the same `matchingOptions`), and `PanelWidths` with `sidePanel` and
  `PanelSplitter` for every side panel; in command tests
  `commands/CommandTesting.kt` (`commandContext`, `assertRoundTrip`); in every test `testing/` (`testDirectory()`, the fakes
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
| `.kubriko/project.json` | Creator | Its format is checked again within a second. |
| `.kubriko/scenes.json` (Scene descriptions) | Creator | Read again whenever it is used; a file that can't be read holds none. |
| `<package>/generated/*.kt` | Creator | Checksum mismatch → Creator stops updating it and offers to discard the edits. Committed, so projects build without Creator. |
| `composeResources/files/scenes/**/*.json` | Shared | Sidebar refreshes; an open Scene shows the change. |
| `AGENTS.md` (the game's notes) and the `CLAUDE.md` importing it | Shared | Read at the start of every conversation; Creator writes `AGENTS.md` through `set_game_notes`, and `CLAUDE.md` only when the project has none. |
| Hand-written Kotlin and resources | User | Sidebar refreshes; an open Scene is built again. |
| Template files (`Game.kt`, build files) | User after creation | `templateVersion` in `.kubriko/project.json`; upgrades are shown as a diff. |
| The Kubriko entries of the build files (`kubriko` version, `kubriko-*` libraries, Creator's Maven Local block) | Creator | Brought back in line with the code and Creator's version after every regeneration. |
| The `game.*` keys of `gradle.properties` (the game's identity) | Creator | Edited line by line by Project settings; a value changed by hand shows there the next time it opens. |
| `.kubriko/local/` | Creator | Git-ignored. |

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
  only the Scene view, a new or removed Scene the resource tree too, and every change the Version control panel. Resource scans and
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
- **Kinds:** a `ReadCommand` is never recorded. A `WriteCommand`'s `validate` returns a `Rejection` in the user's words, and `apply`
  returns the inverse `Invocation` (none for git operations, which can't be undone); one that rewrites files without an inverse
  (discarding changes, the template upgrade) says `isRewritingFiles`, which clears the history. An `ActionCommand` (build, play)
  runs outside the change lock; a `ReadCommand` runs under it, so an AI never reads a change half made (one that reads only
  Creator's own knowledge says `readsProject = false`, so a download doesn't hold up edits).
- **`CommandExecutor`** (one per workspace) applies changes one at a time. Consecutive changes with the same `mergeKey` (typing into
  one field) are one undo step, as are those `mergesWith` joins (the renames of one Property); steps of different conversations,
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
- **AI input:** an input that doesn't decode is rejected with `InputProblem`'s one sentence rather than kotlinx.serialization's
  message, and `check_change` runs a write tool's decoding and validation without applying it. A sealed type marked
  `@SummarizedSchema` (`BlockDefinition`) lists only its subclasses' names, which `get_block_catalog` describes once.
- **Addressing:** types and Scenes by name, Events, Blocks, and texts by index, Scene Actors by ID. The Event, Block, and Property
  commands take either kind of type (`actorType` names an Actor type or a Manager type).
- **Renames follow everywhere:** Blocks, collision Events, placeholders, texts, Kotlin Blocks and Kotlin UI (`KotlinIdentifiers`,
  which knows no grammar, so a local variable of the same name is renamed too; the import lines a Kotlin Block or Kotlin UI
  starts with are left alone), the Actors of every Scene, and a Property's saved values in Scenes. Removing a Property keeps its Scene values (undo brings them back); adding one drops a leftover value of another
  kind, which wouldn't load; a Property's kind can't change. Renaming a Property renames it through the family of the Actor type that
  declares it, and is refused on one that only inherits it; adding a Property or a parent is refused when a descendant has one of
  that name of another kind.
- **Batches** (`place_actors`, `remove_actors`, `restore_actors`, `set_actors_values`, `set_actors_state`) are one undo step each,
  and name no Actor twice and at least one. Paged reads (`read_scene`, `list_actor_types`) keep large games readable.

## Project files

- **Format versions** (`project/ProjectFormat`): `.kubriko/project.json` records one `formatVersion` for all of a project's Creator
  files, with the `minimumCreatorVersion` that raised it. It is raised by any change to what Creator persists in `.kubriko/` type
  files: a new or renamed field, a new Block, Event, Condition, or Property kind, a new enum value, a changed default, or a new
  meaning for an existing field (any change an older Creator would read and then handle wrongly). A raise that needs no rewrite is
  an empty `FormatMigration { }` whose KDoc names what it added, so an older Creator opens the project read-only with the version
  it needs instead of dropping fields; type files are still read ignoring unknown keys. `PersistedShapeTest` pins the persisted
  shape per format version (`persisted-shape-<version>.txt`; never edit a released version's file). `FormatFixturesTest` pins the files as written, with one fixture project per version (`src/test/resources/formats/v<version>`): a raise copies the newest folder to a new one and edits the copy to show what it adds, a released version's folder is never edited, and an older fixture is read through `ProjectFormat.prepare()`. A name added to
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
  are never touched, and a file without the template's shape is left alone entirely. Projects that aren't opted in are never changed.
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
  named by its path (`levels/1-1`); `delete_folder` takes only an empty folder, so undo is exact. A Scene file that can't be moved
  (Windows refuses while the game has it open) fails the rename before anything else changes, and a folder rename moves the Scenes
  it moved back.

## Builds and playing

- **Opening** never waits for Gradle: it reads the build files, and asks Gradle for the desktop module in the background; the first
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
- **The session** (`tool-creator-session`) runs at the project's Kubriko version and reports to `SessionServer` (which takes only
  the first connection that says Hello after each launch): a session of any version runs, and a command it is too old for is
  refused (`SessionController.request` takes the version a command needs and answers `GameAnswer.Unsupported`, which the game
  commands reject with `error_game_unsupported`).

## Scene editing

Creator's own Scene editor (`scenes/`, `ui/scenes/`), never a window of its own and not `tools/scene-editor`.

- It works only when the project's Kubriko version equals Creator's and its Compose and Kotlin are no newer major or minor version
  (`SharedLibraries`, which gates the `androidx.compose` modules holding Compose's runtime too; another newer shared library loads with a console warning): the project's classes load into a class loader
  whose parent is Creator's (Kubriko, Compose, and Kotlin shared; the project's jars are copied and the class files of its folders
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
- Loaded classes are kept across Scenes until a type or a source changes (`markOutdated`), Reload, or the workspace closes. A change
  while a Scene is open builds again in the background, and a change during that build builds once more after it rather than
  cancelling it; the new `SceneView` creates its Actors while the old one is still shown, then takes over its camera. A failed
  build keeps the old classes until a build works.
- Until newer classes are swapped in, Actors of block-built types whose look (`ActorTypeLook`) changed are drawn by Creator itself
  (`PreviewActor`), following the types in memory, so a change shows before it is even written.
- The editor's instance ticks without the focus (`shouldPauseOnFocusLoss = false`), since a Scene swapped in while Creator is in
  the background would otherwise draw nothing until it is focused.

## AI

- **One AI panel** holds both ways of working with AI (an outside client over MCP, and the in-app assistant), since nobody uses
  them at once; the map ("What the AI knows about your game") stays behind a button in it, not on the toolbar. The map is computed
  once per burst of changes, off the main thread, and only while it is shown (`GameMapState`).
- **AI features off** (`CreatorSettingsController.isAiEnabled`) is the one check: the MCP server stays stopped, the assistant starts,
  fetches, and installs nothing (`AssistantHost` holds its check), and the UI (through `LocalIsAiEnabled`) shows no AI surface. Every new AI surface reads it and
  joins `AiFeaturesOffTest`. Commands, undo, and the game notes stay.
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
  can report them.
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
- **Guards:** `EvalSetTest` replays the calls models make for realistic requests, so a tool change that would break them fails
  without tokens, and `TokenBudgetTest` keeps the tool list, the digest, and the big reads under budgets set just above what they
  cost.

## Settings

- **Creator settings** (`settings/`) are this user's on this computer; an unreadable file means the defaults. The side panels'
  widths are dragged at their edges and kept per panel (`panelWidths`, by an ID that never changes), saved once per drag, and
  clamped to the window when drawn, so the panels of a row never take more than half of it.
- **Project settings** live in the project where its build reads them, so it builds the same without Creator. The game's identity is
  `game.*` keys in `gradle.properties`, edited line by line (`GradleProperties`: other lines untouched, values escaped as Gradle
  reads the ISO-8859-1 file); a project whose build doesn't read them (`GameIdentity.isReadByBuild`) is offered the template upgrade.

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
- The workspace keeps the focus when nothing in it is focused, so Cmd/Ctrl+Z always reaches the undo history.
- A field above the sidebar's groups filters everything in it by name, showing the folders that hold matches opened; the folders
  the user collapsed are left as they were and come back once the filter is cleared.
- What the center shows is one `WorkspaceSelection` (a type, a Scene being prepared, open, or failed, or nothing), changed only
  through `SelectionController`, the one place that pairs selecting a type with closing the Scene. The workspace screen's
  own panels, dialogs, and AI-follow state live in `WorkspaceUiState`, a plain class its tests reach without a UI.
- The type editor's selected Event is its `ListItemKeys` key, not its index (`EventSelectionState`), so it stays on the same
  Event through undo, redo, and the AI's Event changes; an added Event is selected only once its add succeeded, found
  by its key in the type as it is at that moment (an add may wait behind an AI tool call that changes the Events).

## Library side

What Creator needs from the libraries reaches projects only through a Kubriko release, since projects resolve Kubriko from Maven
Central and opted-in ones use Creator's own version, so a Creator release follows each Kubriko release that changes its library side.
Nothing goes into a
published artifact before the Creator feature that uses it exists, and library changes Creator needs follow the public API rules in
the root `CLAUDE.md`.
