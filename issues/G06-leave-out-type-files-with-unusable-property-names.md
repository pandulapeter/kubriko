<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Leave a type file out as unreadable when one of its own Properties has a name the generated code can't use

**Challenged:** amended — creator/CLAUDE.md has no `workspace` row since ae027ca3e: the reasons a definition file is left out are in Project files → Block type files; no `UnreadableBlockTypeFileEntryTest` exists; this plan, not G01, adds the reserved-name sentence to the Format versions paragraph, since it is what makes such files unreadable.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** ACT-7  ·  **Depends on:** none (lane order: after G05)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/BlockTypeStore.kt`, `actors/LoadedBlockTypes.kt` (`BlockTypeFileProblem`), `workspace/UnreadableBlockTypeFileEntry.kt`, `creator/src/main/composeResources/values/strings.xml`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/actors/BlockTypeStoreTest.kt`, `creator/CLAUDE.md` (Project files: the Block type files paragraph, and one sentence in Format versions)

## Problem

`BlockTypeStore.load` checks only the type's name:

```kotlin
when {
    definition.name != file.nameWithoutExtension -> BlockTypeFileProblem.NameMismatch(definition.name)
    !BlockTypeNaming.isWellFormed(definition.name) -> BlockTypeFileProblem.InvalidName(definition.name)
    BlockTypeNaming.isReserved(definition.name, kind) -> BlockTypeFileProblem.ReservedName(definition.name)
    else -> null
}?.let { unreadable += UnreadableBlockTypeFile(path, it) } ?: run { readable += path to definition }
```

Property names are validated only by the editor (`ui/actorTypes/PropertyList.kt`) and the commands (`commands/actorTypes/ActorTypeValidation.validateProperty` → `PropertyNaming.validate`). A file edited by hand, by git, or by an AI agent writing JSON directly, with a Property called `class`, `body`, `velocity`, `2x`, or two called `score` and `Score`, is loaded, and the generators write it verbatim (`appendLine("    var ${property.name}: ${PropertyKind.of(property).kotlinType} = state.${property.name}")` in `ActorTypeCodeGenerator`, `var ${property.name} by ...` in `ManagerTypeCodeGenerator`): the game stops compiling with an error in generated code the user didn't write, or, for `velocity`/`body`, shadows the generated members.

## Fix

- New problem in `LoadedBlockTypes.kt`:
  ```kotlin
  /**
   * The type has a Property called [name], which can't be a Property's name ([PropertyNaming]).
   */
  data class InvalidPropertyName(val name: String) : BlockTypeFileProblem
  ```
- In `load`, after the reserved-name check: `definition.properties.firstNotNullOfOrNull { property -> property.name.takeIf { PropertyNaming.validate(it, definition.properties.map { other -> other.name } - it) != null } }?.let(BlockTypeFileProblem::InvalidPropertyName)`. Validate each type's **own** `properties` only: a child repeating an inherited Property of the same name and kind is allowed (it changes the initial value), and a different kind is already ignored by `ActorTypeFamily.propertiesOf`, so cross-file checks stay with the commands. `minus` removes one occurrence, so a name listed twice counts as taken. Same rule for Actor and Manager types.
- `UnreadableBlockTypeFileEntry`: `is BlockTypeFileProblem.InvalidPropertyName -> ConsoleEntry.Status(Res.string.status_type_file_property_name_invalid, isError = true, arguments = listOf(path, problem.name))`.
- `strings.xml`, next to the other `status_type_file_*` entries: `<string name="status_type_file_property_name_invalid">%1$s has a Property named %2$s, which can't be a Property's name (it has to start with a lowercase letter, use only letters and numbers, not be a name Kotlin or the generated code uses, and differ from the type's other Properties by more than case), so that type is left out and no code is generated until it is renamed.</string>`
- `creator/CLAUDE.md` → Project files → Block type files: the sentence "A definition file that can't be read, holds a type named other than the file, or a name another file holds too (ignoring case) is left out" also lists "has a Property whose name can't be one (`PropertyNaming`)".
- `creator/CLAUDE.md` → Project files → Format versions (G01's policy, which has landed by now), one sentence: a name added to `PropertyNaming`'s reserved names makes existing files with that Property unreadable, so such an addition needs a format raise whose migration renames the Property (with `BlockTypeEdits`' rename, which follows Blocks, placeholders, and Kotlin).

## Tests

In `BlockTypeStoreTest` (real temp folder, as the neighbouring `aTypeFileWithAnInvalidOrReservedNameIsLeftOut` does):
- `aTypeWithAnInvalidPropertyNameIsUnreadable`: `.kubriko/actors/Ball.json` with a Property `2x` → `UnreadableBlockTypeFile(".kubriko/actors/Ball.json", BlockTypeFileProblem.InvalidPropertyName("2x"))`, and `Ball` isn't in `types`.
- `aTypeWithAReservedPropertyNameIsUnreadable`: a Manager `Game` with a Property `velocity` → `InvalidPropertyName("velocity")`.
- `aTypeWithTwoPropertiesDifferingOnlyInCaseIsUnreadable`: `score` and `Score` → `InvalidPropertyName("score")` (the first one found).
- `aChildRepeatingItsParentsPropertyLoads`: `Enemy` with `hp` and `Bat(parent = "Enemy")` with `hp` both load.
- A save over the unreadable file still throws (`save` refuses files in `unreadableFiles`), covered by the existing test for the other problems; extend it with this problem if it lists them.
- The console text: no test renders `UnreadableBlockTypeFileEntry` at HEAD; the exhaustive `when` there makes the compiler require the new branch.
