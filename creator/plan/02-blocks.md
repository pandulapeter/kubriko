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
case in the integration test's every-Block Actor type.

## Next: Manager types

Game-wide state and logic (score, lives, game over, the rules of a level) belong to a Manager, which Kubriko uses for everything that
isn't an object in the Scene, rather than to an invisible "controller" Actor, GameMaker's habit. Manager types are built the way Actor
types are, so the Block editor serves both.

- **The model:** `.kubriko/managers/<Name>.json` (Creator-owned, like Actor types), with Properties and Events made of the same Blocks.
  Names share one namespace with Actor types, since both become classes in the `generated` package.
- **Events:** when the game starts (`onInitialize`), every frame (`onUpdate`), and when a key is pressed (through `KeyboardInputManager`).
  Blocks that need an Actor (velocity, edges, collisions, removing itself) aren't offered in a Manager; the Block catalog says so.
- **Reaching a Manager from Actors:** Blocks that set or change a Manager's Property ("Change score of Game by 1"), conditions that check
  one, and Log placeholders that show one (`{Game.score}`). The generated Actor resolves the Manager in `onAdded`; renaming a Manager
  type or its Properties updates every Actor type that uses them, as renaming an Actor type does.
- **On-screen text:** a Manager can show text in the viewport (a "Show text" Block list drawn by the Manager's `Composable`), with
  placeholders for its Properties, which is how a score or lives counter is shown.
- **Generated code:** one plain `Manager` subclass per type (Properties as public `var`s, Events as `onInitialize` / `onUpdate`),
  following the engine's no-allocation rules, plus a generated `KubrikoWiring.kt` that lists every generated Manager for
  `Kubriko.newInstance`. New projects call it from `Game.kt`; existing ones get that one call added through the template drift
  flow (`06-project-upkeep.md`), shown as a diff and applied only to a `Game.kt` that still looks like the template's.
- **Editor:** a "Managers" section in the sidebar next to Actor types, and the Actor type editor reused without Appearance.
- **Commands and AI tools:** create, rename, and delete Manager types; Properties, Events, and Blocks through the existing commands,
  generalized from Actor types to both (addressed by name either way).
- **Later:** Managers in the Scene editor (their Properties shown per Scene), and a Manager reacting to Actors (an Actor of a type
  added or removed, for "all Bricks are gone").

## Next additions

- **More Properties:** color and Actor type reference Properties (the number, text, and toggle ones exist).
- **More flow:** combining conditions (and, or), comparing two Properties, and a Palette button that adds an If with its End. Sections
  live in the flat Block list (see `../CLAUDE.md` → Commands), so drag-and-drop stays a reorder.
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

- **Paddle game:** the Getting started tutorial's game rebuilt from Blocks, including score and sounds; possible once Manager types (score
  and its text) and audio exist. It doubles as the beginner reference project.
- **Platformer** and **top-down** starters (physics or collision, keyboard and gamepad input).

## Scene editor (in `tools/scene-editor`)

Taking commands from Creator (open, save, select, place) so the AI and Creator's own palette can drive it; an Actor type palette with sprite
thumbnails; layer filtering and locking; multi-select; Creator's look (it still has its own).
