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

- **Manager types have a Kotlin UI:** verbatim Compose statements after the texts in the generated `Composable`, edited in a dialog
  from the "Kotlin UI" section under the texts, and by `set_kotlin_ui`. Leading import lines become the file's imports; other
  Manager types, `actorManager`, `viewportManager`, and `stateManager` are looked up only when the code names them.
- **Checked live** on a scratch Breakout copy: the section's preview, the dialog with the code (it soft-wraps long lines), and the
  regenerated Manager. Not played. The integration test compiles a Kotlin UI using all of the above, and a broken one fails it.
- **Scene settings** (previous session) and the paused condition are in, see `../CLAUDE.md`.
- **AI work is paused** by the user's choice: only fix bugs in what exists.

## Next

1. Audio (with a Scene's music), sprites, and the rest of `02-blocks.md`.
2. Scene settings in Creator: a panel beside the Scene editor (today only the embedded editor and `set_scene_settings` edit them).
3. UI elements (`02-blocks.md` → Game UI), once a second UI need comes.

## Never checked by hand

- Typing in the Kotlin UI dialog (each keystroke is a `set_kotlin_ui`, merged into one undo step), and a game with a Kotlin UI
  actually played (the generated code compiles; nobody has seen it draw).
- Placing and editing `SceneSettings` from the embedded Scene editor's palette and inspector (the color and `SceneOffset` editors),
  and that its camera start doesn't move the editor's camera. `set_scene_settings` from a real AI session.
- A ball bouncing exactly at the bounds' edge, and going to another Scene with different (or no) settings.
- The Scene editor creating an edited Actor again when a drag ends or the selection moves on (unit-tested in `SceneDocumentTest`;
  watch the debug overlay's collision mask follow a resize, and that undo still selects the right Actor).
- The go to a Scene card's Scene menu (the pause card was seen in a screenshot), and the upgrade dialog's "deleted" file row.
- A paused game in the embedded Scene editor (its Managers' Events don't run there, so it should be unaffected).
- The "score of Game" Property menu in an Actor's Block.
- A Scene whose Actors use a Manager, in the embedded Scene editor (`createCustomManagers`, `isInSceneEditor`).
- The Manager type, text, and upgrade tools from a real AI session: ask for a score again ("stop the paddle on key release and show a
  score"), and watch the activity view pace a burst of calls.
- Connect for the plain "Claude Code" row in the Claude app, and for Claude Desktop from a real chat (writing its config needs the user).
- Codex (out of quota until Nov 21, 2026), Cursor, Gemini CLI, Windows, and fish aliases with a real fish.
- In the editor: the Event move buttons, the New Scene field, undo and redo, and Connect and Disconnect against a real config. Watch
  for a palette drag adding its Block twice.

## Working with the live app

- **Guard every synthetic click and keystroke.** The user works alongside, and Android Studio covers the same screen area. Only click
  when the topmost window under the point belongs to Creator's PID. Prefer editing `.kubriko/*.json` on disk, which Creator reloads.
- Test on a scratch copy of `~/Projects/breakout` (it has the user's uncommitted work), opened with `:creator:run --args=<dir>`.
  The real one was opened with unfinished code once, and still shows the upgrade notice: leave it to the user.
- Never recompile `:creator` while `:creator:run` is up, and restart it with the publish tasks after `tools/creator-session` changes.
- The integration test: `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=0.7.5-creator ./gradlew :creator:test --rerun`.
