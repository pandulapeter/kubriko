<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Kubriko Creator: plan

Only future work lives here; once something is implemented, it is deleted from these files, and `../CLAUDE.md` gains only the rules it
introduces that the code can't show. Each topic has a file of its own, so a session only needs the one it works on.

Finishing an item takes one more step before the next starts: run `/code-review` on the item's commits (`git log` from where it
started), fix what it finds in commits of their own, verify it with unit tests first and with an end-to-end scenario only where
`../CLAUDE.md` → Running and testing asks for one (never leaving a check to the user), and only then delete the item here. A file
left with no items is deleted too, and `python3 .github/scripts/renumber_creator_plans.py` (from the repository's root) then
numbers the rest again and rewrites every reference to them, in the same commit; given the files' names without their numbers
(`blocks git`), it puts those first in that order instead, which is how the roadmap is reordered. The review checks the change against
`../CLAUDE.md` → Definition of done, so what a whole-codebase sweep would find later is found while the item is fresh.

[HANDOVER.md](HANDOVER.md) is where the last session stopped: start there.

## Roadmap, in order

The files are numbered in the order they are worked on, and each depends only on the files before it, so every item of a file can be
done once the session reaches it. An item that turns out to wait for later work moves to the file it waits for, never staying behind
in an earlier one, and an item that waits for something outside this repository (a protocol, a vendor, a release) is listed under
its file's Waiting on others. Inside a file, its Order section says what comes first. A file left with only what waits on others
loses its number and moves to Waiting below, and the rest are numbered again, since nothing in it can be worked on until what it
waits for happens.

1. [Folders](01-folders.md) - remembering which folders are open, and dragging whole folders.
2. [Sprite editor](02-sprite-editor.md) - sprite sheets as an appearance, then a pixel-art editor for sprites and their animations,
   frame by frame with onion skinning, like GameMaker 6.1's.
3. [Search](03-search.md) - the sidebar filter, quick open, find usages, search in Blocks and pages, and searching conversations.
4. [Blocks, Actor types, and Manager types](04-blocks.md) - Blocks for the simple behavior a first game still needs Kotlin for, the
   rest of the Block set, UI elements, the Scene editor's handles and snapping, Convert to code, and the Paddle game template.
5. [Reuse, Traits, and bigger games](05-reuse-and-traits.md) - parent types, custom Blocks (the user's and the AI's own,
   with parameters, results, and Kotlin bodies, shared between projects), Traits, what bigger games need, and the
   platformer and top-down templates.
6. [Version control](06-git.md) - change markers, restoring, checkpoints, nested repositories, branches, remotes, cloning, and
   conflicts.
7. [Debugger](07-debugger.md) - pausing, stepping, and slowing the game for the user and the AI, the debug agent and its panels, other
   targets, and retiring the debug menu.
8. [Installing Creator](08-installing-creator.md) - first-run setup that downloads the JDK, Gradle, and the Android SDK with progress,
   a new Android emulator from Play's menu, installers, and updates.
9. [Export](09-export.md) - release builds stripped of Creator, Release, Icons, and Signing settings, exports for every target, and
   Play on an iOS device.
10. [Demo games](10-demo-games.md) - five bundled games of increasing complexity, each built entirely in Creator, improving Creator
   wherever it falls short.
11. [Project upkeep](11-project-upkeep.md) - multi-module projects, every entry point (IntelliJ's run configurations included),
    editions such as a demo and a full version, opting existing projects in, Kubriko versions, and template drift.
12. [Tesselar in Creator](12-tesselar.md) - playing Tesselar and running its tools from Creator, its rendering on the engine's batched
    looks, and the asset editor extension point that hosts its editors.
13. [Rehearsals](13-rehearsals.md) - automated tests: the ones a project already has found, listed, and run with one click, a test kit
    for games in Kotlin, and the AI writing and keeping them.
14. [Later](14-later.md) - the code editor and what waits for demand, a decision, or the benchmarks.

## Waiting

Files holding only what waits for something outside this repository. One gets a number again, in the place its dependencies
allow, once something in it can be worked on.

- [AI assistant](ai-assistant.md) - what waits on ACP, the Kotlin SDK, and vendors.

## Structure

Each item's code goes into the lowest of Creator's ten modules that can hold it (`../CLAUDE.md` → Modules): the model, code
generation, and project files in `:creator:core`, a command in `:creator:commands`, the open project's wiring in
`:creator:workspace` (with every change routed through `ProjectChanges`), and screen state in a tested holder such as
`WorkspaceUiState` rather than in a Composable.

## Vision

A beginner downloads one installer, picks a template, and has a running game in minutes, without knowing Gradle, Kotlin Multiplatform, or
IntelliJ, building behavior visually or by asking Claude, and seeing every change as readable Events and Blocks. The project is always a plain
Gradle project that builds in IntelliJ exactly as if written by hand.

**Non-goals:** replacing IntelliJ; a scripting language or runtime interpreter (gameplay is always compiled Kotlin); GameMaker file
compatibility or a copy of its look, icons, or action set; running Creator itself on mobile or the web.

**Two reference projects keep the design honest:** a beginner game built only from Blocks (the Getting started paddle game, and the
Breakout in `~/Projects/breakout`), and Tesselar (`12-tesselar.md`). The principles every plan keeps, and who owns which
project file, are in `../CLAUDE.md`.

## Terminology

| Kubriko Creator | Kubriko code | GameMaker 6.1 |
|---|---|---|
| Actor type | A class implementing `Actor` | Object |
| Actor | An instance placed in a Scene | Instance |
| Event | A Trait callback (`update`, `onCollisionDetected`, …) | Event |
| Block | One generated statement inside an Event | Action |
| Custom Block | A generated Kotlin function: Blocks (or a Kotlin body) with parameters and a result | Script |
| Kotlin Block | A verbatim Kotlin snippet | "Execute code" |
| Manager type | A class extending `Manager`: game-wide state and logic | Global variables, or a controller object |
| Property | An `@Exposed` property (or a Manager's `var`) | Variable |
| Rehearsal | A Kotlin test (`rehearse { }`) playing the game without a window | (none) |
| Scene | A serialized JSON scene file | Room |
| Screenplay | The game's design documents: Markdown pages in `screenplay/` | (none; a design document) |
| Timer | Two `Long` fields counting down | Alarm |

Our own names, icons, and wording throughout; "inspired by GameMaker" is a factual credit, not branding.

## Risks

- **Maintenance:** every Kotlin, Gradle, AGP, Compose, and Android SDK bump can break Creator; the integration test builds every template.
- **Compile times** slow the edit-play loop; the build benchmark (`benchmark/CreatorBuildBenchmarkTest`) measures every change to them.
- **Session protocol drift** across Kubriko versions; it is versioned and checked.
- **AI cost and privacy:** users pay for their own usage and send project files to the API; the UI says so.
- **Agent churn:** the ACP adapters release often and ACP v2 is still a draft; agent versions are pinned to the registry's, and the panel
  needs Node.js for npx agents.

## Open questions

1. The oldest Kubriko version Creator supports for projects that aren't opted in (sessions work from 0.7.4; opted-in projects are raised to
   Creator's own version, `../CLAUDE.md` → Project files).
2. Whether the Showcase should become editable in Creator.
3. Whether web export should offer one-click hosting.
4. Whether a free, MPL-2.0 desktop app hosting Claude Code over ACP (as Android Studio does) has to accept Anthropic's Commercial Terms
   itself. The terms now allow users to sign in to a hosted, unmodified Claude Code with their own subscription, so this is asked before
   release, not before building: ask Anthropic (Claude Code's Legal and compliance page points to sales), naming Android Studio, Zed,
   and JetBrains IDEs, which host the same adapter, as the precedent.
5. Whether the Kubriko library's own documentation should recommend the `screenplay/` folder to games written without Creator.
