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

## Roadmap, in order

1. [Blocks and Actor types](02-blocks.md) - next: on-screen text and score, Scenes, audio, sprites, and the rest of the Block set (Properties, flow, Timers, and spawning exist);
   the Paddle game template; Convert to code; drag-and-drop. The editor comes before more AI work.
2. [AI assistant](01-ai-assistant.md) - the command registry, its MCP server, one-click setup, the game tools, and the activity view exist,
   and real Claude Code sessions use them; the in-app panel with the user's API key waits until the editor does more. **Every new editor
   feature lands with its command, its AI tool, and their tests.**
3. [Version control](03-git.md) - sidebar change markers, restoring old versions, branches, remotes.
4. [Debugger](04-debugger.md) - the debug agent, debugger panels, `tools/debug-overlay`, retiring the debug menu.
5. [Export and distribution](05-export-and-distribution.md) - signed Android builds, desktop installers, web export, iOS, the JDK download,
   Creator's own installers.
6. [Project upkeep](06-project-upkeep.md) - template drift, Kubriko version upgrades, opting existing projects in.
7. [Later](07-later.md) - code editor, hot reload, language server, asset editor extension point.

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
| `.kubriko/actors/*.json`, `.kubriko/project.json` | Creator | Reloaded within a second; code regenerated. |
| `<package>/generated/*.kt` | Creator | Checksum mismatch → Creator stops updating it and offers to discard the edits. Committed, so projects build without Creator. |
| `composeResources/files/scenes/*.json` | Shared | Sidebar refreshes. |
| Hand-written Kotlin and resources | User | Sidebar refreshes; an open Scene offers to reload. |
| Template files (`Game.kt`, build files) | User after creation | Not tracked yet (`06-project-upkeep.md`). |
| `.kubriko/local/` | Creator | Git-ignored. |

## Terminology

| Kubriko Creator | Kubriko code | GameMaker 6.1 |
|---|---|---|
| Actor type | A class implementing `Actor` | Object |
| Actor | An instance placed in a Scene | Instance |
| Event | A Trait callback (`update`, `onCollisionDetected`, …) | Event |
| Block | One generated statement inside an Event | Action |
| Kotlin Block | A verbatim Kotlin snippet | "Execute code" |
| Property | An `@Exposed` property | Variable |
| Scene | A serialized JSON scene file | Room |
| Timer | `Timer` | Alarm |

Our own names, icons, and wording throughout; "inspired by GameMaker" is a factual credit, not branding.

## Risks

- **Maintenance:** every Kotlin, Gradle, AGP, Compose, and Android SDK bump can break Creator; the integration test builds every template.
- **Compile times** slow the edit-play loop; hot reload (`07-later.md`) is the answer.
- **Session protocol drift** across Kubriko versions; it is versioned and checked.
- **AI cost and privacy:** users pay for their own usage and send project files to the API; the UI says so.

## Open questions

1. The oldest Kubriko version Creator supports (sessions work from 0.7.4; Scene editing and Actor types need the first release with
   `tool-creator-api` and `tool-creator-session`).
2. Whether the Showcase should become editable in Creator.
3. Whether web export should offer one-click hosting.
4. Whether Creator may run the user's own Claude Code for its assistant panel, so subscription users need no API key. Calling the API with
   a subscription is not allowed; see `01-ai-assistant.md` → in-app assistant panel for what the terms say and what to ask Anthropic.
