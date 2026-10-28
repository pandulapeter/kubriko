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

## State (not pushed since a957441b)

- **Scene settings exist** as the generated `SceneSettings` Actor (bounds, background color, camera start), with
  `set_scene_settings`. The user chose bounds only: the view stays `Dynamic`, and edge Events use the bounds when a Scene has
  them. Music waits for the Audio Blocks.
- **The if Block can check whether the game is paused.**
- **Checked live** on a scratch Breakout copy (template version 2, regenerated): the background fills the bounds behind every
  Actor, the camera start shifts the view, and the ball stays within the bounds (frames 0.4 s apart, so no bounce was caught exactly
  at an edge). Unit tests and the integration test pass (the integration test is quick with warm Gradle daemons, and a deliberately
  broken `SceneSettings` made it fail, so it does compile).
- **AI work is paused** by the user's choice: only fix bugs in what exists.

## Next

1. Kotlin UI for Manager types (`02-blocks.md` → Game UI).
2. Audio (with a Scene's music), sprites, and the rest of `02-blocks.md`.
3. Scene settings in Creator: a panel beside the Scene editor (today only the embedded editor and `set_scene_settings` edit them).

## Never checked by hand

- Placing and editing `SceneSettings` from the embedded Scene editor's palette and inspector (the color and `SceneOffset` editors),
  and that its camera start doesn't move the editor's camera. `set_scene_settings` from a real AI session.
- A ball bouncing exactly at the bounds' edge, and going to another Scene with different (or no) settings.
- The Scene editor creating an edited Actor again when a drag ends or the selection moves on (unit-tested in `SceneDocumentTest`;
  watch the debug overlay's collision mask follow a resize, and that undo still selects the right Actor).
- The go to a Scene card's Scene menu (the pause card was seen in a screenshot), and the upgrade dialog's "deleted" file row.
- A paused game in the embedded Scene editor (its Managers' Events don't run there, so it should be unaffected).
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
