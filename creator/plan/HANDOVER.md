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

## State (17 commits on main not pushed, b5b65fbfb through this handover update; ask before pushing; `library.version` is 0.8.1, unreleased; 0.8.0 is on Maven Central)

- **Parent Actor types** (2026-10-28), generated as real Kotlin inheritance (`<Name>Base` classes with Event hooks), with the
  runParentEvent Block, and the editor's parent menu and greyed inherited Properties and Events. The integration test builds a
  three-generation family for desktop and web; seen working: choosing Brick as GoldBrick's parent in the editor, and the scratch
  Breakout compiling with `GoldBrick : BrickBase` and Ball colliding with `BrickBase`. The editor's inherited rows and their
  replace button were only built and unit-tested, not seen.
- **Plans for scaling** (`01` to `05`): the user wants every item in them done, with nothing capped in number, on a stable, scalable
  foundation first (2026-10-28: "the top priority now is to create a stable, scalable foundation").
- **`:creator:run` always uses local libraries** (`settings.gradle.kts`); the first version of that didn't take effect (fixed in
  b05e09d83), which moved the scratch Breakout to plain `0.8.1` once more: an opened project recovers on its own.
- **Folders** (2026-10-28) for Actor types, Manager types, and Scenes, with Scene names as paths and `rename_scene`. Seen working:
  creating folders, dragging Brick into one and Ball back with Cmd+Z from empty space, and moving `main` into `levels` (with the
  console naming `Game.kt`).
- **Scene Actors have IDs and Scene files one Actor per line** (2026-10-28): every Scene command, the AI tools, and the Scene editor
  address Actors by ID; existing Scenes get IDs when a command next writes them. Not seen in the running app yet (unit-tested).
- **Creator's own Scene editor** (2026-10-28) replaced the embedded library one (`tools/scene-editor`) and the session window: the Scene is drawn by the
  project's classes in Creator's pane, Actors are selected, moved, and removed, and Actor types are dragged in from the sidebar, all
  through commands (so undo and the AI share them). Seen working on the scratch Breakout: moving the Ball, dropping a Brick,
  selecting, Backspace, and Cmd+Z, and the inspector (a Brick's values, rotating it by typing).
- **Static sprites exist** (2026-10-28): an Actor type's `sprite` (`set_appearance`, the appearance column's Sprite menu) draws an
  image from `composeResources/drawable` instead of the shape, which stays the collision mask's; choosing one sizes the type to the
  image. `SpriteManager` is created in `KubrikoWiring` (also in the Scene editor) only while some type has a sprite. **Library
  change for 0.8.1:** a new `SpriteManager.newInstance(imageLoader, …)` overload (the old one is untouched, so binary compatible),
  which Creator's Scene editor uses to read sprites from the project's files (`SpriteLoadingEntryPoint`). Seen working on
  the scratch Breakout: the appearance column, the Ball sprite in the Scene editor, and in the played game.
- **Audio exists** (2026-10-28): playSound, playMusic, and stopMusic Blocks (palette tab "Sound and music"), a Scene's music in
  `SceneSettings` (`set_scene_settings` takes `music`), and the generated `GeneratedAudio` Manager, written only while something plays
  audio. Scene changes now regenerate the code, since a Scene's music decides that. The palette's category tabs wrap now; an Actor
  type's last tab was already cut off before.
- **The audio plugin brings a JitPack block** into `settings.gradle.kts` (JLayer, which its desktop library needs); the integration
  test now resolves the desktop runtime classpath, which is where a missing repository shows.
