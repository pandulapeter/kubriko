<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Large projects

Creator has to hold up for games the size GameMaker made: hundreds of Actor types, Scenes with thousands of Actors, many people on one
project. Nothing a user builds is capped in number. Reuse is `04-reuse-and-traits.md`, and finding things is `03-resource-organization.md`.

**Order:** the remaining caps first (they change file formats), then the Scene list panel, then the AI tools.

## Scene Actors

- An ordering command (`move_actor_order`, to the front or back of the list, which decides the order Actors are added in).
- A user-facing name per Actor once Identifiable is a Trait (`04-reuse-and-traits.md`; the ID stays internal and stable when the name
  changes).
- Showing a git merge conflict in a Scene by Actor (`07-git.md`).

## Scene list panel

The Actors of the open Scene, by type and name, filterable and virtualized, for Scenes too dense to click into. Selecting
one selects it in the Scene editor and centers the camera on it.

## AI tools at scale

- `read_scene` returns a page (an offset and a count), or the Actors of a type, or those in a rectangle, with a total; a summary
  mode (counts per type, the bounds) for orientation. Same for `list_actor_types` with folders (`03-resource-organization.md`).
- Batch commands for what the AI does in bulk: placing a grid of Actors (`place_actors` with a list), and editing many at once, each
  one undo step.
- The project digest, compact Block notation, and the rest of keeping a large game in an agent's context cheaply are in
  `02-ai-assistant.md` → Context for large games.

## No caps

Every user-made thing is unlimited, and the editor stays usable with many of them (virtualized lists, search, folders):

- **Keys:** key Events and held-key conditions offer 10 keys (`KeyChoice`). Every key Compose knows, chosen from a searchable list
  or by pressing it, with the common ones first.
- **Screen texts:** nine places (`ScreenPosition`); with UI elements (`05-blocks.md` → Game UI) any layout.

## Measured, not assumed

Each of these waits for the benchmarks (`benchmark/CreatorBenchmarkTest`, and `CreatorBuildBenchmarkTest` for builds) to show
it's needed:

- Regenerating only the types a change affects (a dependency index: type → types that refer to it), if regenerating every
  type's text stays expensive off the main thread.
- Splitting shared generated files (`GeneratedActorTypes`, `KubrikoWiring`) per type or per family, if the build benchmark
  shows a warm compile after one type change growing with the number of types.
- A per-type instance list in generated code, if moveTowardsNearest over `allActors` shows up in frame times.
- Fingerprinting per folder in `ProjectWatcher` (a folder's modification time and entry count), if a watcher pass over
  thousands of files costs too much.
- A ring buffer for the console, whose list is copied on every line (up to 2,000 entries), if a busy game shows it.
