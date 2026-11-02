<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Rename an inherited Property only on the ancestor that declares it, through every descendant, and check add_property and set_parent against descendants too.

**Challenged:** amended — apply follows K02a's amended order: the family's `put`s first (so the name field's next keystroke sees the rename), then one all-or-nothing Scene change, with every changed member put back if it throws.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** CMD-5 = ACT-5  ·  **Depends on:** K02b (AddProperty's composite inverse, `changeStates` returning replaced states)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/commands/actorTypes/EditProperty.kt`, `commands/actorTypes/AddProperty.kt`, `commands/actorTypes/SetParent.kt`, `commands/actorTypes/ActorTypeValidation.kt`, `commands/actorTypes/PropertySceneValues.kt`, `actors/BlockTypeEdits.kt`, `actors/ActorTypeFamily.kt`, `scenes/SceneStore.kt` (`changeStates` for several types at once), `creator/src/main/composeResources/values/strings.xml`, tests `commands/actorTypes/PropertyCommandsTest.kt`, `commands/actorTypes/ParentCommandsTest.kt`, `creator/CLAUDE.md` (the Properties/inheritance sentences of the Actor types section; grep `redeclare` or `initial value`)

Lane G (G02–G05) restructures code generation in `actors/`; this plan touches only `BlockTypeEdits.kt` and `ActorTypeFamily.kt` there.

## Problem

Children inherit their ancestors' Properties; a child may name one again, of the same kind, to change its initial value (`ActorTypeFamily.propertiesOf`). Blocks of every family member validate and generate against `propertiesOf`, and a Block whose Property no longer exists silently generates nothing.

1. `EditProperty` renames only in `input.actorType`:
   ```kotlin
        context.requireBlockTypes.put(definition.withPropertyReplaced(input.property, input.newProperty))
        ...
        if (input.newProperty.name != input.property) {
            context.changeSceneStates(input.actorType) { it.withValueRenamed(input.property, input.newProperty.name) }
        }
   ```
   Renaming a parent's Property leaves descendants' Blocks inert, their Kotlin Blocks uncompilable, their redeclarations turned into unrelated new Properties, and their Actors' Scene values under the old key. Validation (`validateProperty(input.newProperty, properties, replacedName = input.property)`) checks only the type's own list. Renaming a child's redeclaration splits it off from the parent's silently.
2. `AddProperty` checks `validateInheritedKind`, ancestors only:
   ```kotlin
    val family = ActorTypeFamily(requireBlockTypes.actorTypes.value)
    val parent = family.parentOf(actorType) ?: return null
    val inherited = family.propertiesOf(parent).firstOrNull { it.name == property.name } ?: return null
   ```
   Adding to a parent a name a descendant declares with another kind makes `propertiesOf(descendant)` drop the descendant's own Property (its Blocks go inert); descendants' Scene values of the other kind are not cleaned.
3. `SetParent.validate` checks only `input.actorType`'s own Properties against the new parent's (`validateNewParent(input.actorType, parent, context.requireActorType(input.actorType).properties)`), not its descendants'.

(RemoveProperty on a parent leaving descendants' Blocks inert is documented behaviour, not part of this plan.) `RenameTimer` already works on the family (`family.familyOf(family.rootOf(typeName))`).

## Fix

The user decided (decisions.md, "Family Property rename"): only on the declaring ancestor, carried through all descendants (Blocks, placeholders, Kotlin, overrides, Scene values); rejected on a child with a pointer to the ancestor.

1. `ActorTypeFamily.declarerOf(name: String, property: String): String?`: the topmost of `ancestorsOf(name).reversed() + name` whose own `properties` has `property` with the same kind as `propertiesOf(name)`'s; null if none. KDoc one line.
2. `actors/BlockTypeEdits.kt`: split `withPropertyReplaced` into the list change and `BlockTypeDefinition.withPropertyUsesRenamed(oldName, newName)` (Blocks, texts, Kotlin — today's `else` branch); `withPropertyReplaced` calls it. Behaviour unchanged for the existing callers.
3. `EditProperty` for an Actor type, when the name changes:
   - validate: `val declarer = family.declarerOf(actorType, property)`; if `declarer != actorType` → `Rejection(error_command_property_inherited, property, declarer)`, new string "%1$s comes from %2$s: rename it there, and it changes in every Actor type that inherits it." Changing only the initial value of a redeclaration stays allowed.
   - validate the new name against every family member (`family.familyOf(actorType)`): `PropertyNaming.validate(newName, (propertiesOf(member) + member's own properties).map { it.name } - oldName)` for each, first rejection wins.
   - apply, in K02a's order: first, for each member, the declarer gets `withPropertyReplaced(old, newProperty)`; a member redeclaring it gets `withPropertyReplaced(old, its own definition with the new name)` (keeping its initial value); every other member `withPropertyUsesRenamed(old, new)`; `put` only those that changed, remembering their previous definitions. Then one `scenes.changeStates(typeIds = familyOf(actorType).toSet()) { it.withValueRenamed(old, new) }` (a `changeStates` overload taking a set, written with one `writeAll`) on IO; if it throws, `put` every remembered previous definition back and rethrow.
   - inverse: `EditProperty(actorType, newName, previous)` on the declarer, which renames the family back. Update its AI `description`: "Renaming also updates the Blocks and texts that use it (in every Actor type that inherits it, or in every type, for a Manager type's Property) and the values Actors keep in Scenes. A Property an Actor type inherits is renamed in the type it comes from."
   - Manager types: unchanged.
4. `AddProperty` for an Actor type: `validateInheritedKind` also checks the descendants' own Properties: a descendant declaring `property.name` with another kind → `Rejection(error_command_property_kind_in_descendant, property.name, descendant)`, new string "%2$s, which inherits from this Actor type, already has a Property called %1$s of another kind." Scene cleanup (`withoutMismatchedValue`) runs, after the `put`, for the whole `familyOf(actorType)` through the set overload (K02a's rollback on a throw), and K02b's composite inverse restores what it dropped in every member's Scenes.
5. `SetParent.validate`: pass the own Properties of `input.actorType` and of every descendant (`family.descendantsOf`) to `validateNewParent` (rename its parameter to `properties` of "the Actor type and its descendants"; the rejection message, `error_command_parent_property_kind`, names the Property and the parent, which stays right).
6. `creator/CLAUDE.md`, the inheritance description: one sentence: "Renaming a Property renames it through the family of the Actor type that declares it, and is refused on one that only inherits it; adding a Property or a parent is refused when a descendant has one of that name of another kind."

## Tests

`PropertyCommandsTest` / `ParentCommandsTest`, with `Parent{health: number}`, `Child(parent = Parent){ a setNumber Block and a Kotlin Block using health, a Log Block "{health}" }`, `GrandChild(parent = Child){ health: number = 5 (redeclared) }`, and a Scene with one Actor of each type holding `"health"`:
- `renamingOnTheDeclarerRenamesTheFamily`: `EditProperty Parent health→hp` → Child's Blocks, Log text, and Kotlin use `hp`; GrandChild's redeclaration is `hp` with initial value 5; every Actor's Scene state has `hp` and no `health`; `assertRoundTrip` restores everything.
- `aFamilyRenameWhoseSceneCantBeWrittenChangesNoMember`: K02a's `sceneWriteText` hook failing for the Scene → `Rejected(error_command_failed)`, Parent, Child, and GrandChild equal their originals, the Scene file unchanged.
- `renamingAnInheritedPropertyIsRefused`: `EditProperty GrandChild health→hp` → `Rejected(error_command_property_inherited, "health", "Parent")`; changing only GrandChild's initial value is accepted.
- `aNewNameTakenInTheFamilyIsRefused`: Child declares `score`; `EditProperty Parent health→score` → rejected.
- `addingAPropertyADescendantHasOfAnotherKindIsRefused`: `AddProperty Parent name:text` while Child declares `name: number` → `Rejected(error_command_property_kind_in_descendant, "name", "Child")`.
- `addingCleansTheFamilysScenes`: Child's Actor holds `"speed": "fast"`; `AddProperty Parent speed:number` → the value is dropped from Child's Actor, and undo brings it back.
- `ParentCommandsTest.aParentClashingWithADescendantIsRefused`: `SetParent Child2 → Other` where Child2's child declares `health: text` and Other has `health: number` → `Rejected(error_command_parent_property_kind)`.
