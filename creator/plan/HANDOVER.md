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

- **Scene Blocks exist:** go to a Scene, restart the Scene, and pause / resume / toggle, through the generated `GeneratedScenes`
  Manager and `StateManager`. Template version 3 hands the first Scene to `KubrikoWiring.createManagers(firstScene = …)` and deletes
  the template's `SceneLoader.kt`; the upgrade can now delete files.
- **Generated code reviewed against the examples:** Manager types with key Events are `KeyboardInputAware` Actors (they had never
  received keys), Actors' key Events don't run while paused, Actor types are `Dynamic` only when needed, collision masks follow the
  body's scale and rotation, and only used Managers are looked up.
- **Checked live** on a scratch Breakout copy (version 2): the upgrade proposed exactly Game.kt plus deleting SceneLoader.kt, Enter
  went to `level-2`, W froze and resumed the game (and still worked after
  each Scene change), and S restarted the Scene. All unit tests and the integration test pass.
- **AI work is paused** by the user's choice: only fix bugs in what exists.

## Next

1. Scene settings (`02-blocks.md` → Next additions): size, background, camera start, and music as a `Unique` `Editable` Actor, and a
   condition for whether the game is paused.
2. Kotlin UI for Manager types (`02-blocks.md` → Game UI).
3. Audio, sprites, and the rest of `02-blocks.md`.

## Never checked by hand

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
