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

## State (commits on main not pushed since b5b65fbfb; ask before pushing; `library.version` is 0.8.1, unreleased; 0.8.0 is on Maven Central)

- **Foundation work** (2026-10-28/29, `01-foundation.md`), after a review found generated games with real bugs and a data flow
  that recomputed the whole project on every change. Done: generated games (Log Blocks off outside play sessions through a
  version 5 template upgrade; movement steps at most 33 ms; removed Actors skipped; Actors active off screen; edges from the
  bounding box; child collisions before parents'; goToScene not restarting its own load; shared `ActorClassCode`), opening
  without Gradle, a scope per workspace, Scene classes kept between Scenes, build and Scene timings in the console, the
  benchmark, batched type writes, generation from memory with caches, `ActorTypeFamily` no longer quadratic, Scenes cached
  in `SceneStore`, a watcher that ignores Creator's own writes, `ScenePatch` in the Scene editor, and a narrower workspace
  recomposition. A type change at 200 types went from 1.3 s to 0.14 s, a Scene edit at 5,000 Actors from 1.26 s to 45 ms.
  On 2026-10-29 also: the Scene editor gets parsed Actors from `SceneStore` (read while the project builds), picking goes
  through `SceneBoundsGrid`, the type editor's lists are lazy (the Block list's drops from `LaidOutRows`, with auto-scroll),
  and the undo history and git state are collected where they are shown. What is left of `01-foundation.md` is only
  conditional (measured first) or a manual check.
- **The build benchmark** (2026-10-29, `benchmark/CreatorBuildBenchmarkTest`): the 200-type project built for real, with a
  baseline in `02-fast-iteration.md`. It found the build cache hiding compiles (so it passes `--no-build-cache`) and Kotlin
  rebuilding all of common code on every change (KT-62686). With the user's agreement, Creator's builds now pass Kotlin's
  internal property for incremental common code (`IncrementalCompilationPolicy`): 5.5 s to 0.7 s per type change at 200 types;
  off for projects declaring an `expect`, and a clean build (`--rerun-tasks`) after a failed build, Reload, or classes that
  couldn't be loaded.
- **`Logger.log`** (`tools/logger`, db2184aaa) builds its list once instead of three times, with no API change: a library
  change for 0.8.1.
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

1. The rest of `02-fast-iteration.md`: step 5 (steps 1 to 4 are done), each measured with `CreatorBuildBenchmarkTest` (a
   row each).
2. The rest in file order: `03-large-projects.md` (caps), `04-resource-organization.md` (Search, the previous next item), and on.

## Never checked by hand

- Warming Kotlin's compile daemon (2026-10-29, `kubrikoCreatorWarmUp`), seen working: opening the scratch Breakout ran it
  right after the classes loaded (a fresh Kotlin daemon started, nothing in the console), the first change after that built
  in 3.0 s, and the integration test runs it on the template. Not compared in the app against the same change without it
  (the command line said 2.9 to 3.2 s down to 1.0 to 1.5 s with a cold Kotlin daemon), and not seen: a change made while
  the warm-up still runs (it should wait for it), and a project whose warm-up fails (nothing should show).
- The Scene editor ticking without the focus (2026-10-29, `SceneView`'s `shouldPauseOnFocusLoss = false`): a Scene swapped
  in while Creator was in the background was seen staying empty before it; not seen since. Also worth a look: what it
  costs while Creator sits in the background with a Scene open (it now draws every frame there too).

- Swapping the open Scene's classes (2026-10-29), seen working on the scratch Breakout: "Updating…", Bricks redrawn as
  circles and back with zoom and selection kept, a broken source showing the notice and recovering once removed. Not seen:
  a change made while that build ran (it should build once more after it), closing the Scene or opening another one
  during the build, Reload during it, and a Scene of many Actors (whether the old one stays smooth while the new one loads).

- Class folders instead of the jar (2026-10-29): the benchmark loads them and creates a 1,000-Actor Scene from them, and the
  scratch Breakout's classpath lists the three folders with no jar built; seen in the app: opening the scratch Breakout's
  `main` from them. Not seen: a sprite and a Scene's music still found, and a Scene kept open (outdated) while a type
  change is built.

- The configuration cache (2026-10-29, template version 6), seen working: two classpath builds of the scratch Breakout with
  the cache on (the second reused it, 717 to 286 ms) and the play task storing an entry, both from the command line; the
  integration test and the benchmark build with it. Not seen: a game played from Creator with it on, the version 6 upgrade's
  diff in the dialog, and Creator's own console after a build that reused the cache.

- Keeping Gradle warm (2026-10-29), seen working: opening the scratch Breakout built it with nothing clicked ("Built in 2.9
  seconds." in the console), and on a later launch, opening `main` right after it without a build ("The Scene is open, in
  0.0 seconds."). Not seen: opening a Scene while that build still runs (it should wait for it, not build twice), Play
  pressed during it (it should start once the build ends), and a project whose build fails on open (the console shows the
  problems right away, and the first Scene shows the failure without building again).

- Incremental common code in the running app (only measured by the benchmark and unit-tested): a type change in the scratch
  Breakout opening the Scene faster, Reload taking a full build, and a broken Kotlin Block's failed build followed by a clean one.

- Foundation (2026-10-29), seen working: playing the scratch Breakout with the new generated code (bounces, Bricks removed, no
  errors), dragging the Ball in the Scene editor and undoing it, and going back to a Scene without building. Not seen: a Log Block's line reaching the console in a play session
  after the version 5 upgrade; quitting with a change made less than 100 ms before (it should be written); the Scene editor
  after an inspector edit (a replaced Actor) and after placing and removing Actors (`ScenePatch` add and remove); a Scene
  changed by git while open; and the version 5 upgrade's diff on the real Breakout copy.

- The lazy Block list: dragging an existing Block by its handle up past the top edge (auto-scroll upwards); the palette
  drag downwards was seen working, and so were the animated moves, the flash of a clicked or dropped Block, and dragging an
  Event to the top with the selected one staying selected. Not seen: a Block or Event flashing after undoing its removal, and
  typing into a just-added Block (an edit keeps its key, so the field should keep the focus).
- Deselecting in the sidebar: clicking the selected type or Scene again, and clicking empty space (also a Kotlin file
  row, which isn't clickable, counts as empty space).
- The Scene editor after the 2026-10-29 changes: clicking Actors (picked through `SceneBoundsGrid`), selecting right after
  placing, removing, and undoing (a full rebuild of the grid), and a click after a drag (an in-place grid update).

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
- In the editor: the New Scene field, undo and redo, and Connect and Disconnect against a real config. Watch
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
