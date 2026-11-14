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
import com.pandulapeter.kubrikoCreator.actors.AnimatedValue
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
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.PropertyNaming
import com.pandulapeter.kubrikoCreator.actors.PropertyReference
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.actors.TimerNaming
import com.pandulapeter.kubrikoCreator.actors.actorTypeReference
import com.pandulapeter.kubrikoCreator.actors.areEdgesAlong
import com.pandulapeter.kubrikoCreator.actors.isFinite
import com.pandulapeter.kubrikoCreator.actors.numberPropertyReferences
import com.pandulapeter.kubrikoCreator.actors.numberValues
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import com.pandulapeter.kubrikoCreator.actors.readsEdges
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypes
import com.pandulapeter.kubrikoCreator.actors.timerName
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.SimilarNames
import com.pandulapeter.kubrikoCreator.foundation.ColorHex

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
    blockTypes == null -> Rejection(CommandProblem.NO_ACTOR_TYPES)
    blockTypes[name] == null -> SimilarNames.rejection(LookupProblem.TYPE_MISSING, LookupProblem.TYPE_MISSING_SIMILAR, name, blockTypes.types.value.map { it.name }, name)
    else -> null
}

/**
 * Why a new Actor type or Manager type can't be called [name].
 */
internal fun CommandContext.validateNewTypeName(name: String, kind: BlockTypeKind): Rejection? {
    val types = blockTypes ?: return Rejection(CommandProblem.NO_ACTOR_TYPES)
    return BlockTypeNaming.validate(name, types.takenNames, kind)?.let(::Rejection)
}

internal fun CommandContext.validateActorType(name: String): Rejection? = when {
    blockTypes == null -> Rejection(CommandProblem.NO_ACTOR_TYPES)
    blockTypes.actorType(name) == null -> missingActorType(name, blockTypes.actorTypes.value.map { it.name }, blockTypes.managerTypes.value.map { it.name })
    else -> null
}

internal fun CommandContext.validateManagerType(name: String): Rejection? = when {
    blockTypes == null -> Rejection(CommandProblem.NO_ACTOR_TYPES)
    blockTypes.managerType(name) == null -> missingManagerType(name, blockTypes.managerTypes.value.map { it.name }, blockTypes.actorTypes.value.map { it.name })
    else -> null
}

/**
 * Why there is no Actor type called [name] among [actorTypeNames]: it is one of [managerTypeNames], or similar to some.
 */
internal fun missingActorType(name: String, actorTypeNames: List<String>, managerTypeNames: List<String>) = if (name in managerTypeNames) {
    Rejection(LookupProblem.ACTOR_TYPE_IS_MANAGER_TYPE, name)
} else {
    SimilarNames.rejection(LookupProblem.ACTOR_TYPE_MISSING, LookupProblem.ACTOR_TYPE_MISSING_SIMILAR, name, actorTypeNames, name)
}

private fun missingManagerType(name: String, managerTypeNames: List<String>, actorTypeNames: List<String>) = if (name in actorTypeNames) {
    Rejection(LookupProblem.MANAGER_TYPE_IS_ACTOR_TYPE, name)
} else {
    SimilarNames.rejection(LookupProblem.MANAGER_TYPE_MISSING, LookupProblem.MANAGER_TYPE_MISSING_SIMILAR, name, managerTypeNames, name)
}

internal fun CommandContext.validateEvent(typeName: String, eventIndex: Int): Rejection? = validateType(typeName) ?: run {
    val events = requireType(typeName).events
    when {
        eventIndex in events.indices -> null
        events.isEmpty() -> Rejection(LookupProblem.EVENT_MISSING_NONE, typeName, eventIndex.toString())
        else -> Rejection(LookupProblem.EVENT_MISSING, typeName, eventIndex.toString(), events.lastIndex.toString())
    }
}

