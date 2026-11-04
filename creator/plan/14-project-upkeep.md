<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Project upkeep

## Template drift

Moving more out of hand-owned files into generated ones (the Managers from project settings, a Scene registry), so that fewer upgrades
are needed.

## Kubriko versions

Opted-in projects follow Creator's own Kubriko version. Left: a link to the release notes of the versions a raise skipped, suggesting a
Creator update for a project on a newer version than Creator's, and refusing to raise a project to a version that isn't published (a
packaged Creator built from `main` after a version bump would otherwise break the project's build).

## Incremental builds

Creator's builds turn on Kotlin's internal incremental compilation of common code (`IncrementalCompilationPolicy`, around KT-62686).
Once projects are on Kotlin 2.5, use its separate compilation of common code instead, and remove the policy.

## Kubriko dependencies

The sync (`KubrikoBuildFiles`) only knows the template's single-module layout: one root `build.gradle.kts` with a `commonMain` block.
Multi-module projects (a shared module plus platform apps) need it to find the module that holds the code it scans.

## Existing projects

A clear "level" per project (any project: build, play, export; with `Editable` Actor types and a `CreatorEntryPoint`: Scene editing; opted
in: Actor types), an opt-in flow that generates the entry point, its service file, and `.kubriko/project.json` as a confirmed diff, and
explanations instead of failures for layouts Creator can't interpret.

- **Every entry point** and **several editions:** see Entry points and Editions below, which every layout but the template's waits for.
- **The code's module.** Generated code, the build file sync, and the sources Creator scans need the module that holds the game's code,
  which in a multi-module project isn't the desktop one (see Kubriko dependencies above).
- **Types and Scenes in every Kubriko module.** The sidebar already lists every group in each module that uses Kubriko
  (`ProjectModules`), but only the module the generated code goes to (`CreatorProjectSettings.sourceDirectory`) adds Actor types and
  Manager types, and only the one the game loads Scenes from adds Scenes. Each of them should take new ones (the user's choice, after
  the sidebar showed them empty):
  - **Model.** `.kubriko/project.json` maps each module that holds types to its package and source folder (the current pair becomes
    the game's module's entry), and each type file records its module (`"module": "tools"`, absent for the game's), so a project
    without the field reads as before. Both need a format raise with an empty migration. Moving a type between modules is not offered:
    it would be a modularization change, which Creator leaves to the user.
  - **Code.** Each module gets its own generated package with its own registry, Scene loader, audio, and `KubrikoWiring` part, written
    by the same generators run once per module, and the build file sync runs per module (each depends on the libraries its own sources
    name). A type may refer only to types of its own module and of modules it depends on; the dependencies come from the Gradle model
    `DesktopModules` already fetches (`project(":x")` lines), and `validate` rejects the rest in the user's words, naming both modules.
    Parent types follow the same rule.
  - **Scenes.** A Scene is named by its path within its module's Scenes folder, so names repeat across modules: `SceneStore` keys
    Scenes by module and path (`tools/sandbox` written as `tools:sandbox` wherever a module is named, the game's needing none), commands
    and AI tools take that name, and placing an Actor checks that the Scene's module can reach the type's. Today two modules with a Scene
    of the same name show both in the sidebar, but every command acts on the first file found; this removes that ambiguity.
  - **Commands and AI.** `create_actor_type`, `create_manager_type`, `create_scene`, and `create_folder` take an optional `module`
    (absent: the game's), `list_*` and the digest name each item's module only in a project of several, and `get_project_overview`
    lists the modules and what each may refer to.
  - **Scene editor.** It loads the game module's classes, so a Scene of another module opens only when the game depends on it; one that
    can't says why (`SceneEditingState.Unsupported`) rather than failing to create its Actors.
  - Checked with unit tests per part, the format fixtures, and one end-to-end scenario on a two-module copy of the Breakout fixture that
    adds a type and a Scene to the second module, builds, and plays.
- **Every resource.** The sidebar lists only the folders Creator knows about (sprites, sounds, Scenes); a project's own resource
  folders (`files/model`) should show too, opening in the system's editor until a project registers an editor of its own.

## Entry points

An entry point is something the project can start: a desktop `main`, a web page, an Android app, or an iOS app. A template project has
one per target it turns on and should never show more unless asked to; Tesselar has its game on four platforms beside two desktop
tools; a game with a demo has every target twice. Today Play's web, Android, and iOS targets come only from `GameTargets`, which reads
the template's layout (one module, `androidApp/`, and `iosApp/` at the root), so a project with an entry point module per platform plays
only on the desktop, and the toolbar's choice of the game's module (`GameModuleChoice`) mixes up what to play with what the Scene
editor loads. Three levels, each hidden until it is needed: a beginner sees one entry point per target and the Play menu as it is
now; someone who wants a demo adds an edition from Project settings without touching Gradle; and a project laid out by hand in
IntelliJ is read as it is, its run configurations included, without Creator changing its build.

- **Found in the build.** Creator reads every entry point from the Gradle model `DesktopModules` already fetches, never changing the
  build: a `run` task is a desktop one, `wasmJsBrowserDevelopmentExecutableDistribution` a web one (it exists only with
  `binaries.executable()`), an `install…Debug` task an Android app (each flavor its own), and `embedAndSignAppleFrameworkForXcode` the
  framework of an iOS app, whose Xcode project is found beside it or in `iosApp/` and whose schemes come from `xcodebuild -list -json`
  (each scheme its own entry point), with a console line naming the missing scheme when a clone has none shared. Each has a stable ID
  from its kind and where it is (`desktop:app:desktop`, `android:app:android:demo`, `ios:app/ios/iosApp:[iOS] Tesselar`), which
  everything below is keyed by. `GameModules` becomes `EntryPoints`, and `WebPlayer`, `AndroidPlayer` (its `output-metadata.json` under
  the chosen module's `build/`), and `IosSimulatorPlayer` take the chosen entry point instead of their fixed paths. An entry point that
  isn't the template's starts as it always does, so Play from this Scene and the Log Blocks switch say in the console that they can't
  reach it.
- **The game's or a tool.** Tools (Tesselar's model and region editors, a level generator, an asset converter with a window) are left
  out of Play's choices and run from a Tools menu beside Play (and the `run_tool` command), their output in the console like a game's,
  without what only a game has: Play from this Scene, the Log Blocks switch, hot reload's restart from the same Scene, and the game
  tools (`start_game` and the rest), which a tool's session refuses as `GameStep.Elsewhere` does. A tool and the game can run at once,
  each with its own Stop. Unless told otherwise, an entry point is the game's when it is the template's, when its module has an entry
  point of another kind beside it under the same parent (`:app:desktop` beside `:app:web`), or when it is the project's only desktop
  one; any other desktop-only one is a tool. Tesselar comes out right with no setting; a wrong guess is put right in Project settings
  → Entry points, and that is committed.
- **What the Scene editor loads** is the game's desktop entry point (of the edition chosen, below), never a tool's, so running a tool or
  playing on another target never builds its classes again. Today's toolbar choice of the game's module becomes this, and is offered
  only when the game has several desktop entry points, which is rare once tools are told apart.
- **Choosing which to play.** For each kind, the entry point Play starts is this computer's choice (`.kubriko/local/module.json`
  becomes `entry-points.json`, read from the old file once), offered in Play's menu only when a kind has several. The project names
  a default for each kind in `.kubriko/project.json`, so a fresh clone plays the same one as everyone else; until it names one, the
  first in Gradle's order is the default, as today.
- **From IntelliJ.** Shared run configurations (`.run/*.run.xml` and `.idea/runConfigurations/*.xml`) and this computer's own (the
  `RunManager` of `.idea/workspace.xml`, read only, since IntelliJ writes that file whenever it likes) are entry points too, named as
  IntelliJ names them (Tesselar's `[Desktop] Tesselar`, `[Web] Tesselar`, `Model Editor`, and `Region Editor`):
  - Gradle ones (`GradleRunConfiguration`) by their task: a `run`, `wasmJsBrowser…Run`, or `install…` task is the entry point of the
    module it names, played with the configuration's script parameters (its `-P` and `-D` values join Creator's build arguments), VM
    options, and environment. The web's dev server task plays the way `WebPlayer` plays, from the distribution. Several tasks, or
    tasks that start nothing, can't play.
  - Application and Kotlin ones (`Application`, `JetRunConfigurationType`) by main class and module (IntelliJ's module name, such as
    `Tesselar.app.desktop.desktopMain`, mapped back to its Gradle project), with VM options, program arguments, environment, and the
    working directory; Creator starts them as it starts the game now, with the module's launch classpath and the session. A second
    `main` in a module is a second desktop entry point of it.
  - Android app ones by module and launch activity; the build variant isn't part of the configuration but IntelliJ's choice on each
    computer, so the flavor comes from Creator's own choice (Editions below).
  - Kotlin Multiplatform's iOS ones by Xcode project, scheme, and configuration.
  - `$PROJECT_DIR$`, `$MODULE_DIR$`, `$USER_HOME$`, and environment variables written as IntelliJ writes them are expanded. Anything
    else (another macro, a before-launch step that isn't a Gradle task, a compound, a test, a remote debugger) is listed with why it
    can't play from Creator, never hidden or half run, and a configuration that maps onto a found entry point takes its place rather
    than showing it twice.
  - IntelliJ's files change only when the user edits such an entry point in Creator (below), as a confirmed diff that keeps every
    element and attribute Creator doesn't know, in the order it found them, so IntelliJ opens the file as it was plus the change.
    A file that changes outside Creator is read again within a second, as the build files are.
- **Settings per entry point.** Project settings → Entry points (`05-settings.md`) shows and edits them. Each value lives where
  IntelliJ would read it too, so a project opened in IntelliJ starts what Creator starts: an entry point's name, task or main class,
  arguments, JVM options, environment, and working directory are a shared run configuration in `.run/`, which Creator writes only
  once an entry point needs more than the build gives it, so a beginner's project gains no file; what only Creator needs (game or
  tool, the edition, the default for each kind, hidden) is in `.kubriko/project.json` under the entry point's ID; and what is this
  computer's only (a path on this machine, a secret in an environment variable) goes to `.kubriko/local/entry-points.json`, merged
  over the shared one. Format raise with an empty migration.
- **Adding one.** New entry point (Project settings → Entry points, and the `create_entry_point` command and AI tool) makes one of
  three, the simplest first, each with its own short explanation in the dialog:
  - **A variation:** an existing entry point under a name of its own with other arguments ("Desktop, starting in Scene `boss`",
    "Desktop with cheats", a Gradle property the build reads), so trying a setup is one Play instead of a code change. Only a run
    configuration is written; the build is untouched.
  - **An edition** (below): the whole game again, on every target it has.
  - **A tool:** a new desktop module (`tools/<name>`, a template of its own: a `main` opening a window with a Kubriko viewport and
    the game's code module as a dependency) for a level editor or a generator, as a confirmed diff of `settings.gradle.kts` and the
    new files. Its module shows in the sidebar like Tesselar's editors do, and it is a tool from the start. For a project not on the
    template's layout it waits for The code's module above.
  - Anything else (a module laid out by hand, an Android flavor that is more than an edition) is written in IntelliJ or by hand,
    and found from there.
- **Commands and AI.** `get_project_overview` lists the entry points by kind (only in a project of more than one per target), `play`
  and `start_game` take an optional `entryPoint` and `edition` beside `target`, keeping today's rule that a call with none plays the
  same place every time, and `create_entry_point`, `set_entry_point` (its settings, as Project settings shows them), and `run_tool`
  join them, with the same diffs and confirmations as the panel.
- **Checked** against the Showcase's `app/` modules and Tesselar's (the same layout) with unit tests over recorded Gradle models,
  run configuration files written by IntelliJ (not by hand; Tesselar's four among them), `workspace.xml`, and `xcodebuild -list`
  output, the round trip of an edited `.run.xml` byte for byte apart from the change, and one end-to-end scenario playing a
  multi-module fixture in the browser and running one of its tools beside it.

## Editions

A demo and a full version, a free and a paid one, a build for an event: the same game made more than once, with some differences.

- **The beginner's way, in one module.** A template upgrade (as `GameSettings-10` was) adds editions to the build: `gradle.properties`
  lists them (`game.editions=full,demo`, the first being the default) with what differs for each (`game.edition.demo.name`,
  `.applicationIdSuffix`, so both install side by side on a phone, and `.version`), and the build reads `-Pkubriko.edition` (which
  Creator passes, and IntelliJ and plain Gradle leave out, building the default) into a generated `GameEdition` constant beside
  `GameSettings`. Blocks get a condition "the edition is", and hand-written code reads `GameEdition.current`. The constant is known at
  compile time in a release build, so the shrinker drops what only another edition uses, as it drops Log Blocks
  (`13-export-and-distribution.md`), and the export check looks for it. Each edition other than the default gets a run configuration
  per target in `.run/`, so IntelliJ plays the demo too. Removing the last edition but one removes the block again as a diff.
- **Scenes per edition.** An edition can leave Scenes and Scene folders out (`.kubriko/project.json`); its generated Scene registry
  lists only its own, a Block naming a left-out Scene is a health check warning for that edition, and its build leaves the files out
  of its resources. Whether Compose resources can be filtered per build without a module per edition is the first thing to find
  out; if they can't, a left-out Scene is only unreachable, and Project settings says so rather than promising a smaller game.
- **The advanced ways, found rather than made:** a module per edition (`:app:demo:desktop`), Android product flavors, several iOS
  schemes, a Gradle property the build already reads, or IntelliJ run configurations passing one. Project settings → Entry points
  puts each into an edition (a flavor, scheme, or module whose name is an edition's starts there), so Play and Export treat it as
  they treat the generated ones, without Creator touching the build; a project with editions of both kinds is fine.
- **Playing and exporting.** Play's menu offers the edition only when there are several. Choosing one chooses every target's entry
  point together and the Scene editor's classes (a Block reading `GameEdition` previews as it plays); Export makes the editions
  chosen in one go, each named, versioned, and signed as its own identity says (`13-export-and-distribution.md`).
- **Checked** with unit tests over the property block (parsed exactly as the template's Gradle code parses it, as `GameSettings.of`
  is), the registry per edition, and recorded flavor models, and one end-to-end scenario on a copy of the Breakout fixture that adds a
  demo edition leaving a Scene out, builds both, and plays the demo, whose `GameEdition` Block shows.

**Tesselar** (two Kubriko instances, procedural world, its own cuboid model format, a version catalog, and code in `:gameplay` with
`:app:desktop` beside the desktop `:editor:model` and `:editor:region` tools) is the advanced reference. It plays unchanged on the desktop,
`:app:desktop` being the first of its desktop modules, and the toolbar picks another; its `:app:web`, `:app:android`, and `app/ios/iosApp`
(with the shared `[iOS] Tesselar` scheme), and its four shared IntelliJ run configurations, wait for Entry points, which tells its
`:editor:model` and `:editor:region` apart as tools. Scene editing needs its logic-layer Actors to adopt `Editable` and `.kubriko/project.json` to name the
editable instance, and is worth little there: its levels are procedural regions, edited in its own region editor. Its models and regions
need the asset editor extension point (`17-later.md`), where its two editor modules would be the first editors Creator hosts; until
then they run from the Tools menu. Blocks don't
apply. The goal is working on Tesselar entirely inside Creator: its play, debugging, version control, and AI host, with its own editors
inside, on the engine's rendering rather than private copies of it (`06-batched-rendering.md`).
