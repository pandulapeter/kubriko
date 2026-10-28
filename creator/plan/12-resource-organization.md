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

The sidebar is a flat list per group, which stops working past a few dozen Actor types, Scenes, or sprites. This adds folders to every
group and a search over all of them. Folders are plain files where the format allows it, so the project stays readable in IntelliJ
and git (`README.md` → Principles 2).

Folders for Actor types, Manager types, and Scenes exist, with Scene names as paths (`../CLAUDE.md` → Commands → Folders). Left below:
virtual folders for sprites (once `.kubriko/sprites` exists), real folders shown for sounds and music, remembering expanded folders,
and all of the search.

## Where folders live

| Group | Folders are | Notes |
|---|---|---|
| Actor types, Manager types | Subfolders of `.kubriko/actors` and `.kubriko/managers` | Names stay unique across folders, since every type is a Kotlin class in the one `generated` package and a Scene type ID; generated code stays flat. |
| Scenes | Subfolders of `composeResources/files/scenes` | A Scene's name becomes its path (`levels/1-1`), which goToScene, `set_scene_settings`, and the other Scene commands take; `GeneratedScenes` reads `files/scenes/<path>.json` as now. |
| Sounds, music | Subfolders under `composeResources/files` | Already addressed by path. |
| Sprites | Virtual | Compose resources take no subfolders in `drawable` (only a folder's direct files become accessors), so a sprite's folder is kept in `.kubriko/sprites/<name>.json` (`08-sprite-editor.md`), and sprite names stay unique. |
| Kotlin files | The package tree | Read-only structure; moving them is IntelliJ's job. |

## The sidebar

- A tree per group: folders first, then items, both sorted by name; expanded folders remembered per project in `.kubriko/local`.
- Create, rename, and delete folders from a folder's menu (only empty folders are deleted; moving a whole folder into another by
  dragging is left); drag items into another folder of the same group. Every one of these is a command with its AI tool (`create_folder`, `move_to_folder`,
  `rename_folder`, `delete_folder`), undone like any other, and they move the files (a type's JSON and nothing else, since its
  generated Kotlin doesn't depend on the folder; a Scene's file, with every goToScene Block and Scene reference following the new path,
  as renames already do).
- Collapsing a group hides it, as today; groups nothing uses stay hidden.
- The sidebar's Actor type drag into a Scene works from any folder.

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
