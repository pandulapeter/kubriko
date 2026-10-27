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

## Packages (`com.pandulapeter.kubrikoCreator`)

| Package | What it holds |
|---|---|
| (root) | `AppController` (screens, creating/opening/closing projects, recent projects), `CreatorConfiguration` (bundled versions). |
| `project` | Gradle inspection (`ProjectInspector`, `ModuleNode`), `ResourceScanner` (sidebar groups), `ProjectWatcher` (1 s fingerprint poll of `.kubriko/actors`, Scenes, and sources, excluding generated code). |
| `actors` | The Block model: `ActorTypeDefinition` / `EventDefinition` / `BlockDefinition` (JSON in `.kubriko/actors`, hex colors, no empty fields), `BlockKind` (palette + which Events allow a Block), pure edits (`ActorTypeEdits.kt`), `ActorTypeCodeGenerator`, `GeneratedFile` (checksummed header), `ActorTypeStore` (saving regenerates every type; skips hand-edited files and reports them). |
| `workspace` | `WorkspaceController` (sessions, console, routing watcher changes), `ActorTypesController` (in-memory edits, ordered background writes, reload from disk unless a write is pending), `GradleWorkspaceOpener`. |
| `scenes` | `SceneEditingController`, `ProjectClasspath`, `ClassLoaderEntryPointLoader`. |
| `git` | `JGitRepository` (status, commit all, history, discard, create), `ChangeGrouping` (an Actor type's JSON + generated Kotlin = one change), `GitController`. |
| `template` | `ProjectGenerator` (renders `resources/templates/<id>/`, listed in `files.txt`, `.template` suffix, `dot-` prefix for dot files; regenerates Actor types; runs Gradle's `wrapper` task; first git commit), naming and form rules. |
| `recent` | `RecentProjectsStore` (platform settings folder). |
| `ui`, `ui/actorTypes`, `ui/scenes`, `ui/git`, `ui/components` | Compose UI. The Actor type editor follows GameMaker's object window: appearance, Event column, the selected Event's Blocks, Block palette. |
| `window` | JetBrains Runtime title bar the content extends under, and the 0.85 interface scale (both from the user's Campfire app). |

## Conventions specific to Creator

- Desktop-sized UI from the start: `ui/components` (`CompactTextField`, `IconActionButton` with tooltip, `PanelHeader`), vector icons in
  `composeResources/drawable`, everything in `strings.xml` with plain apostrophes.
- Generated Kotlin must compile for every combination of Events and Blocks (unknown collision targets are left out, Kotlin Blocks get their own
  `run { }` scope, Blocks outside their Event become comments) and follow the engine's no-allocation rules.
- Nothing under `engine/`, `plugins/`, or `tools/` may depend on `creator`. Library changes Creator needs follow the public API rules (confirm first).
- Generated code, `.kubriko/`, and Scenes are owned as described in `plan/README.md` → Files and ownership.
