<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Generate standalone Actor types and the base class of a family through one Actor class emitter, with imports per file

**Challenged:** amended — the comment-Block test used an every-frame Event, which is `Dynamic` by itself, so it couldn't pass; the `onAdded` import bug is fixed at HEAD (474baad22, 26d638baf: `ActorTypeFeatures.overridesOnAdded` with `isFamily`, and both tests), so that part and its tests are dropped and the emitter writes `onAdded` exactly when `overridesOnAdded` says, keeping the family rule (an added Event without Blocks still overrides it); the key-order test used keys whose list and name orders agree, so it couldn't fail; the root's needs are gathered per member scope, as `usesNumberText` is; the "Imports cover the whole family" and Conventions sentences no longer exist, so the doc change goes to the actors package guide; `creator/plan/05` is left to D01, which runs after lane M.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** ACT-3 (rest; F3 leftover: the family's bare `protected` Timer fields), plan 05's "Imports per file rather than per family"  ·  **Depends on:** G03
**Files:** `actors/ActorClassEmitter.kt` (new), `actors/ActorTypeCodeGenerator.kt`, `actors/ActorFamilyCodeGenerator.kt`, `actors/ActorClassCode.kt`, `actors/ActorTypeFeatures.kt`, `actors/BlockCodeGenerator.kt` (G03's `legacyNeedsOfComments` goes), tests `actors/ActorTypeCodeGeneratorTest.kt`, `actors/ActorFamilyCodeGeneratorTest.kt`, `actors/BlockCodeTest.kt`, `creator/src/test/resources/generated/*.kt.txt` (regenerated), `actors/CLAUDE.md` (the Parent Actor types bullet)

(`actors/` = `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/`; tests mirror it.)

## Problem

`ActorTypeCodeGenerator.generateActorType` (a standalone type: one final class) and `ActorFamilyCodeGenerator.appendRootBase` (the abstract `<Name>Base` of a family) each write their own copy of the traits list, `onAdded` with the lookups, `update` (Timer countdown, every-frame, movement, edges, mask), key dispatch, collision dispatch, `collidableTypes`, and the number text function. `ActorClassCode` shares the leaves (`appendVelocityField`, `appendTimerFields`, `appendTimerCountdown`, `appendEdgeHandling`, ...), but the two still drift:

- Timer fields: the standalone class uses `appendTimerFields(timers)` (KDoc'd `private var`), the family root writes its own bare copy:
  ```kotlin
  features.timers.forEach { timer ->
      appendLine("    protected var ${TimerNaming.remainingField(timer)} = 0L")
      appendLine("    protected var ${TimerNaming.periodField(timer)} = 0L")
  }
  ```
- Key order: standalone `keyEvents.groupBy { it.key!! }.toSortedMap()` (the key list's order, which `ActorTypeCodeGeneratorTest.keyEventsMakeTheActorKeyboardAwareAndShareOneBranchPerKey` pins), the family `features.keyEvents.mapNotNull { it.key }.distinct().sortedBy { it.composeKeyName }`.
- `onAdded`: both already decide it through `ActorTypeFeatures.overridesOnAdded` (lookups, or an added Event: with Blocks for a standalone type, any for a family, whose root calls the `onAddedToScene` hook that a child may override), and import `Kubriko` exactly then (fixed at HEAD by 474baad22 and 26d638baf, with `anAddedToSceneEventWithoutManagerLookupsStillImportsKubriko`, `anAddedToSceneEventWithoutBlocksOrManagerLookupsNeitherOverridesOnAddedNorImportsKubriko`, and `aFamilyWhoseOnlyEventIsAddedToTheSceneImportsKubrikoForItsOnAdded`). The emitter keeps that rule; nothing to fix here.
- Imports are the whole family's in every file of it (`ActorFamilyCodeGenerator.generate` uses one `features.imports` for every member), which `creator/plan/05-reuse-and-traits.md` lists as future work ("some generated files import what they don't use"). A child file that only overrides `onEveryFrame` imports `CollisionDetector`, `KClass`, `Random`, and the rest.

## Fix

1. **`ActorClassEmitter`** writes "the class that does the work" from `ActorTypeFeatures` and a role:
   ```kotlin
   internal enum class ActorClassRole { STANDALONE, FAMILY_ROOT }
   ```
   `STANDALONE`: `class Name private constructor(state: State) : Visible, ..., Editable<Name>`, `override val body = state.body`, its own collision mask built from `bodyCenter`, members `private`, followed by the draw function and `appendSavedState` in the same class. `FAMILY_ROOT`: `abstract class NameBase(final override val body: BoxBody, color, properties..., final override val collisionMask: <mask type>)`, members `protected`, Events as `protected open` hooks (the existing `Hooks`), followed in the same file by the concrete class `ActorFamilyCodeGenerator` already writes. Everything both have is written once, by one function each (traits, color and velocity fields, `isAlwaysActive`, `isRemoved`, Property fields, `bodyCenter` (standalone: when collidable; root: when collidable and dynamic, as now), `collidableTypes`, lookups, Timer fields, `onAdded`, `update`, key dispatch, collision dispatch, `bounceOff`, `asText`). How the Events' code gets into `update`, `onAdded`, `onKeyPressed`, and `onCollisionDetected` is the one difference, a parameter of the emitter: `INLINE` (standalone, today's output) or `HOOK_CALLS` (family root). G05 replaces `INLINE` with private functions, so keep that choice in one place. Keep each function under the size check's 120 lines and the file under 500 (`.github/scripts/check_sizes.py`).
   `ActorTypeCodeGenerator.generateActorType` keeps its signature and family dispatch, and calls the emitter for a standalone type; `ActorFamilyCodeGenerator.appendRootBase` becomes a call to the emitter. `appendBase` and `appendConcrete` (middle and leaf classes) stay in `ActorFamilyCodeGenerator`.
2. **One shape**, the decided option for each difference (this changes output on purpose):
   - Timer fields: `appendTimerFields(timers, visibility)` with its KDoc, `private` standalone, `protected` in a family.
   - Key order: the key list's order (`KeyChoice` order, as standalone today) in families too.
   - `onAdded`: written exactly when `features.overridesOnAdded` holds (the role sets `isFamily`: `FAMILY_ROOT` is `true`, `STANDALONE` `false`, as the two generators pass it at HEAD); the function that writes it adds `com.pandulapeter.kubriko.Kubriko` to the file's imports, and `helpers.extensions.get` only with lookups. No output change.
3. **Imports per file.** Each generated file imports what its own text uses: the class parts it writes (each emitter function adds its imports to the file's set as it writes, e.g. the edge handling adds `SceneUnit`, the collision mask its mask class, the key dispatch `Key` and `KeyboardInputAware`) and the `BlockCode.imports` and `needs` of the Events whose code is in that file (a child's overrides, the root's hooks). Replace `ActorTypeFeatures.imports` with that per-file set; `features` keeps deciding traits and members, which belong to the root. The root declares what every member's Events need (lookups, `Dynamic`, `asText`, `bounceOff`): take each member's `BlockCode` in the scope its code is generated in (as `usesNumberText` takes `scopeOf(member)` today) and declare the union on the root, so a child's Log of its own number Property still gets the root's `protected fun Float.asText()`.
4. **Comments need nothing.** Delete G03's `legacyNeedsOfComments`: a Block that generates a comment (outside its Event, an unknown or unchosen Actor type, an empty Kotlin Block, a key condition on an unknown key) adds no needs, so a broken Block no longer makes a type `Dynamic` or look up a Manager.
5. **Docs.** In the actors package guide, the Parent Actor types bullet: add that the topmost type's base class is written by the same `ActorClassEmitter` as a standalone type, and that each generated file imports only what its own classes use. Leave `creator/plan/05-reuse-and-traits.md` ("Imports per file rather than per family") to D01, which runs after lane M.

## Tests

Expected text that changes, and why: `GeneratedCodeGoldenTest`'s family files (`EveryParent`/`EveryChild`/... and anything colliding with them: KDoc'd `protected` Timer fields, key order, fewer imports); regenerate with `KUBRIKO_CREATOR_UPDATE_GOLDEN=true` and check that the diff shows only those three kinds of change. Standalone golden files should only change where a comment-Block carve-out mattered; if `EveryBlock.kt` changes otherwise, the emitter isn't equivalent: fix it. In `BlockCodeTest.everyBlockStatesWhatItsCodeNeeds`, the carve-out rows become empty needs. (`addedToSceneBlocksRunAfterTheManagersAreResolved` already has its Edge Event lookup at HEAD.)

New tests:
- `ActorFamilyCodeGeneratorTest.familyTimerFieldsAreDocumentedLikeAStandaloneTypes`: the root of a family with a Timer contains the KDoc'd `"    protected var timer1RemainingInMilliseconds = 0L\n    protected var timer1PeriodInMilliseconds = 0L\n"` block, `appendTimerFields`' text, with `protected`.
- `ActorFamilyCodeGeneratorTest.aFamilyHandlesKeysInTheOrderOfTheKeyList`: root with `KEY_PRESSED` Events for `ENTER` and `SPACE`: `Key.Spacebar` comes before `Key.Enter` (`KeyChoice.common` order; fails before, where `sortedBy { it.composeKeyName }` puts `Enter` first). Not `SPACE` and `LEFT`: `DirectionLeft` sorts first both ways.
- `ActorFamilyCodeGeneratorTest.aChildFileImportsOnlyWhatItsClassesUse`: with this test class's `enemy`/`bat`/`wall`, `generate(bat)` doesn't contain `import com.pandulapeter.kubriko.collision.CollisionDetector` or `import kotlin.reflect.KClass` (the root's dispatch needs them, `Bat.kt` doesn't), and still contains `com.pandulapeter.kubriko.types.SceneOffset` (its own `velocity = SceneOffset.Zero`).
- The existing `onAdded` tests listed in Problem pass unchanged.
- `ActorTypeCodeGeneratorTest.aBlockThatBecomesACommentNeedsNothing`: an added-to-the-Scene Event with only `MoveTowardsNearest("Missing")` and `RemoveOther` generates no `Dynamic`, no `update(`, and no `actorManager` (fails before: the carve-outs made it `Dynamic` and look `actorManager` up). Not an every-frame Event: a non-empty one makes the type `Dynamic` by itself, by design.

Compile coverage: these change generated shapes, so the integration test (`KUBRIKO_CREATOR_INTEGRATION=true ./gradlew :creator:test --tests '*ProjectTemplateIntegrationTest*'`, or lane T's T04 nightly job once it exists) has to pass before this lands.
