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

## Game UI

Creator owns Kubriko's side of a game; a UI beyond its Manager types' texts is Compose, which is Kotlin, the user's own code
(Principle 2). A visual tree of UI elements (columns, rows, buttons, and the like) was dropped on 2026-11-17: a real game's UI,
such as Tesselar's (over 200 Composables: a Material theme, an animated pause menu, settings panels of sliders and switches,
platform effects, and focus handling), can't be built from Blocks, so a tree would only be outgrown into code, a dead end.
What building the demo games shows every small game needs is added to texts instead, one specific thing at a time.

- **Hand-written Composables:** a Manager type shows a `@Composable fun <Name>(<manager>: <ManagerType>, windowInsets: WindowInsets)`
  from the project's own code, chosen by its fully qualified name (`composable`, format raise), drawn on top of its texts and
  Kotlin UI, so its Properties are Compose state the UI follows. Creator makes a new one (`create_composable`: a file of its
  own in a `ui` package beside the generated one, which is the user's from then on), opens it in the system's editor, and the
  AI writes Compose in it, the one kind of Kotlin file it edits. A Composable the project no longer has shows where the
  Manager type is edited, as the build names it too.

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

Hand-written Composables first, then the Scene editor, Convert to code, and the Paddle game.
