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

## Scene editor (Creator's own, `ui/scenes`)

- Editors for hand-written types' other state values (through `set_actor_state`).
- Placing hand-written Actor types (their default state through their metadata).
- Sprite thumbnails in the sidebar while dragging.

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

The Scene editor first, then Convert to code, and the Paddle game.
