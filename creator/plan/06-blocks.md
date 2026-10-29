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

Blocks are the heart of the product. Each addition below lands with its command and AI tool (`02-ai-assistant.md`), generator tests, and a
case in the integration test's every-Block types (an Actor type and two Manager types).

## More for Manager types

- **Managers in the Scene editor:** their Properties shown and set per Scene (a library change, so it waits for a release).
- **Reacting to Actors:** an Actor of a type added or removed, for "all Bricks are gone", and counting the Actors of a type.
- **More text:** showing a text only while a toggle Property is on, and fonts.

## Game UI

A Manager's texts on screen suit a score or a lives counter, and its Kotlin UI is the escape hatch for anything else. Game UI grows
from them in two more steps, always generated into ordinary Compose (no runtime interpreter):

1. **UI elements (once a second UI need comes, such as a title screen with a Start button):** `texts` becomes a sealed `elements`
   tree (column, row, box, text, image, button, progress bar) that the editor builds visually, with a text as one kind of element so
   existing files still load. It also lifts the cap on where a text goes (today one of the nine `ScreenPosition`s). Buttons run a new Manager Event, "When a button is clicked", so UI behavior stays Events and Blocks.
   Worth doing before many projects depend on the flat `texts` list.
2. **Hand-written Composables:** a Manager shows a `@Composable` from the project's own code, chosen by name, so a project that
   outgrows Creator's UI keeps it in its own files.

## Next additions

- **More Properties:** color and Actor type reference Properties.
- **More flow:** combining conditions (and, or), comparing two Properties, and a Palette button that adds an If with its End. Sections
  live in the flat Block list (see `../CLAUDE.md` → Commands), so drag-and-drop stays a reorder.
- **More audio:** volume, sounds and music as Properties (so one Block plays the sound an Actor keeps), and adding sound and music
  files from Creator (today they are copied into `composeResources/files` by hand).
- **Scene settings in the game:** an option that fits the game's view to the Scene's bounds (through
  `ViewportManager.setAspectRatioMode`); the Scene editor's inspector already edits the settings.
- **Camera:** follow an Actor, shake, zoom.
- **Size and scale Blocks:** a box collision mask keeps the size it was built with, so the generated code builds a new mask when
  the size changes.
- **Appearance:** sprite sheets with animation settings (static sprites exist), from `.kubriko/sprites/<name>.json` as the sprite
  editor (`07-sprite-editor.md`) plans it, Blocks to set the frame, speed, and looping, and a sprite thumbnail in the appearance
  column; tinting a sprite with the color. Layer and drawing order are part of the Visible Trait (`05-reuse-and-traits.md`).
- **More Events:** removed from the Scene, key released and held, left the
  Scene bounds (as opposed to touching the edge). Pointer and gamepad Events come with their Traits (`05-reuse-and-traits.md`).
- **More drag and drop:** dragging a Block into another Event.
- **Simple and Advanced mode:** Simple hides Kotlin files, Kotlin Blocks, and Gradle output; switching never changes the project.

## Dangling references

A Block naming an Actor type, Scene, sound, or Timer that no longer exists keeps the name and generates a comment, so the game
quietly stops doing it, while the Block card looks exactly as before (`ActorTypeChoice`, `SceneChoice`, `AudioFileChoice`). Show
such a Block with a warning ("Brick no longer exists") from `BlockScope.actorTypeNames`, `sceneNames`, and `audioFiles`, list them
in the console after a delete, and offer them to find usages (`04-resource-organization.md`).

## Convert to code

One-way: removes an Actor type's JSON and the generated header, moves the class out of the `generated` package into the user's code, and adds
its metadata to `GameSerialization`; shown as a diff and confirmed (offering a commit first). Also offered when generated code was changed by hand.

## Templates

- **Paddle game:** the Getting started tutorial's game rebuilt from Blocks, including score and sounds (the score and its text come from
  a Manager type, the sounds from the audio Blocks). It doubles as the beginner reference project.
- **Platformer** and **top-down** starters (physics or collision, keyboard and gamepad input).

## Scene editor (Creator's own, `ui/scenes`)

Selecting, moving, removing, placing from the sidebar, and the inspector exist (`../CLAUDE.md` → Processes → Scene editing). Next: editors
for hand-written types' other state values (through `set_actor_state`), resize and rotate handles, an outline that follows rotation,
snapping to a grid, keyboard nudging, duplicating, placing hand-written Actor types (their default state through their metadata), sprite
thumbnails in the sidebar while dragging, multi-select, and layer filtering and locking. None of it needs a library change. A changed
Actor is created again, so it is drawn after the others of its drawing order until the Scene is opened again; if that shows, Actor
types could take a new state in place (an `Editable` reloading itself).
