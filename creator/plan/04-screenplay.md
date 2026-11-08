<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Screenplay and the writers' room

Every Kubriko Creator game carries its design documents in the project, as Markdown next to the code, kept up to date by the user
and the AI alike: what the game is, and what is planned for it. Creator already tells the game as a film (Actor types are the cast,
Manager types the crew, and Scenes the scenes being shot), so the documents are the game's **Screenplay**, made of **pages**, and
the guided way of writing one together with the assistant, and building the prototype it describes, is the **writers' room**.

The screenplay serves three readers: the user, who keeps the design in one place; the AI, which learns the game in the game's own
words instead of grepping Kotlin; and the conversation between them, which can now say "the hero's double jump" and mean a page, an
Actor type, and its Events at once.

What exists to build on: the game's notes (`AGENTS.md`, `GameNotes`, `set_game_notes`, `read_game_notes`), the conversation start
(`ConversationStart`), the shared Markdown rendering (`ui.components.MarkdownText`), its plan checklist
(`PlanChecklist`), Keep and Undo for a turn's changes (`TurnChangesCard`), the rename logic's reference edits, the usage finder deletes clean up with (`ProjectReferences`), and the Game map's
health checks.

## What the screenplay is, and what it isn't

- **Shared, not the AI's:** the design is the user's; the AI keeps it in step with the game and proposes what it writes (Keep and
  Undo), never writing pages the user didn't see.
- **As few pages as the game allows, split by size, not by part:** a game starts as one page, `README.md`, and a simple one
  (Breakout) stays that way. A topic gets a page of its own (`combat.md`) only once the page holding it passes the length limit
  or the topic stands alone (a level set, a progression system), with folders where one grows (`levels/cave.md`). Never a page
  per Actor type, Manager type, Scene, module, or package: a page says how things behave for the player and mentions the types,
  never restating their Properties or Events, which the AI already reads. The system prompt and `create_page`'s description
  say so until `ScreenplayStyle` checks it.
- **Not the game's notes:** `AGENTS.md` says how to work on the project (conventions, what to avoid), for agents; the screenplay
  says what the game is, for everyone. Both stay vendor-neutral: Creator writes `AGENTS.md`, the cross-vendor convention, and a
  `CLAUDE.md` holding only `@AGENTS.md` when the project has none, since Claude Code doesn't read `AGENTS.md` by itself; pages are
  plain Markdown with no vendor's file, and `AGENTS.md` points every agent at `screenplay/`. No part of this is Claude's alone.

## Deleting what is referred to

Deleting an Actor type, Manager type, Scene, or Property cleans up what refers to it, asks first, and shows what still
dangles after a change outside Creator (`../CLAUDE.md` → Commands). What is left:

- **The other kinds,** once each can be deleted at all: sprites, sounds, and music (their Blocks removed, an Actor type's
  sprite cleared; dangling ones already show on their cards), a Timer (its start and stop Blocks and its Event), and a folder
  with what is in it (`delete_folder` takes only an empty one today). `ProjectReferences` gains each, so the confirmation,
  `check_change`, and the AI's result show them too.
- **Pages:** an image of a deleted sprite is copied next to the page first, so that the page keeps what it showed (undo
  removes the copy), once sprites can be deleted.

## Order

What is left of deleting what is referred to comes with each kind's delete. Search (`07-resource-organization.md`) covers
pages once it lands: quick open lists them, and find usages and `search_project` include their mentions and text.

## Testing

- A live scenario (`AssistantLiveScenariosTest`): a one-paragraph pitch to a game that plays, has pages in the shape `../CLAUDE.md` gives them, and
  passes `ScreenplayStyle`.
- Unit tests for deleting each remaining kind as `DeletingWithUsagesTest` and `DeletingEveryThingTest` have them for types,
  Scenes, and Properties: every usage, its cleanup, and a byte-for-byte undo.
- Unit tests for a page's sprite images after a sprite rename, once sprites can be renamed.

## Open questions

1. The names: "Screenplay" and "page" keep the film consistent and avoid "script"'s programming sense. The alternatives were
   "Treatment" (the prose a film is pitched with, close to a plan but odd for what is built), "Story" (too narrative for mechanics),
   and plain "Design".
2. Whether the Kubriko library's own documentation should recommend the same `screenplay/` folder to games written without Creator.
