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

After step 4 so far (2026-10-29, warm JVM):

| Step | Main thread | Until written |
|---|---|---|
| Open the project | 0.3 ms | 322 ms |
| Add a Block to an Actor type | 1.3 ms | 140 ms |
| 10 quick changes to one type | 16 ms | 262 ms |
| Move an Actor (50 / 1,000 / 5,000 Actors) | 1 / 6 / 16 ms | 26 / 36 / 45 ms |
| Undo that move | 9 ms | 41 ms |
| One watcher pass | - | 14 ms |

The biggest cut was `ActorTypeFamily`, which rebuilt a map of every type for every type (quadratic, and made several times per
generated type): 1.1 s to 85 ms for 200 types. Not measured yet: the Scene editor's own work after a change (step 5).

## 4. One project in memory

Done (`../CLAUDE.md`): type changes are written in batches 100 ms after the last one, regenerated from the types in memory
with a cache of the generated files (a run with the same types and Scene music does nothing), Scenes are kept parsed while
their files are unchanged, the build file sync reads only changed sources, the watcher ignores Creator's own writes, and builds,
git commands, and closing wait for pending writes. Left:

- A dependency index (regenerate only the types a change affects) if 85 ms for 200 types grows with bigger projects; measured
  first (`03-large-projects.md` → Measured, not assumed).

## 5. Scene editing in place

`SceneView` applies a `ScenePatch` after each change instead of creating every Actor again (`../CLAUDE.md` → Scene
editing); seen working on the scratch Breakout (dragging the Ball, then undo). The editor gets the Scene's parsed entries
from `SceneStore` instead of its text, read in the background while the project builds (which also fills the cache before
the first command), and picking goes through `SceneBoundsGrid`, updated in place when Actors only move or change. Left:

- A changed Actor is created again, so it moves after the others of its drawing order until the Scene is opened again; Actor
  types could take a new state in place (an `Editable` reloading itself) if that shows.

## 6. The editor's own UI

Done: the console collects its own entries (`WorkspaceConsole`), so a line the game logs no longer recomposes the workspace;
the workspace remembers its folder trees, Scene names, and Actor type list; and `folderTreeRows` groups by folder once instead
of scanning every folder and item for each folder. Events, Blocks, Properties (the whole left column of the type editor), the
palette, and git changes are lazy lists; the Block list's drop gap comes from the rows it has laid out (`LaidOutRows`), and it
scrolls while a Block is held near its top or bottom edge, so every gap can be reached. The undo history and the git state are
collected by the toolbar and the git panel, so a keystroke in a field no longer recomposes the workspace. Seen working on the
scratch Breakout with a 61-Block Event and 40 Properties: the lists drawn as before, and a palette Block held at the Block
list's bottom edge scrolling it from Line 1 to Line 24 and landing in the gap under the pointer (index 25 on disk). Left:

- `aiSteps` stays in the workspace: following the AI and the highlights need its latest step, which changes with every change
  of the list, so moving the list into `AiActivityBar` saves nothing.
- The console's list is copied on every line (up to 2,000 entries, microseconds); a ring buffer if a busy game shows it.
