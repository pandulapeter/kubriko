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

Where the next session starts: what to do next and what still has to be checked by hand. Each session rewrites it before it ends.
What exists is in `../CLAUDE.md`, what is planned in the other files here.

## Status

- Ask before pushing.
- `library.version` is 0.8.1, unreleased (0.8.0 is on Maven Central). It holds library changes Creator uses: `Logger.log` building
  its list once, and the `SpriteManager.newInstance(imageLoader, …)` overload.
- The user wants every item of `01-large-projects.md` to `05-reuse-and-traits.md` done, with nothing capped in number.
- Creator is meant to be AI-centric: the in-app assistant panel (`02-ai-assistant.md`) was moved up to come right after
  `01-large-projects.md`, hosting agents over ACP as Android Studio does.

## Next

`02-ai-assistant.md` → Order: the panel, then the sign-in methods, together with the five "First" items of its Context for large
games, then the files in order, with the rest of `02-ai-assistant.md` alongside them. The ACP client and the Claude Agent behind it
(`Assistant`) exist, unit-tested against a fake agent, and `ClaudeAgentLiveTest` passed against the real `claude-acp` on the
personal login (one `list_scenes` call, no shell), but nothing in the app creates them yet. What is left in
`01-large-projects.md` waits for other plans (Traits, UI elements, git) or for the benchmarks.

## To check by hand

**Large projects (built and unit-tested on 2026-10-29, not yet seen in the app)**
- The paged `read_scene` (summary, type, rectangle), `list_actor_types` by folder, and the batch tools (`place_actors`,
  `remove_actors`, `restore_actors`, `set_actors_values`, `set_actors_state`) from a real AI session, say "fill the top of the
  Scene with a grid of bricks, then make every second one golden": one undo step each, and the open Scene showing the result.
- The Timer menu in a timerFinished Event's header and in the Timer Blocks: choosing, New Timer… typed in place (a bad name shows
  why), Rename this Timer… following through a family, and an old project's numbered Timers showing as `timer1`.
- The key picker (`KeyChoiceButton`): its popup's search, the list scrolling through all 296 keys, choosing by pressing a key
  (Shift, F-keys, the numpad), and an old project's `left`/`space` keys loading.
- The Scene editor's Actors button and list (filtering, selecting, centering, following a canvas selection), and the
  inspector's send-to-back and bring-to-front buttons.
- Getting there: the live check stopped on a Gradle 9.7.1 daemon started from a sandboxed shell, which can't run `uname`, so
  Creator's project model fails with "couldn't open this folder as a Gradle project". Start Creator after `./gradlew --stop` in
  the scratch copy (outside the sandbox).

Each of these is built and unit-tested, but wasn't seen in the running app.

