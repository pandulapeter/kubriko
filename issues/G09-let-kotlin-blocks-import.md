<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Let a Kotlin Block start with import lines that become imports of the file it is generated into

**Decision (user, 2026-11-02):** raise the format version. G09 adds a `FormatMigration` step that changes no file, so an older Creator opens a project using it read-only instead of generating code that doesn't compile.

**Challenged:** amended — the rename path keeps the import header's lines verbatim instead of rebuilding them from `splitImports` (which drops the `import ` prefix and blank lines); creator/CLAUDE.md no longer describes Kotlin Blocks or Kotlin UI's imports, so the docs are the actors package guide's bullet and the Renames line; the Kotlin Block given the import is named, so it reaches the Manager types too.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** ACT-18 (decided: yes)  ·  **Depends on:** G05 (uses G03's `BlockCode.imports` and G04's per-file imports)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/KotlinUi.kt` (shared header split), `actors/BlockCodeGenerator.kt` (Kotlin branch), `actors/BlockTypeEdits.kt` (`withKotlinCode`), `actors/BlockTypeEditsTest.kt` (test), `actors/BlockDefinition.kt` (Kotlin Block `@Description`), `context/BlockNotation.kt` (only if it should show the statements alone; otherwise untouched), `actors/CLAUDE.md` (the "Generated Kotlin compiles" bullet), tests `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/actors/KotlinUiTest.kt`, `actors/ActorTypeCodeGeneratorTest.kt`, `actors/ManagerTypeCodeGeneratorTest.kt`, `actors/ActorFamilyCodeGeneratorTest.kt`, `actors/BlockCodeTest.kt`, `template/ProjectTemplateIntegrationTest.kt` (via `actors/EveryBlockTypes.kt`), `creator/src/test/resources/generated/*.kt.txt` (regenerated), `creator/CLAUDE.md` (Commands → Renames follow everywhere), `project/ProjectFormat.kt` (or wherever `MIGRATIONS` lives), its new migration file, `project/ProjectFormatTest.kt`

## Problem

A Kotlin Block's code goes verbatim into a `run { }` (`BlockCodeGenerator.actorBlockLines`):

```kotlin
is BlockDefinition.Kotlin -> block.code.lines().map { it.trimEnd() }.dropWhile { it.isEmpty() }.dropLastWhile { it.isEmpty() }
    .takeIf { it.isNotEmpty() }
    ?.let { lines -> listOf("run {") + lines.map { if (it.isEmpty()) it else INDENT + it } + "}" }
    .orEmpty()
```

so an `import kotlin.math.sin` at its start ends up inside a function, which doesn't compile, and users (and the AI) have to fully qualify every name the generated file doesn't happen to import. A Manager's Kotlin UI already supports it: `KotlinUi.of` splits leading `import` lines off and `ManagerTypeCodeGenerator` adds `kotlinUi?.imports` to the file's imports. Decision (`decisions.md`): "Imports in Kotlin Blocks: yes."

## Fix

