<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Make BlockKind the descriptor of every per-kind Block fact, replace every `else` over Blocks with an exhaustive `when`, and pin the generated code of the every-Block project

**Challenged:** amended — `creator/CLAUDE.md` has no Conventions section since ae027ca3e/b7d0dad55, so the "how to add a Block" sentence goes into the actors package guide; `EveryBlockTypes` keeps the moved values as named members, since T05 (amended) and G09 use them; the golden files are compared without line-break differences (the Windows job runs `:creator:test` on a CRLF checkout); the pinning lands in the plan's one commit.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** ACT-2 (part 1)  ·  **Depends on:** none (lands before G03)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/BlockKind.kt`, `actors/BlockReferences.kt`, `actors/BlockCodeGenerator.kt`, `actors/ActorTypeFeatures.kt`, `context/ProjectRelations.kt`, `ui/actorTypes/PropertyBlockParameters.kt`, `actors/CLAUDE.md` (one bullet), `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/actors/BlockKindTest.kt`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/actors/EveryBlockTypes.kt` (new, moved out of the integration test), `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/actors/GeneratedCodeGoldenTest.kt` (new), `creator/src/test/resources/generated/*.kt.txt` (new), `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/template/ProjectTemplateIntegrationTest.kt`

(All `actors/`, `context/`, `ui/` paths are under `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/`.)

## Problem

What one Block is lives in many places, and several of them fall back silently, so a new Block compiles and quietly "doesn't move, needs no ActorManager, refers to nothing":

- `actors/ActorTypeFeatures.kt`, private companion:
  ```kotlin
  val BlockDefinition.movesTheActor
      get() = when (this) {
          is BlockDefinition.SetVelocity, ..., is BlockDefinition.Kotlin -> true
          is BlockDefinition.If -> condition is ConditionDefinition.Velocity
          else -> false
      }
  fun BlockDefinition.needsActorManager(knownActorTypeNames: Collection<String>) = when (this) {
      BlockDefinition.RemoveSelf, BlockDefinition.RemoveOther, is BlockDefinition.Kotlin -> true
      is BlockDefinition.AddActor -> actorType in knownActorTypeNames
      is BlockDefinition.MoveTowardsNearest -> actorType in knownActorTypeNames
      else -> false
  }
  ```
- `actors/BlockReferences.kt`: `propertyReference` (`else -> null`), `actorTypeReference` (`else -> null`), `withActorTypeRenamed` (`else -> this`), `timerName` (`else -> null`), `withTimerRenamed` (`else -> this`), `withPropertyReference` (`else -> this`), `audioFile` (`else -> null`).
- `context/ProjectRelations.kt` `addRelationsOf`: `else -> block.propertyReference?...`; `ui/actorTypes/PropertyBlockParameters.kt`: `else -> Unit`.
- Where a Block may go is split between a flag and identity checks in `BlockKind.isAvailableIn`:
  ```kotlin
  fun isAvailableIn(eventType: EventType, kind: BlockTypeKind) = when {
      needsActor && kind != BlockTypeKind.ACTOR -> false
      this == BOUNCE_OFF_EDGE -> eventType == EventType.EDGE_TOUCHED
      this == BOUNCE_OFF_OTHER || this == REMOVE_OTHER -> eventType == EventType.COLLISION
      else -> true
  }
  ```
  while `BlockCodeGenerator.actorBlockLines` repeats the same Event checks per Block to write a comment instead (`BlockDefinition.BounceOffEdge -> if (eventType == EventType.EDGE_TOUCHED) ... else listOf("// Bounce off the edge only works in the Edge touched Event.")`, and the same for `BounceOffOther` and `RemoveOther`).

Plans 05 and 06 (`creator/plan/`) add many Blocks; each one today means finding these places by hand.

## Fix

No generated output changes in this plan; step 1 proves it.