internal fun CommandContext.validateBlock(typeName: String, eventIndex: Int, blockIndex: Int): Rejection? = validateEvent(typeName, eventIndex) ?: run {
    val blocks = requireType(typeName).events[eventIndex].blocks
    when {
        blockIndex in blocks.indices -> null
        blocks.isEmpty() -> Rejection(LookupProblem.BLOCK_MISSING_NONE, typeName, eventIndex.toString(), blockIndex.toString())
        else -> Rejection(LookupProblem.BLOCK_MISSING, typeName, eventIndex.toString(), blockIndex.toString(), blocks.lastIndex.toString())
    }
}

internal fun CommandContext.validateProperty(typeName: String, propertyName: String): Rejection? = validateType(typeName) ?: run {
    val properties = requireType(typeName).properties
    if (properties.any { it.name == propertyName }) {
        null
    } else {
        SimilarNames.rejection(LookupProblem.PROPERTY_UNKNOWN, LookupProblem.PROPERTY_UNKNOWN_SIMILAR, propertyName, properties.map { it.name }, typeName, propertyName)
    }
}

internal fun validateSize(width: Float, height: Float) = if (width.isFinite() && height.isFinite() && width > 0f && height > 0f) null else Rejection(CommandProblem.SIZE_INVALID)

/**
 * Why [event] can't be part of the type [scope] is about. Its collision Event may only refer to the Actor types of the
 * scope.
 */
internal fun validateEvent(event: EventDefinition, scope: BlockScope): Rejection? {
    val key = event.key
    val timer = event.timer
    val otherActorType = event.otherActorType
    event.type.problemIn(scope.kind)?.let { return Rejection(it) }
    return when {
        key != null && event.type != EventType.KEY_PRESSED -> Rejection(PlacementProblem.KEY_NOT_ALLOWED)
        key?.isKnown == false -> unknownKey(key)
        otherActorType != null && event.type != EventType.COLLISION -> Rejection(PlacementProblem.OTHER_ACTOR_TYPE_NOT_ALLOWED)
        timer != null && event.type != EventType.TIMER_FINISHED -> Rejection(PlacementProblem.TIMER_NOT_ALLOWED)
        else -> timer?.let(TimerNaming::validate)?.let(::Rejection) ?: when {
            otherActorType != null && otherActorType !in scope.actorTypeNames -> missingActorType(otherActorType, scope)
            else -> event.blocks.firstNotNullOfOrNull { validateBlock(it, event.type, scope) }
        }
    }
}

/**
 * Why [block] can't be in an Event of [eventType] of the type [scope] is about. A Block may leave its Property or Actor
 * type unchosen, but not name one that doesn't exist.
 */
internal fun validateBlock(block: BlockDefinition, eventType: EventType, scope: BlockScope): Rejection? = BlockKind.of(block).problemIn(eventType, scope.kind)?.let(::Rejection)
    ?: validateBlockValues(block, scope)

