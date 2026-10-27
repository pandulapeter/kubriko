<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Kubriko Creator: design plan

Status: Stage 2 in progress (see sections 19 and 20). The product is called Kubriko Creator; "the editor" below means the Creator app.

## 1. What this is

A standalone desktop application for making Kubriko games without IntelliJ: organize resources, place Actors in Scenes, give Actors behavior with
event lists and visual blocks, write Kotlin when blocks run out, run the game, debug it, and export signed builds.

Conceptually it is inspired by GameMaker 6.1 (2005): a resource tree, objects as lists of events holding actions, a room editor, a simple and an advanced
mode, and one-click running. It uses Kubriko's own terminology and architecture throughout, and borrows ideas only (see section 14).

### Goals

- A beginner can download one installer, pick a template, and have a running game in minutes, without knowing Gradle, Kotlin Multiplatform, or IntelliJ.
- Every project the editor touches is a plain Gradle Kotlin Multiplatform project that builds and runs in IntelliJ exactly as if written by hand.
- Existing Kubriko projects open without migration and gain features step by step, only when their owner opts in.
- Kubriko stays a library first. The editor is one of its consumers, never a requirement.
- Signed exports: APK and AAB everywhere, desktop installers for the host OS, static web builds, and iOS on a Mac.

### Non-goals

- Replacing IntelliJ. Advanced users keep using it, on the same project, at the same time.
- A scripting language or runtime interpreter. Gameplay is always compiled Kotlin.
- Compatibility with GameMaker files, or a recreation of its look, icons, or action set.
- Running the editor itself on Android, iOS, or the web.

## 2. Principles

1. **The library never depends on the editor.** Dependencies point one way only: editor → tools → plugins → engine. The build enforces it (section 11).
2. **Plain files, plain Gradle.** Scenes are JSON in `composeResources`, code is `.kt` files, and the build is the project's own Gradle build. Nothing
   the build needs lives in editor-private files.
3. **Deleting `.kubriko/` never breaks a build.** It only downgrades editor features (block-built Actors become ordinary code Actors).
4. **The editor never rewrites code it did not generate.** Files it generates are marked; hand-written files are only ever opened, never transformed.
5. **Every change outside editor-owned files is shown as a diff and confirmed** (adding a target, a plugin dependency, a Gradle setting).
6. **No engine changes for editor convenience.** If the editor needs something from the engine, it must be justified as public API on its own merits,
   under the usual stability rules. Editor-only runtime needs go into separate opt-in modules.
