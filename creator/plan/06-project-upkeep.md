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
Creator built from `main` after a version bump, without `creator.localKubriko`, would otherwise break the project's build).

## Kubriko dependencies

The sync (`KubrikoBuildFiles`) only knows the template's single-module layout: one root `build.gradle.kts` with a `commonMain` block.
Multi-module projects (a shared module plus platform apps) need it to find the module that holds the code it scans.

## Existing projects

A clear "level" per project (any project: build, play, export; with `Editable` Actor types and a `CreatorEntryPoint`: Scene editing; opted
in: Actor types), an opt-in flow that generates the entry point, its service file, and `.kubriko/project.json` as a confirmed diff, and
explanations instead of failures for layouts Creator can't interpret.

**Tesselar** (two Kubriko instances, procedural world, its own cuboid model format) is the advanced reference: it plays unchanged; Scene
editing needs its logic-layer Actors to adopt `Editable` and `.kubriko/project.json` to name the editable instance; cuboid models need the asset
editor extension point (`07-later.md`); Blocks don't apply.