private fun validateBlockValues(block: BlockDefinition, scope: BlockScope): Rejection? = when {
    block is BlockDefinition.If && !ConditionKind.of(block.condition).isAvailableIn(scope.kind) -> Rejection(PlacementProblem.CONDITION_NOT_ALLOWED)
    !block.areNumbersValid -> Rejection(CommandProblem.NUMBER_INVALID)
    !block.areEdgesAlongTheirAxes -> Rejection(CommandProblem.NUMBER_EDGE_ACROSS)
    block.edgelessNumberValues.any { it.readsEdges } -> Rejection(CommandProblem.NUMBER_EDGE_NOT_TAKEN)
    block is BlockDefinition.SetOpacity && !block.percent.isWithin(0f, 100f) -> Rejection(CommandProblem.OPACITY_OUT_OF_RANGE)
    block is BlockDefinition.SetColor && block.colors.size !in 1..BlockDefinition.SetColor.MAXIMUM_COLORS ->
        Rejection(CommandProblem.COLORS_COUNT, BlockDefinition.SetColor.MAXIMUM_COLORS.toString())
    block is BlockDefinition.SetColor && block.colors.any { !ColorHex.isOpaque(it) } -> Rejection(CommandProblem.COLORS_NOT_OPAQUE)
    block is BlockDefinition.Animate && block.value is AnimatedValue.Opacity && !block.value.numberValues.all { it.isWithin(0f, 100f) } ->
        Rejection(CommandProblem.OPACITY_OUT_OF_RANGE)
    block is BlockDefinition.MoveWithKeys && block.speed is NumberValue.Random -> Rejection(CommandProblem.SPEED_RANDOM_EVERY_FRAME)
    block is BlockDefinition.Animate && block.value is AnimatedValue.Scale && !block.value.numberValues.all { it.isWithin(minimum = 0f) } ->
        Rejection(CommandProblem.SCALE_NEGATIVE)
    ((block as? BlockDefinition.Animate)?.value as? AnimatedValue.Color)?.let { ColorHex.isOpaque(it.from) && ColorHex.isOpaque(it.to) } == false ->
        Rejection(CommandProblem.ANIMATED_COLOR_NOT_OPAQUE)
    ((block as? BlockDefinition.If)?.condition as? ConditionDefinition.KeyHeld)?.key?.isKnown == false -> unknownKey((block.condition as ConditionDefinition.KeyHeld).key)
    else -> block.timerName?.let(TimerNaming::validate)?.let(::Rejection) ?: validateBlockReferences(block, scope)
}

private fun validateBlockReferences(block: BlockDefinition, scope: BlockScope): Rejection? = when {
    block.actorTypeReference?.let { it !in scope.actorTypeNames } == true -> missingActorType(block.actorTypeReference.orEmpty(), scope)
    block is BlockDefinition.GoToScene && block.scene.isNotEmpty() && scope.sceneNames?.contains(block.scene) == false ->
        SimilarNames.rejection(LookupProblem.SCENE_MISSING, LookupProblem.SCENE_MISSING_SIMILAR, block.scene, scope.sceneNames.orEmpty(), block.scene)
    block is BlockDefinition.PlaySound && block.sound.isNotEmpty() && scope.audioFiles?.sounds?.contains(block.sound) == false ->
        SimilarNames.rejection(LookupProblem.SOUND_MISSING, LookupProblem.SOUND_MISSING_SIMILAR, block.sound, scope.audioFiles?.sounds.orEmpty(), block.sound)
    block is BlockDefinition.PlayMusic && block.music.isNotEmpty() && scope.audioFiles?.music?.contains(block.music) == false ->
        SimilarNames.rejection(LookupProblem.MUSIC_MISSING, LookupProblem.MUSIC_MISSING_SIMILAR, block.music, scope.audioFiles?.music.orEmpty(), block.music)
    else -> (listOfNotNull(block.propertyReference) + block.numberPropertyReferences).firstNotNullOfOrNull { reference -> validatePropertyReference(reference, scope) }
}

private fun validatePropertyReference(reference: PropertyReference, scope: BlockScope): Rejection? {
    val manager = reference.manager
    return when {
        manager != null && scope.propertiesOf(manager) == null -> missingManagerType(manager, scope.managerTypes.map { it.name }, scope.actorTypeNames)
        reference.name.isNotEmpty() && scope.propertiesOf(manager).orEmpty().any { it.name == reference.name && PropertyKind.of(it) != reference.kind } ->
            Rejection(reference.kind.otherKindProblem, reference.name, manager ?: scope.typeName)
        reference.name.isNotEmpty() && !scope.hasProperty(reference) -> SimilarNames.rejection(
            reference.kind.missingProblem,
            reference.kind.missingSimilarProblem,
            reference.name,
            scope.propertiesOf(manager).orEmpty().filter { PropertyKind.of(it) == reference.kind }.map { it.name },
            reference.name,
            manager ?: scope.typeName,
        )
        else -> null
    }
}

private fun missingActorType(name: String, scope: BlockScope) = missingActorType(name, scope.actorTypeNames, scope.managerTypes.map { it.name })

