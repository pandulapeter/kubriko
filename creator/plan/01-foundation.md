<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Foundation

Everything else is built on this, so it comes first. Two halves: the games Creator generates have to be as careful as a
hand-written Kubriko game, and Creator itself has to keep one project in memory, change it in small steps, and write
only what changed, so that it holds up at GameMaker scale (hundreds of types, Scenes with thousands of Actors) with the
same care the engine gets.

What a 2026-10-28 review found, and why the order below: the command layer (validate, apply, exact inverses, one path
for the UI and the AI) is sound and stays. Below it, the files are the source of truth and every change recomputes the
whole project, often twice: a keystroke in a number field rereads every definition, every Scene, and every generated
file, regenerates everything, rereads every source file for the build file sync, and walks the tree; a second later the
watcher sees Creator's own write and does it all again. Scene commands parse the file several times on the main thread,
and every Scene change creates every Actor again. That layer is replaced here, not extended.

Each step lands with its tests, and steps 3 to 6 are measured against the benchmark of step 3.

## 1. Generated games

The generated code already avoids per-frame allocation in `update` and `draw`, looks Managers up once, and is `Dynamic`
only when needed. What it gets wrong:

- **Log Blocks in release builds.** `Logger.log` allocates a UUID string and copies the whole log list on every call, so
  a Log Block in an Every frame Event is the most expensive thing a generated game can do. Generated logging goes
  through one `GeneratedLogging.isEnabled` flag (`KubrikoWiring`), which play sessions turn on and a release build
  leaves off, with the message built only inside the check. `Logger.log` itself prepends without copying the list twice
  (an engine-side fix that helps every Kubriko game).
- **goToScene from a repeating Event never finishes.** Every call cancels the load in progress and reads the file again,
  so "if lives are 0, go to Game over" in an Every frame Event restarts the load each tick. A request for the Scene
  already loading is ignored, a decoded Scene's text is cached, and `settings` is `@Volatile` (written on the batch
  thread, read on the tick thread).
- **Tunneling after a hitch.** Movement uses the raw frame time; a 250 ms hitch moves a fast ball past a brick. The
  movement delta is clamped, as Wallbreaker's is.
- **An Actor that was removed keeps colliding.** Removal is asynchronous, so a Brick can be hit, and score, twice.
  Actor types get an `isRemoved` flag that the remove Blocks set, and collision handling and `update` skip removed
  Actors.
- **A child's collision Event that never runs.** `when (other)` branches are sorted by name, so `is MonsterBase` comes
  before `is Zombie` and catches it. Descendants come before their ancestors.
- **Actors that sleep off screen.** `ActorManager` puts far-away Actors to sleep, and Scenes can be larger than the
  screen, so off-screen Timers, spawners, and chasers stop. Generated types are `isAlwaysActive` when they have Timers,
  Every frame Events, or Blocks that act on other Actors; only plain movers may sleep.
- **moveTowardsNearest** keeps the best squared distance in a local instead of computing two square roots per
  comparison. (A per-type instance list waits for measurements in `03-large-projects.md`.)
- **Collision masks and edges that drift.** The mask's size and rotation are set only at construction, and edge checks
  assume a centered pivot. Both follow the body's bounding box, updated in `update` (Dynamic types) and when a Block
  changes the body.
- **Screen texts** keep their `TextStyle` and modifiers in fields instead of building them on every recomposition.
- **Repeated code:** the Bounce Block becomes one private function per type instead of a copy per collision branch.

## 2. Workspace lifecycle and opening

- **A scope per workspace:** everything a workspace launches runs in a child scope of the app's, cancelled by
  `close()`, so pending writes, git refreshes, and the watcher never outlive it. Closing flushes pending writes first.
- **Opening is cancellable without leaks:** an open that is cancelled closes what it created (the Gradle connection,
  the session server, the watcher).
