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

The sprite editor (`ui/sprites`) draws sprites already, frame by frame, through `draw_pixels` and `edit_sprite`, and makes
them from the sidebar's New menu; what is left is working with the frames of an animation, as GameMaker 6.1's sprite
editor does, with onion skinning.

## Animation

- **Frame strip** under the canvas: add (a fully transparent frame), duplicate, delete, and reorder frames (dragged, like Blocks), with a
  thumbnail each. It replaces the toolbar's frame arrows.
- **Onion skinning:** the previous and next frames (one to three each way) drawn faintly under the current one, tinted so they can be told
  apart (previous reddish, next bluish), with their opacity adjustable and a toggle.
- **Preview:** the animation playing at its frames per second, looping, beside the canvas, and its speed set there.

## How it is built

- The frame commands (add, duplicate, delete, move) are commands with their AI tools, changing the image and the sheet
  together through `editSpriteImage` (whose inverse writes both back byte for byte), as `edit_sprite` does; a sprite not
  cut into frames becomes one with its first frame added.
- Compositing the onion skin is a pure operation on `PixelImage`s, so its tests need no Skia.

## Later

Layers per frame (flattened into the sheet when saved), color replacement, and outline and shading helpers.