- Extract `KotlinUi.of`'s header split into `KotlinUi.splitImports(code: String): Pair<List<String>, List<String>>` (imports without the `import ` prefix, then the remaining lines, trailing blank lines dropped), used by `KotlinUi.of` unchanged in behaviour.
- The Kotlin Block's emission uses it: the statements go into `run { }` as now (an import-only Block generates nothing, like an empty one), and the imports go into the Block's `BlockCode.imports`, so they reach exactly the file whose class holds that Event's code (G04's per-file imports: a child's Kotlin Block import lands in the child's file only). An import that duplicates one the file has anyway collapses in the import set. An alias (`import a.B as C`) is kept verbatim after `import `.
- Renames: `KotlinIdentifiers.withRenamed` runs on the whole code string (`BlockTypeEdits.withKotlinCode`) and knows no grammar, so the first segment of an import (`kotlin` in `import kotlin.math.sin`, which has no receiver) would be renamed with a Property or Manager of that name. Have `withKotlinCode` rename only the statements, for Kotlin Blocks and Kotlin UI alike (Kotlin UI has the same exposure today): find where the import header ends (the same line index `splitImports` uses; expose it, for example `KotlinUi.headerLineCount(code)`), apply the change to the text after it, and put the header's original lines back in front unchanged, blank lines and `import ` prefixes included, so a rename never rewrites the header's text. `ManagerFieldMigration` goes through `withKotlinCode` too (`BlockTypeEdits.kt` line ~158), and skipping imports is right there as well.
- Validation: none at the command level (Kotlin code isn't checked anywhere); a wrong import fails the build with the compiler's message, as wrong code does now.
- `@Description` of `BlockDefinition.Kotlin`: add "Leading import lines become imports of the generated file." (it reaches the AI through `get_block_catalog`). In the actors package guide's "Generated Kotlin compiles" bullet, next to "Kotlin Blocks get their own `run { }` scope": "and their leading import lines, like Kotlin UI's (`KotlinUi`), become the file's imports". In `creator/CLAUDE.md` → Commands → Renames follow everywhere, after "a local variable of the same name is renamed too": "; the import lines a Kotlin Block or Kotlin UI starts with are left alone".
- `EveryBlockTypes` (G02): turn its Kotlin Block `"if (isShielded) score += 1f"` into `"import kotlin.math.sin\n\nif (isShielded) score += 1f + sin(0f)"` (it has no `velocity`, so `everyManagerBlock` keeps it), so the integration test compiles an import from a Block in an Actor type, a family member, and a Manager type.

**Format version (decided):** add one `FormatMigration` to `ProjectFormat.MIGRATIONS` whose `apply` changes nothing on disk (its KDoc says why: Kotlin Blocks' leading import lines mean something an older Creator would compile wrongly), with `minimumCreatorVersion` set to the version that ships G09. Projects are raised on opening as with every migration. Update the `PersistedShapeTest`/G01 expectations only if they record the current version.

## Tests

Expected text that changes: the golden files whose types hold the Kotlin Block changed in `EveryBlockTypes` (on purpose: one import and one statement). No existing assert changes.

- `KotlinUiTest.splitImportsTakesOnlyTheLeadingImportLines`: `"import a.B\n\nimport c.D as E\nval x = 1\nimport f.G"` → `listOf("a.B", "c.D as E")` and statements `listOf("val x = 1", "import f.G")` (a later import stays a statement, and the compiler reports it).
- `ActorTypeCodeGeneratorTest.aKotlinBlocksImportsBecomeTheFilesImports`: every-frame `Kotlin("import kotlin.math.sin\nvelocity = SceneOffset(sin(1f).sceneUnit, velocity.y)")`: the file has `import kotlin.math.sin\n` among its sorted imports, and the `run {` block holds only the statement. Fails before (the import is inside `run`).
- `ManagerTypeCodeGeneratorTest.aKotlinBlocksImportsBecomeTheFilesImports`: the same in a Manager's every-frame Event.
- `ActorFamilyCodeGeneratorTest.aChildsKotlinBlockImportGoesIntoTheChildsFileOnly`: `Bat`'s Kotlin Block imports `kotlin.math.sin`; `generate(bat)` has it, `generate(enemy)` doesn't.
- `BlockCodeTest.anImportOnlyKotlinBlockGeneratesNoStatements`: `Kotlin("import kotlin.math.sin")` → `lines` empty, `imports == setOf("kotlin.math.sin")`.
- `BlockTypeEditsTest.renamingAPropertyLeavesKotlinImportsAlone`: a type with a Property `kotlin` and a Kotlin Block `"import kotlin.math.sin\nkotlin = sin(kotlin)"`, renamed to `angle`, gives `"import kotlin.math.sin\nangle = sin(angle)"`; the same for a Manager's Kotlin UI. Fails before (the import's `kotlin` is renamed too).
- `ProjectFormatTest`: a project at the previous version is raised to the new one with no file changed; a project at the new version opened with the previous `MIGRATIONS` count is read-only.
