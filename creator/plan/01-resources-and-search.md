<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Resources and search

Every kind of resource added, renamed, and deleted from Creator, folders for the groups that lack them, and finding things past a
few dozen Actor types, Scenes, or sprites. Folders are plain files where the format allows it, so the project stays readable in
IntelliJ and git (`../CLAUDE.md` → Principles 1). Each addition lands with its command and AI tool.

## Sprites, sounds, and music

Today sprites, sounds, and music are copied into `composeResources` by hand, and none of them can be renamed or deleted from Creator.

- **Adding:** sprites (PNG into `composeResources/drawable`) and sounds and music (into `composeResources/files`), chosen, dropped, or
  pasted, with a name checked against the accessors Compose generates (`SpriteFiles`) and against the files already there.
- **Renaming** follows every Actor type's appearance, every audio Block, the Scene settings' music, and the pages that mention it
  (`withMentionsFollowed`), in one undo step.
- **Deleting** cleans up what refers to it, as deleting an Actor type does (`deleteWithUsages`): its Blocks removed and an Actor
  type's sprite cleared. A page showing a deleted sprite gets a copy of the image next to it first, so it keeps what it showed (undo
  removes the copy).

## Deleting the other kinds

- **A Timer:** its start and stop Blocks and its Event go with it.
- **A folder with what is in it:** `delete_folder` takes only an empty one today; a full one deletes its contents as their own deletes
  would, in one undo step, listing them in the confirmation.

`ProjectReferences` gains each kind, so the confirmation, `check_change`, and the AI's result show them. Tested as
`DeletingWithUsagesTest` and `DeletingEveryThingTest` test types, Scenes, and Properties: every usage, its cleanup, and a byte-for-byte
undo; and a page's sprite images after a sprite rename.

## Folders

- **Sprites:** virtual folders, since Compose resources take no subfolders in `drawable` (only a folder's direct files become
  accessors). A sprite's folder is kept in `.kubriko/sprites/<name>.json`, a file this introduces (Creator-owned, a row of
  `../CLAUDE.md` → Files and ownership) and sprite sheets later extend (`02-blocks.md` → Appearance); sprite names stay unique.
- **Sounds and music:** their real subfolders under `composeResources/files` shown as trees (they are already addressed by path).
- **Kotlin files:** the package tree, read-only (moving them is IntelliJ's job).
- Expanded folders remembered per project in `.kubriko/local`, and moving a whole folder into another by dragging.

## Search

- **The sidebar's filter field** (`SidebarFilter.kt`): Cmd/Ctrl+F to focus it, the matches highlighted, and matching words anywhere
  (`brk` finds `Brick` too, as IDEs do).
- **Quick open** (Cmd/Ctrl+P, or Shift twice): a list over everything (types, Scenes, pages, sprites, sounds, Kotlin files, and
  Properties and custom Blocks by their type) that opens the chosen one.
- **Find usages:** from a type, Property, sprite, sound, or Scene, every Block, condition, text, Scene, and collision Event that refers
  to it, each one clickable (opening the type at that Event and Block, or the Scene with that Actor selected), and the pages that
  mention it. It reads `ProjectReferences`, which the deletes already use, rather than finding them again.
- **Search in Blocks and pages:** texts in Log Blocks, screen texts, Kotlin Blocks, and the screenplay's pages, from the same panel.
- **AI tools:** `search_project` (names, Block texts, and page texts, with paths) and `find_usages`, read-only; `find_usages` builds on
  `ProjectRelations`, which `get_relations` already reads types, Properties, and Scenes from (sprites and sounds are added).
- **Searching conversations:** a search field over the open project's saved conversations, never another project's, that finds a query
  in what the user asked and what the agent answered, ignoring case, and lists the matches with the words around them highlighted;
  choosing one continues that conversation and scrolls to the match. `SavedConversation` keeps only the title, the start of the
  last answer, and the agent's summary, and the transcript itself stays with the agent (`session/load` replays it), so Creator keeps each conversation's
  messages as plain text beside it, without tool calls or images, and capped per conversation. The text is searched in memory, off
  the main thread, and forgotten with the conversation when `SavedConversationStore` drops the oldest beyond
  `conversationsPerProject`. Conversations held by another agent are listed too, marked as such and opened read-only from the kept
  text, since only their own agent can continue them.

## Order

Adding, renaming, and deleting sprites, sounds, and music, then the other deletes, then folders, then the filter field and quick
open, find usages, search in Blocks and pages with the AI tools, and searching conversations last.
