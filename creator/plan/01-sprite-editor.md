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

The sprite editor (`ui/sprites`) draws sprites frame by frame and keeps them in a draft until they are saved, and its frame
strip adds, duplicates, deletes, and reorders frames (`edit_sprite_frames`); what is left is seeing an animation as it is
drawn, as GameMaker 6.1's sprite editor does: onion skinning is done, the preview is left.

## Animation

- **Preview:** the animation playing at its frames per second, looping, beside the canvas, and its speed set there.

## How it is built

- The preview's playing is the editor's own (`SpriteEditorState`), while the speed is the sheet's
  `framesPerSecond`, changed in the draft (a step of it) and saved with the frames, since saving writes the draft's sheet.

## Later

Layers per frame (flattened into the sheet when saved), color replacement, and outline and shading helpers.
