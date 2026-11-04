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
(`ConversationStart`), the assistant's Markdown rendering (`ui/assistant/MarkdownParser`, `MarkdownBlock`), its plan checklist
(`PlanChecklist`), Keep and Undo for a turn's changes (`TurnChangesCard`), the rename logic's reference edits, the usage finder deletes clean up with (`ProjectReferences`), and the Game map's
health checks.

## Pages

- **Plain files:** `screenplay/**/*.md` at the project's root, shared like the game's notes (`../CLAUDE.md` → Files and ownership
  gains the row): Creator and the AI write them through commands, the user can write them anywhere, and a change from outside shows
  within a second. Folders are real folders. `screenplay/README.md` is the first page, "The game", so the folder reads well on
  GitHub and in IntelliJ without Creator.
- **One shape per page,** checked but never forced on the user: a `# Title`, a one-sentence summary as the first paragraph (what the
  page index shows), what is in the game as headed sections, and at the end an optional `## Planned` list. A page holding only
  Planned is a feature not started. When a planned item is built, it leaves Planned and the page describes it; nothing records that
  it once was planned (as in this folder).
- **The first page** holds the pitch, the core loop, who plays it and on what, and the look, plus an optional **Words** list that maps
  the game's own names to the project's ("the hero" is `Player`), which the AI then speaks in.
- **Mentions** are relative Markdown links to the thing's own file (an Actor type's `.kubriko/actors/…json`, a Scene's JSON, a
  sprite, a sound, a Kotlin file, or another page), so they still work on GitHub and in IntelliJ. Creator shows them as chips in
  the thing's `Accent`, opens the thing on a click, and offers them while typing `@`. Renaming or moving a thing, a page
  included, updates every mention and image that points at it, in the same undo step as the rename; deleting one is in
  Deleting what is referred to, below.
- **The sidebar** gets a Screenplay group above the cast, with folders, the filter, drag and drop, and right-click menus as the
  other groups have (`ResourceGroup`, `ResourceGroupTitles`).
- **The page editor** in the center (a `WorkspaceSelection` of its own): a simple Markdown editor, not a word processor. The
  page shows rendered, and clicking into a paragraph edits that paragraph's Markdown in place, so the source is never far but
  never in the way. A small toolbar and the usual shortcuts (Cmd/Ctrl+B, I, K) cover headings, bold, italic, lists, links,
  mentions, and images, each writing plain Markdown. The assistant's Markdown parser and renderer move to
  `:creator:ui-components` and are shared, gaining what pages need beyond chat (images, links, and nested lists). Edits are
  commands with undo (merged per pause in typing, as a Block's text is); the AI's edits show as a diff before Keep.
- **Images** are ordinary Markdown images, so they show on GitHub and in IntelliJ too:
  - An image pasted or dropped onto a page is copied next to the page (into the page's own folder, named after the page and
    numbered), never referenced where it was. Moving a page moves its images with it, deleting one offers to delete the images
    no other page uses, and undo brings both back.
  - An image can also point at a project resource instead of a copy (a sprite in `drawable`), so the page shows the game's
    current art. Such an image, like every link to a project resource, opens that resource in Creator on a click; a link
    outside the project opens in the browser, after the address is shown.
  - Large pasted images are scaled down to a width limit before they are written, since pages live in git.
  - A health check names images no page uses, and images whose file is gone.
- **Mentioned in:** an open Actor type, Manager type, or Scene shows the pages that mention it, so the design is one click away
  from the Events.
- **Commands and tools:** `create_page`, `edit_page` (one section by its heading, or the whole page), `move_page` (renames and
  folders), `delete_page`, `list_pages`, and `read_pages`, which answers again with what changed (`ResultDelta`), like the other reads.
  `add_page_image` puts an image on a page from a game screenshot (`take_screenshot`'s) or a sprite reference, so the agent can
  show what it built. `read_pages` lists a page's images without sending them; the agent asks for one when it needs to see it.
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
  hold design gets the move offered once by the agent. The page editor opens `AGENTS.md` too, which is `02-ai-assistant.md`'s item
  "The game's notes in the editor". The template's `AGENTS.md` points outside agents (Claude Code in a terminal) at `screenplay/`.
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
  `14-project-upkeep.md`) has the agent read the game and propose pages for what it finds, for the user to keep page by page.
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

Pages and their editor first (what is left of deleting what is referred to comes with each kind's delete), then the AI's side
of them, then keeping them up to date, then the writers' room (`ask_user` and the Blank template first). Search (`07-resource-organization.md`) covers pages once it lands: quick open lists them, and find usages
and `search_project` include their mentions and text.

## Testing

- Unit tests for the page store, mentions (parsing, renames, deletions, and links that leave the project), `ScreenplayStyle`'s
  rules (each with what it must not flag), the health checks, and the commands with undo, as for the other kinds.
- `EvalSetTest` requests that touch pages ("make the hero jump twice" editing `hero.md` too), and `TokenBudgetTest` budgets for the
  page index and `read_pages`.
- A `FakeAgent` run through the writers' room, with `ask_user` answered by the test, from pitch to a project that builds; the
  Blank template in `ProjectTemplateIntegrationTest`.
- A live scenario (`AssistantLiveScenariosTest`): a one-paragraph pitch to a game that plays, has pages in the shape above, and
  passes `ScreenplayStyle`.
- Unit tests for deleting each remaining kind as `DeletingWithUsagesTest` and `DeletingEveryThingTest` have them for types,
  Scenes, and Properties: every usage, its cleanup, and a byte-for-byte undo.
- Unit tests for images: copying a pasted or dropped one (name clashes, scaling, a file that can't be read), moving and deleting
  them with their page and undoing it, sprite references after a sprite rename, and links resolved to a resource, a page, or the web.
- Offscreen snapshots (`WorkspaceScreenTest`) of the page editor (rendered, a paragraph being edited, and images of both
  kinds), the mention chips, and the `ask_user` buttons.

## Open questions

1. The names: "Screenplay" and "page" keep the film consistent and avoid "script"'s programming sense. The alternatives were
   "Treatment" (the prose a film is pitched with, close to a plan but odd for what is built), "Story" (too narrative for mechanics),
   and plain "Design".
2. Whether the Kubriko library's own documentation should recommend the same `screenplay/` folder to games written without Creator.
