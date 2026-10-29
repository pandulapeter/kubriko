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
are needed. The plugins a project depends on, and the plugin Managers it creates, already follow its code (`../CLAUDE.md`).

## Kubriko versions

Opted-in projects follow Creator's own Kubriko version. Left: a link to the release notes of the versions a raise skipped, suggesting a
Creator update for a project on a newer version than Creator's, and refusing to raise a project to a version that isn't published (a
packaged Creator built from `main` after a version bump would otherwise break the project's build; `:creator:run` always uses local
libraries now).

## Kubriko dependencies

The sync (`KubrikoBuildFiles`) only knows the template's single-module layout: one root `build.gradle.kts` with a `commonMain` block.
Multi-module projects (a shared module plus platform apps) need it to find the module that holds the code it scans.

## Existing projects

A clear "level" per project (any project: build, play, export; with `Editable` Actor types and a `CreatorEntryPoint`: Scene editing; opted
in: Actor types), an opt-in flow that generates the entry point, its service file, and `.kubriko/project.json` as a confirmed diff, and
explanations instead of failures for layouts Creator can't interpret.

- **The game's module.** Creator plays the first module with a `run` task, so a project with several desktop applications (a game plus
  its own tools) plays whichever comes first in Gradle's order. `.kubriko/project.json` should name the game's module, and a project
  that isn't opted in should be asked once when there is more than one, and remembered in `.kubriko/local/`.
- **The code's module.** Generated code, the build file sync, and the sources Creator scans need the module that holds the game's code,
  which in a multi-module project isn't the desktop one (see Kubriko dependencies above).
- **Every resource.** The sidebar lists only the folders Creator knows about (sprites, sounds, Scenes); a project's own resource
  folders (`files/model`) should show too, opening in the system's editor until a project registers an editor of its own.

**Tesselar** (two Kubriko instances, procedural world, its own cuboid model format, a version catalog, and code in `:gameplay` with
`:app:desktop` beside the desktop `:editor:model` and `:editor:region` tools) is the advanced reference. It plays unchanged, though only
because `:app:desktop` comes first. Scene editing needs its logic-layer Actors to adopt `Editable` and `.kubriko/project.json` to name the
editable instance, and is worth little there: its levels are procedural regions, edited in its own region editor. Its models and regions
need the asset editor extension point (`14-later.md`), where its two editor modules would be the first editors Creator hosts. Blocks don't
apply. The realistic ceiling is Creator as its play, debugging, version control, and AI host, with its own editors inside.
