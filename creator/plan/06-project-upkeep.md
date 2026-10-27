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

`templateVersion` and the upgrade flow exist (`template/ProjectUpgrader`, see `../CLAUDE.md` → Commands), with one step: version 2
passes the generated Managers to Kubriko. Still to do: build files (a plugin a newer Creator needs, as collisions did), steps that
remove or replace lines rather than add them, and moving more out of hand-owned files into generated ones (the Managers and Plugins
from project settings, a Scene registry), so that fewer upgrades are needed.

## Kubriko versions

Creator supports a declared range of Kubriko versions. Raising a project's version is an explicit action showing the release notes, never silent.

## Existing projects

Opening works for any Kubriko project with a Compose desktop module (sessions, Play). Still to do: a clear "level" per project (any project:
build, play, export; with `Editable` Actor types and a `CreatorEntryPoint`: Scene editing; opted in: Actor types), an opt-in flow that
generates the entry point, its service file, and `.kubriko/project.json` as a confirmed diff, and explanations instead of failures for layouts
Creator can't interpret.

**Tesselar** (two Kubriko instances, procedural world, its own cuboid model format) is the advanced reference: it plays unchanged; Scene
editing needs its logic-layer Actors to adopt `Editable` and `.kubriko/project.json` to name the editable instance; cuboid models need the asset
editor extension point (`07-later.md`); Blocks don't apply.
