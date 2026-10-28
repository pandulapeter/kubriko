<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Library releases

Kubriko 0.8.0 shipped Creator's library side (`tool-creator-api`, `tool-creator-session` with protocol 3, `SceneEditor.Content`), so
projects resolve Kubriko from Maven Central. What Creator needs from the libraries reaches projects only through later releases.

## Rules

- **Projects use Creator's own Kubriko version.** Creator raises an opted-in project to the version it was built with whenever it opens it
  (`project/KubrikoBuildFiles`, see `../CLAUDE.md`), and embedded Scene editing needs the two to be equal. So `scene-editor-api` and
  `creator-api` make no binary compatibility promise across versions, and a Creator release follows each Kubriko release that changes them.
- A session that is older than Creator (a project that isn't opted in, or one on a newer version than Creator's, which is kept) keeps
  working, with the features it lacks hidden, rather than being refused.
- `CreatorEntryPoint` only ever gains members with default implementations.
- Nothing speculative goes into a published artifact. Public API is frozen once released (root `CLAUDE.md` → Conventions), and an API
  designed before the Creator feature that uses it exists is the one most likely to be wrong.

## Waiting for a later release

Each one changes the libraries, so each is offered only to sessions whose protocol version has it:

- the template upgrade that makes `GameEntryPoint` report the game's Kubriko instances (`getRunningInstances`, which `start_game` waits on);
- the debug agent, `tools/debug-overlay`, and deprecating the debug menu (`04-debugger.md`);
- Scene editor commands from Creator, Managers' Properties per Scene, multi-select, layers, thumbnails, and Creator's look
  (`02-blocks.md` → Scene editor);
- hot reload in the session process and the asset editor extension point (`07-later.md`).

Blocks, Manager types, templates, git, the AI assistant, and export are Creator-only (generated code uses existing plugins) and don't wait
for any of this.
