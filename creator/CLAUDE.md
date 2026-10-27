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

A standalone desktop app for making Kubriko games without IntelliJ, inspired by GameMaker 6.1: Actor types are built from Events and Blocks,
placed in Scenes, and played with one click. This file describes what exists; what is planned lives in `plan/` (start with `plan/README.md`).

## Running and testing

- Run against this repository's libraries: the `[Desktop] Kubriko Creator` run configuration, or `./gradlew publishDesktopPublicationToMavenLocal
  publishWasmJsPublicationToMavenLocal publishKotlinMultiplatformPublicationToMavenLocal :creator:run -Pcreator.localKubriko=true`.
  `creator.localKubriko` publishes the libraries to Maven Local as `<library.version>-creator` (so they never shadow a real release) and bundles
  that version and `mavenLocal()` into `creator.properties`, so new projects use them. Without it, projects use the released version.
- `./gradlew :creator:test` runs the unit tests (also in the `Tests` workflow). `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=<version>`
  additionally generates every template for real and compiles it, plus an Actor type using every Event and Block, for desktop and web.
- **Every change ships with solid unit tests**: controllers against fakes (`FakeProjectBuild`, `FakeSessionChannel`, `FakeActorTypeRepository`,
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
- **Addressing:** Actor types and Scenes by name, Events, Blocks, and Scene Actors by index. `commands/CommandTesting.kt`
  (`assertRoundTrip`) checks that a command's inverse reverts it and the inverse of the inverse makes it again.
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
  `mcp add` / `get` / `remove` at user scope; Codex can't send headers, so it gets the token in the URL. `AiClientsController`
  checks which are installed and connected, connects (turning the server on first) and disconnects, and connects them again
  when the port or token changes. `ShellProcessRunner` runs them through the user's login shell (an app started from the Finder
  lacks the shell's PATH); the command is user-editable and may set environment variables first (`CLAUDE_CONFIG_DIR=… claude`).
- **The panel** (toolbar → AI connection): a Connect / Disconnect / Install row per client, the on/off switch, the address
  with the token for other MCP clients, Advanced (port, token, commands), and the call log.
- **Playing the game:** `start_game`, `press_keys`, `wait`, `take_screenshot`, and `stop_game` drive a play session through
  session protocol 2 (`SessionCommand`s Creator sends back on the session's connection, answered by id): keys go to the AWT event
  listeners Kubriko's keyboard input reads, screenshots come from the game's `SkiaLayer` (no OS permission), and every step first
  brings the window to the front, since Kubriko pauses unfocused games. Sessions of protocol 1 (older Kubriko) still run; the
  game tools say the version is too old.
- Tests talk to a real server on localhost through the SDK's client, run real shell commands, and play a fake session.

## Packages (`com.pandulapeter.kubrikoCreator`)

| Package | What it holds |
|---|---|
| (root) | `AppController` (screens, creating/opening/closing projects, recent projects), `CreatorConfiguration` (bundled versions), `settingsDirectory`. |
| `project` | Gradle inspection (`ProjectInspector`, `ModuleNode`), `ResourceScanner` (sidebar groups), `ProjectWatcher` (1 s fingerprint poll of `.kubriko/actors`, Scenes, and sources, excluding generated code). |
| `actors` | The Block model: `ActorTypeDefinition` / `EventDefinition` / `BlockDefinition` (JSON in `.kubriko/actors`, hex colors, no empty fields, `@Description`s that the tool schemas and the Block catalog carry), `BlockKind` (palette + which Events allow a Block), pure edits (`ActorTypeEdits.kt`), `ActorTypeCodeGenerator`, `GeneratedFile` (checksummed header), `ActorTypeStore` (saving regenerates every type; skips hand-edited files and reports them). |
| `commands`, `commands/actorTypes`, `commands/scenes`, `commands/git`, `commands/project` | The command layer (see Commands): one object per command, with its `Input`. |
| `mcp`, `mcp/clients` | The AI connection (see AI connection): `McpToolServer`, `McpController`, `McpSettingsStore`, one-click clients. |
| `workspace` | `WorkspaceController` (resources, builds, routing watcher changes, the `CommandExecutor`), `ConsoleLog`, `SessionController` (the session process, its messages, commands to a running game), `WorkspaceRunner` (the `ProjectRunner` behind the build, play, and game tools), `ActorTypesController` (in-memory edits, ordered background writes, reload from disk unless a write is pending), `GradleWorkspaceOpener`. |
| `scenes` | `SceneEditingController`, `ProjectClasspath`, `ClassLoaderEntryPointLoader`, `SceneStore` and `SceneDocument` (Scene files by name). |
| `git` | `JGitRepository` (status, commit all, history, discard, create), `ChangeGrouping` (an Actor type's JSON + generated Kotlin = one change), `GitController`. |
| `template` | `ProjectGenerator` (renders `resources/templates/<id>/`, listed in `files.txt`, `.template` suffix, `dot-` prefix for dot files; regenerates Actor types; runs Gradle's `wrapper` task; first git commit), naming and form rules. |
| `recent` | `RecentProjectsStore` (platform settings folder). |
| `ui`, `ui/actorTypes`, `ui/scenes`, `ui/git`, `ui/mcp`, `ui/components` | Compose UI. The Actor type editor follows GameMaker's object window: appearance, Event column, the selected Event's Blocks, Block palette. |
| `window` | JetBrains Runtime title bar the content extends under, and the 0.85 interface scale (both from the user's Campfire app). |

## Conventions specific to Creator

- **Every editor change is a command** with its AI tool and tests (validation, apply, and the inverse round trip); the UI
  dispatches commands rather than calling controllers. Undo and redo: toolbar buttons naming the step, Ctrl+Z / Ctrl+Shift+Z /
  Ctrl+Y (Cmd on macOS).
- Desktop-sized UI from the start: `ui/components` (`CompactTextField`, `IconActionButton` with tooltip, `PanelHeader`), vector icons in
  `composeResources/drawable`, everything in `strings.xml` with plain apostrophes.
- Generated Kotlin must compile for every combination of Events and Blocks (unknown collision targets are left out, Kotlin Blocks get their own
  `run { }` scope, Blocks outside their Event become comments) and follow the engine's no-allocation rules.
- Nothing under `engine/`, `plugins/`, or `tools/` may depend on `creator`. Library changes Creator needs follow the public API rules (confirm first).
- Generated code, `.kubriko/`, and Scenes are owned as described in `plan/README.md` → Files and ownership.
