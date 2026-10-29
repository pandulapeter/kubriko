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
project. Nothing a user builds is capped in number. Reuse is `05-reuse-and-traits.md`, and finding things is `04-resource-organization.md`.

**Order:** screen texts wait for UI elements, and the rest of Scene Actors for the plans named there.

## Scene Actors

- A user-facing name per Actor once Identifiable is a Trait (`05-reuse-and-traits.md`; the ID stays internal and stable when the name
  changes).
- Showing a git merge conflict in a Scene by Actor (`08-git.md`).

## AI tools at scale

- The project digest, compact Block notation, and the rest of keeping a large game in an agent's context cheaply are in
  `02-ai-assistant.md` → Context for large games.
- Paging `list_manager_types` and `list_scenes` like `list_actor_types`, once a project has enough of them to need it.

## No caps

Every user-made thing is unlimited, and the editor stays usable with many of them (virtualized lists, search, folders):

- **Screen texts:** nine places (`ScreenPosition`) until UI elements (`06-blocks.md` → Game UI) allow any layout.

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
