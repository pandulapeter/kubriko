<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Handover

The state between sessions: where the last one stopped, what to do next, and what was never checked by hand. Each session rewrites it
before it ends. What exists is in `../CLAUDE.md`, what is planned in the other files here.

## State (as of 209bbe1f, not pushed since a957441b)

- **Manager types exist:** game-wide Properties, Events, and texts on screen, built like Actor types. Actors and other Managers use their
  Properties through the Blocks' `manager` and `{Game.score}` placeholders. `KubrikoWiring` creates them.
- **Template upgrades exist:** older projects get a notice and a diff dialog that adds the `KubrikoWiring` calls to `Game.kt` and
  `GameEntryPoint.kt`.
- **Checked live** on a scratch Breakout copy: a Brick hit raised "Score: 20" on screen. All unit tests and the integration test pass.
- **AI work is paused** by the user's choice: only fix bugs in what exists.

## Next

1. The Scene Blocks (`02-blocks.md` → Next additions): go to another Scene, restart, pause, and Scene settings.
2. Kotlin UI for Manager types (`02-blocks.md` → Game UI).
3. Audio, sprites, and the rest of `02-blocks.md`.

## Never checked by hand

- The "score of Game" Property menu in an Actor's Block.
- A Scene whose Actors use a Manager, in the embedded Scene editor (`createCustomManagers`, `isInSceneEditor`).
- The Manager type, text, and upgrade tools from a real AI session.
- Connect for the plain "Claude Code" row in the Claude app, and for Claude Desktop from a real chat (writing its config needs the user).
- Codex (out of quota until Nov 21, 2026), Cursor, Gemini CLI, Windows, and fish aliases with a real fish.
- In the editor: the Event move buttons, the New Scene field, undo and redo, and Connect and Disconnect against a real config. Watch
  for a palette drag adding its Block twice.
- Known wart: a screenshot right after `start_game` can be empty, because the Scene's Actors aren't loaded yet.

## Working with the live app

- **Guard every synthetic click and keystroke.** The user works alongside, and Android Studio covers the same screen area. Only click
  when the topmost window under the point belongs to Creator's PID. Prefer editing `.kubriko/*.json` on disk, which Creator reloads.
- Test on a scratch copy of `~/Projects/breakout` (it has the user's uncommitted work), opened with `:creator:run --args=<dir>`.
  The real one was opened with unfinished code once, and still shows the upgrade notice: leave it to the user.
- Never recompile `:creator` while `:creator:run` is up, and restart it with the publish tasks after `tools/creator-session` changes.
- The integration test: `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=0.7.5-creator ./gradlew :creator:test --rerun`.
