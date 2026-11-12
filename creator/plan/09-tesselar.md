<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Tesselar in Creator

Working on Tesselar (`~/Projects/Tesselar`) entirely inside Creator: its play, debugging, version control, and AI host, with its own
editors inside, on the engine's rendering rather than private copies of it. Tesselar is the advanced reference project: two Kubriko
instances, a procedural world, its own cuboid model format, a version catalog, and code in `:gameplay` with `:app:desktop` beside the
desktop `:editor:model` and `:editor:region` tools. Blocks don't apply to it.

**Every item here that changes Tesselar makes that change as part of the item**, never as a follow-up: the item is done only once
Tesselar uses what it added, built and run against a `publishToMavenLocal` of this repository, in commits of its own following that
repository's conventions. The Showcase's `demo-isometric-graphics` keeps Tesselar's drawing rather than diverging from it, since the
flattened Tesselar game replaces it.

## Playing it

Its `:app:web`, `:app:android`, and `app/ios/iosApp` (with the shared `[iOS] Tesselar` scheme) and its four shared IntelliJ run
configurations play from Creator through Entry points (`08-project-upkeep.md`), which tells `:editor:model` and `:editor:region` apart
as tools that run from the Tools menu. Checked by playing each target and running each tool from Creator.

## Its editors inside Creator

**An asset editor extension point:** a project or plugin registers an editor for its own resource type, running with the project's
classes, and Creator shows it in the center for that resource. A library change (in `tool-creator-api`, members added only with
default implementations), released before Creator relies on it. Tesselar's model and region editors are the first editors it hosts,
in place of running them as tools.

Scene editing is not planned for Tesselar: its levels are procedural regions, edited in its own region editor.

## Its rendering on the engine's

Tesselar draws its scene into one `TriangleBatch` of its own. Moving it onto the engine's batched looks (`Batched`, `CustomLook`,
atlases) takes three steps, each of which is engine work first where Tesselar's case shows the engine lacks something:

1. **Textured faces in the batch.** They flush the batch and draw one by one today, for two reasons. The batch's texture is the ground
   grain, sampled repeating over unbounded coordinates, so it can't share an atlas page with the face textures; move the grain out of
   the batch's texture (drawn as untextured geometry, or as a pass of its own), then put the face textures on one page with an opaque
   white texel for `TriangleBatch.defaultU`/`defaultV`. And faces are shaded through a `ColorFilter` blending toward the ground mist,
   an added color that vertex colors, which only multiply, cannot carry; draw the mist as an overlay quad in its color at its share
   instead.
2. **The always-visible overlay**, which draws a second time into an offscreen `saveLayer`: give a layer's run a way to draw into an
   offscreen layer of its own (`TriangleBatch`'s `replace` mode composites such a group), or keep the overlay outside the batched
   looks.
3. **Cuboids as looks.** Its geometry follows the camera (orbit, tilt, and zoom are baked into the projection) rather than an engine
   body, so each `VolumetricCuboidRenderer` becomes a `CustomLook` whose revision folds in the camera basis, the shared states, and its
   own animation, and the scene pass keeps only the painter-order refinement, the terrain merge, and the overlay.

Its cuboid, terrain, shadow, water, and weather code stays its own.

## Order

Playing it first, then the rendering steps in order, then the asset editor extension point.
