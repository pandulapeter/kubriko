<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->

**Decision (user, 2026-11-02):** the policy covers a new meaning for an existing field too (e.g. G09's Kotlin Block imports): any change an older Creator would read and then handle wrongly raises the format version. Write that into the policy text this plan adds.
# Raise the format version for every new persisted field, Block, Event, or enum value, and pin the persisted shape so a change without a raise fails a test

**Challenged:** amended — `creator/CLAUDE.md` → Format versions already says "(a new persisted field too)" at HEAD, so the quote and the replacement follow that text; the reserved-Property-name note moves to G06, which runs later in the lane and introduces that behaviour; the shape file is compared without line-break differences (the Windows job runs `:creator:test` on a CRLF checkout, and T05's `formats/.gitattributes` lands only after lane G).

**Kind:** process  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** ACT-6  ·  **Depends on:** none
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/project/ProjectFormat.kt` (KDoc), `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/project/FormatMigration.kt` (KDoc), `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/project/PersistedShapeTest.kt` (new), `creator/src/test/resources/formats/persisted-shape-1.txt` (new), `creator/CLAUDE.md` (Format versions paragraph)

## Problem

`actors/BlockTypeStore.kt` decodes type files leniently:

```kotlin
private val json = Json {
    prettyPrint = true
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}
```

and the policy (`ProjectFormat` KDoc: "raised only by a change an older Kubriko Creator can't read"; `creator/CLAUDE.md` → Project files → Format versions: "raised by any change an older Creator can't read (a new persisted field too)", which no test or migration list enforces) only raises `formatVersion` for unreadable changes. A newer Creator that adds an optional field (a Block parameter, an Event or type field) leaves the version alone; an older Creator reads the file without the field, and its next `save()` writes the definition back without it: silent data loss for anyone on two Creator versions (a team, or a downgrade). A new Block or enum value is safe per file (decoding fails, `BlockTypeFileProblem.Unparsable`), but then "nothing is regenerated or synced until it is fixed", so an older Creator stalls on the whole project instead of saying which version it needs.

Decision (`decisions.md`): "Type file format: raise formatVersion for any new persisted field; golden JSON fixtures pin field names."

## Fix

1. **Policy text.** Replace "raised only by a change an older Kubriko Creator can't read" in the `ProjectFormat` class KDoc and "raised by any change an older Creator can't read (a new persisted field too)" in `creator/CLAUDE.md` → Project files → Format versions with: the format version is raised by any change to what Creator persists in `.kubriko/` type files: a new or renamed field, a new Block, Event, Condition, or Property kind, a new enum value, or a changed default. A raise that needs no rewrite is a no-op `FormatMigration { }` in `ProjectFormat.MIGRATIONS` whose KDoc names what it added; an older Creator then opens the project read-only with "needs Creator X" (the existing `ProjectFormatStatus.ReadOnly` path) instead of dropping fields. `ignoreUnknownKeys` stays `true` (a newer Creator still reads older files). Add one line to the `FormatMigration` KDoc: "A raise that only adds to the format is an empty migration." (Alternative considered and not chosen: exempting new subclasses and enum values since they fail safe per file; rejected because the stall above is worse than read-only.) The sentence about adding a reserved Property name is G06's (it runs later in lane G and is what makes such a file unreadable); don't add it here.
2. **Check.** A new test, `PersistedShapeTest`, walks the `SerialDescriptor`s of `ActorTypeDefinition.serializer()` and `ManagerTypeDefinition.serializer()` recursively (classes, sealed subclasses through `SerialKind`/`PolymorphicKind.SEALED` element descriptors, enums, lists, nullable inner descriptors; a visited set by `serialName` stops recursion through `ConditionDefinition` and the like) and renders one sorted line per element: `<serialName>.<elementName>: <element serialName><? if nullable><= if optional>` for class elements, `<enum serialName> = <entry>` for enum entries, and `<sealed serialName> : <subclass serialName>` for sealed subclasses. The test compares the rendered text with `src/test/resources/formats/persisted-shape-<ProjectFormat.CURRENT_VERSION>.txt`, read through the class loader (`javaClass.getResource("/formats/persisted-shape-…")`) with `\r\n` replaced by `\n` first, since the Windows job of `tests.yml` runs `:creator:test` on a CRLF checkout. Normalizing in the comparison rather than adding a `.gitattributes` works however the file is checked out, needs no file T05's `formats/.gitattributes` (amended T05) would also create, and is what `BlockTypeStore` itself does when it compares generated files. On a mismatch it fails with: "The persisted format changed. Raise the format version (an empty FormatMigration in ProjectFormat.MIGRATIONS) and add persisted-shape-<next>.txt; never edit the file of a released version." Write `persisted-shape-1.txt` from the current descriptors (version 1 is `ManagerFieldMigration`; `ProjectFormat.CURRENT_VERSION` is 1 at HEAD — re-check). Defaults aren't visible in descriptors; changed defaults are covered by T05's golden JSON fixtures (encodeDefaults = true writes them), so say so in the test's KDoc.
   - Why a descriptor walk on top of T05's fixtures: `explicitNulls = false` means a new nullable field that defaults to null is never encoded, so an encode-equals-fixture test can't see it; the walk does.
   - `CreatorProjectSettings` is left out: `project.json` is rewritten through `JsonObject` merges (`ProjectFormat.recordVersion`, `ProjectUpgrader.recordVersion`), which keep unknown keys. Scenes are left out: actor states are game-defined.

## Tests

- `PersistedShapeTest.thePersistedShapeIsTheOneOfTheCurrentFormatVersion`: rendered text equals the resource file of `CURRENT_VERSION`. Fails without the fix (the file doesn't exist); prove the guard by temporarily adding a nullable field to `EventDefinition` locally and seeing it fail, then revert.
- `PersistedShapeTest.everyBlockEventConditionAndPropertyKindIsInTheShape`: the rendered text contains `BlockDefinition : <serialName>` for every `BlockKind.entries` default block's serial name (`BlockDefinition.serializer().descriptor` element names), and `EventType = <name>` for every `EventType` serial name, so the walk can't silently skip a branch.

Coordinate with lane T's T05 (golden `.kubriko` fixtures under `src/test/resources/formats/`): same folder, different file names; neither edits the other's files.
