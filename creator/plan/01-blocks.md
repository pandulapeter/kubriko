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

Blocks are the heart of the product. Each addition below lands with its command and AI tool, generator tests, and a case in the
integration test's every-Block types (an Actor type and two Manager types), and is one entry of the Block descriptor (`BlockKind`),
whose exhaustive `when`s then show every place it needs.

## More for Manager types

- **Font files:** fonts the game bundles (`composeResources/font`), added and named like sprites, so a text looks alike on
  every platform; the generic families a text has now are drawn in each platform's own faces, and the web has none.
  - **Files:** `.ttf` and `.otf` right in the game module's `composeResources/font`, a resource kind of their own
    (`ResourceKind.FONT`, a Fonts group after Sprites) named like sprites (the name makes `Res.font`'s accessor, so
    accessors may not clash within `font`), added, renamed, and deleted through the resource commands, dropped from Finder,
    chosen, and pasted like them, with virtual folders as sprites have (a `VirtualFolderStore` both share, fonts' in
    `.kubriko/fonts`; sprites keep their sheets on top) and `move_font_to_folder`.
  - **Texts:** a text keeps its generic family and gains `fontFile` (the font's path), drawn in that file while the game
    has it and in the family otherwise, so a deleted file breaks no build (`Res.allFontResources[accessor]`, as sprites
    are found). Format raise with an empty migration. The font's renames follow into texts and its delete clears
    `fontFile` (a usage of its own), as a sprite's clears an Actor type's.
  - **Generated code:** `Font(resource)` is `@Composable`, so the Manager's UI makes the family in its Composable and
    keeps the style's `copy` in a `remember` keyed by the loaded font, allocating nothing per frame.
  - **Editor:** the text's font menu lists the generic families, then the project's font files.

## Game UI

A Manager's texts on screen suit a score or a lives counter, and its Kotlin UI is the escape hatch for anything else. Game UI grows
from them in two steps, always generated into ordinary Compose (no runtime interpreter):

1. **UI elements:** `texts` becomes a sealed `elements` tree (column, row, box, text, image, button, and progress bar) that the editor
   builds visually, with a text as one kind of element so existing files still load. It lifts the cap on where a text goes (today one
   of the nine `ScreenPosition`s). Buttons run a new Manager Event, "When a button is clicked", so UI behavior stays Events and Blocks.
   Done before many projects depend on the flat `texts` list.
2. **Hand-written Composables:** a Manager shows a `@Composable` from the project's own code, chosen by name, so a project that
   outgrows Creator's UI keeps it in its own files.

## Scene editor (Creator's own, `ui/scenes`)

- Editors for hand-written types' other state values (through `set_actor_state`).
- Resize and rotate handles, and an outline that follows rotation.
- Snapping to a grid, with the grid's size and whether snapping is on by default as Creator settings (Settings → Editor).
- Keyboard nudging, duplicating, and multi-select.
- Placing hand-written Actor types (their default state through their metadata).
- Sprite thumbnails in the sidebar while dragging.
- Layer filtering and locking.

None of it needs a library change. A changed Actor is created again, so it is drawn after the others of its drawing order until the
Scene is opened again; if that shows, Actor types take a new state in place (an `Editable` reloading itself).

## Convert to code

One-way: removes an Actor type's JSON and the generated header, moves the class out of the `generated` package into the user's code,
and adds its metadata to `GameSerialization`; shown as a diff and confirmed (offering a commit first). Also offered when generated
code was changed by hand.

## The Paddle game template

The Getting started tutorial's game rebuilt from Blocks, including score and sounds (the score and its text from a Manager type, the
sounds from the audio Blocks). It doubles as the beginner reference project.

## Order

Manager types first, then UI elements, the Scene editor, Convert to code, and the Paddle game.
