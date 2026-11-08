<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Batched rendering

Every `Visible` Actor costs its own canvas operations each frame (`save`, a transform, a clip, its `draw()`, and `restore`, in
`engine/.../manager/Layers.kt`), so a Scene's drawing cost grows with its Actor count, worst on the web, where each operation crosses
the Wasm boundary. Tesselar solved this for itself, and the gains are proven there: one Actor draws everything into a `TriangleBatch`,
geometry is recomputed only when it changes, bitmaps are marked immutable, and textures share their mip levels. This plan makes the
general parts of that the engine's own, so that every Kubriko game can use them, Creator's generated Actor types use them without the user
knowing, and Tesselar can drop its copies, a step toward working on Tesselar entirely inside Creator (`14-project-upkeep.md`).

**Builds on** the engine's `Batched` Trait and its looks (`SpriteLook` for sprites), `TriangleBatch`'s sampling
(`textureTileMode`, `textureFilterQuality`), `ImageBitmap.markImmutable()`, and the sprites plugin's `TextureMipChain` and
atlases (`SpriteManager.loadAtlas`, `SpriteAtlas`, `SpriteRegion`, `SpriteLook.setRegion`, and `AnimatedSprite.fromAtlas`).

## Tesselar

- **Every item here that unlocks a change in Tesselar makes that change as part of the item**, never as a follow-up: the item is done
  only once `~/Projects/Tesselar` uses what it added, built and run against a `publishToMavenLocal` of this repository, in commits of
  its own following that repository's conventions. Its pinned Kubriko version is already `0.8.1`, the release that holds the engine
  part; it builds only against a local publish until that release is out. The Showcase's `demo-isometric-graphics`, a copy of
  Tesselar, takes the same change where it has the code it replaces.
- **Its scene pass is not yet a client of the engine's custom emission and change gating**, for three reasons the engine part could not
  remove on its own. Its geometry follows the camera (orbit, tilt, and zoom are baked into the projection), not an engine body, so a
  body-and-revision gate would rebuild every cuboid on every camera move; its own gates key on the camera basis instead. The
  always-visible overlay draws a second time into an offscreen `saveLayer`, which a run of a layer cannot express. And its textured
  faces still flush the batch and draw one by one (below). Each `VolumetricCuboidRenderer` can become a `CustomLook` whose revision
  folds in the camera basis, the shared states, and its own animation once all three are solved, with the scene pass keeping only the
  painter-order refinement, the terrain merge, and the overlay.
- **Atlases do not let its textured faces join the batch,** so it takes no atlas change. A native draw takes one image, and the scene
  batch's is the ground grain, sampled repeating over unbounded coordinates: on an atlas page, repeating would wrap across the whole
  page rather than the grain's region, so the grain cannot share a page with face textures (and it has one face texture, so different
  textures were never what kept them apart). Textured faces are also shaded through a `ColorFilter` that blends toward the ground
  mist, an added color that vertex colors, which only multiply, cannot carry (an overlay quad in the mist color at the mist's share
  could). What would unlock it: the grain leaving the batch's texture (drawn as untextured geometry, or as a pass of its own), then one
  page holding the face textures and an opaque white texel for `TriangleBatch.defaultU`/`defaultV`, with the mist as an overlay.
  `demo-isometric-graphics` has neither the grain nor the mist and would need only that white texel, but it is the copy of Tesselar
  that the flattened Tesselar game replaces, so it keeps Tesselar's drawing rather than diverging from it.
- **Textured batch improvements replace no workaround there yet.** Its batch texture is the repeating ground grain, which wants the
  default sampling, and its mean-color stand-in now reads the sprites plugin's `TextureMipChain.meanArgb`; the size test that picks
  it stays its own, since its faces are not sprite looks.
- Its cuboid, terrain, shadow, water, and weather code stays its own.

## Testing

- A generated game's Scene drawn plain and batched compared as images in Creator's end-to-end harness under Xvfb, with a tolerance for
  edge smoothing on ellipses (the engine's own comparison is `BatchedRenderingComparisonTest`).
