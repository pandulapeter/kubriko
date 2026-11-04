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

**What moves and what stays.** Moving to the engine: batching, change-gated geometry, immutable bitmaps, texture mip chains and the
mean-color stand-in for tiny textures, the render warm-up, and background passes for heavy fields. Staying in Tesselar, since they are
about cuboids and its world, not drawing in general: painter-order refinement, the terrain mesh, shadows, water, and weather.

**Order.** The engine and sprites sections touch only `engine/`, `plugins/sprites`, and the Showcase, so they wait for nothing in Creator
and can run in a session of their own beside `04` and `05`. The Creator section follows them and comes before `08`'s Visible Trait and
`09`'s appearance features (sprite sheets, tinting), so those are built on batched looks rather than moved onto them later. Everything
here only adds to the public API: existing `Visible` Actors draw exactly as before. The engine and sprites parts are general engine
features with users of their own (Tesselar and the Showcase), not library changes made for Creator (`../CLAUDE.md` → Library side), so
they ship with the library's next release whenever they are ready.

## Engine

- **Batched Actors:** a new Trait extending `Visible` (its name decided by the item) whose Actors describe their look instead of drawing it.
  Two levels:
  - Declarative looks: a rectangle, an ellipse, or a sprite region, each with a color or tint, which the engine turns into geometry.
  - Custom emission into a `TriangleBatch`, for game-specific geometry such as Tesselar's cuboids.

  The layer loop fills one reused batch per layer with consecutive batched Actors in their existing sort order, and draws it before any
  plain `Visible` Actor, which then draws as today, so the paint order stays exactly what the sort says. Where the sort leaves batched
  Actors of the same drawing order unordered, they are grouped by texture for longer runs. Each Actor's corners are transformed by its
  body on the CPU (position, rotation, scale, and pivot), replacing the per-Actor `save`, transform, and `restore`. A look lies inside the
  body by construction, so it needs no clip. Runs roll over at `TriangleBatch.willOverflow`. The Trait implements `draw()` through a
  one-off batch, so anything that draws a `Visible` directly (the scene editor, debug tools, Creator's preview) keeps working. Steady
  state allocates nothing.
- **Change-gated geometry:** an Actor's vertices are recomputed only when its body or look changes (`BoxBody` already tracks changes;
  a look carries a revision, as Tesselar's shared states do), and appended from the cache otherwise. A run whose members neither changed
  nor reordered redraws the previous frame's arrays without filling them again, so a mostly static Scene (a tile map) costs one native
  draw per run. This is Tesselar's terrain-mesh rule made general.
- **Textured batches:** `TriangleBatch` gains clamped sampling and nearest-neighbor filtering (it only repeats with the default filter today),
  as pixel art and atlases need. Where `TriangleBatchSupport.isTextureSampledPerVertex()` is false (some Android drivers), sprite runs
  fall back to one `drawImageRect` per sprite from the same atlas page, which Android's renderer still merges. A sprite too small on
  screen to show its texture is drawn in its texture's mean color, as Tesselar's faces are.
- **Texture mip chains:** Tesselar's `TextureMipChainCache` (each level built once per texture, shared by everything using it) moves
  to the sprites plugin, so sprites shown smaller than their size neither shimmer nor sample more than they show.
- **Render warm-up and background passes:** Tesselar's `RenderWarmUp` (times the frame until its cost stops falling, so a loading
  screen can wait for the game to run smoothly) and `BackgroundPass` (computes a heavy field off the frame thread from a snapshot of what
  it reads) become engine helpers, each with its own KDoc and tests.
- **Rules and documentation:** the root `CLAUDE.md` → Performance gains Tesselar's rule that per-frame scalar providers are
  primitive-returning `fun interface`s, never Kotlin function types, which box; its `TriangleBatch` section stops calling batching
  something no game gets without restructuring; the engine's README shows batched Actors. Since Creator's AI reads those sections
  (`KubrikoApi`), it learns batched Actors from the same change.
- **Showcase:** `demo-performance` switches between plain and batched Actors, as the in-repo example of both.

## Sprite atlases

- `SpriteManager` packs the sprites a game declares as belonging together into atlas pages at load, off the main thread: pages of a size
  every target supports, each sprite padded by its own edge pixels so neighbors never bleed in, sprite sheets packed whole so
  `AnimatedSprite`'s frames stay regions of one page. Batched sprite looks name a region, and a run continues while its page stays the
  same.
- **Pages are marked immutable.** On the Skia targets, Compose turns a mutable bitmap into an image on every draw, re-copying its pixels.
  Decoded sprites are already immutable (`toComposeImageBitmap` marks them), but a page is a bitmap drawn into, as Tesselar's bakes and
  mip levels are, which is why Tesselar marks every finished bitmap (`ImageBitmap.markImmutable()`, a no-op on Android). That helper
  becomes public engine API with the atlas, its first engine user, and so do mip levels (Texture mip chains above).
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

- In `~/Projects/Tesselar`, against a `publishToMavenLocal` of this repository: its scene pass becomes a client of the engine's custom
  emission and change gating, and its copies of `markImmutable`, `TextureMipChainCache`, `RenderWarmUp`, and `BackgroundPass` are
  deleted in favor of the engine's. Its cuboid, terrain, shadow, water, and weather code stays its own.
- Whatever it still had to keep private for speed is either moved up as well or written down in this file with the reason, so the
  engine keeps up with what Tesselar learns.

## Testing

- Without Skia: the vertices emitted for rotated, scaled, and pivoted bodies match the canvas path's transform; a layer of batched,
  plain, and batched Actors draws as two runs around one plain draw, in order; rollover; no refill while nothing changed; and
  `:tools:test-fixtures`' allocation measurement shows a steady-state frame allocates nothing.
- The same Scene drawn plain and batched compared as images where Skia runs (the Showcase's desktop app, or Creator's end-to-end harness
  under Xvfb for a generated game), with a tolerance for edge smoothing on ellipses.
- Creator: generated code for every shape and for sprites, the preview's look, and the atlas declaration, in `:creator:core`'s tests.
