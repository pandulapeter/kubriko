/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeNaming
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionKind
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.PropertyNaming
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.actors.TimerNaming
import com.pandulapeter.kubrikoCreator.actors.actorTypeReference
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import com.pandulapeter.kubrikoCreator.actors.timerName
import com.pandulapeter.kubrikoCreator.commands.BlockTypes
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.Rejection
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_block_missing
import kubriko.creator.generated.resources.error_command_block_not_allowed
import kubriko.creator.generated.resources.error_command_condition_not_allowed
import kubriko.creator.generated.resources.error_command_event_missing
import kubriko.creator.generated.resources.error_command_event_not_allowed
import kubriko.creator.generated.resources.error_command_key_not_allowed
import kubriko.creator.generated.resources.error_command_manager_type_missing
import kubriko.creator.generated.resources.error_command_music_missing
import kubriko.creator.generated.resources.error_command_no_actor_types
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_other_actor_type_not_allowed
import kubriko.creator.generated.resources.error_command_parent_cycle
import kubriko.creator.generated.resources.error_command_parent_property_kind
import kubriko.creator.generated.resources.error_command_property_kind_in_descendant
import kubriko.creator.generated.resources.error_command_property_missing
import kubriko.creator.generated.resources.error_command_property_unknown
import kubriko.creator.generated.resources.error_command_scene_missing
import kubriko.creator.generated.resources.error_command_size_invalid
import kubriko.creator.generated.resources.error_command_sound_missing
import kubriko.creator.generated.resources.error_command_sprite_missing
import kubriko.creator.generated.resources.error_command_text_size_invalid
import kubriko.creator.generated.resources.error_command_timer_invalid
import kubriko.creator.generated.resources.error_command_timer_not_allowed
import kubriko.creator.generated.resources.error_command_type_missing
import kubriko.creator.generated.resources.error_game_key_unknown

/**
 * The block-built types of the project, which every type command has validated to exist before it is applied.
 */
internal val CommandContext.requireBlockTypes: BlockTypes get() = checkNotNull(blockTypes)

/**
 * The Actor type or Manager type called [name], which the command has validated to exist.
 */
internal fun CommandContext.requireType(name: String) = checkNotNull(blockTypes?.get(name))

internal fun CommandContext.requireActorType(name: String) = checkNotNull(blockTypes?.actorType(name))

internal fun CommandContext.requireManagerType(name: String) = checkNotNull(blockTypes?.managerType(name))

/**
 * What the Blocks of [definition] can refer to.
 */
internal fun CommandContext.scopeOf(definition: BlockTypeDefinition) = BlockScope.of(definition, requireBlockTypes.types.value, scenes.names(), audioFiles())

/**
 * Why there is no Actor type or Manager type called [name] to change.
 */
internal fun CommandContext.validateType(name: String): Rejection? = when {
    blockTypes == null -> Rejection(Res.string.error_command_no_actor_types)
    blockTypes[name] == null -> Rejection(Res.string.error_command_type_missing, name)
    else -> null
}

/**
 * Why a new Actor type or Manager type can't be called [name].
 */
internal fun CommandContext.validateNewTypeName(name: String, kind: BlockTypeKind): Rejection? {
    val types = blockTypes ?: return Rejection(Res.string.error_command_no_actor_types)
    return BlockTypeNaming.validate(name, types.takenNames, kind)?.let(::Rejection)
}

internal fun CommandContext.validateActorType(name: String): Rejection? = when {
    blockTypes == null -> Rejection(Res.string.error_command_no_actor_types)
    blockTypes.actorType(name) == null -> Rejection(Res.string.error_command_actor_type_missing, name)
    else -> null
}

internal fun CommandContext.validateManagerType(name: String): Rejection? = when {
    blockTypes == null -> Rejection(Res.string.error_command_no_actor_types)
    blockTypes.managerType(name) == null -> Rejection(Res.string.error_command_manager_type_missing, name)
    else -> null
}

internal fun CommandContext.validateEvent(typeName: String, eventIndex: Int): Rejection? = validateType(typeName)
    ?: if (eventIndex !in requireType(typeName).events.indices) Rejection(Res.string.error_command_event_missing, typeName, eventIndex.toString()) else null

internal fun CommandContext.validateBlock(typeName: String, eventIndex: Int, blockIndex: Int): Rejection? = validateEvent(typeName, eventIndex)
    ?: if (blockIndex !in requireType(typeName).events[eventIndex].blocks.indices) {
        Rejection(Res.string.error_command_block_missing, typeName, eventIndex.toString(), blockIndex.toString())
    } else {
        null
    }

internal fun CommandContext.validateProperty(typeName: String, propertyName: String): Rejection? = validateType(typeName)
    ?: if (requireType(typeName).properties.none { it.name == propertyName }) Rejection(Res.string.error_command_property_unknown, typeName, propertyName) else null

