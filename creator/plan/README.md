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

The files are numbered in the order they are worked on.

1. [Large projects](01-large-projects.md) - nothing capped (named Timers, every key), the Scene list panel, and AI tools that page.
2. [Folders and search](02-resource-organization.md) - the sidebar filter, quick open, find usages, and the search AI tools.
3. [Reuse, Traits, and bigger games](03-reuse-and-traits.md) - functions, Traits, and what bigger games need.
4. [Blocks, Actor types, and Manager types](04-blocks.md) - the rest of the Block set, the Scene editor's handles and snapping, sprite
   sheets and animation, UI elements, the Paddle game template, Convert to code, and more drag and drop.
5. [Sprite editor](05-sprite-editor.md) - a pixel-art editor for sprites and their animations, frame by frame with onion skinning, like
   GameMaker 6.1's; it follows sprite sheets in `04-blocks.md`.
6. [AI assistant](06-ai-assistant.md) - more for the MCP server, then the in-app panel with the user's API key, which waits until the editor
   does more. **Every new editor feature lands with its command, its AI tool, and their tests.**
7. [Version control](07-git.md) - sidebar change markers, restoring old versions, branches, remotes.
8. [Debugger](08-debugger.md) - the debug agent, debugger panels, `tools/debug-overlay`, retiring the debug menu.
9. [Export and distribution](09-export-and-distribution.md) - signed Android builds, desktop installers, web export, iOS,
   Creator's own installers.
10. [Project upkeep](10-project-upkeep.md) - template drift, Kubriko version upgrades, opting existing projects in.
11. [Onboarding](11-onboarding.md) - first-run setup that downloads the JDK, Gradle, and the Android SDK with progress, five bundled demo
    games of increasing complexity, and cloning a project from version control.
12. [Later](12-later.md) - code editor, language server, asset editor extension point.

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
   Breakout in `~/Projects/breakout`), and Tesselar (see `10-project-upkeep.md`).

## Files and ownership

| Path | Owner | When changed elsewhere |
|---|---|---|
| `.kubriko/actors/**/*.json`, `.kubriko/managers/**/*.json`, `.kubriko/project.json` | Creator | Reloaded within a second; code regenerated. |
| `<package>/generated/*.kt` | Creator | Checksum mismatch → Creator stops updating it and offers to discard the edits. Committed, so projects build without Creator. |
| `composeResources/files/scenes/**/*.json` | Shared | Sidebar refreshes. |
| Hand-written Kotlin and resources | User | Sidebar refreshes; an open Scene offers to reload. |
| Template files (`Game.kt`, build files) | User after creation | `templateVersion` in `.kubriko/project.json`; upgrades are shown as a diff (`10-project-upkeep.md`). |
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
- **Compile times** slow the edit-play loop; the build benchmark (`benchmark/CreatorBuildBenchmarkTest`) measures every change to them.
- **Session protocol drift** across Kubriko versions; it is versioned and checked.
- **AI cost and privacy:** users pay for their own usage and send project files to the API; the UI says so.

## Open questions

1. The oldest Kubriko version Creator supports for projects that aren't opted in (sessions work from 0.7.4; opted-in projects are raised to
   Creator's own version, `../CLAUDE.md` → Conventions).
2. Whether the Showcase should become editable in Creator.
3. Whether web export should offer one-click hosting.
4. Whether Creator may run the user's own Claude Code for its assistant panel, so subscription users need no API key. Calling the API with
   a subscription is not allowed; see `06-ai-assistant.md` → in-app assistant panel for what the terms say and what to ask Anthropic,
   citing Android Studio, which hosts Claude over the Agent Client Protocol (ACP) with the user's own sign-in.
