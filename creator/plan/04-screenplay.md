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

## Pages

Pages exist (`../CLAUDE.md` → Files and ownership, and UI): plain Markdown in `screenplay/`, the page commands and tools, the
Screenplay group in the sidebar, and a page editor that edits one chunk at a time in place. What is left:

- **One shape per page,** checked but never forced on the user: a `# Title`, a one-sentence summary as the first paragraph (what the
  page index shows), what is in the game as headed sections, and at the end an optional `## Planned` list (`PageText` reads them).
  A page holding only Planned is a feature not started. When a planned item is built, it leaves Planned and the page describes it;
  nothing records that it once was planned (as in this folder). A new project's `screenplay/README.md` is the first page, "The game".
- **The first page** holds the pitch, the core loop, who plays it and on what, and the look, plus an optional **Words** list that maps
  the game's own names to the project's ("the hero" is `Player`), which the AI then speaks in.
- **The page editor** still lacks the AI's edits shown as a diff before Keep.
- **Images** (`add_page_image` copies one next to its page or shows a sprite where it is) still lack:
  - Pasting or dropping an image onto a page in the editor, which copies it the same way, and an image button in the editor's
    toolbar. Moving a page moves its images with it, deleting one offers to delete the images no other page uses, and undo
    brings both back.
  - A health check naming images no page uses, and images whose file is gone.
- **Mentioned in:** an open Actor type, Manager type, or Scene shows the pages that mention it, so the design is one click away
  from the Events.
- **Tools:** `read_pages` lists a page's images without sending them; the agent asks for one when it needs to see it.
- **AI features off** changes nothing here: pages, mentions, and their checks are the editor's, not the AI's.

## Deleting what is referred to

Deleting an Actor type, Manager type, Scene, or Property cleans up what refers to it, asks first, and shows what still
dangles after a change outside Creator (`../CLAUDE.md` → Commands). What is left:

- **The other kinds,** once each can be deleted at all: sprites, sounds, and music (their Blocks removed, an Actor type's
  sprite cleared; dangling ones already show on their cards), a Timer (its start and stop Blocks and its Event), a folder with
  what is in it (`delete_folder` takes only an empty one today), and a page. `ProjectReferences` gains each, so the
  confirmation, `check_change`, and the AI's result show them too.
- **Pages:** `ProjectReferences` finds their mentions and images. Deleting turns a mention into its plain text, copies an image
  of a deleted sprite next to the page first, so the page keeps what it showed, and turns a link to a deleted page into plain
  text; undo brings back the links and removes the copies, and a dangling mention chip is marked missing.
- **Renames read `ProjectReferences` too** (each kind follows its own rename code today), so a rename can't miss what a delete
  sees; find usages (`07-resource-organization.md`) reads it as well.

## Pages and the AI

- **Context:** the conversation start gains the page index (each page's path, title, and summary), right after the digest so the
  cached prefix stays stable; whole pages come through `read_pages`. `TokenBudgetTest` gets a budget for the index, and an index
  over it lists folders instead of pages.
- **The game's language:** the system prompt tells the agent to name things the way the pages and their Words do, to describe
  changes as the player sees them ("the hero now jumps twice") before naming types and Blocks, and to ask in game terms.
- **Keeping pages true:** every write command's result names the pages that mention what it changed ("`screenplay/hero.md`
  mentions `Player`"), computed from the mentions, so the agent knows where to look without reading every page. The system prompt
  asks for the matching page edits within the same turn, so Keep and Undo take the change and its description together.
- **Writing rules** (`ScreenplayStyle`, in `:creator:core`), checked on every page the AI writes, with a rejection that says what
  to change, and shown to the user only as quiet hints:
  - Present tense, describing the game as it is; no history ("used to", "now", "was changed", "fixed", "no longer", dates).
  - No narration of the conversation or the work ("I added", "we decided", "as requested").
  - Design, not implementation: no Kotlin, no Block-by-Block retelling; what a page says about a thing links to it instead.
  - Tunable numbers are named by their Property's mention rather than copied, so they can't drift.
  - Planned items are short, one per line or subheading, and leave Planned when built.
  - Pages under a length limit, and one topic per page; a page past it is split.
- **The game's notes and the screenplay:** `AGENTS.md` stays the AI's working rules for this project (conventions, what to avoid),
  and the design moves to pages. `set_game_notes`' description and the system prompt say which goes where; a project whose notes
  hold design gets the move offered once by the agent. The template's `AGENTS.md` points outside agents (Claude Code in a
  terminal) at `screenplay/`.
- **The Game map** shows the pages beside the cast, the crew, and the Scenes.

## Keeping it up to date