**Playing**
- Hot reload: a hand-written Kotlin change (a Composable in `Game.kt`); a Scene, sprite, or sound changed while playing (Hot
  Reload's copy holds the resources too, so whether a reload brings them is open); a Manager type's Property added (should
  restart); a JetBrains Runtime that Gradle has to download first (the first Play waits with only "Building…"); the AI's
  `start_game` after a restart, and with a `scene`.
- A game that crashes (its exit code and last lines in the console), audio and sprites in a played game, a project whose `run`
  task has no Java toolchain (should fall back to Gradle), and Windows paths.
- A game played with the configuration cache on, the version 5 and 6 upgrades' diffs in the dialog, and a Log Block's line
  reaching the console after the version 5 upgrade.
- Hearing the audio: music and sounds from Blocks, music pausing and resuming with the window's focus, a non-looping music played
  again, going between Scenes with the same and different music, and web audio with `Res.getUri` paths. The audio Blocks' file
  menus with no sounds or music in the project (empty, with no hint how to add files).
- A ball bouncing exactly at the bounds' edge, going to another Scene with different (or no) settings, and Playing the scratch
  Breakout with GoldBrick placed.

**Builds**
- Opening a Scene, or pressing Play, while the build at opening still runs (should wait for it, not build twice), and a project
  whose build fails on open (the problems in the console right away, the first Scene showing the failure without building again).
- A change made while the compiler warm-up runs (should wait for it), and a project whose warm-up fails (nothing should show).
- Incremental common code: Reload taking a full build, and a broken Kotlin Block's failed build followed by a clean one.
- Quitting with a change made less than 100 ms before (it should be written).

**The Scene editor**
- Swapping classes: a change made during that build (should build once more after it), closing the Scene, opening another one,
  or Reload during it, and a Scene of many Actors (whether the old one stays smooth while the new one loads).
- A sprite type's preview; a sprite and a Scene's music still found from the class folders; a Scene kept open (outdated) while a
  type change builds.
- A Scene swapped in while Creator is in the background (should no longer stay empty), and what drawing every frame there costs.
- After an inspector edit (a replaced Actor), placing and removing Actors (`ScenePatch`), and a Scene changed by git while open.
- Clicking Actors, selecting right after placing, removing, and undoing (the same Actor back, selected), a click after a drag, and
  an old Scene file gaining IDs on its first edit.
- The inspector's Property fields and color, the Scene settings (adding them from the hint, music, camera start, which shouldn't
  move the editor's camera), scale and size (whether the pivot moving to the middle surprises anyone), and that typing a number
  is one undo step.
- A hand-written Actor type in a Scene (drawn, selectable, movable), an Actor without a `BoxBody`, a Scene whose Actors use a
  Manager, a paused game, zoom to fit with and without Scene settings, the middle-button pan, scroll pan on a mouse wheel,
  dropping onto a zoomed-out view, the version notice, and the outdated notice after a type change followed by Reload.

**The type editor and the sidebar**
- Dragging a Block by its handle up past the top edge (auto-scroll upwards), a Block or Event flashing after undoing its removal,
  typing into a just-added Block (the field should keep the focus), and a palette drag adding its Block twice.
- Deselecting by clicking the selected type or Scene again, or empty space (a Kotlin file row counts as empty space).
- Parents: the inherited Properties and Events, replacing an inherited Event (it starts with runParentEvent), the parent menu
  leaving out the type's own descendants, deleting a parent with children (rejected in the console), and a child of another
  shape colliding.
- Folders: renaming a Scene folder that holds the open Scene, and dragging a Manager type into a folder.
- Sprites: a non-square or translucent image, a `.webp` or `.jpg` (its size isn't read when ImageIO can't), deleting the image
  while a type uses it (the game should build and draw nothing), undo and redo of choosing a sprite, the sprite menu's hint with
  no images, and a game window opened before the template upgrade (an older `Game.kt` has no `KubrikoWiring`, so
  `kubriko.get<SpriteManager>()` would throw).
- A rename following Kotlin code from the UI. Adding a Property named like another Manager type's field (`lives` beside a `Lives`
  type) moves that field to `livesManager`, and Kotlin code isn't updated for that.
- The go to a Scene card's Scene menu, the "score of Game" Property menu in an Actor's Block, the New Scene field, and undo and redo.

**Project files**
- The dependency sync and the version 4 upgrade on the real Breakout copy (on `0.7.5-creator` and template version 3): opening it
  should raise it to Creator's version, offer the upgrade, and after it drop nothing it uses. Then remove the last collision Event,
  watch `plugin-collision` leave the build, and undo it.
- A project whose sources import a plugin by hand (say physics) keeping it; a dependency added by hand before any code uses it is
  removed on the next regeneration, which the user may not expect.

**AI connections**
- `set_scene_settings`, and the Manager type, text, and upgrade tools from a real AI session ("stop the paddle on key release and
  show a score"), watching the activity view pace a burst of calls.
- Connect for the plain "Claude Code" row in the Claude app, Claude Desktop from a real chat (writing its config needs the user),
  and Connect and Disconnect against a real config.
- Codex (out of quota until Nov 21, 2026), Cursor, Gemini CLI, Windows, and fish aliases with a real fish.

## Working with the live app

- **Guard every synthetic click and keystroke.** The user works alongside, and Android Studio covers the same screen area. Click only
  when the topmost window under the point belongs to Creator's PID (`CGWindowListCopyWindowInfo` in a Swift script), and press keys
  only while the target app is frontmost. Prefer editing `.kubriko/*.json` on disk, which Creator reloads.
- Test on a fresh scratch copy of `~/Projects/breakout` (it has the user's uncommitted work), opened with `:creator:run --args=<dir>`;
  the copy gets `0.8.1-creator` and the Maven Local block on its own. Leave the real one, which shows the upgrade notice, to the user.
- Never recompile `:creator` while `:creator:run` is up, and restart it with the publish tasks after `tools/creator-session` changes.
- The integration test: `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=0.8.1-creator ./gradlew :creator:test --tests
  "*ProjectTemplateIntegrationTest*" --rerun` (after a `:creator:run` or `publishToMavenLocal` with `-Pcreator.localKubriko=true`,
  since 0.8.1 isn't released).
