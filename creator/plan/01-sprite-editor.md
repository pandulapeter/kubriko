<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Sprite editor

A pixel-art editor for sprites and their animations, inspired by GameMaker 6.1's sprite and image editors: a beginner draws the ball, the
paddle, or a walking character without leaving Creator, frame by frame, with onion skinning. It starts with sprite sheets as an
appearance (Sprite sheets, below), which give it something to edit.

## Sprite sheets

- **Appearance:** sprite sheets with animation settings (static sprites exist): the frame size, frame count, frames per row, frames
  per second, and origin kept in the sprite's own file in `.kubriko/sprites` (`SpriteFolderStore`, which holds its folder), Blocks to set the frame,
  speed, and looping, a sprite thumbnail in the appearance column, and tinting a sprite with the color. Each is a property of the
  type's batched look (`core/.../actors/CLAUDE.md`), and a sprite sheet joins the game's one atlas whole (`AnimatedSprite.fromAtlas`,
  `GeneratedSprites`). Layer and drawing order come with the Visible Trait (`04-reuse-and-traits.md`).

## Files

- **The PNG sprite sheet is the source of truth**, in the project's `composeResources/drawable`. Kubriko's sprites plugin already plays an
  animation from a sheet (`AnimatedSprite`: frames of one size in rows, `framesPerRow` wide), so that is the format under the hood, not
  a format of Creator's own or GIF: the game and IntelliJ need nothing else (`../CLAUDE.md` → Principles 1).
- The sprite's file in `.kubriko/sprites` keeps what the sheet can't (frame size, frame count, frames per row, frames per second, and the
  origin), as sprite sheets already use it. Deleting it leaves a plain image, which can be opened again by entering the frame size.
- New sprites are added, renamed, and deleted as the sidebar already does; the editor adds New sprite and opens a
  sprite from the sidebar.
- A new sprite starts as one fully transparent frame of the size the user picks (32 by 32 by default).
- Importing: a PNG, a sheet sliced by frame size, or a strip of separate images (one per frame). Exporting is the sheet itself.

## The editor

- **Canvas:** nearest-neighbor zoom (1x to 32x), a pixel grid from 4x, panning, the cursor's pixel position, and a checkerboard behind
  transparent pixels.
- **Tools:** pencil, eraser, line, rectangle and ellipse (outline or filled), flood fill (contiguous or every pixel of the color), color
  picker, and a rectangular selection that moves, copies, and pastes (also between frames and sprites).
- **Colors:** a left and a right color (left and right mouse buttons, as in GameMaker), picked with the shared
  `ColorPickerDialog` of `:creator:ui-components` (made public for this; hue, saturation, brightness, opacity, and hex code; `checkerboard()` for what is see-through), transparency as a color, a palette that the
  project keeps (`.kubriko/palette.json`), and the colors the sprite already uses.
- **Whole image:** flip, rotate by 90 degrees, shift with wrapping, resize the canvas (anchored), and scale by a whole number, applied to one
  frame or to all of them.
- **Undo and redo:** one step per stroke or operation, in the workspace's history like every other change.

## Animation

- **Frame strip** under the canvas: add (a fully transparent frame), duplicate, delete, and reorder frames (dragged, like Blocks), with a
  thumbnail each.
- **Onion skinning:** the previous and next frames (one to three each way) drawn faintly under the current one, tinted so they can be told
  apart (previous reddish, next bluish), with their opacity adjustable and a toggle.
- **Preview:** the animation playing at its frames per second, looping, beside the canvas, and its speed set there.

## How it is built

- Every edit is a command with its AI tool (`../CLAUDE.md` → Commands): frame commands, whole-image operations, and drawing as
  `draw_pixels` (a list of pixels and colors, which the user's strokes go through too), so the AI can draw a sprite and its inverse is the
  pixels it replaced. Reading tools return the frame as an image, as `take_screenshot` does.
- Pixels are an `IntArray` per frame with pure operations (flood fill, shapes, transforms, compositing the onion skin), so the tests need
  no Skia; PNGs are read and written with `javax.imageio`. The canvas draws an `ImageBitmap` of the frame with `FilterQuality.None`.
- Written in the background and in order with the Block types' failure rules (`../CLAUDE.md` → Threads, writes, and failures, and
  Project files): a failed write is retried and reported, Build and Play warn naming what isn't saved, and git commands wait until
  it is on disk; reloaded when the PNG changes on disk (edited in another program). Its writes reach the resource tree, the Scene
  view, and the playing game through `ProjectChanges`, as the type store's do.
- The frames, their operations, and the sheet's store go into `:creator:core` (which runs no engine), the commands into
  `:creator:commands`, and the editor into the app's `ui`.

## Sprites in the game

A sheet the editor changes reaches the game through the appearance sprite sheets have (Sprite sheets, above), and a
new Actor type's size defaults to the frame size of the sprite it is given.

## Order

Sprite sheets first, since the editor edits them and the game plays them, then the files, the editor, and animation.

## Later

Layers per frame (flattened into the sheet when saved), color replacement, and outline and shading helpers.