7. **Two reference projects keep the design honest:** a beginner game built entirely from blocks (the Getting started tutorial's paddle game), and
   Tesselar (section 13). A design that serves both is the right one.

## 3. Terminology

Kubriko names everywhere. GameMaker equivalents are listed only to make the inspiration traceable.

| Kubriko Creator | Kubriko code | GameMaker 6.1 concept |
|---|---|---|
| Resource tree | Files under `composeResources` and `src` | Resource tree |
| Actor type | A class implementing `Actor` | Object |
| Actor | An instance placed in a Scene | Instance |
| Trait | `Visible`, `Dynamic`, `CollisionDetector`, … | (implicit in events) |
| Event | A Trait callback (`update`, `onCollisionDetected`, …) | Event |
| Block | One generated statement inside an Event | Action |
| Kotlin block | A verbatim Kotlin snippet inside an Event | "Execute code" action |
| Property | An `@Exposed` property of an Actor type | Variable |
| Scene | A serialized JSON scene file | Room |
| Camera | `ViewportManager` | View |
| Timer | `Timer` | Alarm |
| Manager | `Manager` | (global game settings) |
| Plugin | A `plugin-*` module and its Manager | Extension |

## 4. Architecture

### Processes

The central constraint: the editor is a Compose desktop app built on one Kubriko version, while the project it edits may use another, and a JVM process
cannot host two Compose runtimes in one UI. So the editor never loads the user's classes into its own process.

```
┌─────────────────────────────── Editor process ───────────────────────────────┐
│ Project shell: resource tree, Actor editor, block editor, code editor,       │
│ build and export, settings. Works on files only, never on user classes.      │
└───────────┬───────────────────────────────────────────────┬──────────────────┘
            │ Gradle Tooling API                            │ Session protocol (localhost)
┌───────────▼───────────┐                       ┌───────────▼───────────────────┐
│ Gradle daemon         │                       │ Session process (child JVM)   │
│ builds, runs, exports │                       │ the project's own classpath + │
└───────────────────────┘                       │ its Kubriko version's         │
                                                │ scene editor and debug agent  │
                                                │ Edit mode = Scene editor      │
                                                │ Play mode = running game      │
                                                └───────────────────────────────┘
```

- **Editor process.** Everything that only needs files: it parses scene JSON, block definitions, and Gradle's model, but never instantiates an Actor.
- **Session process.** Started from the project's desktop classpath through a Gradle init script, which adds `tool-creator-session` (bringing `tool-scene-editor` and the debug agent)
  (section 9) at the project's own Kubriko version, without touching the project's build files. It hosts a Scene in one of two modes: **Edit** (the
  existing scene editor's canvas, transform tools, and inspector) and **Play** (the game running with the debug agent attached). This is the GameMaker
  "run the room" loop, and it means the scene editor and the debugger are the same piece of infrastructure.
- **Session protocol.** A small JSON-over-localhost protocol: load and save Scene, select Actor, report the `@Exposed` properties of code Actors (so the
  editor's inspector knows them without loading classes), live Actor tree, logs, frame metrics, pause and step.

Scenes are edited inside Creator's own window whenever the project uses the Kubriko version Creator was built with, which is always the case for
projects Creator created (done 2026-10-27): the init script's `kubrikoCreatorClasspath` task compiles the project and writes its runtime
classpath, Creator loads it into a class loader whose parent is its own (so Kubriko, Compose, and Kotlin are shared, and only the project's
classes are new; the project's jars are copied first so that the next build can replace them), finds the project's `CreatorEntryPoint`, and
draws the Scene editor into its center pane through the public `SceneEditor.Content`. When Actor types change, the open Scene offers to reload.
A project on another Kubriko version, which can't share Creator's classes, still edits its Scenes in a session window of its own.

This replaces the earlier idea of deprecating `SceneEditor.show(...)`: the published scene editor stays, as the engine behind the editor's scene view,
and keeps working on its own for library users.

### Modules

| Module | Published | Purpose |
|---|---|---|
| `creator` | As an application, not a library | The editor process (Kubriko Creator itself). |
| `tools/scene-editor`, `-api`, `-noop` | Yes, unchanged API | The session process's Edit mode. `-api` (`Editable`, `@Exposed`) is the contract between projects and the editor. |
| `tools/creator-api` (new) | Yes | The opt-in contract: `CreatorEntryPoint`, found through `java.util.ServiceLoader`, hands over the project's `SerializationManager` (and, from Stage 3, its `Kubriko` instances for the debugger). Only opted-in projects depend on it. |
| `tools/creator-session` (new, desktop only) | Yes | Injected into the session process at the project's Kubriko version, never declared by projects: the launcher, Edit mode, and the agent that answers the session protocol (logs first; Actor tree, properties, metrics, pause and step in Stage 3). |
| `tools/debug-overlay` (new) | Yes | Minimal in-game overlay for library-only users: FPS, frame time, Actor count, recent logs. Replaces `debug-menu`. |

Generated code must not need a runtime library. If Blocks ever need helpers beyond the public engine and plugin API, those go into a separate opt-in module
(working name `tool-creator-blocks`), never into the engine.

## 5. Projects

### Creating

Templates are complete, working Gradle projects: Compose Multiplatform with Desktop, Web, and Android targets (iOS added on demand on a Mac), the Gradle
wrapper, a version catalog, and a `Game.kt` that creates the `Kubriko` instance. Initial set:

- **Empty game**: one Scene, no Actors.
- **Paddle game**: the Getting started tutorial's game, rebuilt from Blocks. Doubles as the beginner reference project.
- **Platformer starter** and **Top-down starter**: physics or collision, keyboard and gamepad input, a few block-built Actor types.

### Opening

Detection goes through the Gradle Tooling API, never by guessing layouts: the editor asks Gradle for modules, source sets, targets, and dependencies,
and looks for an `io.github.pandulapeter.kubriko` dependency. Capability levels:

| Level | Requires | Available |
|---|---|---|
| 0 | Any Kubriko project | Resource browsing, code editing, run, debug (with the agent injected), export for configured targets. |
| 1 | Actor types implementing `Editable`, plus a small entry point exposing the project's `SerializationManager` (section 18) | Scene editing in the session process. |
| 2 | Opted in (creates `.kubriko/`) | Block-built Actor types, project settings panels, generated wiring. |

Anything the editor cannot interpret (custom Gradle logic, an unusual layout) lowers the level with an explanation instead of failing.

### Files and ownership

| Path | Owner | Notes |
|---|---|---|
| `src/**/*.kt` | User | Opened in the code editor; never rewritten. |
| `src/**/generated/kubriko/*.kt` | Editor | Starts with a "generated by Kubriko Creator" header. Committed, so the project builds without the editor. |
| `composeResources/files/scenes/*.json` | Shared | Written deterministically (stable key order, fixed number formatting) so diffs stay readable. |
| `.kubriko/project.json` | Editor | Which module and Kubriko instance holds the game, editor mode, template origin. Committed. |
| `.kubriko/actors/*.json` | Editor | Block definitions for block-built Actor types. Committed. |
| `.kubriko/local/` | Editor | Window layout, recent files, caches. Git-ignored. |

Projects are git repositories by default (section 21).

Both tools can have the project open at once. The editor watches the file system and reloads changed files. Generated files store a content hash; if one
was edited by hand, the editor stops regenerating it and offers two choices: discard the edit, or convert the Actor type to code.

### Library versions

The editor supports a declared range of Kubriko versions (the oldest it can inject the session tools into, up to its own). Raising the Kubriko version
in a project's version catalog is a separate, explicit action that shows the release notes. The editor never upgrades silently.

## 6. The editor UI

Three columns: the resource tree, tabbed editors in the center, and an inspector. The session window docks to the right or floats.

**Resource tree** groups: Scenes, Actor types, Sprites, Sounds, Music, Shaders, Kotlin files, and (Advanced mode) Managers and Plugins. Each maps to plain
files; the tree is a view, not a database.

**Simple mode** (the default for templates) shows Scenes, Actor types, Sprites, Sounds, and Music; Blocks only; Run on Desktop and Web. **Advanced mode**
adds Kotlin files, Kotlin blocks, Managers and Plugins, all export targets, and Gradle output. Switching never changes the project.

**Look and feel:** a desktop app, not a phone app: the whole interface is drawn at 0.85 of Material's size (`ProvideInterfaceScale`,
as Campfire does), the content extends under the title bar on macOS and Windows through the JetBrains Runtime's custom title bar
(`window/ExtendedTitleBar.kt`, from Campfire), compact one-line fields and icon buttons with tooltips replace Material's touch-sized ones, and
every action has an icon (vector drawables in `composeResources/drawable`). A unit test fails on any string or icon that nothing uses.

**Center editors:** Scene (the session's Edit mode, with the Actor type palette), Actor type (section 7), Kotlin code (section 8), and simple viewers for
sprites, sprite sheets, and audio.

### The Scene editor

The existing scene editor (`tools/scene-editor`) grows into the GameMaker-style room editor, running in the session process. Its public API is small
(`SceneEditor.show(...)` and the Composable), so its internals can be reworked freely, and library users of `SceneEditor.show(...)` get the
improvements too. A data-only room editor inside the editor process was considered and rejected: it can't draw code Actors faithfully (Tesselar being
the extreme case), and it would mean maintaining a second renderer. Placing existing Actor types never needs a compile, so the session approach costs
nothing there; only creating or changing an Actor type does, and hot reload shortens that later.

- **Scene format unchanged:** a JSON list of serialized `Editable` Actors, as today.
- **Only placed Actor types must be `Editable`.** Actors spawned at runtime (projectiles, procedural content) need nothing. Block-built Actor types get
  their `State` class generated, so beginners never see serialization; hand-written placeable Actor types need a `State` data class, as they do today.
- **Scene settings are an Actor.** Size, background color, starting camera position and zoom, and the music to play are properties of a `Unique`,
  `Editable` Actor that applies itself in `onAdded`, the way `demo-performance`'s `Camera` Actor works. No format change; Scenes without one keep
  today's behavior.
- **New capabilities:** taking commands from the editor over the session protocol (open, save, select, place), an Actor type palette with sprite
  thumbnails, layer filtering and locking, and multi-select.

**Project settings:** targets, application ID and name, icons, the Kubriko instance's Managers and Plugins (toggling one adds the dependency and the
registration in generated wiring, shown as a diff), aspect ratio mode, and target frame rate.

## 7. Actor types, Events, and Blocks

### The Actor type editor

- **Traits:** a checklist (`Visible`, `Dynamic`, `Collidable`, `CollisionDetector`, `KeyboardInputAware`, `PointerInputAware`, `RigidBody`, `Unique`, …).
  Ticking a Trait adds its Events to the list; Plugins appear only when their Manager is enabled.
- **Body:** size, pivot, sprite (with animation settings for sprite sheets), layer, drawing order.
- **Properties:** typed fields (number, text, toggle, color, scene offset, reference to an Actor type). Each becomes an `@Exposed` property, so it is
  editable per Actor in the Scene editor.
- **Events:** the list of Events with their Blocks.

### Events

| Event | Generated from |
|---|---|
| Added to scene | `onAdded` |
| Removed from scene | `onRemoved` |
| Every frame | `Dynamic.update` |
| Draw (if overriding the sprite) | `Visible.draw` |
| Collision with *Actor type* | `CollisionDetector.onCollisionDetected`, filtered by type |
| Key pressed, released, held | `KeyboardInputAware` |
| Pointer pressed, released, moved | `PointerInputAware` |
| Gamepad button, stick | `GamepadInputAware` |
| Timer *n* finished | A generated `Timer` field updated in `update` |
| Left the scene bounds | A generated check against `ViewportManager` bounds in `update` |

### Blocks

Grouped into palettes of our own design: **Movement** (set or add velocity, move toward, bounce, wrap), **Actors** (add an Actor of a type, remove
self, find nearest), **Flow** (if, else, repeat, compare property, check key), **Values** (set or change a property, random number), **Timers**,
**Audio** (play sound, play or stop music), **Scene** (go to Scene, restart Scene, pause), **Camera** (follow, shake, zoom), and **Kotlin** (verbatim
snippet, Advanced mode only).

Every Block is a template that expands to a few lines of ordinary Kotlin calling public engine or plugin API. A sketch:

```kotlin
// Generated by Kubriko Creator from .kubriko/actors/Ball.json. Use "Convert to code" in the editor before editing by hand.
class Ball private constructor(state: State) : Editable<Ball>, Visible, Dynamic, CollisionDetector {
    // Body, Properties as @Exposed setters, State serialization …

    override fun update(deltaTimeInMilliseconds: Int) {
        // Every frame
        body.position += velocity * deltaTimeInMilliseconds.toFloat()
        // Left the scene bounds
        if (body.axisAlignedBoundingBox.min.x < viewportManager.topLeft.value.x) velocity = velocity.copy(x = -velocity.x)
    }

    override fun onCollisionDetected(collidables: List<Collidable>) {
        // Collision with Paddle
        if (collidables.any { it is Paddle }) {
            velocity = velocity.copy(y = -velocity.y)
            soundManager.play(Res.files.bounce)
        }
    }
}
```

Generated code follows the engine's performance rules (no per-frame allocation from the templates themselves; the collision filter above would use an
index loop in the real templates) and the repository's code style, so converting to code yields something a person would keep.

**Convert to code** removes the generated header and the block definition, after which the Actor type is a code Actor like any other. It is one-way,
like moving from actions to code in GameMaker, and confirmed with a diff.

### Wiring

A generated `KubrikoWiring.kt` lists the Managers and Plugins enabled in project settings, the Serialization metadata for every `Editable` Actor type
(block-built and code), and the Scene registry. Loading a Scene is `serializationManager.deserializeActors(json)` followed by `actorManager.add(...)`,
the same pattern `game-blockys-journey`'s `LoadingManager` uses, so the generated code is a few lines anyone could write by hand. The same file
implements the session entry point from section 18, so opted-in projects get level 1 and the debugger without writing anything. The template's
`Game.kt` calls into it and is never regenerated, so users can add their own Managers there.

## 8. Code editing

- **First version:** a Compose code editor with Kotlin syntax highlighting, search, multiple tabs, and Gradle compiler errors shown inline at their
  line. Paired with an "Open in IntelliJ" action for anything more.
- **Later:** completion, navigation, and live diagnostics through JetBrains' `kotlin-lsp`, once its multiplatform support is solid enough. The editor talks
  standard LSP, so the server can be swapped.
- **Later:** Compose Hot Reload in the session process, so Kotlin and Block edits apply to a running desktop game without a restart. This matters more
  than completion: Kotlin compile times are the editor's biggest disadvantage against scripting-based engines.

## 9. Running and debugging

- **Run** starts the session process in Play mode on the current Scene, or runs the project's real desktop or web target through Gradle.
- **The debug agent** answers the session protocol: Actor tree, live property values (read through `@Exposed` where available), logs from `Logger`,
  `MetadataManager` metrics, pause, single-step through a `TickSource`, and editing a property while paused.
- **The debugger panels** live in the editor: Actors, Inspector, Logs, and a frame-time graph.
- **Other targets:** Android through `adb reverse` to the same localhost port; web through a WebSocket variant of the protocol. Both come after desktop.
- **IntelliJ users** can add the debug agent to a debug build and attach the editor to a game they started themselves.

### Retiring the debug menu

1. Ship `tools/debug-overlay` and the debug agent.
2. Deprecate `tool-debug-menu`, `-api`, and `-noop` for one release, pointing to both replacements.
3. Remove them in a later release. The Showcase moves to the overlay first, as the proof that it covers what the Showcase needs.

## 10. Building and exporting

All builds go through the Gradle Tooling API against the project's own wrapper, with the editor's bundled JDK 21. The editor shows structured progress
and maps errors to files.

| Target | How | Signing |
|---|---|---|
| Desktop (run) | The project's `run` task | None |
| Web | The Wasm distribution task, zipped as static files | None needed |
| Android | Android SDK command-line tools, downloaded by the editor after the user accepts the licenses; APK and AAB | Keystore created with `keytool` and stored outside the project, with a prominent backup reminder; passwords in the OS keychain |
| Windows | `packageExe` / `packageMsi`, on Windows only | Authenticode through `signtool`, with the user's own certificate |
| macOS | `packageDmg`, on macOS only | Signing and notarization with the user's Apple Developer account |
| Linux | `packageDeb` / `packageRpm`, on Linux only | None standard |
| iOS | `xcodebuild`, on macOS with Xcode only | The user's Apple Developer account |

Desktop installers can only be built for the host OS. For the others, the editor can generate an optional GitHub Actions workflow that builds all
three, mirroring how this repository publishes the Showcase. Desktop exports run ProGuard with the rules from `app/desktop/proguard-rules.pro`.

## 11. Keeping Kubriko a library

- The engine, plugins, and tools keep their published APIs and stay fully usable from IntelliJ alone.
- A build check fails if any module under `engine/`, `plugins/`, or `tools/` depends on a module under `creator/`.
- `Tests` keeps running without the editor modules. The editor gets its own test job and its own release workflow.
- Anything the editor needs from the engine goes through the normal public API process. Awkwardness found while building the editor is treated as a
  library usability bug and fixed there for everyone.

## 12. Distribution

- One installer per OS, built with `jpackage`. Its bundled runtime cannot build user projects (jpackage strips the `bin/` launchers, section 18),
  so the editor downloads a full JDK 21 into its data folder on first launch, alongside Gradle, and points the Tooling API at it.
- The first project build downloads Gradle and dependencies, and Android export downloads the SDK. Both happen behind a progress screen with sizes shown
  up front.
- The editor checks for updates and offers them; it never updates itself silently.
- Signing the editor's own installers needs the certificates from section 10, for the project, not for users.
- MPL-2.0, like the rest of the repository.

## 13. Reference projects

### Paddle game (beginner)

Built entirely in Simple mode: three block-built Actor types (Ball, Paddle, Wall), one Scene, sounds, and a score shown through a Block. It must be
possible to recreate the Getting started tutorial's result without writing Kotlin. This proves the Block set is sufficient for a small game.

### Tesselar (advanced)

Tesselar is the opposite end: two Kubriko instances (2D logic and a separate isometric renderer built on `TriangleBatch`), a procedural world
(`LogicManager` scatters 64 NPCs and 256 trees and bushes), its own cuboid model format, and a large Compose UI layer (minimap, joystick overlay).

| Feature | Fit |
|---|---|
| Open, build, run, export, debug | Works unchanged at level 0. A good stress test for project detection and the session process with two instances. |
| Scene editing | Possible if its logic-layer Actors adopt `Editable`. `.kubriko/project.json` names the logic instance as the editable one; the session shows the top-down logic plane (already drawn by the planar renderer behind the minimap), to place hand-made landmarks among the procedural content. |
| Cuboid models | Needs an **asset editor extension point**: a project or plugin registers an editor for its own resource type, running in the session process. Tesselar's old model editor would be the first one. Stage 4. |
| Blocks | Not applicable. Its Actors stay code Actors, which is the intended path for advanced projects. |

If the extension point turns out to cost more than it's worth, Tesselar staying at level 0 to 1 is acceptable.

## 14. Copyright and naming

Ideas, workflows, and general concepts (event lists, rooms, resource trees, simple and advanced modes) are not protected. To keep a clear distance:
no "GameMaker" in the product's name or marketing beyond a factual "inspired by" credit, our own icons and artwork, our own Block names, palettes, and
wording rather than a copy of its action list, and no import or export of its file formats. This is common practice, not legal advice.

## 15. Stages

**Priority (decided 2026-10-27):** Blocks are the heart of the product, as actions were in GameMaker; Kotlin is just one of the things a Block
can do. Stage 2 therefore comes before the rest of Stage 1, and the code editor stays a minor feature (syntax highlighting and compiler errors,
no language server unless there's demand).

Each stage is worth having even if the next one never happens. Estimates are for Claude doing the implementation in working sessions with Péter
reviewing and testing; most of the elapsed time is review and hands-on testing.

| Stage | Contents | Estimate |
|---|---|---|
| 0. Spikes | Session process started through an init script against an existing project; Tooling API builds with structured errors; `jpackage` of an empty editor. Each spike answers a question in section 17. | 1–2 sessions |
| 1. Project shell | Templates, opening at levels 0 and 1, resource tree, file watching, basic code editor, Run on Desktop and Web, the session process in Edit mode (scene editor ported into it), Android signed export, host-OS installer export. | About 1 week |
| 2. Actor types and Blocks | Actor type editor, Events, Block palettes, code generation, Convert to code, generated wiring, Simple mode, the Paddle game template. | About 1 week |
| Git (alongside 2) | Version control built in, as described in section 21: a repository and first commit for new projects, change markers, commit, history, and discarding changes, then remotes. | 3–5 sessions for the local part |
| 3. Debugger | Debug agent, Play mode, debugger panels, `debug-overlay`, debug menu deprecation, Showcase migration. | 3–5 sessions |
| 4. Comforts | Hot reload, `kotlin-lsp`, Android and web debugging, iOS export, generated CI workflow, asset editor extension point. | Open-ended, each item independent |

## 16. Risks

- **Maintenance.** Every Kotlin, Gradle, Android Gradle Plugin, Compose, and Android SDK bump can break the editor in ways the library never sees.
  Mitigation: the editor's test job builds every template and the two reference projects on each of those bumps.
- **Compile times.** A slow edit-run loop undermines "just play around." Mitigation: the session process stays warm, Gradle runs continuously, and hot
  reload is prioritized within Stage 4.
- **Session protocol drift.** The protocol spans Kubriko versions. It is versioned from the first release, and the editor negotiates the version.
- **Young upstream tools.** `kotlin-lsp` and Compose Hot Reload may lag. Both are optional by design.
- **Support load.** Installers, SDK downloads, and machine-specific failures produce a steady stream of reports a library never gets.

## 17. Open questions

1. ~~Name and location~~: Kubriko Creator, in this repository under `creator/`.
2. The oldest Kubriko version the editor supports. Level 0 works with 0.7.4; levels 1 and 2 need the first release that ships `tools/creator-api`
   and `tools/creator-session`.
3. Whether the Showcase should become editable in the editor as a showcase of its own, or stay a plain IntelliJ project.
4. Whether web export should offer one-click hosting (for example GitHub Pages), or stay a zip.
5. ~~Spike results~~: answered in section 18. The entry point lives in a new `tools/creator-api` module, and the debugger reaches `Kubriko`
   instances through it; the engine stays untouched.

## 18. Stage 0 results (2026-10-27)

All three spikes ran on macOS against the published Kubriko 0.7.4 from Maven Central. The spike code is throwaway and was not committed.

### Spike 1: the session process

A Gradle init script plus a 3 KB launcher (plain Java, reflection only, so it compiles against nothing) added a `kubrikoSession` task to:

- **A throwaway test project** (one module, one `Editable` Actor type). Edit mode opened the published scene editor in the project's own process, with
  the project's `Ball` Actor type in its palette. Play mode ran the game, and the agent streamed its `Logger` entries over a localhost socket.
- **Tesselar**, unmodified (`git status` clean afterwards): Play mode ran the real game at 120 fps, and the agent connected.

How the init script works: after all projects are evaluated, it finds each module with a Compose desktop `run` task, creates a configuration that
extends the module's runtime classpath (so dependency resolution stays unified), reads the module's Kubriko engine version from it, adds
`tool-scene-editor` at that same version, and registers a `JavaExec` task that copies `run`'s main class, JVM arguments, and Java launcher. This works
for both plain JVM and Kotlin Multiplatform desktop modules, because both have the `run` task.

Measured: first start 9 s (test project) and 37 s (Tesselar, including dependency resolution). Warm starts: **3–4 s** from task invocation to an open
window, for both.

Findings:

- **Level 1 needs an opt-in entry point.** Every project builds its `SerializationManager` in its own code, so it can't be discovered. The spike used
  a convention object with a `serializationManager()` function. The real design should make this a small interface in `scene-editor-api`, found
  through `java.util.ServiceLoader` (a new public API, so it needs confirming first), and the editor can generate it for opted-in projects.
- **The debugger needs the `Kubriko` instance.** Logs work without cooperation, because `Logger` is global. The Actor tree, metrics, and pausing need
  a handle to the running instance, which nothing exposes today. Either the same entry point returns it, or the engine gets a public way to find live
  instances (an API change, to confirm first).
- **Included builds run the init script too.** It has to skip builds that weren't given its properties, as Tesselar's build-logic showed.
- **Configuration cache:** the spike ran with `--no-configuration-cache`. The real init script must be made compatible with it.
- **macOS window activation:** a process started from the background can't bring its window to the front. The editor will have to ask the session to
  activate its window when the user clicks into the scene view; that needs checking in Stage 1.

### Spike 2: the Gradle Tooling API

A small Java probe connected to the test project, listed its tasks (`run` and `packageDistributionForCurrentOS` among them), and ran
`compileKotlin` with progress listeners. With a deliberate typo, the Kotlin compiler error arrived as a **structured Problems API event**
(`Kotlin compiler: UNRESOLVED_REFERENCE`, with a line-in-file location), and also as `e: file://…:19:30` text on standard error as a fallback. A
warm model fetch and build took about 1 s. Nothing to change in the design.

### Spike 3: packaging

`packageDmg` of a Compose desktop app with Kubriko took 33 s: a 72 MB `.dmg` and a 140 MB `.app`, with a jlink'd JDK 21 runtime of seven modules.
With `includeAllModules = true`: 109 MB and 243 MB. Either way, **jpackage removes the runtime's `bin/` directory**, so there is no `java` executable
for starting a Gradle daemon. Section 12 now says the editor downloads a full JDK on first launch. Windows and Linux packaging remain untested, since
they need those hosts.

### Verdict

The design holds. The session-process architecture works on a real, unmodified, multi-module project, with fast warm starts and no version conflicts.
The changes are the entry-point opt-in for level 1 (and possibly the debugger), and downloading a full JDK instead of reusing the bundled runtime.
Stage 1 can start.

## 19. Stage 1 progress

Done (2026-10-27), not yet released:

- **`tools/creator-api`**: `CreatorEntryPoint`, with `createSerializationManager()` and `createCustomManagers()`, found through
  `java.util.ServiceLoader`. The debugger's access to `Kubriko` instances will be added to it in Stage 3, as a member with a default implementation.
- **`tools/creator-session`**: the launcher (`edit` and `play`), the `ServiceLoader` lookup with clear failures, `Logger` forwarding, and the
  versioned line protocol (`SessionProtocol`, `SessionMessage`). The session connects to Creator, which listens on a free localhost port.
- **`creator`** (the app): open a project (folder picker or first argument), Gradle-based project inspection, the resource tree (Scenes, sprites, sounds,
  music, and Kotlin files), Play and Stop, clicking a Scene to edit it, and a console with session logs, compiler errors with their locations, and
  the build output's tail when a build fails without reporting problems. The init script ships as an app resource.
- **Unit tests** cover both new library modules and every non-UI part of the app (controllers against fake builds and session channels, the
  socket server and connection over real localhost sockets, the resource scanner on real temporary folders, and the init script's contract with
  `tool-creator-session`). They run in the `Tests` workflow as `:creator:test`. Every new part of Creator comes with tests in the same change.
- Verified end to end against a test project on the locally published `0.7.5-creator`: Edit mode opened its Scene with the saved Actors, and Play
  mode ran the game and streamed its logs.

- **Project templates** (2026-10-27): "New project" on the welcome screen (name, package, and location, with validation) generates the
  Starter template and opens it. The Starter is a single-module project with desktop and web targets, one Scene, a bouncing `Ball` Actor type
  editable in the Scene editor, a `SceneLoader` Manager, and the generated `GameEntryPoint` with its service file, so it reaches level 1 with no
  setup. The template's files are app resources (`templates/<id>/`, listed in `files.txt`, stored with a `.template` suffix), the versions it
  writes are bundled from this repository's build (`creator.properties`), and the Gradle wrapper is generated by running Gradle's own `wrapper`
  task, so no wrapper binaries are shipped. Android is left out of the template until Creator can download the Android SDK, since an Android
  target can't even be configured without it. Verified end to end: the generated project compiled for both targets, its Scene opened in Edit
  mode, and Play mode ran it.

Developing against unreleased modules: until a Kubriko release ships `tool-creator-api` and `tool-creator-session`, projects can only
resolve them from Maven Local. The `[Desktop] Kubriko Creator` run configuration (`.run/Desktop_Creator.run.xml`) does it all in one go:
`./gradlew publishDesktopPublicationToMavenLocal publishWasmJsPublicationToMavenLocal publishKotlinMultiplatformPublicationToMavenLocal
:creator:run -Pcreator.localKubriko=true`. The `creator.localKubriko` property publishes the libraries under `<library.version>-creator`, so
that they never stand in for a real release in other projects, and bundles that version and `mavenLocal()` into Creator, so the projects it
creates use them. Without it, Creator uses the released version, and a project whose Kubriko version can't be found says so in the console.
`KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=<that version> ./gradlew :creator:test` also generates every template for real and
compiles it for every target; it's opt-in because it downloads Gradle and every dependency.

## 20. Stage 2 progress

First slice done (2026-10-27):

- **Data model** (`creator/.../actors`): an `ActorTypeDefinition` (name, shape, color, default size, Events) stored as readable JSON in
  `.kubriko/actors/<Name>.json`. Events: *When added to the Scene*, *Every frame*, *When a key is pressed* (arrows, Space, Enter, and WASD),
  *When an edge of the screen is touched*, and (added later) *When it collides with*. Blocks: *Set velocity*, *Add to velocity*, *Stop*, *Jump to position*, *Bounce off the edge* (only
  offered in the edge Event), *Remove this Actor*, and *Run Kotlin code*. Every generated Actor has a `velocity` in scene units per second and
  moves by it each frame, like GameMaker's built-in speed.
- **Code generation**: one ordinary Kotlin class per Actor type in the project's `generated` package (`Editable`, with a color property the
  Scene editor can change per Actor), plus a `GeneratedActorTypes` registry that the template's `GameSerialization` spreads into its
  `SerializationManager`. The output is deterministic and reads like hand-written code.
- **Opt-in**: a project with `.kubriko/project.json` (package and source folder) gets the Actor types group; new projects have it from the
  template. The Starter's Ball is now block-built: *Set velocity* when added, and *Bounce off the edge*.
- **Editor**: Actor types at the top of the resource tree (create by name, select), and an editor with the appearance and one card per Event,
  where Blocks are added from a palette, edited, reordered, and removed. Changes show at once and are saved and regenerated in the background,
  in order.
- Verified end to end: a new project's block-built Ball bounced in Play mode, and an Actor type using every Event and Block compiled for desktop
  and web (the opt-in integration test now covers this).

Collisions (2026-10-27): every generated Actor type is `Collidable`, with a mask matching its shape that follows it. A *When it collides
with <Actor type>* Event makes it a `CollisionDetector` with one branch per other Actor type, running once per overlapping Actor, and offers two
Blocks of its own: *Bounce off it* (turns the velocity away from the other Actor along the axis it was hit on) and *Remove the other Actor*.
Collision Events pointing at a deleted or unchosen Actor type are left out, so the code always compiles; saving any Actor type regenerates all of
them, since they refer to each other. Kotlin Blocks now get a scope of their own (`run { }`), so two of them can declare the same name. New
projects add the collision plugin and its Manager. Verified by building a Breakout from Blocks alone (a Ball bouncing off a keyboard-driven
Paddle and removing Bricks) and playing it.

Next in Stage 2: Properties (user-defined variables exposed in the Scene editor) with Blocks to change and test them, Timers, a score or
other on-screen text, sprites as appearance, placing Actor types from inside Creator (the scene editor already offers every Actor type in its
own window), Convert to code, detecting hand-edited generated files, and drag-and-drop from the Block palette.

Still open from Stage 1: the code editor (minor), Android signed export (with the Android SDK download, which also lets templates gain an Android
target), desktop installers, downloading a full JDK on first launch.

## 21. Version control (git)

Hobbyists rarely set up version control on their own, and a broken game with no way back is the fastest way to lose one. So git is part of
Creator from the start, kept to the few things a beginner needs, while the project stays an ordinary git repository that IntelliJ, GitHub
Desktop, or the command line can work with at the same time.

**Why the formats already fit:** Scenes and Actor type definitions are deterministic JSON (stable key order, fixed number formatting), and
generated code is deterministic too, so diffs show only what changed. Generated code is committed, so a clone builds without Creator, and
`.kubriko/local/` is git-ignored.

**Implementation:** [JGit](https://www.eclipse.org/jgit/) (Eclipse Distribution License, BSD-style, so compatible with MPL-2.0), so nobody has to
install git first and Windows needs nothing extra. If a `git` executable is found it can be offered for operations JGit handles poorly (large
repositories, some credential helpers), but nothing requires it.

**Local, first:**

- New projects get a repository with a first commit ("Created with Kubriko Creator"); a checkbox on the New project screen can turn it off.
  Opening a project that has no repository offers to create one, once.
- The resource tree marks new and changed files, and Actor types and Scenes count as changed when their JSON or generated code did.
- A Changes panel lists what changed since the last commit, grouped the way the resource tree is (an Actor type's JSON and its generated
  Kotlin are one entry), with a message field and a Commit button. Discarding an entry's changes restores both files.
- A History panel lists commits; selecting one shows what it changed, and any version of a Scene or an Actor type can be restored from it.
- The toolbar shows the current branch. Branches are created and switched from a menu; merging is left to other tools at first.
- Before a risky operation (Convert to code, deleting an Actor type, upgrading Kubriko), Creator offers to commit first.

**Remotes, second:** publishing to a new GitHub repository, push, and pull (fast-forward only, with a clear message and a pointer to another tool
when histories have diverged), with credentials kept in the OS keychain. A generated GitHub Actions workflow (section 10) builds on top of this.

**Conflicts:** Creator never writes conflict markers into the JSON it owns. If a pull would conflict, it stops before changing anything and
explains which files clash.

**Testing:** every operation runs against real temporary repositories in unit tests (JGit needs no installed git), including the "both files
of an Actor type are one entry" grouping and the deterministic formats that keep diffs small.