1. **Pin the output first** (write the golden files before steps 2 and 3, all in this plan's one commit, as EXECUTION.md asks). Move the every-Block definitions of `ProjectTemplateIntegrationTest` (`properties`, `everyBlock`, `everyManagerBlock`, `everyEvent`, `everyBlockInEveryEvent`, `family`, `everyManager`, `hud`, and what they use) verbatim into `internal object EveryBlockTypes` in the test source set (`actors/EveryBlockTypes.kt`), keeping each as a named member with its KDoc (`everyBlock`, `everyManagerBlock`, `everyEvent(...)`, `everyBlockInEveryEvent`, `family`, `everyManager`, `hud`) plus `val all: List<BlockTypeDefinition> = listOf(everyBlockInEveryEvent, everyManager, hud) + family`, and have the integration test use it (`all` where it lists the four today, in `everyTemplateGeneratesAProjectThatCompilesForEveryTarget` and `theIntegrationProjectUsesEveryBlockAndCondition`). T05 reads `EveryBlockTypes.all`; G05 and G09 edit `everyEvent`'s comment and one Kotlin Block here. Add `GeneratedCodeGoldenTest.theEveryBlockProjectGeneratesTheCodeItDidBefore`: a temporary folder with `.kubriko/project.json` (`{ "packageName": "com.example.integration" }`, as `BlockTypeStoreTest.optIn` writes it) and the empty `drawable/every-block.png` the sprite needs, saves `EveryBlockTypes.all` through `BlockTypeStore.open(root)`, calls `regenerateAll()`, and compares every file under `store.generatedPath` with `src/test/resources/generated/<file name>.txt` (and the set of names with the set of resources, so a new or vanished file fails too), each resource's text with `\r\n` replaced by `\n` (the Windows job of `tests.yml` checks text out as CRLF; the generators write `\n`). Normalizing in the comparison rather than adding a `.gitattributes` works however the file is checked out, needs no file T05's `formats/.gitattributes` (amended T05) would also create, and is what `BlockTypeStore` itself does when it compares generated files. Find the resources as `File("src/test/resources/generated")`, asserting `isDirectory` first (the update mode has to write there, so the class loader won't do). With `KUBRIKO_CREATOR_UPDATE_GOLDEN=true` it writes the resources instead and fails with "Golden files written; review the diff", so a deliberate change (G04, G05, G09) is one command plus a readable diff in review. Write the resources with the code at the start of this plan.
2. **Descriptor.** `BlockKind`'s constructor takes every fact that is constant per kind, so a new entry can't leave one out:
   ```kotlin
   internal enum class BlockKind(
       val title: StringResource,
       val createDefault: () -> BlockDefinition,
       val needsActor: Boolean,
       val onlyIn: EventType? = null,
       val misplacedComment: String? = null,
   )
   ```
   `needsActor` loses its default (every entry states it). `onlyIn` is `EDGE_TOUCHED` for `BOUNCE_OFF_EDGE` and `COLLISION` for `BOUNCE_OFF_OTHER` and `REMOVE_OTHER`, each with today's comment text verbatim (`"// Bounce off the edge only works in the Edge touched Event."`, `"// Bounce off it only works in a collision Event."`, `"// Remove the other Actor only works in a collision Event."`); an `init { require((onlyIn == null) == (misplacedComment == null)) }` keeps the two together. `isAvailableIn` becomes `!(needsActor && kind != BlockTypeKind.ACTOR) && (onlyIn == null || onlyIn == eventType)`. `BlockCodeGenerator.blockLines` checks, in this order, `needsActor` (today's `"// This Block only works in an Actor type."`) and then `onlyIn` (its `misplacedComment`) before `actorBlockLines`, and the three Blocks' branches in `actorBlockLines` lose their `if (eventType == ...)`. Facts that depend on the Block's values (whether `AddActor` names a known type, whether an `If` checks velocity) are not descriptor fields: G03 derives them from the emitted code.
3. **Exhaustive `when`s.** Rewrite every function listed in Problem as `when (this)` / `when (block)` with every `BlockDefinition` subclass named and no `else` (group the "nothing" cases on one or two lines, as `ActorTypeValidation.areNumbersFinite` already does). `withActorTypeRenamed` and `withTimerRenamed` become `when (this) { is AddActor -> if (actorType == oldName) copy(...) else this; ...; <the rest> -> this }`. In `ProjectRelations.addRelationsOf`, list the property-referencing Blocks explicitly and the rest as `-> Unit`. Leave `ActorTypeFeatures.movesTheActor` and `needsActorManager` exhaustive too (G03 deletes them).
4. **Docs.** In the actors package guide (`creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/CLAUDE.md`), one new bullet: a new Block is its `BlockDefinition` subclass, its `BlockKind` entry (every per-kind fact), and its emission in `BlockCodeGenerator`; the compiler then names every remaining place (references, validation, notation, the editor row, its icon), and the integration test's `theIntegrationProjectUsesEveryBlockAndCondition` names `EveryBlockTypes`, which must use it. (G03 adds that the emission states its needs.)

## Tests

- `GeneratedCodeGoldenTest.theEveryBlockProjectGeneratesTheCodeItDidBefore` (above) passes before and after steps 2 and 3, unchanged.
- `BlockKindTest.theBlocksThatOnlyWorkInOneEventSayWhereAndWhy`: for each entry, `onlyIn` and `misplacedComment` are the literal pairs above, and null for every other entry.
- `BlockKindTest.aBlockOutsideItsEventBecomesItsComment`: `BlockCodeGenerator.blockLines(BlockDefinition.RemoveOther, EventType.EVERY_FRAME, BlockScope("Ball"))` is `listOf("// Remove the other Actor only works in a collision Event.")`, and in a Manager scope it is `listOf("// This Block only works in an Actor type.")` (the actor check comes first).
- The existing `BlockKindTest`, `ActorTypeCodeGeneratorTest`, `ActorFamilyCodeGeneratorTest`, `ManagerTypeCodeGeneratorTest`, `BlockTypeEditsTest`, and context tests pass with no expected text changed. If any expected text has to change, the plan is wrong: stop and find the behaviour difference.