/**
 * Why [key] can't be one an Event or a condition reacts to, naming the keys called similarly.
 */
internal fun unknownKey(key: KeyChoice) =
    SimilarNames.rejection(LookupProblem.GAME_KEY_UNKNOWN, LookupProblem.GAME_KEY_UNKNOWN_SIMILAR, key.composeKeyName, KeyChoice.all.map { it.composeKeyName }, key.composeKeyName)

/**
 * Why [property] can't be one of [properties] of a type of [kind] (which it replaces, if [replacedName] names one of them).
 */
internal fun validateProperty(property: PropertyDefinition, properties: List<PropertyDefinition>, kind: BlockTypeKind, replacedName: String? = null): Rejection? =
    PropertyNaming.validate(property.name, properties.map { it.name }.filter { it != replacedName }, kind)?.let(::Rejection)
        ?: if (property is PropertyDefinition.Number && !property.initialValue.isFinite()) Rejection(CommandProblem.NUMBER_INVALID) else null

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
        ?: definition.properties.indices.firstNotNullOfOrNull { index -> validateProperty(definition.properties[index], definition.properties.take(index), definition.kind) }
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
        ActorTypeFamily.isAncestor(actorType, parent, actorTypes.associate { it.name to it.parent }) -> Rejection(CommandProblem.PARENT_CYCLE, parent, actorType)
        else -> {
            val inherited = ActorTypeFamily(actorTypes).propertiesOf(parent).associateBy { it.name }
            properties.firstOrNull { property ->
                inherited[property.name]?.let { PropertyKind.of(it) != PropertyKind.of(property) } == true
            }?.let { Rejection(CommandProblem.PARENT_PROPERTY_KIND, it.name, parent) }
        }
    }
}

/**
 * Why an Actor type can't be drawn as [sprite]: it isn't an image of the project.
 */
internal fun CommandContext.validateSprite(sprite: String): Rejection? {
    val files = spriteFiles()
    return if (SpriteFiles.accessorOf(sprite) == null || files?.files?.containsKey(sprite) == false) {
        SimilarNames.rejection(LookupProblem.SPRITE_MISSING, LookupProblem.SPRITE_MISSING_SIMILAR, sprite, files?.paths.orEmpty(), sprite)
    } else {
        null
    }
}

/**
 * Why [text] can't be shown by a Manager type.
 */
internal fun validateText(text: ScreenTextDefinition) = if (text.size.isFinite() && text.size > 0f) null else Rejection(CommandProblem.TEXT_SIZE_INVALID)

/**
 * Whether every number [this] Block takes is finite and, where a field allows only some, within them. A Property's value
 * or an edge can't be checked before the game runs, so the generated code keeps it within them instead.
 */
private val BlockDefinition.areNumbersValid
    get() = numberValues.all { it.isFinite } && when (this) {
        is BlockDefinition.If -> (condition as? ConditionDefinition.Chance)?.percent?.isWithin(0f, 100f) ?: true
        is BlockDefinition.Repeat -> times.isWithin(minimum = 0f) && times.isWholeWhenFixed
        is BlockDefinition.StartTimer -> seconds.isWithin(minimum = 0f)
        is BlockDefinition.Animate -> seconds.isWithin(minimum = 0f)
        is BlockDefinition.SetFrame -> frame.isWithin(minimum = 0f) && frame.isWholeWhenFixed
        is BlockDefinition.SetVelocity, is BlockDefinition.AddToVelocity, is BlockDefinition.MoveInDirection, is BlockDefinition.MoveWithKeys, is BlockDefinition.JumpTo, is BlockDefinition.AddActor,
        is BlockDefinition.MoveTowardsNearest, is BlockDefinition.SetNumber, is BlockDefinition.ChangeNumber, is BlockDefinition.SetAnimation,
        BlockDefinition.Stop, BlockDefinition.RunParentEvent, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther, BlockDefinition.RemoveOther,
        is BlockDefinition.SetText, is BlockDefinition.SetToggle, is BlockDefinition.StopTimer, BlockDefinition.Else, BlockDefinition.End,
        is BlockDefinition.Log, is BlockDefinition.Kotlin, is BlockDefinition.GoToScene, BlockDefinition.RestartScene, is BlockDefinition.PauseGame,
        is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic, BlockDefinition.StopMusic, is BlockDefinition.SetColor, is BlockDefinition.SetOpacity,
        is BlockDefinition.ChangeOpacity,
        -> true
    }

