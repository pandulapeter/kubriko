<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Blocks and Actor types

Blocks are the heart of the product. Each addition below lands with its command and AI tool (`01-ai-assistant.md`), generator tests, and a
case in the integration test's every-Block Actor type.

## Next additions

- **More Properties:** color and Actor type reference Properties (the number, text, and toggle ones exist).
- **More flow:** combining conditions (and, or), comparing two Properties, and a Palette button that adds an If with its End. Sections
  live in the flat Block list (see `../CLAUDE.md` → Commands), so drag-and-drop stays a reorder.
- **Actors:** add an Actor of a type at a position (spawning), find the nearest of a type.
- **On-screen text and score:** a Scene-level value store (score, lives) with Blocks to change it, and text drawn by a Manager.
- **Scene:** go to another Scene, restart the Scene, pause; Scene settings (size, background, camera start, music) as a `Unique` `Editable` Actor.
- **Audio:** play a sound, play or stop music (from the project's resources).
- **Camera:** follow an Actor, shake, zoom.
- **Appearance:** sprites and sprite sheets (with animation settings) instead of shapes; layer and drawing order.
- **More Events:** removed from the Scene, key released and held, pointer pressed / released / moved, gamepad buttons and sticks, left the
  Scene bounds (as opposed to touching the edge).
- **Palette:** grouped into categories with tabs, as GameMaker's action palette; drag-and-drop from the palette into the Block column and within it.
- **Simple and Advanced mode:** Simple hides Kotlin files, Kotlin Blocks, and Gradle output; switching never changes the project.

## Convert to code

One-way: removes an Actor type's JSON and the generated header, moves the class out of the `generated` package into the user's code, and adds
its metadata to `GameSerialization`; shown as a diff and confirmed (offering a commit first). Also offered when generated code was changed by hand.

## Templates

- **Paddle game:** the Getting started tutorial's game rebuilt from Blocks, including score and sounds; possible once Properties, Timers,
  text, and audio exist. It doubles as the beginner reference project.
- **Platformer** and **top-down** starters (physics or collision, keyboard and gamepad input).

## Scene editor (in `tools/scene-editor`)

Taking commands from Creator (open, save, select, place) so the AI and Creator's own palette can drive it; an Actor type palette with sprite
thumbnails; layer filtering and locking; multi-select; Creator's look (it still has its own).
