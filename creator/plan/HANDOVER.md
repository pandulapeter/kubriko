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

## State (committed on main but not pushed; `library.version` is 0.8.1 on main, unreleased; 0.8.0 is on Maven Central)

- **Audio exists** (2026-10-28): playSound, playMusic, and stopMusic Blocks (palette tab "Sound and music"), a Scene's music in
  `SceneSettings` (`set_scene_settings` takes `music`), and the generated `GeneratedAudio` Manager, written only while something plays
  audio. Scene changes now regenerate the code, since a Scene's music decides that. The palette's category tabs wrap now; an Actor
  type's last tab was already cut off before.
- **The audio plugin brings a JitPack block** into `settings.gradle.kts` (JLayer, which its desktop library needs); the integration
  test now resolves the desktop runtime classpath, which is where a missing repository shows.

- **Kubriko 0.8.0 is released.** `00-library-release.md` is now the policy for later releases.
- **Projects follow Creator's Kubriko version and code** (`project/KubrikoBuildFiles`, after every regeneration, so also on open and on
  creation): the catalog's `kubriko` version is raised to Creator's own (a newer one is kept), Creator's Maven Local block follows
  `useMavenLocal`, and the `kubriko-*` libraries are exactly the ones the sources name. Changes go to the console.
- **Plugin Managers moved into generated code:** `KubrikoWiring` creates `CollisionManager` and `KeyboardInputManager` only when some type
  needs them (`PluginManagers`), Actor types are `Collidable` only when a collision Event involves them, and template version 4 drops the
  two Managers from `Game.kt`. The starter template no longer depends on the collision or keyboard plugins.
- **AI work is paused** by the user's choice: only fix bugs in what exists.

## Next

1. Sprites as an Actor type's appearance, then the rest of `02-blocks.md`. Their plugins come and go with the Blocks that use them,
   as audio's does (`PluginManagers`, `KubrikoWiring`, a generated Manager written only when used); not in a template upgrade. The
   sprite editor (`08-sprite-editor.md`) is next after sprites.
2. Scene settings in Creator: a panel beside the Scene editor (today only the embedded editor and `set_scene_settings` edit them).
3. UI elements (`02-blocks.md` → Game UI), once a second UI need comes.

## Never checked by hand

- Hearing the audio: the scratch Breakout played with a playMusic Block in a Manager's gameStarted Event and a playSound Block in a
  collision Event, and ran without errors, but nobody listened. Also: music pausing and resuming with the window's focus, a non-looping
  music played again after it ended, a Scene's music set from the embedded Scene editor's inspector (a plain text field: the path), and
  going between Scenes with the same and different music. Web audio with `Res.getUri` paths.
- The audio Blocks' file choices with no sounds or music in the project (the menu is empty, with no hint how to add files).

- The dependency sync and the version 4 upgrade on the real Breakout copy (it is on `0.7.5-creator` and template version 3): opening it
  should raise it to Creator's version, offer the upgrade, and after the upgrade drop nothing it uses. Then remove the last collision
  Event and watch `plugin-collision` leave the build, and undo it.
- A project whose sources import a plugin by hand (say physics) keeping it; a dependency added by hand before any code uses it is removed
  on the next regeneration, which the user may not expect.
- A rename following Kotlin code from the UI (unit-tested in `BlockTypeEditsTest` and `KotlinIdentifiersTest`). Adding a Property
  named like another Manager type's field (`lives` beside a `Lives` type) moves that field to `livesManager`, and Kotlin code isn't
  updated for that.
- Placing and editing `SceneSettings` from the embedded Scene editor's palette and inspector (the color and `SceneOffset` editors),
  and that its camera start doesn't move the editor's camera. `set_scene_settings` from a real AI session.
- A ball bouncing exactly at the bounds' edge, and going to another Scene with different (or no) settings.
- The Scene editor creating an edited Actor again when a drag ends or the selection moves on (unit-tested in `SceneDocumentTest`;
  watch the debug overlay's collision mask follow a resize, and that undo still selects the right Actor).
- The go to a Scene card's Scene menu (the pause card was seen in a screenshot).
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
- The integration test: `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=0.8.0 ./gradlew :creator:test --rerun`.
