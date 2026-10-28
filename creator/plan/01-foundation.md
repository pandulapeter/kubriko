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

Done (`../CLAUDE.md` → Conventions specific to Creator), except:

- `Logger.log` copies the log list once instead of three times (`tools/logger`, a library change without API changes): in the
  working tree, waiting for the user's review.
- A box mask keeps the size it was built with; when Blocks that change an Actor's size or scale arrive (`06-blocks.md`), the
  generated code builds a new mask when the size changes.
- Dynamic Actors are always active now; a per-type "Sleep while far off screen" choice for big levels comes with
  `03-large-projects.md`.

## 2. Workspace lifecycle and opening

Done (`../CLAUDE.md` → Processes): opening reads files only, each workspace has its own scope and writes pending changes before
closing, Scene classes stay loaded between Scenes, and builds and Scene openings are timed in the console. Seen working on
the scratch Breakout: the workspace showing within seconds of `:creator:run`, the Scene open in 5.3 s cold, then in 0.0 s
after selecting an Actor type and going back.

## 3. Benchmark

Exists (`benchmark/CreatorBenchmarkTest`, `KUBRIKO_CREATOR_BENCHMARK=true`): 200 Actor types in families of ten, 20 Manager types,
and Scenes of 50, 1,000, and 5,000 Actors, timed without Gradle. Baseline on 2026-10-28 (M-series Mac), main thread / until
written:

| Step | Main thread | Until written |
|---|---|---|
| Open the project | 104 ms | 1,716 ms |
| Add a Block to an Actor type | 8 ms | 1,332 ms |
| 10 quick changes to one type | 16 ms | 12,696 ms |
| Move an Actor (50 / 1,000 / 5,000 Actors) | 2 / 4 / 16 ms | 1,260 ms each |
| Undo that move | 15 ms | 1,270 ms |
| One watcher pass | - | 9 ms |

Every change costs over a second of whole-project work, even a move in a Scene (which regenerates for the Scenes' music), and
quick changes queue one each. Targets: a single change under 100 ms until written, quick changes coalesced into one write,
and main-thread work under 16 ms at 5,000 Actors.

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