Creator can't write the design for the user, but it makes a missing or stale page visible everywhere it matters:

- **Health checks** (the Game map's, and in the sidebar beside their subject): an Actor type, Manager type, or Scene no page
  mentions; a mention of something deleted; a page off the shape above; and a Planned item whose subject already exists, which may
  be built.
- **Committing** (`ui/git`): when the staged changes touch the game but no page, the commit form says so, with "Ask the assistant
  to update the screenplay" one click away. It never refuses the commit.
- **Project settings → Screenplay:** these reminders on or off per project, on for new ones.
- **Writing one for an existing game:** "Write the screenplay" (the assistant panel's menu, and the opt-in of
  `14-project-upkeep.md`) has the agent read the game and propose pages for what it finds, for the user to keep page by page. The
  first time a game without a screenplay opens while AI features are on, the assistant offers it once; declining is remembered.
- **Every template and demo game** ships with its screenplay, which is also what its tour shows (`15-onboarding.md`).

## The writers' room

Creating a project with the assistant: it asks what the game is, writes the first pages with the user, and builds the smallest
playable version of them, then the next, playing and showing each one.

1. **Starting:** the new project screen offers "Start from an idea" beside the templates (only while AI features are on): a
   paragraph pitch, the name, and the targets. The project is created from a new Blank template (one empty Scene, no Ball), and the
   workspace opens on the assistant in the writers' room.
2. **Questions:** the agent asks a few at a time, through a new tool, `ask_user` (a question, two to four answers, and an "Other"
   field), whose answers the panel shows as buttons; the tool returns the choice. Creator owns it, so every agent gets it, not only
   ones with a question tool of their own. The order: the core loop, controls, winning and losing, the cast, the look, and what the
   first version leaves out.
3. **Pages first:** the agent writes "The game" and a page per main part, with everything not yet built under Planned, before any
   Actor type. The user sees them appear and can edit them before the next step.
4. **Slices:** the agent picks the smallest playable slice from the Planned items, lays it out as a plan to approve
   (`PlanChecklist`), builds it with Blocks rather than Kotlin Blocks where the Block set allows, plays it (`start_game`,
   `take_screenshot`), and shows the result. The built items leave Planned in the same turn.
5. **Rounds:** after each slice the agent asks what to change or build next; the user can stop at any time, and "Continue in the
   writers' room" (the assistant panel's menu, and every Planned item's right-click menu as "Build this with the assistant") starts
   again from the pages, in a new conversation.
6. **Not only new games:** "Plan a feature" runs the same questions for one new page in an existing game.

The writers' room is a conversation with extra rules (`AssistantSystemPrompt` gains a section, sent only to these conversations)
and the `ask_user` tool, not a wizard of its own: the agent leads, and everything it does goes through the usual commands, Keep,
and Undo. Shapes and colors drawn by Blocks stand in for art until the sprite editor (`10-sprite-editor.md`) exists.

## Order

What is left of images next (and of deleting what is referred to, with each kind's delete), then the AI's side
of them, then keeping them up to date, then the writers' room (`ask_user` and the Blank template first). Search (`07-resource-organization.md`) covers pages once it lands: quick open lists them, and find usages
and `search_project` include their mentions and text.

## Testing

- Unit tests for deleting what mentions point at, `ScreenplayStyle`'s
  rules (each with what it must not flag), the health checks, and the commands with undo, as for the other kinds.
- `EvalSetTest` requests that touch pages ("make the hero jump twice" editing `hero.md` too), and `TokenBudgetTest` budgets for the
  page index and `read_pages`.
- A `FakeAgent` run through the writers' room, with `ask_user` answered by the test, from pitch to a project that builds; the
  Blank template in `ProjectTemplateIntegrationTest`.
- A live scenario (`AssistantLiveScenariosTest`): a one-paragraph pitch to a game that plays, has pages in the shape above, and
  passes `ScreenplayStyle`.
- Unit tests for deleting each remaining kind as `DeletingWithUsagesTest` and `DeletingEveryThingTest` have them for types,
  Scenes, and Properties: every usage, its cleanup, and a byte-for-byte undo.
- Unit tests for images: pasting or dropping one, moving and deleting them with their page and undoing it, and sprite references
  after a sprite rename.
- Offscreen snapshots of the page editor's images of both kinds and the `ask_user` buttons.

## Open questions

1. The names: "Screenplay" and "page" keep the film consistent and avoid "script"'s programming sense. The alternatives were
   "Treatment" (the prose a film is pitched with, close to a plan but odd for what is built), "Story" (too narrative for mechanics),
   and plain "Design".
2. Whether the Kubriko library's own documentation should recommend the same `screenplay/` folder to games written without Creator.
