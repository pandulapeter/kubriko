<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Later

Work with no place in the roadmap yet: each waits for demand, a measurement, or a decision, named with it.

- **Code editor (minor, like GameMaker's code action):** Kotlin syntax highlighting, search, tabs, compiler errors inline at their line,
  and "Open in IntelliJ". A language server (`kotlin-lsp`) only if there's demand.
- **Resources changed while playing:** decide what a Scene, sprite, or sound changed during a play session does (Hot Reload's copy
  holds the resources too, so a reload may not bring them), then make it do that.
- **A shared collection of custom Blocks** beyond the user's own My Blocks (`03-reuse-and-traits.md`): bundled with Creator, or
  published by others, once enough users ask; decide first how a shared one's Kotlin body is trusted.
- **More of the sprite editor:** layers per frame (flattened into the sheet when saved), color replacement, and outline and
  shading helpers, once people draw enough in it to ask.
- **"Don't ask again"** on confirmations, listed in Creator settings so they can be turned back on, once a confirmation is asked often
  enough to need it.

## When the benchmarks show it

Each waits for the benchmarks (`benchmark/CreatorBenchmarkTest`, and `CreatorBuildBenchmarkTest` for builds) to show it's needed:

- Regenerating only the types a change affects (a dependency index: type → types that refer to it), if regenerating every type's
  text stays expensive off the main thread.
- Splitting shared generated files (`GeneratedActorTypes`, `KubrikoWiring`) per type or per family, if the build benchmark shows a
  warm compile after one type change growing with the number of types.
- A per-type instance list in generated code, if moveTowardsNearest over `allActors` shows up in frame times.
- Fingerprinting per folder in `ProjectWatcher` (a folder's modification time and entry count), if a watcher pass over thousands of
  files costs too much.