internal fun validateSize(width: Float, height: Float) = if (width.isFinite() && height.isFinite() && width > 0f && height > 0f) null else Rejection(Res.string.error_command_size_invalid)

/**
 * Why [event] can't be part of the type [scope] is about. Its collision Event may only refer to the Actor types of the
 * scope.
 */
internal fun validateEvent(event: EventDefinition, scope: BlockScope): Rejection? = when {
    !event.type.isAvailableIn(scope.kind) -> Rejection(Res.string.error_command_event_not_allowed, event.type.serialName)
    event.key != null && event.type != EventType.KEY_PRESSED -> Rejection(Res.string.error_command_key_not_allowed)
    event.key?.isKnown == false -> Rejection(Res.string.error_game_key_unknown, event.key.composeKeyName)
    event.otherActorType != null && event.type != EventType.COLLISION -> Rejection(Res.string.error_command_other_actor_type_not_allowed)
    event.timer != null && event.type != EventType.TIMER_FINISHED -> Rejection(Res.string.error_command_timer_not_allowed)
    event.timer != null && !TimerNaming.isValid(event.timer) -> Rejection(Res.string.error_command_timer_invalid)
    event.otherActorType != null && event.otherActorType !in scope.actorTypeNames -> Rejection(Res.string.error_command_actor_type_missing, event.otherActorType)
    else -> event.blocks.firstNotNullOfOrNull { validateBlock(it, event.type, scope) }
}

/**
 * Why [block] can't be in an Event of [eventType] of the type [scope] is about. A Block may leave its Property or Actor
 * type unchosen, but not name one that doesn't exist.
 */
internal fun validateBlock(block: BlockDefinition, eventType: EventType, scope: BlockScope): Rejection? = when {
    !BlockKind.of(block).isAvailableIn(eventType, scope.kind) -> Rejection(Res.string.error_command_block_not_allowed, block.serialName, eventType.serialName)
    block is BlockDefinition.If && !ConditionKind.of(block.condition).isAvailableIn(scope.kind) -> Rejection(Res.string.error_command_condition_not_allowed)
    !block.areNumbersFinite -> Rejection(Res.string.error_command_number_invalid)
    ((block as? BlockDefinition.If)?.condition as? ConditionDefinition.KeyHeld)?.key?.isKnown == false -> Rejection(Res.string.error_game_key_unknown, (block.condition as ConditionDefinition.KeyHeld).key.composeKeyName)
    block.timerName?.let(TimerNaming::isValid) == false -> Rejection(Res.string.error_command_timer_invalid)
    block.actorTypeReference?.let { it !in scope.actorTypeNames } == true -> Rejection(Res.string.error_command_actor_type_missing, block.actorTypeReference.orEmpty())
    block is BlockDefinition.GoToScene && block.scene.isNotEmpty() && scope.sceneNames?.contains(block.scene) == false -> Rejection(Res.string.error_command_scene_missing, block.scene)
    block is BlockDefinition.PlaySound && block.sound.isNotEmpty() && scope.audioFiles?.sounds?.contains(block.sound) == false -> Rejection(Res.string.error_command_sound_missing, block.sound)
    block is BlockDefinition.PlayMusic && block.music.isNotEmpty() && scope.audioFiles?.music?.contains(block.music) == false -> Rejection(Res.string.error_command_music_missing, block.music)
    else -> block.propertyReference?.let { reference ->
        when {
            reference.manager != null && scope.propertiesOf(reference.manager) == null -> Rejection(Res.string.error_command_manager_type_missing, reference.manager)
            reference.name.isNotEmpty() && !scope.hasProperty(reference) -> Rejection(Res.string.error_command_property_missing, reference.name, reference.kind.serialName)
            else -> null
        }
    }
}

/**
 * Why [property] can't be one of [properties] (which it replaces, if [replacedName] names one of them).
 */
internal fun validateProperty(property: PropertyDefinition, properties: List<PropertyDefinition>, replacedName: String? = null): Rejection? =
    PropertyNaming.validate(property.name, properties.map { it.name }.filter { it != replacedName })?.let(::Rejection)
        ?: if (property is PropertyDefinition.Number && !property.initialValue.isFinite()) Rejection(Res.string.error_command_number_invalid) else null

/**
 * Why the new type [definition] can't be part of the project: its Blocks are checked against the project with it, so
 * an Actor type's Blocks may use what its parent has.
 */
