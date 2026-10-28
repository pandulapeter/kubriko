<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Blocks, Actor types, and Manager types

Blocks are the heart of the product. Each addition below lands with its command and AI tool (`01-ai-assistant.md`), generator tests, and a
case in the integration test's every-Block types (an Actor type and two Manager types).

## More for Manager types

- **Managers in the Scene editor:** their Properties shown and set per Scene.
- **Reacting to Actors:** an Actor of a type added or removed, for "all Bricks are gone", and counting the Actors of a type.
- **More text:** showing a text only while a toggle Property is on, and fonts.

## Game UI

A Manager's texts on screen suit a score or a lives counter, and its Kotlin UI is the escape hatch for anything else. Game UI grows
from them in two more steps, always generated into ordinary Compose (no runtime interpreter):

1. **UI elements (once a second UI need comes, such as a title screen with a Start button):** `texts` becomes a sealed `elements`
   tree (column, row, box, text, image, button, progress bar) that the editor builds visually, with a text as one kind of element so
   existing files still load. Buttons run a new Manager Event, "When a button is clicked", so UI behavior stays Events and Blocks.
   Worth doing before many projects depend on the flat `texts` list.
2. **Hand-written Composables:** a Manager shows a `@Composable` from the project's own code, chosen by name, so a project that
   outgrows Creator's UI keeps it in its own files.

## Next additions

- **More Properties:** color and Actor type reference Properties.
- **More flow:** combining conditions (and, or), comparing two Properties, and a Palette button that adds an If with its End. Sections
  live in the flat Block list (see `../CLAUDE.md` → Commands), so drag-and-drop stays a reorder.
- **More audio:** volume, sounds and music as Properties (so one Block plays the sound an Actor keeps), and adding sound and music
  files from Creator (today they are copied into `composeResources/files` by hand).
- **Scene settings in Creator:** a panel for them beside the Scene editor, and fitting the view to the bounds (through
  `ViewportManager.setAspectRatioMode`).
- **Camera:** follow an Actor, shake, zoom.
- **Appearance:** sprite sheets with animation settings (static sprites exist), from `.kubriko/sprites/<name>.json` as the sprite
  editor (`08-sprite-editor.md`) plans it, Blocks to set the frame, speed, and looping, and a sprite thumbnail in the appearance
  column; tinting a sprite with the color; layer and drawing order.
- **More Events:** removed from the Scene, key released and held, pointer pressed / released / moved, gamepad buttons and sticks, left the
  Scene bounds (as opposed to touching the edge).
- **More drag and drop:** reordering Events the same way, dragging a Block into another Event, and scrolling the Block list while
  dragging near its edge.
- **Simple and Advanced mode:** Simple hides Kotlin files, Kotlin Blocks, and Gradle output; switching never changes the project.

## Convert to code

One-way: removes an Actor type's JSON and the generated header, moves the class out of the `generated` package into the user's code, and adds
its metadata to `GameSerialization`; shown as a diff and confirmed (offering a commit first). Also offered when generated code was changed by hand.

## Templates

- **Paddle game:** the Getting started tutorial's game rebuilt from Blocks, including score and sounds (the score and its text come from
  a Manager type, the sounds from the audio Blocks). It doubles as the beginner reference project.
- **Platformer** and **top-down** starters (physics or collision, keyboard and gamepad input).

## Scene editor (in `tools/scene-editor`)

Library changes, so each reaches projects only with a later Kubriko release (`00-library-release.md`).
Taking commands from Creator (open, save, select, place) so the AI and Creator's own palette can drive it; an Actor type palette with sprite
thumbnails; layer filtering and locking; multi-select; Creator's look (it still has its own).
