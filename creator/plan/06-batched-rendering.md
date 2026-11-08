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
(`textureTileMode`, `textureFilterQuality`), `ImageBitmap.markImmutable()`, and the sprites plugin's `TextureMipChain`.

**Order.** The sprites section touches only `engine/`, `plugins/sprites`, and the Showcase, so it waits for nothing in
Creator. The Creator section follows it and comes before `08`'s Visible Trait and `09`'s appearance features (sprite
sheets, tinting), so those are built on batched looks rather than moved onto them later. Everything here only adds to the
public API: existing `Visible` Actors draw exactly as before.

## Sprite atlases

- `SpriteManager` packs the sprites a game declares as belonging together into atlas pages at load, off the main thread: pages of a size
  every target supports, each sprite padded by its own edge pixels so neighbors never bleed in, sprite sheets packed whole so
  `AnimatedSprite`'s frames stay regions of one page. Batched sprite looks name a region, and a run continues while its page stays the
  same.
- **Pages are marked immutable.** On the Skia targets, Compose turns a mutable bitmap into an image on every draw, re-copying its pixels.
  Decoded sprites are already immutable (`toComposeImageBitmap` marks them), but a page is a bitmap drawn into, so it is marked with the
  engine's `ImageBitmap.markImmutable()` once packed. A page's mip levels come from the sprites plugin's `TextureMipChain`, and a
  `SpriteLook` naming a region of a page joins the runs of every other sprite on that page.
- **At load, not at build time,** so that hand-written games benefit too and projects keep no generated images in version control;
  Creator only decides what goes together. Build-time packing can follow if loading times ever ask for it.
- The packer works on sizes alone, so the layout, padding, and pages are unit tested without images; only copying pixels needs Skia.

## Creator

- Generated Actor types implement the batched Trait with a declarative look from their shape, color, and sprite
  (`ActorTypeCodeGenerator.drawFunction` and its sprite counterpart stop generating `draw()`), and `PreviewActor` draws a not yet compiled
  type through the same look, so the Scene editor's preview and the game always match. Hand-written Actor types are unchanged.
- The generated wiring declares one atlas of every sprite the project's Actor types use, split into pages when it doesn't fit, so a
  beginner never sees the word atlas. Sprite sheets (`09-blocks.md` → Appearance, `10-sprite-editor.md`) join it whole.
- Every later appearance feature (sprite sheets and frames, tinting, drawing order, layer, the visible toggle) is a property of the look.
  A type that one day draws with code of its own (a Kotlin Block in a drawing Event) becomes a plain `Visible`, ending the run it sits in.
- Opted-in projects are raised to Creator's own Kubriko version (`../CLAUDE.md` → Project files), so the generated code may rely on the
  new library API; the integration test builds every template with it.

## Tesselar

- **Every item here that unlocks a change in Tesselar makes that change as part of the item**, never as a follow-up: the item is done
  only once `~/Projects/Tesselar` uses what it added, built and run against a `publishToMavenLocal` of this repository, in commits of
  its own following that repository's conventions. Its pinned Kubriko version is already `0.8.1`, the release that holds the engine
  part; it builds only against a local publish until that release is out. The Showcase's `demo-isometric-graphics`, a copy of
  Tesselar, takes the same change where it has the code it replaces.
- **Its scene pass is not yet a client of the engine's custom emission and change gating**, for three reasons the engine part could not
  remove on its own. Its textured faces are drawn one by one through a parallelogram `drawImage` with a shading `ColorFilter`, each
  flushing the batch, because faces of different textures cannot share one batch until they share an atlas page (Sprite atlases above).
  Its geometry follows the camera (orbit, tilt, and zoom are baked into the projection), not an engine body, so a body-and-revision
  gate would rebuild every cuboid on every camera move; its own gates key on the camera basis instead. And the always-visible overlay
  draws a second time into an offscreen `saveLayer`, which a run of a layer cannot express. Once atlases exist, each `VolumetricCuboidRenderer`
  can become a `CustomLook` whose revision folds in the camera basis, the shared states, and its own animation, with the scene pass
  keeping only the painter-order refinement, the terrain merge, and the overlay.
- **Textured batch improvements replace no workaround there yet.** Its batch texture is the repeating ground grain, which wants the
  default sampling, and its mean-color stand-in now reads the sprites plugin's `TextureMipChain.meanArgb`; the size test that picks
  it stays its own, since its faces are not sprite looks.
- Its cuboid, terrain, shadow, water, and weather code stays its own.

## Testing

- A generated game's Scene drawn plain and batched compared as images in Creator's end-to-end harness under Xvfb, with a tolerance for
  edge smoothing on ellipses (the engine's own comparison is `BatchedRenderingComparisonTest`).
- The atlas packer's layout, padding, and pages, without images.
- Creator: generated code for every shape and for sprites, the preview's look, and the atlas declaration, in `:creator:core`'s tests.