private val BlockDefinition.areEdgesAlongTheirAxes
    get() = when (this) {
        is BlockDefinition.SetVelocity -> areEdgesAlong(x, y)
        is BlockDefinition.AddToVelocity -> areEdgesAlong(x, y)
        is BlockDefinition.JumpTo -> areEdgesAlong(x, y)
        is BlockDefinition.AddActor -> areEdgesAlong(x, y)
        is BlockDefinition.Animate -> (value as? AnimatedValue.Position)?.let { areEdgesAlong(it.x, it.y) } ?: true
        is BlockDefinition.MoveInDirection, is BlockDefinition.MoveWithKeys, is BlockDefinition.MoveTowardsNearest, is BlockDefinition.SetNumber, is BlockDefinition.ChangeNumber, is BlockDefinition.SetAnimation,
        is BlockDefinition.If, is BlockDefinition.Repeat, is BlockDefinition.StartTimer, is BlockDefinition.SetFrame,
        BlockDefinition.Stop, BlockDefinition.RunParentEvent, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther, BlockDefinition.RemoveOther,
        is BlockDefinition.SetText, is BlockDefinition.SetToggle, is BlockDefinition.StopTimer, BlockDefinition.Else, BlockDefinition.End,
        is BlockDefinition.Log, is BlockDefinition.Kotlin, is BlockDefinition.GoToScene, BlockDefinition.RestartScene, is BlockDefinition.PauseGame,
        is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic, BlockDefinition.StopMusic, is BlockDefinition.SetColor, is BlockDefinition.SetOpacity,
        is BlockDefinition.ChangeOpacity,
        -> numberValues.none { it is NumberValue.BetweenEdges && it.from.axis != it.to.axis }
    }

/**
 * The numbers of [this] Block that are a direction, a speed, an opacity, a scale, or a time, which an edge, a position,
 * means nothing as.
 */
private val BlockDefinition.edgelessNumberValues
    get() = when (this) {
        is BlockDefinition.MoveInDirection -> listOf(direction, speed)
        is BlockDefinition.MoveTowardsNearest -> listOf(speed)
        is BlockDefinition.MoveWithKeys -> listOf(speed)
        is BlockDefinition.SetOpacity -> listOf(percent)
        is BlockDefinition.ChangeOpacity -> listOf(by)
        is BlockDefinition.Animate -> if (value is AnimatedValue.Position) listOf(seconds) else value.numberValues + seconds
        else -> emptyList()
    }

private val NumberValue.isWholeWhenFixed get() = this !is NumberValue.Fixed || value == value.toLong().toFloat()

private fun NumberValue.isWithin(minimum: Float, maximum: Float = Float.POSITIVE_INFINITY) = when (this) {
    is NumberValue.Fixed -> value in minimum..maximum
    is NumberValue.Random -> from in minimum..maximum && to in minimum..maximum
    is NumberValue.Property, is NumberValue.Edge, is NumberValue.BetweenEdges -> true
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
    if (inherited != null && PropertyKind.of(inherited) != kind) return Rejection(CommandProblem.PARENT_PROPERTY_KIND, property.name, parent)
    return family.descendantsOf(actorType)
        .firstOrNull { descendant -> requireActorType(descendant).properties.any { it.name == property.name && PropertyKind.of(it) != kind } }
        ?.let { Rejection(CommandProblem.PROPERTY_KIND_IN_DESCENDANT, property.name, it) }
}
