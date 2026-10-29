<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Folders and search

Finding things past a few dozen Actor types, Scenes, or sprites. Folders for Actor types, Manager types, and Scenes exist, with Scene
names as paths (`../CLAUDE.md` → Commands → Folders); left are the other groups' folders and all of the search. Folders are plain
files where the format allows it, so the project stays readable in IntelliJ and git (`README.md` → Principles 2).

## Folders left

- **Sprites:** virtual folders, since Compose resources take no subfolders in `drawable` (only a folder's direct files become
  accessors); a sprite's folder is kept in `.kubriko/sprites/<name>.json` (`05-sprite-editor.md`), and sprite names stay unique.
- **Sounds and music:** their real subfolders under `composeResources/files` shown as trees (they are already addressed by path).
- **Kotlin files:** the package tree, read-only (moving them is IntelliJ's job).
- Expanded folders remembered per project in `.kubriko/local`, and moving a whole folder into another by dragging.

## Search

- **Filter field** at the top of the sidebar (and Cmd/Ctrl+F there): narrows every group to the items whose names match (folders on the
  way stay open, matches highlighted), case-insensitive, matching words anywhere (`brk` finds `Brick` too, as IDEs do). Escape clears it.
- **Quick open** (Cmd/Ctrl+P, or Shift twice): a list over everything (types, Scenes, sprites, sounds, Kotlin files, and Properties and
  functions by their type) that opens the chosen one.
- **Find usages:** from a type, Property, sprite, sound, or Scene, every Block, condition, text, Scene, and collision Event that refers
  to it, each one clickable (opening the type at that Event and Block, or the Scene with that Actor selected). The rename logic already
  finds these (`KotlinIdentifiers`, the Block reference edits); this lists them instead of changing them.
- **Search in Blocks:** texts in Log Blocks, screen texts, and Kotlin Blocks, from the same panel.
- AI tools: `search_project` (names and Block texts, with paths) and `find_usages`, read-only.

## Order

The filter field and quick open next, then find usages; sprite folders with the sprite editor's files.
