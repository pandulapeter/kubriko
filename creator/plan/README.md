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

Only future work lives here; once something is implemented, it is deleted from these files and described in `../CLAUDE.md` instead. Each topic
has a file of its own, so a session only needs the one it works on.

[HANDOVER.md](HANDOVER.md) is where the last session stopped: start there.

## Roadmap, in order

0. [Library releases](00-library-release.md) - 0.8.0 shipped Creator's library side; how later library changes reach projects.
1. [Large projects](09-large-projects.md), [Reuse, Traits, and bigger games](11-reuse-and-traits.md), [Folders and
   search](12-resource-organization.md), and [Fast iteration](10-fast-iteration.md) - Creator has to scale to GameMaker-sized games with
   nothing capped. **File format changes come first** (stable Scene Actor IDs, one Actor per line, folders for types and Scenes, parent
   Actor types), before projects have many files; then incremental Scene editing, functions, Traits, search, and compile times.
2. [Blocks, Actor types, and Manager types](02-blocks.md) - the rest of the Block set, the Scene editor's handles and snapping, UI
   elements, the Paddle game template, Convert to code, and more drag and drop. The editor comes before more AI work.
3. [Sprite editor](08-sprite-editor.md) - a pixel-art editor for sprites and their animations, frame by frame with onion skinning, like
   GameMaker 6.1's; it follows sprites as an Actor type's appearance.
4. [AI assistant](01-ai-assistant.md) - more for the MCP server, then the in-app panel with the user's API key, which waits until the editor
   does more. **Every new editor feature lands with its command, its AI tool, and their tests.**
5. [Version control](03-git.md) - sidebar change markers, restoring old versions, branches, remotes.
6. [Debugger](04-debugger.md) - the debug agent, debugger panels, `tools/debug-overlay`, retiring the debug menu.
7. [Export and distribution](05-export-and-distribution.md) - signed Android builds, desktop installers, web export, iOS, the JDK download,
   Creator's own installers.
8. [Project upkeep](06-project-upkeep.md) - template drift, Kubriko version upgrades, opting existing projects in.
9. [Later](07-later.md) - code editor, language server, asset editor extension point.

## Vision

A beginner downloads one installer, picks a template, and has a running game in minutes, without knowing Gradle, Kotlin Multiplatform, or
IntelliJ, building behavior visually or by asking Claude, and seeing every change as readable Events and Blocks. The project is always a plain
Gradle project that builds in IntelliJ exactly as if written by hand.

**Non-goals:** replacing IntelliJ; a scripting language or runtime interpreter (gameplay is always compiled Kotlin); GameMaker file
compatibility or a copy of its look, icons, or action set; running Creator itself on mobile or the web.

## Principles

1. **The library never depends on Creator.** Kubriko stays a library first.
2. **Plain files, plain Gradle.** Nothing the build needs lives in editor-private files; deleting `.kubriko/` never breaks a build, it only
   turns block-built Actor types into ordinary code.
3. **Creator never rewrites code it didn't generate,** and stops updating generated code that was changed by hand.
4. **Every change outside Creator-owned files is shown as a diff and confirmed.**
5. **No engine changes for editor convenience**; editor-only runtime needs go into opt-in modules.
6. **The AI can do exactly what the user can, through the same validated commands, and nothing else.**
7. **Two reference projects keep the design honest:** a beginner game built only from Blocks (the Getting started paddle game, and the
   Breakout in `~/Projects/breakout`), and Tesselar (see `06-project-upkeep.md`).

## Files and ownership

| Path | Owner | When changed elsewhere |
|---|---|---|
| `.kubriko/actors/*.json`, `.kubriko/managers/*.json`, `.kubriko/project.json` | Creator | Reloaded within a second; code regenerated. |
| `<package>/generated/*.kt` | Creator | Checksum mismatch → Creator stops updating it and offers to discard the edits. Committed, so projects build without Creator. |
| `composeResources/files/scenes/*.json` | Shared | Sidebar refreshes. |
| Hand-written Kotlin and resources | User | Sidebar refreshes; an open Scene offers to reload. |
| Template files (`Game.kt`, build files) | User after creation | `templateVersion` in `.kubriko/project.json`; upgrades are shown as a diff (`06-project-upkeep.md`). |
| The Kubriko entries of the build files (`kubriko` version, `kubriko-*` libraries, Creator's Maven Local block) | Creator | Brought back in line with the code and Creator's version after every regeneration. |
| `.kubriko/local/` | Creator | Git-ignored. |

## Terminology

| Kubriko Creator | Kubriko code | GameMaker 6.1 |
|---|---|---|
| Actor type | A class implementing `Actor` | Object |
| Actor | An instance placed in a Scene | Instance |
| Event | A Trait callback (`update`, `onCollisionDetected`, …) | Event |
| Block | One generated statement inside an Event | Action |
| Kotlin Block | A verbatim Kotlin snippet | "Execute code" |
| Manager type | A class extending `Manager`: game-wide state and logic | Global variables, or a controller object |
| Property | An `@Exposed` property (or a Manager's `var`) | Variable |
| Scene | A serialized JSON scene file | Room |
| Timer | `Timer` | Alarm |

Our own names, icons, and wording throughout; "inspired by GameMaker" is a factual credit, not branding.

## Risks

- **Maintenance:** every Kotlin, Gradle, AGP, Compose, and Android SDK bump can break Creator; the integration test builds every template.
- **Compile times** slow the edit-play loop; `10-fast-iteration.md` measures and cuts them, up to hot reload.
- **Session protocol drift** across Kubriko versions; it is versioned and checked.
- **AI cost and privacy:** users pay for their own usage and send project files to the API; the UI says so.

## Open questions

1. The oldest Kubriko version Creator supports for projects that aren't opted in (sessions work from 0.7.4; opted-in projects are raised to
   Creator's own version, `00-library-release.md`).
2. Whether the Showcase should become editable in Creator.
3. Whether web export should offer one-click hosting.
4. Whether Creator may run the user's own Claude Code for its assistant panel, so subscription users need no API key. Calling the API with
   a subscription is not allowed; see `01-ai-assistant.md` → in-app assistant panel for what the terms say and what to ask Anthropic.
