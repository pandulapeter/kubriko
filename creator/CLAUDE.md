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

A standalone desktop app for making Kubriko games without IntelliJ, inspired by GameMaker 6.1: Actor types and Manager types are built from
Events and Blocks, Actors are placed in Scenes, and the game is played with one click. This file describes what exists; what is planned
lives in `plan/` (start with `plan/README.md`).

## Running and testing

- Run against this repository's libraries: the `[Desktop] Kubriko Creator` run configuration, or `./gradlew publishDesktopPublicationToMavenLocal
  publishWasmJsPublicationToMavenLocal publishKotlinMultiplatformPublicationToMavenLocal :creator:run -Pcreator.localKubriko=true`.
  `creator.localKubriko` publishes the libraries to Maven Local as `<library.version>-creator` (so they never shadow a real release) and bundles
  that version and `mavenLocal()` into `creator.properties`, so new projects use them. Without it, projects use the released version.
- `./gradlew :creator:test` runs the unit tests (also in the `Tests` workflow). `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=<version>`
  additionally generates every template for real and compiles it, plus an Actor type and two Manager types using every Event and Block (and
  each other's Properties), for desktop and web.
- **Every change ships with solid unit tests**: controllers against fakes (`FakeProjectBuild`, `FakeSessionChannel`, `FakeBlockTypeRepository`,
  `FakeGitRepository`), file code against real temporary folders, git against real temporary repositories (JGit needs no installed git),
  sockets over real localhost connections. `StringResourcesTest` fails on any string or icon nothing uses, and on escaped apostrophes.
- UI changes are checked with a window-only screenshot (`screencapture -l <window id>`) before they are called done.

## Processes

- **Creator** works on files and Gradle only. It drives the project's own build through the Gradle Tooling API (`gradle/GradleProjectConnection`)
  with `src/main/resources/kubriko-creator-session.init.gradle.kts`, which adds two tasks to every Compose desktop module without touching the
  project: `kubrikoCreatorSession` (runs the module's classpath plus `tool-creator-session` at the project's Kubriko version, in `play` or `edit`
  mode, reporting back over a versioned JSON-lines protocol on a localhost port, `SessionServer`) and `kubrikoCreatorClasspath` (compiles and
  writes the Kubriko version and runtime classpath to a file).
- **Embedded Scene editing** (`scenes/`): when the project's Kubriko version equals Creator's, Creator loads the project's classes into a class
  loader whose parent is its own (Kubriko, Compose, and Kotlin shared; project jars copied first so rebuilds can replace them), finds the
  project's `CreatorEntryPoint` through `ServiceLoader`, and draws `SceneEditor.Content` in its center pane. Loaders are released a few seconds
  after their editor leaves composition. Otherwise the Scene opens in a session window.

## Commands

Every change to a project goes through a command (`commands/`), so the user interface, undo and redo, and the AI share one
path. `CommandRegistry.editor` lists them all; the AI tool list (`ToolDefinition`: name, description, input schema) is generated
from it, each input schema from the input class's serial descriptor (`JsonSchema`, with `@Description` on every property).

- **Kinds:** `ReadCommand` (reports, never recorded), `WriteCommand` (`validate` returns a `Rejection` with a user-facing
  `StringResource`; `apply` returns `Applied` with the inverse `Invocation`, or none for git operations, which can't be undone),
  and `ActionCommand` (build, play; run outside the change lock).
- **`CommandExecutor`** (one per workspace): applies changes one at a time, keeps the undo and redo history (consecutive changes
  with the same `mergeKey`, like typing into one field, are one step; a step that no longer validates is dropped), and decodes
  AI tool calls (`call(name, JsonObject)`). UI callbacks use `dispatch`, which starts undispatched, so in-memory changes show
  before it returns. Rejections of dispatched changes and of undo steps go to the console.
- **Enforced by tests** (`CommandRegistryTest`): every `EditorCommand` class is registered, tool names are snake_case, every
  part of every schema is described, and nothing outside `commands/` opts in to `@EditorMutation`, the `RequiresOptIn` marker on
  every controller and store function that changes the project. A new mutation is marked, so only a command can call it.
- **Addressing:** Actor types, Manager types, and Scenes by name, Events, Blocks, texts, and Scene Actors by index. The Event, Block, and
  Property commands take either kind of type (their `actorType` input names an Actor type or a Manager type). `commands/CommandTesting.kt`
  (`assertRoundTrip`) checks that a command's inverse reverts it and the inverse of the inverse makes it again.
- **Properties** (number, text, toggle) are values each Actor keeps: generated as `@Exposed` fields kept in `State`, so the
  Scene editor edits them per Actor, changed by the setNumber / changeNumber / setText / setToggle Blocks, and shown by Log
  Blocks as `{name}` (whole numbers without ".0"). A Block may leave its Property unchosen and generates a comment until it
  names one of its kind. Scenes keep the values: renaming moves them (and the Blocks and placeholders), removing keeps them
  (loading ignores them, and undo brings them back), and adding drops a leftover value of another kind, which wouldn't load.
  A Property's kind can't change.
- **Flow** stays in each Event's flat Block list, as GameMaker's actions do: if (with a condition: a Property, a key held,
  the velocity, a chance, or whether the game is paused), else, repeat, and end Blocks mark sections, so every Block keeps its plain index. `BlockNesting`
  works out each Block's depth for the editor's indentation and the generator alike; an if or repeat without an end runs to
  the end of the Event, and an else or end that belongs to no section shows a warning and generates a comment.
- **Timers** 1 to 8 per Actor, like GameMaker's alarms: the startTimer (seconds, once or repeating) and stopTimer Blocks, and a
  timerFinished Event per Timer (its number set like a key Event's key). Generated as two `Long` fields per Timer that is used,
  counted down at the start of `update`, rather than Kubriko's `Timer`, whose duration is fixed, so starting one never allocates.
- **Other Actor types** in Blocks: addActor spawns one through its own `metadata.instantiate(position)` (so with its size
  and initial Property values, at a position relative to the Actor or the Scene), and moveTowardsNearest aims the velocity at
  the closest Actor of a type (a loop over `allActors` by index). Like collision Events, they follow renames, are rejected for
  types that don't exist, and generate a comment for a type deleted later.
- **Manager types** (`ManagerTypeDefinition`, in `.kubriko/managers`) hold game-wide state, like the score: Properties, Events
  (gameStarted, everyFrame, keyPressed, timerFinished), and texts shown on top of the game (`ScreenTextDefinition`: placeholders,
  one of nine places, size, color). Both kinds are a `BlockTypeDefinition` and share one namespace, the edits (`BlockTypeEdits.kt`),
  the Block editor, and the commands; Blocks and conditions that need an Actor (`BlockKind.needsActor`, the velocity condition) and
  Actor-only Events are rejected in a Manager and hidden from its palette. Each one becomes a `Manager` subclass whose Properties are
  Compose state (`mutableFloatStateOf` for numbers, so nothing boxes), whose texts are `BasicText`s in its `Composable`, followed by
  its Kotlin UI (`kotlinUi`, `set_kotlin_ui`, edited in a dialog): verbatim Compose statements whose leading import lines become the
  file's imports (`KotlinUi`), with other Manager types, `actorManager`, `viewportManager`, and `stateManager` looked up only when
  the code names them, and which, like a Kotlin Block, doesn't follow renames. The Manager is
  `KeyboardInputAware` and adds itself as an Actor when it has key pressed Events (as the examples' Managers do; `GeneratedScenes`
  adds it back after each Scene's `removeAll`). Its
  Events don't run in the Scene editor (`isInSceneEditor`), nor every frame while the game is paused. The generated
  `KubrikoWiring.createManagers(isInSceneEditor, firstScene)` creates all of them, plus `GeneratedScenes`; the template passes it to
  `Kubriko.newInstance` in `Game.kt` (with the first Scene) and to the Scene editor in `GameEntryPoint.kt`.
- **Scenes and pausing** in Blocks: goToScene and restartScene go through the generated `GeneratedScenes` Manager
  (`ScenesCodeGenerator`, written into every project), which reads `files/scenes/<name>.json` from the project's resources in
  the background, then removes every Actor and adds the Scene's (the first Scene, loaded when the game starts, keeps what the
  Managers added before it) and logs a Scene it can't read. Manager types keep their Properties. goToScene is rejected for a
  Scene that doesn't exist (`BlockScope.sceneNames`, known to commands and the editor but not to the generator) and generates a
  comment until one is chosen. pauseGame pauses, resumes, or toggles through `StateManager.updateIsRunning`; key presses still
  arrive while paused, so Actors' key pressed Events return early then (as Wallbreaker's `Ball` does) and Managers' still run, so a
  Manager's key can resume the game.
- **Scene settings** are an Actor, not a separate file: the generated `SceneSettings` (`SceneSettingsCodeGenerator`,
  written into every project and always in `GeneratedActorTypes`) is `Unique`, `Editable`, and placed like any other Actor. Its body
  is the Scene's bounds, which it fills with its background color behind every other Actor (`drawingOrder` at the maximum) and which
  edge Events use instead of the screen's edges (through `GeneratedScenes.settings`, which it sets while it is in the Scene); a
  Scene without one keeps the screen's edges. Its camera start moves the camera when it is added, except in the Scene editor. The
  view stays `Dynamic`, so a window of another size shows more or less around the bounds. `set_scene_settings` changes it (adding one
  at the end of the Scene if it has none), editing its state as JSON (`SceneSettingsState`; colors are saved as Compose's packed
  value); otherwise it is placed and edited in the Scene editor, from its palette and inspector.
- **Reaching a Manager:** the Property Blocks and conditions have an optional `manager`, and Log Blocks and texts show `{Manager.name}`
  placeholders (`PropertyPlaceholders`). `BlockScope` is what a type's Blocks can refer to, for the generator, validation, and the
  editor's Property choices alike: an Actor looks the Managers it uses up in `onAdded` (a field named after the type, unless a
  Property has that name), a Manager through `manager<T>()`, and a Manager naming itself means its own Properties. Renaming a
  Manager type or one of its Properties follows every Block, placeholder, and text of every type.
- **Template upgrades** (`template/ProjectUpgrader`): the template files belong to the user, so `.kubriko/project.json` records the
  `templateVersion` they have. When it is older than Creator needs, the workspace shows a notice, and its dialog shows each change as a
  diff (`LineDiff`), offers to commit first, and applies it (`upgrade_project`; `get_project_upgrade` for the AI) only to a file that
  still looks like the template's. Files changed too much are listed with what to add by hand, and a file that already uses
  `KubrikoWiring` counts as done, however it got there. Version 2 is the Manager wiring; version 3 drops the template's `SceneLoader`
  for `createManagers(firstScene = …)` and deletes `SceneLoader.kt` if it is still the template's (`resources/upgrades` keeps that
  copy; one changed by hand stays). A change's `after` is null for a file it deletes.
- **Scenes** are edited as files (`scenes/SceneStore`, `SceneDocument`): Actors are `typeId` + JSON state, positions live in
  `body.position`, and placing works for block-built Actor types only (their state is known without loading classes). Renaming an
  Actor type renames its Actors in every Scene. A command that changes the open Scene marks it outdated (or closes it if deleted).
  Changes made inside the embedded Scene editor are the library tool's own and don't go through commands.

## AI connection (MCP)

`mcp/McpToolServer` serves `CommandRegistry.editor.tools` as MCP tools over Streamable HTTP (Ktor CIO, the official Kotlin MCP SDK)
on `127.0.0.1:<port>/mcp`. Every request needs the token, as `Authorization: Bearer <token>` or, for clients that can't send
headers, `?token=` (compared in constant time); the SDK's DNS rebinding protection limits hosts to localhost. Tool calls run
`CommandExecutor.call` of the open project on the main dispatcher, so AI changes enter the same undo history; text results are
JSON, a result's image (a screenshot) becomes MCP image content, rejections are `isError` results in the user's words, and every
call is logged. Read tools are marked read-only, `WriteCommand.isDestructive` ones destructive. `McpController` (app-level,
created in `main`) keeps `McpSettings` (off by default, port 7781, a random token, connected clients, command overrides) in
`mcp.json` in the settings folder and restarts the server when they change; a taken port shows as `Failed`.

- **One-click clients** (`mcp/clients`): `AiClient` lists the supported tools (Claude Code, Codex, Gemini CLI), each a CLI with
  `mcp add` / `get` / `remove` at user scope; Codex can't send headers, so it gets the token in the URL. `AiClientSetup` is one way
  to run a tool: its program with its default configuration (which apps such as the Claude app use too, so it is always listed,
  run as `command claude` past an alias of the same name that runs something else), the user's replacement command, and each
  shell alias that runs the program (`claude-personal` → "Claude Code (personal)"), found by parsing the shell's `alias` listing
  (`ShellAliases`: zsh, bash, and fish quoting). Setups are connected separately and remembered by id (`claudeCode@claude-personal`). `AiClientsController`
  finds the setups, checks which are installed and connected, connects (turning the server on first) and disconnects, and connects them again
  when the port or token changes. `ShellProcessRunner` runs them through the user's shell as an interactive login shell, like a
  terminal (an app started from the Finder lacks the shell's PATH, and aliases such as `claude-personal` live in `.zshrc`),
  dropping the shell's job control notices; the command is user-editable. A tool counts as connected only when its output names
  the server, since a wrapper alias can succeed without doing anything.
- **Apps with a configuration file** (`AiConfigFileClient`: Claude Desktop, Cursor) are connected by editing their JSON file
  (`McpConfigFile`): only `mcpServers.kubriko-creator` changes, the rest keeps its values, order, and indentation, the file is
  replaced atomically through a link and keeps its permissions (a new one is owner-only), and a file that isn't valid JSON is never
  written. An app counts as installed once its configuration folder exists. Claude Desktop only starts stdio servers, so its entry
  runs `mcp-remote` through the full path of the `npx` the user's shell finds (`ProcessRunner.locate`), with that folder on the
  PATH, and passes the token through the environment; without npx the row asks for Node.js. Cursor gets the URL and header.
- **The panel** (toolbar → AI connection): a Connect / Disconnect / Install row per client, the on/off switch, the address
  with the token for other MCP clients, Advanced (port, token, commands), and the call log.
- **Playing the game:** `start_game`, `press_keys`, `wait`, `take_screenshot`, and `stop_game` drive a play session through
  session protocol 2 (`SessionCommand`s Creator sends back on the session's connection, answered by id): keys go to the AWT event
  listeners Kubriko's keyboard input reads, screenshots come from the game's `SkiaLayer` (no OS permission), and every step first
  brings the window to the front, since Kubriko pauses unfocused games, and waits up to two seconds for the focus to arrive
  (keys aren't pressed without it; `press_keys` fails instead). Unexpected exceptions in a tool are `isError` results that name
  the exception. Sessions of protocol 1 (older Kubriko) still run; the
  game tools say the version is too old.
- **Showing the AI's work:** every command has a `title` and a `target` (`CommandTarget`: an Actor type with an optional Event
  and Block index, a Scene, version control, or the game), asked before and after it runs. `CommandExecutor.aiSteps` records each
  AI tool call as a running, then finished (or rejected) `AiStep`. The workspace shows them in `AiActivityBar` (step number, the
  step before, the current one pulsing while it runs, a Follow switch), follows the AI to the Actor type and Event it works on,
  and lights up the finished step's target (`rememberFlashColor`) in the editor and the sidebar (`ui/AiFollowing.kt`).
- Tests talk to a real server on localhost through the SDK's client, run real shell commands, and play a fake session.

## Packages (`com.pandulapeter.kubrikoCreator`)

| Package | What it holds |
|---|---|
| (root) | `AppController` (screens, creating/opening/closing projects, recent projects), `CreatorConfiguration` (bundled versions), `settingsDirectory`. |
| `project` | Gradle inspection (`ProjectInspector`, `ModuleNode`), `ResourceScanner` (sidebar groups), `ProjectWatcher` (1 s fingerprint poll of `.kubriko/actors`, Scenes, and sources, excluding generated code). |
| `actors` | The Block model: `BlockTypeDefinition` (`ActorTypeDefinition`, `ManagerTypeDefinition`) / `PropertyDefinition` / `EventDefinition` / `BlockDefinition` (JSON in `.kubriko/actors` and `.kubriko/managers`, hex colors, no empty fields, `@Description`s that the tool schemas and the Block catalog carry), `PropertyKind` and `PropertyNaming`, `BlockTypeNaming`, `BlockKind` (palette + which Events and kinds allow a Block), `BlockScope`, pure edits (`BlockTypeEdits.kt`), `BlockCodeGenerator` (Blocks to statements) with `ActorTypeCodeGenerator`, `ManagerTypeCodeGenerator` (classes, the registry, and `KubrikoWiring`), `ScenesCodeGenerator` (`GeneratedScenes`), and `SceneSettingsCodeGenerator` (`SceneSettings`), `GeneratedFile` (checksummed header), `BlockTypeStore` (saving regenerates every type; skips hand-edited files and reports them). |
| `commands`, `commands/actorTypes`, `commands/managerTypes`, `commands/scenes`, `commands/git`, `commands/project` | The command layer (see Commands): one object per command, with its `Input`. |
| `mcp`, `mcp/clients` | The AI connection (see AI connection): `McpToolServer`, `McpController`, `McpSettingsStore`, one-click clients. |
| `workspace` | `WorkspaceController` (resources, builds, routing watcher changes, the `CommandExecutor`), `ConsoleLog`, `SessionController` (the session process, its messages, commands to a running game), `WorkspaceRunner` (the `ProjectRunner` behind the build, play, and game tools), `BlockTypesController` (in-memory edits, ordered background writes, reload from disk unless a write is pending), `GradleWorkspaceOpener`. |
| `scenes` | `SceneEditingController`, `ProjectClasspath`, `ClassLoaderEntryPointLoader`, `SceneStore` and `SceneDocument` (Scene files by name), `SceneSettingsState`. |
| `git` | `JGitRepository` (status, commit all, history, discard, create), `ChangeGrouping` (a type's JSON + generated Kotlin = one change), `GitController`. |
| `template` | `ProjectGenerator` (renders `resources/templates/<id>/`, listed in `files.txt`, `.template` suffix, `dot-` prefix for dot files; regenerates the block-built types; runs Gradle's `wrapper` task; first git commit), naming and form rules, `ProjectUpgrader` and `LineDiff` (template upgrades). |
| `recent` | `RecentProjectsStore` (platform settings folder). |
| `ui`, `ui/actorTypes`, `ui/scenes`, `ui/git`, `ui/mcp`, `ui/components` | Compose UI. `BlockTypeEditor` follows GameMaker's object window: appearance (texts on screen, for a Manager type) and Properties, Event column, the selected Event's Blocks, Block palette. |
| `window` | JetBrains Runtime title bar the content extends under, and the 0.85 interface scale (both from the user's Campfire app). |

## Conventions specific to Creator

- **Every editor change is a command** with its AI tool and tests (validation, apply, and the inverse round trip); the UI
  dispatches commands rather than calling controllers. Undo and redo: toolbar buttons naming the step, Ctrl+Z / Ctrl+Shift+Z /
  Ctrl+Y (Cmd on macOS).
- **Beginner-friendly by default:** the Block palette shows one category at a time (`BlockCategory`, tabs like GameMaker's, without the
  ones a Manager has no Blocks in),
  Blocks are added by a click (at the end) or dragged (from the palette or by a Block's handle) to where they go
  (`BlockDragState`: an in-window drag, dropped through `add_block` / `move_block`), each Block is a card indented by its
  section, the console starts closed and opens for problems and what the game logs, the sidebar hides asset groups nothing
  uses yet and collapses Kotlin files, and New project keeps the package under More options.
- Desktop-sized UI from the start: `ui/components` (`CompactTextField`, `IconActionButton` with tooltip, `PanelHeader`), vector icons in
  `composeResources/drawable`, everything in `strings.xml` with plain apostrophes.
- Generated Kotlin reads like the examples' hand-written Kubriko code: an Actor type is `Dynamic` only when it moves or has work every
  frame (a Kotlin Block counts), looks up only the Managers its Blocks use, and builds its collision mask with the body's scale and
  rotation, centered on the body wherever its pivot is (the Scene editor creates an edited Actor again from its state, so the mask
  follows a new size too).
- Generated Kotlin must compile for every combination of Events and Blocks (unknown collision targets are left out, Kotlin Blocks get their own
  `run { }` scope, Blocks outside their Event become comments) and follow the engine's no-allocation rules.
- Nothing under `engine/`, `plugins/`, or `tools/` may depend on `creator`. Library changes Creator needs follow the public API rules (confirm first).
- Generated code, `.kubriko/`, and Scenes are owned as described in `plan/README.md` → Files and ownership.