- **Opening never waits for Gradle.** The desktop module's path is the only thing the `GradleProject` model is fetched
  for, and that model configures every task of every project. It is remembered in `.kubriko/local/workspace.json`
  (git-ignored) and checked in the background; a project opened for the first time uses `:` or the module the template
  names, and the check corrects it before the first build needs it. The Tooling API connection opens in the
  background too.
- **Scene classes stay loaded:** selecting an Actor type no longer closes the Scene editor's loaded classes; they are
  dropped only when they are outdated (a type or source changed) or the workspace closes, so going back to a Scene
  doesn't build again.
- **Timings in the console** (in debug builds, or with a setting): how long opening, each build, and each Scene load
  took, so slowness reported by users comes with numbers.

## 3. Benchmark

Before the data flow changes, a generated benchmark project (`KUBRIKO_CREATOR_BENCHMARK=true`, like the integration
test): 200 Actor types with Events and Blocks of every kind, 20 Manager types, and Scenes of 50, 1,000, and 5,000
Actors. It times opening (without Gradle), a type edit through the command layer until its code is written, a Scene
edit until it is written, an undo, and a watcher pass, and prints them. Every later step reports its numbers against
it; targets: a single edit under 16 ms of main-thread work and under 100 ms until written, at 5,000 Actors.

## 4. One project in memory

- **`ProjectModel`** holds the open project: block types (as `BlockTypesController` does today), Scenes as parsed
  documents (`SceneDocumentState`: entries in order and by ID, parsed once, on first use, off the main thread), the
  Scene index (names, folders, the music of each), and the resource index. Commands validate and apply against it
  without touching the disk; the stores become loaders and writers only.
- **Writes are coalesced and delayed:** a `ProjectWriter` keeps the latest content per file, writes after a short quiet
  period (and at most a few times a second while typing), in change order, and is flushed before every build, play,
  git operation, and close. A failed write is reported and retried with the next change.
- **Creator ignores its own writes:** the writer records what it wrote (path, length, modification time); the watcher
  reports changed paths instead of areas, and a path that matches Creator's own last write is dropped. A real outside
  change reloads only the files it names.
- **Code generation from memory:** generation reads the in-memory definitions and Scene music instead of reloading
  them, and compares against a cache of the generated files' text (read once when the project opens, then kept by the
  writer) instead of reading every file. Hand edits are still detected, by reading only the file about to be written.
  Regenerating every type's text stays as long as the benchmark shows it's cheap (it's pure CPU, off the main thread);
  a dependency index regenerates only affected types if it isn't.
- **Build file sync only when it can change:** the Kubriko libraries a project uses change only when the plugins the
  generated code needs change, or a hand-written source changes; each source file's imported libraries are cached by
  its length and modification time.
- **Text fields stay instant** (in-memory changes show before anything is written), and typing merges into one undo
  step, as today.

## 5. Scene editing in place

- Scene commands change the `SceneDocumentState` and publish what changed (`SceneChange`: Actors added, removed, or
  changed by ID, or the whole Scene for an outside change).
- `SceneView` applies changes: it creates only the changed Actor from its new state and swaps it in `ActorManager`, adds
  and removes only the affected ones, and reloads everything only for a whole-Scene change. Actors are created off the
  main thread where the project's classes allow it.
- **Picking** goes through a uniform grid of Actor bounds, updated with the changes, so a click doesn't walk every Actor
  or allocate their bounds.
- Undo keeps the changed Actor's state, never the file (already true for most Scene commands; checked for all).

## 6. The editor's own UI

- **Narrow state:** the workspace screen collects only what it lays out; the sidebar, editor, console, and Scene pane
  each collect their own flows, and derivations (folder trees, Scene name lists, filtered type lists) are remembered
  from their inputs, so a console line or a key press recomposes only what shows it.
- **The console** is a ring buffer with a version counter instead of a list copied on every line.
- **Lazy lists** for Events, Blocks, Properties, the palette, and git changes, which are scrolled `Column`s today.
- **AI tool calls** run their commands on the main thread only for the part that touches UI state; reads and writes
  happen in the background like the UI's.
