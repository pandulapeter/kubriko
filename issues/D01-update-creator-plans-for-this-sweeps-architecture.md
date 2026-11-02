<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Bring Kubriko Creator's plan files in line with what this sweep built: remove the costs it fixed, record the ones it left, and tie Functions, Traits, new Blocks, and sprite writes to the new code generation and write pipeline

**Challenged:** amended — named the plans and types the conditions hang on as they are planned: G02 makes `BlockKind` the descriptor (there is no `BlockDescriptor`), K01c is the name-to-file index, and G04 brings imports per file.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** ARCH-10 (per-save rescan), INF-10b/ARCH-4, CMD-9, CMD-10, ACT-2/ACT-3/ACT-4 (via G02–G05), INF-2/ARCH-3 (via K01), UI-3/UI-11/UI-14 (via H plans)  ·  **Depends on:** every other plan of this sweep (T, S, A, G, K, H, P, M lanes)
**Files:**
- `creator/plan/01-large-projects.md`
- `creator/plan/05-reuse-and-traits.md`
- `creator/plan/06-blocks.md`
- `creator/plan/07-sprite-editor.md`

## Problem

`creator/plan/` holds only future work (its README: "once something is implemented, it is deleted from these files"). This sweep's
plans changed what several items cost or rest on, and some fixed items the plan files still list:

- `01-large-projects.md` → Known costs lists, among others:
  ```markdown
  - **Wide families regenerate quadratically.** Generating one member of a family (`ActorFamilyCodeGenerator`) builds the family's
    `ActorTypeFeatures` and a `BlockScope` (each O(types)) again, …
  - **The Block editor rebuilds the project's scope per keystroke.** `WorkspaceScreen` remembers `BlockScope.of(…)` …
  - **Choosers list everything.** `MenuButton` composes every option of a `DropdownMenu`, … Direction:
    `KeyChoiceButton`'s popup (a search field over a `LazyColumn`), generalized, with folder paths.
  - **Sidebar rows** are rebuilt inside the lazy list's builder on every AI step's flash (`FolderTreeSection`), …
  ```
  H05 built `SearchableChoiceButton` (UI-11, decided "build"), H02's `WorkspaceUiState` presenter may have moved the per-keystroke
  scope off the screen (UI-3, UI-5), UI-14 targeted the sidebar rows, and G04 replaced the family emitter. Not listed although still
  open after the sweep, unless the K plans fixed them: `BlockTypeStore.save` walking every type file per save (ARCH-10:
  `definitionFiles(definition.kind)` at `actors/BlockTypeStore.kt:104`, a full `walkTopDown` at `:271`, so a rename that saves
  many types costs O(n·m)), validation doing blocking reads on the main thread (CMD-9, K06), every Scene edit regenerating all
  code (CMD-10, K09), and a second resource refresh per command (INF-10b, K10). (Pods and DerivedData are already in
  `ProjectTree.ignoredDirectoryNames`: nothing to add for them.)
- `05-reuse-and-traits.md` describes Functions ("Generated as Kotlin functions on the class (and the family's base class)") and
  Traits without saying what they now build on: G02's Block descriptor, G03's structured `BlockCode` (which replaced
  text-search feature detection), G04's single Actor class emitter, and G05's per-Event private
  functions. Its Parent Actor types list still has "**Imports per file** rather than per family", which G04 took on (its emitter writes imports per file, from G03's `BlockCode`).
- `06-blocks.md` says what each new Block lands with (command, AI tool, generator tests, an integration case) but not that a Block
  is now one descriptor entry (G02) whose exhaustive `when`s the compiler walks through.
- `07-sprite-editor.md` → How it is built: "Written in the background and in order, like Block types, and reloaded when the PNG
  changes on disk", which since K01 means more: failed writes retried and reported, Build and Play warning about unsaved types,
  Commit and Discard refused until on disk (decisions.md).

## Fix

Read each file at the HEAD you start from (F9 changed the README and HANDOVER; other sessions may have edited these files).
For every conditional item, check the code and say in the commit's report which branch you took.

1. **`01-large-projects.md` → Known costs.**
   - Delete "Choosers list everything" if `SearchableChoiceButton` (H05) is what the Actor type, Scene, audio, and parent choices use
     (`grep -rn "SearchableChoiceButton" creator/src/main`); if some still use `MenuButton`, shorten the item to those.
   - Delete "Sidebar rows" if `FolderTreeSection` remembers its rows per group and carries the item in the row; otherwise keep it.
   - Delete or shorten "The Block editor rebuilds the project's scope per keystroke" by what H02/UI-3 moved (if the scope is now
     derived off the main thread or remembered on a structural projection, delete it).
   - Delete or shorten "Wide families regenerate quadratically" by what G04 changed (if features are computed once per family per
     regeneration and `ActorTypeFamily` has a children map, delete it; keep the benchmark direction if `BenchmarkProject` still has
     only families of ten).
   - Add, only if K01c was dropped or left out the index (it plans exactly this one): "**Every type save lists every type file.** `BlockTypeStore.save` walks the
     kind's whole folder to delete a moved type's old file, and `delete` walks both kinds, so a rename that saves the types that
     refer to it costs O(types × files). Direction: a name-to-file index built by `load()`, kept by `save` and `delete`, and rebuilt
     by `reloadFromDisk`; measure with `CreatorBenchmarkTest` first."
   - Add one item each for CMD-9 ("validation reads Scenes on the main thread"), CMD-10 ("every Scene edit regenerates all code and
     refreshes resources"), and INF-10b ("two resource refreshes per command") only if K06, K09, or K10 were dropped or left that
     part; phrase each as a cost with a direction, like the others.
2. **`05-reuse-and-traits.md`.**
   - Under **Order**, add: "Both build on how code is generated: a Block is one descriptor (`BlockKind`), its code a
     structured `BlockCode` that names its imports and features, one emitter writes every Actor class (standalone or in a family),
     and each Event is a private function. A function is then one more member the emitter writes, and a Trait one more feature
     `BlockCode` reports." Check the names (`BlockKind`, `BlockCode`, `ActorClassEmitter`) with `grep` at
     execution.
   - Under Functions, note G05's constraint: a Kotlin Block's `return` returns from its Event's function, so "Run function" and
     function results must not change what an existing Kotlin Block's `return` does.
   - Delete "**Imports per file** rather than per family" if the generated files import per file (G04).
3. **`06-blocks.md`**, intro: "Each addition below lands with its command and AI tool (`02-ai-assistant.md`), generator tests, and a
   case in the integration test's every-Block types (an Actor type and two Manager types)" gains ", and is one entry of the Block
   descriptor (`BlockKind`, G02), whose exhaustive `when`s then show every place it needs".
4. **`07-sprite-editor.md`** → How it is built: replace "Written in the background and in order, like Block types," with "Written in
   the background and in order with the Block types' failure rules (`../CLAUDE.md` → Threads, writes, and failures): a failed write
   is retried and reported, Build and Play warn naming what isn't saved, and Commit and Discard wait until it is on disk;". Point at
   wherever `creator/CLAUDE.md` now states K01's rules (grep for "Commit" and "unsaved").
5. Nothing that this sweep finished stays in any plan file, and no history goes in ("fixed in the fifth sweep" is not written
   anywhere). Keep the Oxford comma in every list. Run `python3 .github/scripts/check_doc_names.py --staged`.

## Tests

None (documentation only). `check_doc_names.py --staged` passes for every name the edits quote.
