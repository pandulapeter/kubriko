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

- **The game's module, shared.** The toolbar's choice of the game's module is this computer's (`.kubriko/local/module.json`); an
  opted-in project with several desktop modules should name it in `.kubriko/project.json`, so everyone working on it plays the same one.
- **Every entry point.** Play's web, Android, and iOS targets come only from `GameTargets`, which reads the template's layout of an
  opted-in project (one module, `androidApp/`, and `iosApp/` at the root), so a project with an entry point module per platform plays only
  on the desktop. Creator should read every entry point from the Gradle model `DesktopModules` already fetches, never changing the build:
  a `run` task is a desktop one, `wasmJsBrowserDevelopmentExecutableDistribution` a web one (it exists only with `binaries.executable()`),
  an `install…Debug` task an Android app (each flavor its own), and `embedAndSignAppleFrameworkForXcode` the framework of an iOS app, whose
  Xcode project is found beside it or in `iosApp/` and whose scheme comes from `xcodebuild -list -json`, with a console line naming the
  missing scheme when a clone has none shared. `GameModules` becomes the entry points by kind, each kind with a choice of its own
  (`.kubriko/local/module.json`), offered in Play's menu only when a kind has several; the desktop's still decides the Scene editor's
  classes. `WebPlayer`, `AndroidPlayer` (its `output-metadata.json` under the chosen module's `build/`), and `IosSimulatorPlayer` take the
  chosen entry point instead of their fixed paths. Project settings → Targets lists what was found, read only, for a project that isn't
  opted in; turning targets on and off stays the template's. An entry point that isn't the template's starts as it always does, so Play
  from this Scene and the Log Blocks switch say in the console that they can't reach it. Checked against the Showcase's `app/` modules
  (the same layout as Tesselar's) with unit tests over recorded models, and one end-to-end scenario playing a multi-module fixture in
  the browser.
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

**Tesselar** (two Kubriko instances, procedural world, its own cuboid model format, a version catalog, and code in `:gameplay` with
`:app:desktop` beside the desktop `:editor:model` and `:editor:region` tools) is the advanced reference. It plays unchanged on the desktop,
`:app:desktop` being the first of its desktop modules, and the toolbar picks another; its `:app:web`, `:app:android`, and `app/ios/iosApp`
(with the shared `[iOS] Tesselar` scheme) wait for Every entry point. Scene editing needs its logic-layer Actors to adopt `Editable` and `.kubriko/project.json` to name the
editable instance, and is worth little there: its levels are procedural regions, edited in its own region editor. Its models and regions
need the asset editor extension point (`17-later.md`), where its two editor modules would be the first editors Creator hosts. Blocks don't
apply. The goal is working on Tesselar entirely inside Creator: its play, debugging, version control, and AI host, with its own editors
inside, on the engine's rendering rather than private copies of it (`06-batched-rendering.md`).
