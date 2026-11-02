<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Split the `actors` package into the Block model (`actors`), code generation (`actors.codegen`), and storage (`actors.store`), with the generated file names in the model, and hold the three to that order in the layering test

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** ACT-17 (folded into the modularization by the verifier)  ·  **Depends on:** M04, and the G lane (G02–G09) having landed
**Files:**
- `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/**` (moves into `actors/codegen/` and `actors/store/`), new `actors/GeneratedNames.kt`
- every importer of a moved declaration in main and test sources (about 45 files: `grep -rl "import com.pandulapeter.kubrikoCreator.actors\.\(<moved names>\)" creator/src`), and the same-package users inside `actors/` that now need imports
- `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/actors/**` (the tests of moved files move with them)
- `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/PackageLayeringTest.kt`
- `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/CLAUDE.md` (file names it cites), `creator/CLAUDE.md` (any `actors/<File>.kt` path it cites)

Paths are below `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/`.

## Problem

`actors/` holds 55 files (5,516 lines at `ecf76faf6`) that mix three roles (ACT-17): the model (`*Definition`, `*Kind`, naming,
`BlockScope`, families), code generation (`*CodeGenerator`, `ActorClassCode`, `ActorTypeFeatures`, `GeneratedFile`,
`PluginManagers`), and storage (`BlockTypeStore`, `BlockTypeRepository`, `LoadedBlockTypes`, `RegeneratedCode`,
`ManagerFieldMigration`). They become one `core` module (M07), where nothing keeps the model from depending on the generators.
Today it does in exactly one place, found by classifying every file and checking same-package references:

```kotlin
// actors/BlockTypeNaming.kt:32-36 (the names a type may not take)
        ActorTypeCodeGenerator.REGISTRY_NAME,
        ManagerTypeCodeGenerator.WIRING_NAME,
        ScenesCodeGenerator.SCENES_NAME,
        SceneSettingsCodeGenerator.SETTINGS_NAME,
        AudioCodeGenerator.AUDIO_NAME,
```

with the constants declared in the generators (`ActorTypeCodeGenerator.kt:44 const val REGISTRY_NAME = "GeneratedActorTypes"`,
`AudioCodeGenerator.kt:21`, `ManagerTypeCodeGenerator.kt:35`, `ScenesCodeGenerator.kt:18`, `SceneSettingsCodeGenerator.kt:21`).
`BlockScope` is used by the model (`BlockTypeEdits`, `PropertyNaming`, `ManagerFieldMigration`), so it is model, not codegen.

## Fix

Drop this plan if the user decides at execution that one `actors` package inside `core` is enough; it is the low-value part of the split.

1. **Classify at the HEAD you start from** (the G lane reworked code generation: G02's Block descriptor, G03's structured
   `BlockCode`, G04's single class emitter, G05's per-Event functions). The rule, applied to every file of `actors/`:
   - `actors.codegen`: every `*CodeGenerator`, and every file used only by generators and the store (at `ecf76faf6`:
     `ActorClassCode`, `ActorTypeFeatures`, `GeneratedFile`, `PluginManagers`; after G03/G04, the `BlockCode` types and the class
     emitter).
   - `actors.store`: `BlockTypeStore`, `BlockTypeRepository`, `LoadedBlockTypes`, `RegeneratedCode`, `ManagerFieldMigration`, and
     whatever K01/K11 added for type files (a write queue, a `.bak` reader) if it lives in `actors`.
   - `actors` (model): everything else, including `BlockScope`, the naming objects, `KotlinIdentifiers`, `KotlinUi`, `ColorHex`
     (if M04 hasn't moved it), `ComposeKeys`, `BlockReferences`, G02's descriptor.
   Check with a throwaway script (same-package references aren't imports, so search each file's body for the other files'
   top-level names) that no model file uses a codegen or store name, and no codegen file uses a store name. At `ecf76faf6` the
   only such use is `BlockTypeNaming`'s, fixed in step 2.
2. **`actors/GeneratedNames.kt`**:
   ```kotlin
   /**
    * The names of the files Kubriko Creator generates besides one per type, which no type may take.
    */
   internal object GeneratedNames {
       const val ACTOR_TYPES = "GeneratedActorTypes"
       const val WIRING = "KubrikoWiring"
       const val SCENES = "GeneratedScenes"
       const val SCENE_SETTINGS = "SceneSettings"
       const val AUDIO = "GeneratedAudio"
   }
   ```
   with the literals copied from the generators at execution. The generators' constants become references to these (or are
   removed and their users point here; pick one, don't keep both as literals). `BlockTypeNaming` uses `GeneratedNames`.
3. **Move** with `git mv`, change each moved file's `package` line, add the imports that same-package use now needs, rewrite
   importers (`sed` on `import com.pandulapeter.kubrikoCreator.actors.<Name>$`), move each moved file's test to the matching test
   folder and package, and run `./gradlew spotlessApply` (ktlint orders imports).
4. **Layering.** `PackageLayeringTest` gains a rule inside the core layer: `actors` may not import `actors.codegen` or
   `actors.store`, and `actors.codegen` may not import `actors.store` (literal list `actorsSublayers = listOf("actors",
   "actors.codegen", "actors.store")`, same mechanism as the layers). Add `actors.codegen` and `actors.store` to the core layer's
   packages.
5. **Docs.** `actors/CLAUDE.md` (the package guide) keeps its place and gets one sentence: "`actors` is the model,
   `actors.codegen` turns it into Kotlin, `actors.store` reads and writes `.kubriko/`; the model never imports the other two."
   Update any `actors/<File>.kt` path it or `creator/CLAUDE.md` cites for a moved file (`check_doc_names.py --staged`).

No behaviour changes; nothing is renamed but the five constants.

## Tests

- `PackageLayeringTest.theActorsModelDoesNotImportCodegenOrStore` (new): fails if, say, `actors/BlockTypeNaming.kt` imports
  `actors.codegen.ActorTypeCodeGenerator`; check it bites by temporarily adding such an import.
- `BlockTypeNamingTest` (existing or new): `validate("KubrikoWiring", emptyList())` and `validate("GeneratedActorTypes", emptyList())`
  are rejected as reserved (literals), so the moved names still reserve the files.
- The whole `./gradlew :creator:allTests` with the same test count as before.