internal fun CommandContext.validateContents(definition: BlockTypeDefinition): Rejection? {
    val types = requireBlockTypes.types.value + definition
    val scope = BlockScope.of(definition, types, scenes.names(), audioFiles())
    val actorType = definition as? ActorTypeDefinition
    return actorType?.let { validateSize(it.width, it.height) }
        ?: actorType?.parent?.let { parent -> validateNewParent(definition.name, parent, actorType.properties) }
        ?: actorType?.sprite?.ifEmpty { null }?.let { validateSprite(it) }
        ?: definition.properties.indices.firstNotNullOfOrNull { index -> validateProperty(definition.properties[index], definition.properties.take(index)) }
        ?: definition.events.firstNotNullOfOrNull { validateEvent(it, scope) }
        ?: (definition as? ManagerTypeDefinition)?.texts?.firstNotNullOfOrNull { validateText(it) }
}

/**
 * Why the Actor type [actorType] can't inherit from [parent]: it doesn't exist, it would make a cycle, or it has one of
 * [properties] (the own Properties of the Actor type and its descendants) of another kind.
 */
internal fun CommandContext.validateNewParent(actorType: String, parent: String, properties: List<PropertyDefinition>): Rejection? {
    val actorTypes = requireBlockTypes.actorTypes.value
    return validateActorType(parent) ?: when {
        ActorTypeFamily.isAncestor(actorType, parent, actorTypes.associate { it.name to it.parent }) -> Rejection(Res.string.error_command_parent_cycle, parent, actorType)
        else -> {
            val inherited = ActorTypeFamily(actorTypes).propertiesOf(parent).associateBy { it.name }
            properties.firstOrNull { property ->
                inherited[property.name]?.let { PropertyKind.of(it) != PropertyKind.of(property) } == true
            }?.let { Rejection(Res.string.error_command_parent_property_kind, it.name, parent) }
        }
    }
}

/**
 * Why an Actor type can't be drawn as [sprite]: it isn't an image of the project.
 */
internal fun CommandContext.validateSprite(sprite: String): Rejection? =
    if (SpriteFiles.accessorOf(sprite) == null || spriteFiles()?.files?.containsKey(sprite) == false) Rejection(Res.string.error_command_sprite_missing, sprite) else null

/**
 * Why [text] can't be shown by a Manager type.
 */
internal fun validateText(text: ScreenTextDefinition) = if (text.size.isFinite() && text.size > 0f) null else Rejection(Res.string.error_command_text_size_invalid)

private val BlockDefinition.areNumbersFinite
    get() = when (this) {
        is BlockDefinition.SetVelocity -> x.isFinite() && y.isFinite()
        is BlockDefinition.AddToVelocity -> x.isFinite() && y.isFinite()
        is BlockDefinition.JumpTo -> x.isFinite() && y.isFinite()
        is BlockDefinition.If -> when (val condition = condition) {
            is ConditionDefinition.Number -> condition.value.isFinite()
            is ConditionDefinition.Velocity -> condition.value.isFinite()
            is ConditionDefinition.Chance -> condition.percent.isFinite() && condition.percent in 0f..100f
            is ConditionDefinition.Text, is ConditionDefinition.Toggle, is ConditionDefinition.KeyHeld, is ConditionDefinition.Paused -> true
        }
        is BlockDefinition.Repeat -> times >= 0
        is BlockDefinition.StartTimer -> seconds.isFinite() && seconds >= 0f
        is BlockDefinition.AddActor -> x.isFinite() && y.isFinite()
        is BlockDefinition.MoveTowardsNearest -> speed.isFinite()
        is BlockDefinition.SetNumber -> value.isFinite()
        is BlockDefinition.ChangeNumber -> by.isFinite()
        BlockDefinition.Stop, BlockDefinition.RunParentEvent, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther, BlockDefinition.RemoveOther,
        is BlockDefinition.SetText, is BlockDefinition.SetToggle, is BlockDefinition.StopTimer, BlockDefinition.Else, BlockDefinition.End,
        is BlockDefinition.Log, is BlockDefinition.Kotlin, is BlockDefinition.GoToScene, BlockDefinition.RestartScene, is BlockDefinition.PauseGame,
        is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic, BlockDefinition.StopMusic -> true
    }

/**
 * Why the Actor type [actorType] can't have [property]: one of its ancestors or descendants has a Property of that name
 * of another kind. One of the same kind only gives it another initial value.
 */
internal fun CommandContext.validateInheritedKind(actorType: String, property: PropertyDefinition): Rejection? {
    val family = ActorTypeFamily(requireBlockTypes.actorTypes.value)
    val kind = PropertyKind.of(property)
    val parent = family.parentOf(actorType)
    val inherited = parent?.let { family.propertiesOf(it).firstOrNull { inherited -> inherited.name == property.name } }
    if (inherited != null && PropertyKind.of(inherited) != kind) return Rejection(Res.string.error_command_parent_property_kind, property.name, parent)
    return family.descendantsOf(actorType)
        .firstOrNull { descendant -> requireActorType(descendant).properties.any { it.name == property.name && PropertyKind.of(it) != kind } }
        ?.let { Rejection(Res.string.error_command_property_kind_in_descendant, property.name, it) }
}
