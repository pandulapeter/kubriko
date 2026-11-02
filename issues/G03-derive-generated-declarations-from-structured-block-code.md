<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Have each Block's emission return a structured BlockCode (lines, needs, imports, Managers) and derive every feature and import from it instead of searching the generated text

**Challenged:** amended — `PluginManagers.of` calls `BlockCodeGenerator.usesAudio`, so that one stays; the legacy carve-outs are per kind of type (a Manager's Actor-only Blocks are comments with no needs today), or Manager output changes; the tests that call `blockLines`/`eventLines`/`templateLiteral` directly are listed; the absoluteValue difference reaches Manager types too, and text that merely contains "absoluteValue" stops importing it; the emission stays under the 120-line member limit P01 enforces; docs go to the actors package guide.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** ACT-2 (part 2), F3 leftover (`ManagerTypeCodeGenerator` `toList` copy)  ·  **Depends on:** G02
**Files:** `actors/BlockCode.kt` (new: `BlockCode`, `BlockNeed`), `actors/BlockCodeGenerator.kt`, `actors/ActorTypeFeatures.kt`, `actors/ActorTypeCodeGenerator.kt`, `actors/ActorFamilyCodeGenerator.kt`, `actors/ManagerTypeCodeGenerator.kt`, `actors/BlockScope.kt` (`managersUsedBy` goes), `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/actors/BlockCodeTest.kt` (new), `actors/ActorTypeCodeGeneratorTest.kt` and `actors/BlockKindTest.kt` (call sites of `blockLines`, `eventLines`, and `templateLiteral` only: `.lines` / `.code`, expected text unchanged), `actors/ManagerTypeCodeGeneratorTest.kt`, `actors/CLAUDE.md` (G02's bullet)

(`actors/` = `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/`; tests mirror it under `creator/src/test/kotlin/...`.)

## Problem

What a type's generated class must declare and import is worked out apart from the code that needs it, partly by searching the generated text:

```kotlin
// ActorTypeFeatures.imports
val blocksByEvent = edgeEvents.flatMap { event -> event.blocks.map { it to event.type } } + collisionEvents.flatMap { event -> event.blocks.map { it to event.type } }
if (usesBounceOff || blocksByEvent.any { (block, eventType) -> blockLines(block, eventType, scope).any { "absoluteValue" in it } }) add("kotlin.math.absoluteValue")

// ActorTypeCodeGenerator.generateActorType
if (generatedEvents.any { event -> event.blocks.any { it is BlockDefinition.Log && ".$NUMBER_TEXT_FUNCTION()" in templateLiteral(it.message, scope) } }) {
    appendNumberTextFunction("private")
}

// ManagerTypeCodeGenerator.generateManagerType
if (texts.any { ".$NUMBER_TEXT_FUNCTION()" in it } || generatedBlocks.any { it is BlockDefinition.Log && ".$NUMBER_TEXT_FUNCTION()" in templateLiteral(it.message, scope) }) {
```

(and the same `usesNumberText` search in `ActorFamilyCodeGenerator.generate`). The rest are per-Block predicates kept next to, not in, the emission: `BlockCodeGenerator.usesScenes`, `usesPause`, `usesAudio`, `ActorTypeFeatures.movesTheActor`, `needsActorManager`, `usesKeyHeld`, the `MoveTowardsNearest`/`AddActor`/`JumpTo`/`Chance` import checks in `ActorTypeFeatures.imports`, `ManagerTypeCodeGenerator`'s own `usesActorManager`/`usesKeyHeld`/`AddActor` import checks, and `BlockScope.managersUsedBy`. A new Block's emission can name `stateManager` or `Random` without any of them knowing, and the result doesn't compile; the text searches also generate edge and collision Blocks a second time.

Leftover from review batch F3: `ManagerTypeCodeGenerator.generateManagerType` takes `knownActorTypeNames: Collection<String> = emptyList()` and copies it on every call (`BlockScope(name, BlockTypeKind.MANAGER, definition.properties, knownActorTypeNames.toList(), managerTypes)`); every caller (`BlockTypeStore.regenerate`, the tests) passes a `List`.

## Fix

Output for the golden every-Block project (G02's `GeneratedCodeGoldenTest`) and for every existing generator test stays byte-identical. The one deliberate difference is noted under step 3.

1. **Model** (`actors/BlockCode.kt`):
   ```kotlin
   /** The statements of Blocks, and what the class they are generated into has to declare and import for them. */
   internal data class BlockCode(
       val lines: List<String>,
       val needs: Set<BlockNeed> = emptySet(),
       val imports: Set<String> = emptySet(),
       val managers: Set<String> = emptySet(),
   ) {
       operator fun plus(other: BlockCode): BlockCode
       fun indented(depth: Int): BlockCode
   }
   internal enum class BlockNeed { MOVEMENT, ACTOR_MANAGER, VIEWPORT_MANAGER, KEYBOARD_INPUT_MANAGER, STATE_MANAGER, SCENES, AUDIO, NUMBER_TEXT, BOUNCE_OFF }
   ```
   `imports` are fully qualified names the lines use directly (`kotlin.math.absoluteValue`, `kotlin.random.Random`, `com.pandulapeter.kubriko.logger.Logger`, `com.pandulapeter.kubriko.types.SceneOffset`, `com.pandulapeter.kubriko.helpers.extensions.normalized`, `androidx.compose.ui.input.key.Key`); `needs` are members the class declares (a lookup, the velocity field and `Dynamic`, `asText`, `bounceOff`); `managers` are the other Manager types whose fields the lines use. How a need is declared differs per kind of class, so it stays in the generators, each as an exhaustive `when (need)`: Actor classes `private/protected lateinit var ...` plus their imports, Manager classes `by manager<...>()`.
2. **Emission.** `BlockCodeGenerator.blockLines` becomes `blockCode(block, eventType, scope): BlockCode`, `eventLines` becomes `eventCode(blocks, eventType, scope): BlockCode` (nesting lines carry no needs), `templateLiteral` returns `TemplateLiteral(code: String, usesNumberText: Boolean, managers: Set<String>)`, and `propertyExpression` reports the Manager it reaches through. Each `actorBlockLines` branch returns its needs next to its lines, copied from today's predicates (keep the emission's functions under the 120-line limit P01 makes the size check apply to members; split by category of Block if the needs push `actorBlockLines`, about 100 lines at HEAD, past it):
   - `SetVelocity`, `AddToVelocity`, `Stop`, `JumpTo`: `MOVEMENT`, import `SceneOffset`. `BounceOffEdge` in an edge Event: `MOVEMENT`, imports `absoluteValue` and `SceneOffset`. `BounceOffOther` in a collision Event: `MOVEMENT`, `BOUNCE_OFF`. `If` with a `Velocity` condition: `MOVEMENT`.
   - `RemoveSelf`, `RemoveOther` (in a collision Event): `ACTOR_MANAGER`. `AddActor` of a known type: `ACTOR_MANAGER`, imports `SceneOffset` (and, in a Manager, `com.pandulapeter.kubriko.helpers.extensions.sceneUnit`, which Actor classes import always). `MoveTowardsNearest` of a known type: `MOVEMENT`, `ACTOR_MANAGER`, imports `normalized` and `SceneOffset`.
   - `GoToScene` with a Scene, `RestartScene`: `SCENES`. `PauseGame`, `If` with `Paused`: `STATE_MANAGER`. `PlaySound`/`PlayMusic` with a file, `StopMusic`: `AUDIO`. `If` with `KeyHeld`: `KEYBOARD_INPUT_MANAGER`, import `Key`. `If` with `Chance`: import `Random`. `Log`: import `Logger`, plus `NUMBER_TEXT` and the placeholders' `managers` from its `TemplateLiteral`. Property Blocks and Property conditions: the Manager their reference reaches, when it exists.
   - `Kotlin`: `MOVEMENT`, `ACTOR_MANAGER`, `VIEWPORT_MANAGER` (today's `usesKotlin`), and `kotlin.math.absoluteValue` when its code names `absoluteValue` as a whole word (`KotlinUi.names`-style regex, or `KotlinIdentifiers.find`).
   **Equivalence carve-outs** (kept so output doesn't change here; G04 removes them on purpose): today a Block that generates a comment can still count. Keep exactly these: `MOVEMENT` for `MoveTowardsNearest` of an unknown or unchosen type, for `BounceOffEdge` and `BounceOffOther` outside their Event, and for an empty `Kotlin` Block (also its `ACTOR_MANAGER` and `VIEWPORT_MANAGER`); `ACTOR_MANAGER` for `RemoveOther` outside a collision Event; `KEYBOARD_INPUT_MANAGER` for a `KeyHeld` condition on an unknown key. Put them in one clearly named private function (`legacyNeedsOfComments`) with a KDoc that G04 deletes it. They are per kind of type, as the predicates they copy were: the `MOVEMENT` and `ACTOR_MANAGER` ones are an Actor generator's (`ActorTypeFeatures`); in a Manager scope the Actor-only Blocks are `"// This Block only works in an Actor type."` with no needs at all, since `ManagerTypeCodeGenerator`'s `usesActorManager` never counted `RemoveOther`, `RemoveSelf`, or `MoveTowardsNearest`. The empty-`Kotlin` and unknown-key `KeyHeld` ones apply to both kinds (both generators count any Kotlin Block and any `KeyHeld` condition today).
3. **Consumers.** `ActorTypeFeatures` computes one `BlockCode` per generated Event (in the scope it gets today, so a family's stays the root's) and derives `isDynamic` (`MOVEMENT`, plus today's Event-based conditions: edge Events, Timers, non-empty every-frame Events), `usesBounceOff` (`BOUNCE_OFF`), every lookup, and the Block imports from them; the class-level imports that come from Events or class parts (collision, keyboard awareness, edges' `SceneUnit`, sprites, `Kubriko` and `get` with lookups) stay as they are. `ManagerTypeCodeGenerator` does the same and gets `usesNumberText` from its texts' `TemplateLiteral`s. `ActorTypeCodeGenerator` and `ActorFamilyCodeGenerator` read `NUMBER_TEXT` instead of searching. Delete `BlockCodeGenerator.usesScenes` and `usesPause`; keep `usesAudio`, which `PluginManagers.of` calls for `usesAudio` (the whole project's Blocks, not one class's), and leave `PluginManagers` as it is; `ActorTypeFeatures.movesTheActor`, `needsActorManager`, and `BlockScope.managersUsedBy`. The deliberate differences, both only about the `kotlin.math.absoluteValue` import: a Kotlin Block that names `absoluteValue` now imports it in every Event, not only in an Actor type's edge and collision Events (where the old search found it), in Manager types too (which never imported it); and a Block that only contains the word (a Log or set-text Block's text, a Property named `absoluteValue`) in an edge or collision Event no longer imports it. Neither occurs in the golden project or the existing tests.
4. Change `generateManagerType`'s `knownActorTypeNames` to `List<String> = emptyList()` and pass it through without `.toList()`.
5. Extend G02's bullet in the actors package guide: the emission returns its lines with what they need (`BlockCode`), and every lookup, import, and trait follows from that.

## Tests

- `GeneratedCodeGoldenTest` and every existing generator test pass with no expected text changed.
- `BlockCodeTest.everyBlockStatesWhatItsCodeNeeds`: for each `BlockKind.entries` default Block, plus the variants that change the answer (a `PlaySound` with a file and without, `AddActor`/`MoveTowardsNearest` of a known type and of none, each condition kind in an `If`, a Log with `{score}` of a number Property and of a Manager's Property), in an Actor scope with `actorTypeNames = listOf("Ball")` and one Manager type `Game` with a number Property `score`: `needs`, `imports`, and `managers` equal a literal table in the test. Write the table from today's predicates before changing the code, so it fails if a branch forgets one.
- `BlockCodeTest.aKotlinBlockNamingAbsoluteValueImportsItInEveryEvent`: a type whose every-frame Event has `Kotlin("velocity = SceneOffset(velocity.x.raw.absoluteValue.sceneUnit, velocity.y)")` imports `kotlin.math.absoluteValue` (fails before: only edge and collision Events were searched).
- `BlockCodeTest.aManagersActorOnlyBlocksNeedNothing`: in a Manager scope, `RemoveOther`, `RemoveSelf`, and `MoveTowardsNearest("Ball")` give the actor-only comment with empty `needs`, `imports`, and `managers` (fails if the Actor carve-outs leak into Managers).
- `ManagerTypeCodeGeneratorTest.aTextShowingANumberGetsTheNumberTextFunction`: a Manager whose only text is `"{score}"` (number Property) has `private fun Float.asText()`; with `"{title}"` (text Property) it has none.
