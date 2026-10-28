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
project. Nothing a user builds is capped in number. This file covers the data and the editor; compile times are `10-fast-iteration.md`,
reuse is `11-reuse-and-traits.md`, and finding things is `12-resource-organization.md`.

Scene Actors have stable IDs and Scene files hold one Actor per line (`../CLAUDE.md` → Commands → Scenes). Left from that: an
ordering command (`move_actor_order`, to the front or back of the list, which decides the order Actors are added in), a user-facing
name per Actor once Identifiable is a Trait (`11-reuse-and-traits.md`; the ID stays internal and stable when the name changes), and
showing a git merge conflict in a Scene by Actor (`03-git.md`).

**Order:** incremental Scene editing first, then the rest.

## Incremental Scene editing

Every edit rewrites the whole file and creates every Actor again (`SceneView.load` parses each state twice), which is fine at 30
Actors and not at 3,000.

- **In memory first:** the open Scene's entries live in a `SceneDocumentState` (by ID) that commands change; the file is written in
  the background, coalesced (at most a few times a second, and on close), like Block types are. Commands on Scenes that aren't open keep
  writing directly.
- **Patch the view:** `SceneView` replaces only the changed Actor (create it from its new state, swap it in `ActorManager`), and adds
  or removes only the affected ones. A full reload stays for files changed outside Creator.
- **Picking and drawing outlines** go through a uniform grid of Actor bounds kept up to date with the patches, so a click doesn't walk
  every Actor; the inspector, an Actor list (below), and the outline only read the selected and visible ones.
- **Undo** keeps the changed Actor's state, not the file.
- A Scene list panel (the Actors of the open Scene, by type and name, filterable, virtualized) for Scenes too dense to click into.

## Code generation

- **Only what changed:** saving a type regenerates that type and the types that depend on it (Blocks naming it, collision Events with
  it, children with `11-reuse-and-traits.md`), plus the registry and wiring when the set of types or the plugins changed. A dependency
  index (type → types that refer to it) is kept from the definitions, so it costs nothing per keystroke.
- Scene changes regenerate only when a Scene's music set changes.
- Writes stay coalesced per type while the user types (they are already ordered; they should also be debounced).

## AI tools at scale

- `read_scene` returns a page (an offset and a count), or the Actors of a type, or those in a rectangle, with a total; a summary
  mode (counts per type, the bounds) for orientation. Same for `list_actor_types` with folders (`12-resource-organization.md`).
- Batch commands for what the AI does in bulk: placing a grid of Actors (`place_actors` with a list), and editing many at once, each
  one undo step.

## No caps

Every user-made thing is unlimited, and the editor stays usable with many of them (virtualized lists, search, folders):

- **Timers:** today 1 to 8 per Actor (`TIMER_NUMBERS`), copied from GameMaker's alarms. Named Timers instead, as many as needed
  (a Timer is a pair of fields only once a Block or Event uses it), with old numbered ones kept by name ("Timer 1").
- **Keys:** key Events and held-key conditions offer 10 keys (`KeyChoice`). Every key Compose knows, chosen from a searchable list
  or by pressing it, with the common ones first.
- **Events, Blocks, Properties, texts, Scenes, types:** no limits exist, but their lists aren't virtualized and have no search;
  `LazyColumn` for the Event, Block, and Property lists, and the sidebar search covers all of them.
- **Screen texts:** nine places (`ScreenPosition`); with UI elements (`02-blocks.md` → Game UI) any layout.

## Files on disk

- `ProjectWatcher` fingerprints every watched file each second; for large projects, fingerprint per folder first (a folder's
  modification time and entry count), and only list folders that changed.
- `ResourceScanner` walks the whole tree on every refresh; it should rescan only the folders the watcher reports.
