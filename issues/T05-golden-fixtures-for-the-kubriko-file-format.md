<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Pin the `.kubriko` file format with golden fixture projects, one frozen folder per format version, that decode to known values and that the current format re-encodes byte for byte

**Challenged:** amended — G02 already moves the every-Block values into `actors/EveryBlockTypes` (`EveryBlockTypes.all`), so this plan uses that instead of moving them again and leaves the integration test alone; the `.gitattributes` goes in the fixture folder (`R/.gitattributes`), not the repository root; the version-folder check counts directories only (G01's `persisted-shape-*.txt` share `R`); a raise by lane G is a stop; the step runs between phases 1 and 2, as EXECUTION.md orders it.

**Kind:** tests  ·  **Severity:** high  ·  **Platforms:** all
**Findings:** TST-1, ARCH-8 (decision: one type, guarded by fixtures)  ·  **Depends on:** T01, G01 (the format policy and its `persisted-shape-<N>.txt`), and every lane G plan that changes the persisted shape (at least the Kotlin Block imports plan, ACT-18; re-check the G list at execution). Cross-lane: run as the serial step on `main` after lane G has merged, before the phase 2 worktrees are cut (EXECUTION.md, phase 1→2). A fixture written before a G plan adds a field would fail as soon as that plan merges, because `encodeDefaults = true` writes the new field.
**Files:** (T = `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator`, R = `creator/src/test/resources/formats`)
- New: `R/v<N>/…` for the current version N and `R/v0/…` (see Fix), `T/project/FormatFixturesTest.kt`
- Read, not changed: `T/actors/EveryBlockTypes.kt` (G02 moved the every-Block values there from `ProjectTemplateIntegrationTest`; G09 gave one Kotlin Block an import)
- New: `R/.gitattributes` (one line; see Tests)
- Docs: `creator/CLAUDE.md` (Format versions paragraph, one sentence; G01 writes the policy itself)

## Problem

Nothing pins the field names, discriminators, or defaults of Kubriko Creator's own files. `T/actors/BlockTypeStoreTest.definitionsAreStoredAsReadableJsonThatSurvivesUnknownFields` checks three substrings (`"name": "Ball"`, `"type": "bounceOffEdge"`, `"color": "#6060AA"`) plus a round trip. `M/actors` declares about 120 distinct `@SerialName`s (M = `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator`), and nearly none is checked. Renaming a `@SerialName` or changing a default therefore round-trips fine in every test and breaks every saved project. There is no `creator/src/test/resources` folder at HEAD.

The store writes through:

```kotlin
private val json = Json {
    prettyPrint = true
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}
```

so a file's exact text follows from the definition, and a changed default changes it. ARCH-8's decision keeps `ActorTypeDefinition` as the disk format, the model, and the AI tool schema at once (`@SerialName` and `@Description` on the same fields). The fixtures are what make a schema change visible as a format change. G01 adds a descriptor-shape check (`PersistedShapeTest`, `R/persisted-shape-<N>.txt`). The shape check sees new nullable fields that are never encoded. These fixtures see names, values, and defaults as written. Neither plan edits the other's files.

Format versions at HEAD: `ProjectFormat.MIGRATIONS = listOf<FormatMigration>(ManagerFieldMigration)`, so `CURRENT_VERSION` is 1. Version 0 differs only in Manager field names inside Kotlin Blocks and Kotlin UI (`ManagerFieldMigration` → `withManagerFieldsSuffixed`). No lane G plan raises it, but re-read `MIGRATIONS`: if `CURRENT_VERSION` is above 1 when this runs, every version in between needs a folder of its own, which this plan doesn't write, so stop and report.

## Fix

1. **Shared values.** Use G02's `internal object EveryBlockTypes` (`T/actors/EveryBlockTypes.kt`, `val all: List<BlockTypeDefinition>`), which lane G moved out of `ProjectTemplateIntegrationTest` and which `theIntegrationProjectUsesEveryBlockAndCondition` keeps using every Block, Condition, Event, and Property kind. Don't move or copy it: `FormatFixturesTest` sits in `project`, so it imports `com.pandulapeter.kubrikoCreator.actors.EveryBlockTypes`. If lane G left the values in the integration test after all, stop and report rather than moving them here.
2. **Fixture project for the current version N** (`R/v<N>/`): a minimal opted-in project, laid out the way a project is on disk:
   - `.kubriko/project.json`: `{ "packageName": "com.example.formats", "templateVersion": <current ProjectUpgrader version>, "formatVersion": N, "minimumCreatorVersion": "0.0.0-fixture" }`, in the same format `ProjectFormat.recordVersion` writes.
   - `.kubriko/actors/…` and `.kubriko/managers/…`: the types of `EveryBlockTypes.all`, at least one of them in a folder (`family/…`).
   - `.kubriko/scenes.json`: two descriptions.
   - `src/commonMain/composeResources/files/scenes/level.json`: a Scene with one block-built Actor (`SceneDocument.blockBuiltState`) and one entry with a non-empty `id`.

   Write the type files once by saving `EveryBlockTypes` through `BlockTypeStore.save` into a temporary opted-in project, then copy them into `R/v<N>/`. Then read every file by hand against the `@SerialName`s in `M/actors`, since this is the moment the format is frozen: every field appears, and nothing that should be absent does. From then on the files are never regenerated, only hand-edited when a version is raised (step 4).
3. **Fixture project for version 0** (`R/v0/`): the same project as it was before `ManagerFieldMigration`. That means `"formatVersion"` absent from `project.json`, and Kotlin Blocks and Kotlin UI naming the other Managers by their old field names. Read `ManagerFieldMigration`, `withManagerFieldsSuffixed`, and `ManagerFieldMigrationTest` for the old names. Only the files that differ need to be hand-edited from the v<N> copy.
4. **Fixture policy.** Put this in the KDoc of `FormatFixturesTest` and as one sentence in `creator/CLAUDE.md` → Format versions, next to G01's policy:
   - A released version's folder is never edited.
   - Raising the format version (G01: any new persisted field, Block, Event, Condition, Property kind, enum value, or changed default) copies `R/v<N>/` to `R/v<N+1>/` and edits the copy to show the addition.
   - A test that reads a fixture of an older version reads it through `ProjectFormat.prepare()`.
   - "Released" means: present on `main` when a Creator release was cut. Until releases are tagged, treat every folder on `main` as released. Version 0 is kept because `ManagerFieldMigration` still reads it.
5. **`FormatFixturesTest`** copies a fixture folder into a `testDirectory()` before each use, so the resources are never written to.
   - `theCurrentFormatDecodesToTheKnownTypes`: copy `v<CURRENT_VERSION>`, call `BlockTypeStore.open(dir)!!.load()`. `unreadableFiles` is empty, and `types == (EveryBlockTypes, with the folders the fixture uses).sortedBy { it.name }`. `ProjectFormat(dir, "test").prepare()` is `Writable` and changes no file (compare every file's bytes before and after).
   - `theCurrentFormatIsWrittenByteForByte`: for each loaded type, `save` it into a fresh opted-in `testDirectory()`. The written file's text equals the fixture's text exactly, line breaks included. Compare with `assertEquals` on strings, so the diff shows.
   - `projectSettingsKeepTheirFieldNames`: decode `R/v<N>/.kubriko/project.json` with `CreatorProjectSettings.serializer()` (through a `Json { ignoreUnknownKeys = true }`, as `BlockTypeStore.open` does) to the literal `CreatorProjectSettings(packageName = "com.example.formats", templateVersion = …, formatVersion = N, minimumCreatorVersion = "0.0.0-fixture")`.
   - `sceneDescriptionsAndScenesKeepTheirShape`: `SceneDescriptions(dir, OwnWrites()).all()` equals the literal map. `SceneDocument.parse(levelText)` equals the literal `List<SceneActorEntry>`, and `SceneDocument.format(parsed) == levelText`.
   - `everyOlderFormatIsBroughtUpToTheCurrentOne`: for every `R/v<k>/` with `k < CURRENT_VERSION`, copy it, then `ProjectFormat(dir, "test").prepare()` is `Writable`. After that, `load()` has no unreadable files and equals the current known types, minus any field added after `k`, which takes its default; with v0 → v1 only, that means exactly equal. The recorded `formatVersion` is `CURRENT_VERSION`.
   - `thereIsAFixtureForEveryFormatVersion`: the names of the directories under `R` are exactly `v0..v<CURRENT_VERSION>` (files there, such as G01's `persisted-shape-<N>.txt` and the `.gitattributes`, are not counted). This fails when G01's raise lands without a new fixture folder, with a message pointing to step 4.

   Find the resources with `File("src/test/resources/formats")`, asserting `isDirectory` first, as `CommandRegistryTest` does after TST-13, or through the classloader (`javaClass.getResource("/formats")`). Prefer the classloader: Gradle puts `src/test/resources` on the test classpath, so it works whatever the working directory.
6. Keep `BlockTypeStoreTest.definitionsAreStoredAsReadableJsonThatSurvivesUnknownFields` as it is. Unknown fields surviving a read is a separate guarantee.

## Tests

The tests above. Each must fail without its fixture, and when a pinned name changes: rename one `@SerialName` locally, for example `BlockDefinition.BounceOffEdge`'s, and `theCurrentFormatDecodesToTheKnownTypes` and `theCurrentFormatIsWrittenByteForByte` fail. Change one default, and `theCurrentFormatIsWrittenByteForByte` fails. Revert both. Line breaks: the store writes LF, and `actions/checkout` on `windows-latest` checks text out as CRLF (`core.autocrlf` is true there). So create `R/.gitattributes` (`creator/src/test/resources/formats/.gitattributes`) with one line, `* text eol=lf`, which applies to every file under that folder and nothing else (there is no `.gitattributes` anywhere in the repository at HEAD, and none at its root is wanted). It is a test fixture's, not a project's: the "No .gitattributes in projects" decision is about generated projects. Being inside the folder, it moves with it when M07 moves the fixtures. It also covers G01's `persisted-shape-<N>.txt` in the same folder. Gradle's default excludes leave it out of the test resources, and the version-folder check ignores files anyway. Compare without normalizing anything.
