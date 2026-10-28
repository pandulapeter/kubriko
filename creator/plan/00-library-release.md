<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# The groundwork release

The next Kubriko release (0.8.0) ships the library side of Creator, so projects and Creator's own
development resolve Kubriko from Maven Central instead of `<version>-creator` builds in Maven Local. It doesn't ship a usable Creator.

## Why it can't be "every library change Creator will ever need"

`tool-creator-session` runs at the **project's** Kubriko version, and embedded Scene editing needs the project's version to **equal**
Creator's. So every session feature (the debug agent, hot reload, Scene editor commands) reaches a project only through a Kubriko release
the project upgrades to, whenever it lands. This release can't make that go away; what it can do is make later releases additive:

- a session that is older than Creator keeps working, with the features it lacks hidden, rather than being refused;
- `CreatorEntryPoint` only ever gains members with default implementations;
- nothing speculative goes into a published artifact. Public API is frozen once released (root `CLAUDE.md` → Conventions), and an API
  designed before the Creator feature that uses it exists is the one most likely to be wrong.

## Already in (since 0.7.4)

- `tool-creator-api` (`CreatorEntryPoint`) and `tool-creator-session` (Edit and Play modes, protocol 2) as new artifacts.
- `SceneEditor.Content` and the rest of the `scene-editor-api` contract Creator embeds.
- `Kubriko.newInstance` keeps the **last** of several Managers of one class (a behavior change for the release notes).
- `ViewportManager.aspectRatioMode` and `setAspectRatioMode`, for fitting the Scene editor's view to a Scene's bounds.
- Pointer input documentation for touchscreens on desktop.

## Before the release

Done: protocol 3 (the protocol only grows, unknown commands are answered with a failure, Creator runs sessions of any version),
`AwaitActorsDrawn` (used by `start_game`), and `CreatorEntryPoint.getRunningInstances`. The templates' `GameEntryPoint` doesn't
report the game's instances yet: a template upgrade (Creator-only, after the release) keeps them where it can read them.

1. **Embedded Scene editing across patch versions:** decide whether Creator may embed a project whose Kubriko version differs only in the
   patch number, which then has to keep `scene-editor-api` and `creator-api` binary compatible within a minor version. Without it, every
   Creator release built on a newer Kubriko stops embedding Scenes of projects on this one.
2. **Release steps:** the tests, the `release-notes` skill, the publish workflow, then in Creator: the bundled `kubrikoVersion`, the
   templates, the integration test's default `KUBRIKO_CREATOR_VERSION`, the Breakout reference project, and `../CLAUDE.md` (Building)
   and `HANDOVER.md` switch to the released version. `creator.localKubriko` stays, for work that changes the libraries.

## Not in this release

Each one changes the libraries, so each will ride a later release, offered only to sessions whose protocol version has it:

- the debug agent, `tools/debug-overlay`, and deprecating the debug menu (`04-debugger.md`);
- Scene editor commands from Creator, Managers' Properties per Scene, multi-select, layers, thumbnails, and Creator's look
  (`02-blocks.md` → Scene editor);
- hot reload in the session process and the asset editor extension point (`07-later.md`).

Blocks, Manager types, templates, git, the AI assistant, and export are Creator-only (generated code uses existing plugins) and don't wait
for any of this.