- **A color picker** (`ui/components/ColorPickerDialog`, from Tesselar's model editor) opens from every color field's swatch, with
  opacity; the sprite editor should reuse it. Seen working in a screenshot (a hue click, Done, the Actor type's color saved).

- **Kubriko 0.8.0 is released.** `00-library-release.md` is now the policy for later releases.
- **Projects follow Creator's Kubriko version and code** (`project/KubrikoBuildFiles`, after every regeneration, so also on open and on
  creation): the catalog's `kubriko` version is raised to Creator's own (a newer one is kept), Creator's Maven Local block follows
  `useMavenLocal`, and the `kubriko-*` libraries are exactly the ones the sources name. Changes go to the console.
- **Plugin Managers moved into generated code:** `KubrikoWiring` creates `CollisionManager` and `KeyboardInputManager` only when some type
  needs them (`PluginManagers`), Actor types are `Collidable` only when a collision Event involves them, and template version 4 drops the
  two Managers from `Game.kt`. The starter template no longer depends on the collision or keyboard plugins.
- **AI work is paused** by the user's choice: only fix bugs in what exists.

## Next

The plans were renumbered on 2026-10-28 into the order they are worked on (`README.md` → Roadmap), after a review found that the
foundation under the command layer doesn't scale and that generated games had real bugs. Work through `01-foundation.md` step by
step, committing each part:

1. Generated games (`01-foundation.md` → 1), with `Logger.log` in `tools/logger` (a library change without API changes).
2. Workspace lifecycle and opening (→ 2).
3. The benchmark (→ 3), then one project in memory (→ 4), Scene editing in place (→ 5), and the editor's UI (→ 6).
4. Then `02-fast-iteration.md`, and the rest in file order. Search, the next item before, is now `04-resource-organization.md`.

## Never checked by hand

- Parents in the editor: the inherited Properties and Events, replacing an inherited Event (it starts with runParentEvent), the
  parent menu leaving out the type's own descendants, deleting a parent with children (rejected in the console), and a child of
  another shape colliding. Playing the scratch Breakout with GoldBrick placed.
- Scene Actor IDs in the running app: selecting what was just placed, undoing a removal (the same Actor back, selected), and an old
  Scene file gaining IDs on its first edit.
- Folders: renaming a Scene folder that holds the open Scene, and dragging a Manager type into a folder.
- The inspector's Property fields and color, the Scene settings (adding them from the hint, music, camera start), scale, and size
  (whether the pivot moving to the middle surprises anyone), and that typing a number is one undo step.
- The Scene editor: a hand-written Actor type in a Scene (drawn, selectable, movable), an Actor without a `BoxBody`, zoom to fit
  with and without Scene settings, the middle-button pan, scroll pan on a mouse wheel, dropping onto a zoomed-out view, the version
  notice, and the outdated notice after an Actor type change followed by Reload.

- Sprites: a non-square or translucent image, a `.webp` or `.jpg` (its size isn't read when ImageIO can't, so the size stays), deleting
  the image while an Actor type uses it (the game should build and draw nothing), and undo and redo of choosing a sprite from the UI.
  The sprite menu's hint with no images in the project. A game window opened before the template upgrade (an older `Game.kt` has no
  `KubrikoWiring`, so `kubriko.get<SpriteManager>()` would throw).

- Hearing the audio: the scratch Breakout played with a playMusic Block in a Manager's gameStarted Event and a playSound Block in a
  collision Event, and ran without errors, but nobody listened. Also: music pausing and resuming with the window's focus, a non-looping
  music played again after it ended, and going between Scenes with the same and different music. Web audio with `Res.getUri` paths.
- The audio Blocks' file choices with no sounds or music in the project (the menu is empty, with no hint how to add files).

- The dependency sync and the version 4 upgrade on the real Breakout copy (it is on `0.7.5-creator` and template version 3): opening it
  should raise it to Creator's version, offer the upgrade, and after the upgrade drop nothing it uses. Then remove the last collision
  Event and watch `plugin-collision` leave the build, and undo it.
- A project whose sources import a plugin by hand (say physics) keeping it; a dependency added by hand before any code uses it is removed
  on the next regeneration, which the user may not expect.
- A rename following Kotlin code from the UI (unit-tested in `BlockTypeEditsTest` and `KotlinIdentifiersTest`). Adding a Property
  named like another Manager type's field (`lives` beside a `Lives` type) moves that field to `livesManager`, and Kotlin code isn't
  updated for that.
- That a Scene's camera start doesn't move the Scene editor's camera. `set_scene_settings` from a real AI session.
- A ball bouncing exactly at the bounds' edge, and going to another Scene with different (or no) settings.
- The go to a Scene card's Scene menu (the pause card was seen in a screenshot).
- A paused game in the Scene editor (its Managers' Events don't run there, so it should be unaffected).
- The "score of Game" Property menu in an Actor's Block.
- A Scene whose Actors use a Manager, in the Scene editor (`createCustomManagers`, `isInSceneEditor`).
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
- The integration test: `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=0.8.1-creator ./gradlew :creator:test --tests
  "*ProjectTemplateIntegrationTest*" --rerun` (after a `:creator:run` or `publishToMavenLocal` with `-Pcreator.localKubriko=true`,
  since 0.8.1 isn't released; the libraries changed since 0.8.0).
- The last session's scratch Breakout lived in its scratchpad, so make a fresh copy; a copy opened by `:creator:run` gets
  `0.8.1-creator` and the Maven Local block on its own.
