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

**Order:** Known costs need nothing else and can go first; screen texts wait for UI elements, and the rest of Scene Actors for the
plans named there.

## Scene Actors

- A user-facing name per Actor once Identifiable is a Trait (`05-reuse-and-traits.md`; the ID stays internal and stable when the name
  changes).
- Showing a git merge conflict in a Scene by Actor (`08-git.md` → Conflicts).

## AI tools at scale

- The project digest, compact Block notation, and the rest of keeping a large game in an agent's context cheaply are in
  `02-ai-assistant.md` → Context for large games.
- Paging `list_manager_types` and `list_scenes` like `list_actor_types`, once a project has enough of them to need it.

## No caps

Every user-made thing is unlimited, and the editor stays usable with many of them (virtualized lists, search, folders):

- **Screen texts:** nine places (`ScreenPosition`) until UI elements (`06-blocks.md` → Game UI) allow any layout.

## Known costs

Found in review rather than measured, but each grows with the project and runs often enough to matter:

- **Wide families regenerate quadratically.** Generating one member of a family (`ActorFamilyCodeGenerator`) builds the family's
  `ActorTypeFeatures` and a `BlockScope` (each O(types)) again, and `ActorTypeFamily.childrenOf` scans every parent link per call
  (`descendantsOf` recursively), so 200 enemies under one parent cost about 200 × 200 scopes per regeneration, after every edit;
  `PluginManagers.of` also builds a second `ActorTypeFamily`. `BenchmarkProject` only has families of ten, so nothing catches it.
  Direction: a wide family in the benchmark, a children map in `ActorTypeFamily`, the features computed once per family per
  regeneration, and the family passed into `PluginManagers.of`.
- **The Block editor rebuilds the project's scope per keystroke.** `WorkspaceScreen` remembers `BlockScope.of(…)` (which builds an
  `ActorTypeFamily` of every type), `Inheritance.of`, and the folder groups on the whole `blockTypes` list, which changes with every
  character typed into a Block, on the main thread. Direction: remember them on a structural projection (names, parents, Properties,
  Timers), or derive them in `BlockTypesController` off the main thread, and add a UI-latency case to `CreatorBenchmarkTest`.
- **Renaming rewrites Scenes per keystroke.** `EditProperty` renames a Property's values in every Scene (`changeSceneStates`) on each
  keystroke of the name field, so a type placed in 50 Scenes rewrites up to 50 files per character; and renaming a Scene folder calls
  `renameScene` per Scene, each walking the whole project and reading every `.kt` file (`SceneRenaming.kt`). Direction: rename in
  Scenes when the name is committed (or once per merged step), and scan hand-written files once per folder rename.
- **Reordering an Actor rebuilds the Scene.** `ScenePatch` gives up on any change of order (`move_actor_order`, undoing a removal,
  placing anywhere but at the end), so `SceneView` creates every Actor again: in a Scene of thousands, every bring-to-front and undo
  deserializes them all. Direction: keep the Actors before the first changed index and recreate only the rest.
- **A game that logs every frame floods the UI thread.** Session messages are collected on the main thread and each log line copies
  the console's list (`ConsoleLog.append`, up to 2,000 entries) and emits it; the console then stops following new lines once it is
  full, since it scrolls on a change of size (`ui/Console.kt`), and each new line reopens a collapsed console
  (`ui/WorkspaceConsole.kt`). Direction: append in batches into a ring buffer, follow the end only while the list is at its end, and
  open the console by itself once per play session.
- **Choosers list everything.** `MenuButton` composes every option of a `DropdownMenu`, which the Actor type, Scene, audio, and parent
  choices use, with no filter or folders: choosing a collision target among 300 types means scrolling them all. Direction:
  `KeyChoiceButton`'s popup (a search field over a `LazyColumn`), generalized, with folder paths.
- **Sidebar rows** are rebuilt inside the lazy list's builder on every AI step's flash (`FolderTreeSection`), and each composed row
  searches its group linearly. Direction: remember the rows per group and carry the item in the row.

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
