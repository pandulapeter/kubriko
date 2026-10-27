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

Template files (`Game.kt`, the build files) belong to the user after creation, yet a newer Creator sometimes needs them to change (collisions
needed `CollisionManager` in `Game.kt` and the collision plugin in the build). `.kubriko/project.json` records the template version a project
was created with; when Creator needs something newer, it shows the exact change as a diff, offers to commit first, and applies it only when the
file still looks like the template produced it, explaining what to add by hand otherwise. A generated `KubrikoWiring.kt` (Managers and Plugins
from project settings, Scene registry) would shrink what lives in hand-owned files.

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
