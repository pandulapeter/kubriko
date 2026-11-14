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

## Simple behavior without Kotlin

Kotlin Blocks are for advanced cases only: the agent writes one only when no combination of Blocks can do the job or the user asks
for code (`AssistantSystemPrompt`). The first writers' room game (a side-scrolling shooter with scrolling stars and exploding aliens)
needed twelve Kotlin Blocks, all for simple behavior. Each item replaces some of them, and the section is done when the eval set
(`EvalSet`) builds the same game with no Kotlin Block left.

- **The shooter without Kotlin:** add the writers' room shooter to the eval set and record it (`EvalSetLiveTest` with
  `KUBRIKO_CREATOR_EVAL_RECORD=true`, which spends a real agent's requests) once a live run can be spent; whatever it still
  writes a Kotlin Block for becomes an item here.

## More for Manager types

- **More text:** showing a text only while a toggle Property is on, and fonts.
- **Managers in the Scene editor:** their Properties shown and set per Scene (a library change, released before Creator relies on
  it).

## Next additions

- **More Properties:** color and Actor type reference Properties.
- **More flow:** combining conditions (and, or), comparing two Properties, and a Palette button that adds an If with its End. Sections
  live in the flat Block list (`BlockNesting`), so drag-and-drop stays a reorder.
- **More audio:** volume, and sounds and music as Properties (so one Block plays the sound an Actor keeps).
- **Scene settings in the game:** an option that fits the game's view to the Scene's bounds (through
  `ViewportManager.setAspectRatioMode`); the Scene editor's inspector already edits the settings.
- **Camera:** follow an Actor, shake, zoom.
- **Size and scale Blocks:** a box collision mask keeps the size it was built with, and building a new one would leave the
  collision plugin's spatial grid with the old mask, so `BoxCollisionMask` gets a size it changes in place (a library change,
  released before Creator relies on it), which the generated update then follows, for these Blocks and for Animate's scale.
- **More Events:** removed from the Scene, and key released and held.
  Pointer and gamepad Events come with their Traits (`02-reuse-and-traits.md`).
- **More drag and drop:** dragging a Block into another Event.
- **Simple and Advanced mode:** Simple hides Kotlin files, Kotlin Blocks, and Gradle output; switching never changes the project.

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

Simple behavior first, then Manager types, the next additions, UI elements, the Scene editor, Convert to code, and the Paddle game.
