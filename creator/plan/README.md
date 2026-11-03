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
started), fix what it finds in commits of their own, and only then delete the item here. The review checks the change against
`../CLAUDE.md` → Definition of done, so what a whole-codebase sweep would find later is found while the item is fresh.

[HANDOVER.md](HANDOVER.md) is where the last session stopped: start there.

## Roadmap, in order

The files are numbered in the order they are worked on. Every item of `01` to `05` is to be done, none capped in number.

1. [Large projects](01-large-projects.md) - costs that grow with the project (wide families, per-keystroke work, Scene reorders, a
   flooded console, two long menus), then what is left once the plans it waits for land: screen texts in any layout, Actor names, and
   what the benchmarks show is needed.
2. [AI assistant](02-ai-assistant.md) - what is left of the in-app assistant (ACP protocol 2, the registry's icons, other agents'
   fallbacks, Codex's profile), more context for large games, and more for the MCP server, alongside `04` to `07`.
3. [Settings](03-settings.md) - the rest of Project settings (Targets, Release, Icons, and Signing) and of Creator settings.
4. [Folders and search](04-resource-organization.md) - the rest of the sidebar filter, quick open, find usages, and the search AI tools.
5. [Reuse, Traits, and bigger games](05-reuse-and-traits.md) - functions, Traits, and what bigger games need.
6. [Blocks, Actor types, and Manager types](06-blocks.md) - the rest of the Block set, the Scene editor's handles and snapping, sprite
   sheets and animation, UI elements, the Paddle game template, Convert to code, and more drag and drop.
7. [Sprite editor](07-sprite-editor.md) - a pixel-art editor for sprites and their animations, frame by frame with onion skinning, like
   GameMaker 6.1's; it follows sprite sheets in `06-blocks.md`.
8. [Version control](08-git.md) - sidebar change markers, restoring old versions, branches, remotes.
9. [Debugger](09-debugger.md) - the debug agent, debugger panels, `tools/debug-overlay`, retiring the debug menu.
10. [Export and distribution](10-export-and-distribution.md) - debug and release builds, signed Android builds, desktop installers,
    web export, iOS, and Creator's own installers.
11. [Project upkeep](11-project-upkeep.md) - template drift, Kubriko version upgrades, opting existing projects in.
12. [Onboarding](12-onboarding.md) - first-run setup that downloads the JDK, Gradle, and the Android SDK with progress, five bundled demo
    games of increasing complexity, and cloning a project from version control.
13. [Later](13-later.md) - code editor, language server, asset editor extension point.

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
Breakout in `~/Projects/breakout`), and Tesselar (see `11-project-upkeep.md`). The principles every plan keeps, and who owns which
project file, are in `../CLAUDE.md`.

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
   release, not before building; see `02-ai-assistant.md` → What the terms allow.
