/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * The Property [this] Block changes (or checks, in an if Block), if it uses one.
 */
public val BlockDefinition.propertyReference: PropertyReference?
    get() = when (this) {
        is BlockDefinition.SetNumber -> PropertyReference(property, PropertyKind.NUMBER, manager)
        is BlockDefinition.ChangeNumber -> PropertyReference(property, PropertyKind.NUMBER, manager)
        is BlockDefinition.SetText -> PropertyReference(property, PropertyKind.TEXT, manager)
        is BlockDefinition.SetToggle -> PropertyReference(property, PropertyKind.TOGGLE, manager)
        is BlockDefinition.If -> when (val condition = condition) {
            is ConditionDefinition.Number -> PropertyReference(condition.property, PropertyKind.NUMBER, condition.manager)
            is ConditionDefinition.Text -> PropertyReference(condition.property, PropertyKind.TEXT, condition.manager)
            is ConditionDefinition.Toggle -> PropertyReference(condition.property, PropertyKind.TOGGLE, condition.manager)
            is ConditionDefinition.KeyHeld, is ConditionDefinition.Velocity, is ConditionDefinition.Chance, is ConditionDefinition.Paused -> null
        }
        is BlockDefinition.SetVelocity, is BlockDefinition.AddToVelocity, is BlockDefinition.MoveInDirection, BlockDefinition.Stop, BlockDefinition.RunParentEvent,
        is BlockDefinition.JumpTo, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther,
        BlockDefinition.RemoveOther, is BlockDefinition.AddActor, is BlockDefinition.MoveTowardsNearest,
        is BlockDefinition.StartTimer, is BlockDefinition.StopTimer, is BlockDefinition.GoToScene, BlockDefinition.RestartScene,
        is BlockDefinition.PauseGame, is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic, BlockDefinition.StopMusic, is BlockDefinition.SetFrame, is BlockDefinition.SetAnimation,
        is BlockDefinition.SetColor, is BlockDefinition.SetOpacity, is BlockDefinition.ChangeOpacity, is BlockDefinition.Animate,
        BlockDefinition.Else, is BlockDefinition.Repeat, BlockDefinition.End, is BlockDefinition.Log, is BlockDefinition.Kotlin,
        -> null
    }

/**
 * The Actor type [this] Block adds or moves towards, if it is one of those and one is chosen.
 */
public val BlockDefinition.actorTypeReference: String?
    get() = when (this) {
        is BlockDefinition.AddActor -> actorType
        is BlockDefinition.MoveTowardsNearest -> actorType
        is BlockDefinition.SetVelocity, is BlockDefinition.AddToVelocity, is BlockDefinition.MoveInDirection, BlockDefinition.Stop, BlockDefinition.RunParentEvent,
        is BlockDefinition.JumpTo, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther,
        BlockDefinition.RemoveOther, is BlockDefinition.SetNumber, is BlockDefinition.ChangeNumber, is BlockDefinition.SetText,
        is BlockDefinition.SetToggle, is BlockDefinition.StartTimer, is BlockDefinition.StopTimer, is BlockDefinition.GoToScene,
        BlockDefinition.RestartScene, is BlockDefinition.PauseGame, is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic,
        BlockDefinition.StopMusic, is BlockDefinition.SetFrame, is BlockDefinition.SetAnimation, is BlockDefinition.SetColor, is BlockDefinition.SetOpacity, is BlockDefinition.ChangeOpacity, is BlockDefinition.Animate, is BlockDefinition.If, BlockDefinition.Else, is BlockDefinition.Repeat, BlockDefinition.End,
        is BlockDefinition.Log, is BlockDefinition.Kotlin,
        -> null
    }?.ifEmpty { null }

/**
 * [this] Block with the Actor type it adds or moves towards renamed from [oldName] to [newName], if it is that one.
 */
internal fun BlockDefinition.withActorTypeRenamed(oldName: String, newName: String) = when (this) {
    is BlockDefinition.AddActor -> if (actorType == oldName) copy(actorType = newName) else this
    is BlockDefinition.MoveTowardsNearest -> if (actorType == oldName) copy(actorType = newName) else this
    is BlockDefinition.SetVelocity, is BlockDefinition.AddToVelocity, is BlockDefinition.MoveInDirection, BlockDefinition.Stop, BlockDefinition.RunParentEvent,
    is BlockDefinition.JumpTo, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther,
    BlockDefinition.RemoveOther, is BlockDefinition.SetNumber, is BlockDefinition.ChangeNumber, is BlockDefinition.SetText,
    is BlockDefinition.SetToggle, is BlockDefinition.StartTimer, is BlockDefinition.StopTimer, is BlockDefinition.GoToScene,
    BlockDefinition.RestartScene, is BlockDefinition.PauseGame, is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic,
    BlockDefinition.StopMusic, is BlockDefinition.SetFrame, is BlockDefinition.SetAnimation, is BlockDefinition.SetColor, is BlockDefinition.SetOpacity, is BlockDefinition.ChangeOpacity, is BlockDefinition.Animate, is BlockDefinition.If, BlockDefinition.Else, is BlockDefinition.Repeat, BlockDefinition.End,
    is BlockDefinition.Log, is BlockDefinition.Kotlin,
    -> this
}

/**
 * The Timer [this] Block starts or stops, if it is one of those.
 */
public val BlockDefinition.timerName: String?
    get() = when (this) {
        is BlockDefinition.StartTimer -> timer
        is BlockDefinition.StopTimer -> timer
        is BlockDefinition.SetVelocity, is BlockDefinition.AddToVelocity, is BlockDefinition.MoveInDirection, BlockDefinition.Stop, BlockDefinition.RunParentEvent,
        is BlockDefinition.JumpTo, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther,
        BlockDefinition.RemoveOther, is BlockDefinition.SetNumber, is BlockDefinition.ChangeNumber, is BlockDefinition.SetText,
        is BlockDefinition.SetToggle, is BlockDefinition.AddActor, is BlockDefinition.MoveTowardsNearest,
        is BlockDefinition.GoToScene, BlockDefinition.RestartScene, is BlockDefinition.PauseGame, is BlockDefinition.PlaySound,
        is BlockDefinition.PlayMusic, BlockDefinition.StopMusic, is BlockDefinition.SetFrame, is BlockDefinition.SetAnimation, is BlockDefinition.SetColor, is BlockDefinition.SetOpacity, is BlockDefinition.ChangeOpacity, is BlockDefinition.Animate, is BlockDefinition.If, BlockDefinition.Else,
        is BlockDefinition.Repeat, BlockDefinition.End, is BlockDefinition.Log, is BlockDefinition.Kotlin,
        -> null
    }

/**
 * [this] Block with the Timer it starts or stops renamed from [oldName] to [newName], if it is that one.
 */
public fun BlockDefinition.withTimerRenamed(oldName: String, newName: String): BlockDefinition = when (this) {
    is BlockDefinition.StartTimer -> if (timer == oldName) copy(timer = newName) else this
    is BlockDefinition.StopTimer -> if (timer == oldName) copy(timer = newName) else this
    is BlockDefinition.SetVelocity, is BlockDefinition.AddToVelocity, is BlockDefinition.MoveInDirection, BlockDefinition.Stop, BlockDefinition.RunParentEvent,
    is BlockDefinition.JumpTo, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther,
    BlockDefinition.RemoveOther, is BlockDefinition.SetNumber, is BlockDefinition.ChangeNumber, is BlockDefinition.SetText,
    is BlockDefinition.SetToggle, is BlockDefinition.AddActor, is BlockDefinition.MoveTowardsNearest, is BlockDefinition.GoToScene,
    BlockDefinition.RestartScene, is BlockDefinition.PauseGame, is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic,
    BlockDefinition.StopMusic, is BlockDefinition.SetFrame, is BlockDefinition.SetAnimation, is BlockDefinition.SetColor, is BlockDefinition.SetOpacity, is BlockDefinition.ChangeOpacity, is BlockDefinition.Animate, is BlockDefinition.If, BlockDefinition.Else, is BlockDefinition.Repeat, BlockDefinition.End,
    is BlockDefinition.Log, is BlockDefinition.Kotlin,
    -> this
}

/**
 * [this] Block with the Property of [manager] (the type's own one, if null) that it changes, reads as a number, or shows
 * in a Log Block, renamed from [oldName] to [newName].
 */
internal fun BlockDefinition.withPropertyRenamed(oldName: String, newName: String, manager: String? = null) = when {
    propertyReference?.let { it.manager == manager && it.name == oldName } == true -> withPropertyReference(manager, newName)
    this is BlockDefinition.Log -> copy(message = PropertyPlaceholders.withPropertyRenamed(message, manager, oldName, newName))
    else -> this
}.withNumberValues { value ->
    if (value is NumberValue.Property && value.manager == manager && value.property == oldName) value.copy(property = newName) else value
}

/**
 * [this] Block with the Properties of the Manager type [oldName] it uses (or shows, in a Log Block) taken from [newName]
 * instead.
 */
internal fun BlockDefinition.withManagerRenamed(oldName: String, newName: String) = when {
    propertyReference?.manager == oldName -> withPropertyReference(newName, propertyReference!!.name)
    this is BlockDefinition.Log -> copy(message = PropertyPlaceholders.withManagerRenamed(message, oldName, newName))
    else -> this
}.withNumberValues { value -> if (value is NumberValue.Property && value.manager == oldName) value.copy(manager = newName) else value }

/**
 * [this] Block using the Property called [name] of [manager] (the type's own one, if null) instead, if it is one that
 * uses a Property.
 */
public fun BlockDefinition.withPropertyReference(manager: String?, name: String): BlockDefinition = when (this) {
    is BlockDefinition.SetNumber -> copy(manager = manager, property = name)
    is BlockDefinition.ChangeNumber -> copy(manager = manager, property = name)
    is BlockDefinition.SetText -> copy(manager = manager, property = name)
    is BlockDefinition.SetToggle -> copy(manager = manager, property = name)
    is BlockDefinition.If -> when (val condition = condition) {
        is ConditionDefinition.Number -> copy(condition = condition.copy(manager = manager, property = name))
        is ConditionDefinition.Text -> copy(condition = condition.copy(manager = manager, property = name))
        is ConditionDefinition.Toggle -> copy(condition = condition.copy(manager = manager, property = name))
        is ConditionDefinition.KeyHeld, is ConditionDefinition.Velocity, is ConditionDefinition.Chance, is ConditionDefinition.Paused -> this
    }
    is BlockDefinition.SetVelocity, is BlockDefinition.AddToVelocity, is BlockDefinition.MoveInDirection, BlockDefinition.Stop, BlockDefinition.RunParentEvent,
    is BlockDefinition.JumpTo, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther,
    BlockDefinition.RemoveOther, is BlockDefinition.AddActor, is BlockDefinition.MoveTowardsNearest, is BlockDefinition.StartTimer,
    is BlockDefinition.StopTimer, is BlockDefinition.GoToScene, BlockDefinition.RestartScene, is BlockDefinition.PauseGame,
    is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic, BlockDefinition.StopMusic, is BlockDefinition.SetFrame, is BlockDefinition.SetAnimation, is BlockDefinition.SetColor, is BlockDefinition.SetOpacity, is BlockDefinition.ChangeOpacity, is BlockDefinition.Animate, BlockDefinition.Else,
    is BlockDefinition.Repeat, BlockDefinition.End, is BlockDefinition.Log, is BlockDefinition.Kotlin,
    -> this
}

/**
 * The sound or music file [this] Block plays, if it is one of those and one is chosen.
 */
internal val BlockDefinition.audioFile
    get() = when (this) {
        is BlockDefinition.PlaySound -> sound
        is BlockDefinition.PlayMusic -> music
        is BlockDefinition.SetVelocity, is BlockDefinition.AddToVelocity, is BlockDefinition.MoveInDirection, BlockDefinition.Stop, BlockDefinition.RunParentEvent,
        is BlockDefinition.JumpTo, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther,
        BlockDefinition.RemoveOther, is BlockDefinition.SetNumber, is BlockDefinition.ChangeNumber, is BlockDefinition.SetText,
        is BlockDefinition.SetToggle, is BlockDefinition.AddActor, is BlockDefinition.MoveTowardsNearest,
        is BlockDefinition.StartTimer, is BlockDefinition.StopTimer, is BlockDefinition.GoToScene, BlockDefinition.RestartScene,
        is BlockDefinition.PauseGame, BlockDefinition.StopMusic, is BlockDefinition.SetFrame, is BlockDefinition.SetAnimation, is BlockDefinition.SetColor, is BlockDefinition.SetOpacity, is BlockDefinition.ChangeOpacity, is BlockDefinition.Animate, is BlockDefinition.If, BlockDefinition.Else,
        is BlockDefinition.Repeat, BlockDefinition.End, is BlockDefinition.Log, is BlockDefinition.Kotlin,
        -> null
    }?.ifEmpty { null }

/**
 * Every number [this] Block takes, in the order its fields are declared: the same ones [withNumberValues] replaces, read
 * without copying the Block.
 */
public val BlockDefinition.numberValues: List<NumberValue>
    get() = when (this) {
        is BlockDefinition.SetVelocity -> listOf(x, y)
        is BlockDefinition.AddToVelocity -> listOf(x, y)
        is BlockDefinition.MoveInDirection -> listOf(direction, speed)
        is BlockDefinition.JumpTo -> listOf(x, y)
        is BlockDefinition.SetNumber -> listOf(value)
        is BlockDefinition.ChangeNumber -> listOf(by)
        is BlockDefinition.AddActor -> listOf(x, y)
        is BlockDefinition.MoveTowardsNearest -> listOf(speed)
        is BlockDefinition.StartTimer -> listOf(seconds)
        is BlockDefinition.SetFrame -> listOf(frame)
        is BlockDefinition.SetAnimation -> listOf(speed)
        is BlockDefinition.SetOpacity -> listOf(percent)
        is BlockDefinition.ChangeOpacity -> listOf(by)
        is BlockDefinition.Animate -> value.numberValues + seconds
        is BlockDefinition.Repeat -> listOf(times)
        is BlockDefinition.If -> when (val condition = condition) {
            is ConditionDefinition.Number -> listOf(condition.value)
            is ConditionDefinition.Velocity -> listOf(condition.value)
            is ConditionDefinition.Chance -> listOf(condition.percent)
            is ConditionDefinition.Text, is ConditionDefinition.Toggle, is ConditionDefinition.KeyHeld, is ConditionDefinition.Paused -> emptyList()
        }
        BlockDefinition.Stop, BlockDefinition.RunParentEvent, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf,
        BlockDefinition.BounceOffOther, BlockDefinition.RemoveOther, is BlockDefinition.SetText, is BlockDefinition.SetToggle,
        is BlockDefinition.StopTimer, is BlockDefinition.GoToScene, BlockDefinition.RestartScene, is BlockDefinition.PauseGame,
        is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic, BlockDefinition.StopMusic, is BlockDefinition.SetColor, BlockDefinition.Else,
        BlockDefinition.End, is BlockDefinition.Log, is BlockDefinition.Kotlin,
        -> emptyList()
    }

/**
 * [this] Block with each number it takes replaced by what [transform] makes of it.
 */
public fun BlockDefinition.withNumberValues(transform: (NumberValue) -> NumberValue): BlockDefinition = when (this) {
    is BlockDefinition.SetVelocity -> copy(x = transform(x), y = transform(y))
    is BlockDefinition.AddToVelocity -> copy(x = transform(x), y = transform(y))
    is BlockDefinition.MoveInDirection -> copy(direction = transform(direction), speed = transform(speed))
    is BlockDefinition.JumpTo -> copy(x = transform(x), y = transform(y))
    is BlockDefinition.SetNumber -> copy(value = transform(value))
    is BlockDefinition.ChangeNumber -> copy(by = transform(by))
    is BlockDefinition.AddActor -> copy(x = transform(x), y = transform(y))
    is BlockDefinition.MoveTowardsNearest -> copy(speed = transform(speed))
    is BlockDefinition.StartTimer -> copy(seconds = transform(seconds))
    is BlockDefinition.SetFrame -> copy(frame = transform(frame))
    is BlockDefinition.SetAnimation -> copy(speed = transform(speed))
    is BlockDefinition.SetOpacity -> copy(percent = transform(percent))
    is BlockDefinition.ChangeOpacity -> copy(by = transform(by))
    is BlockDefinition.Animate -> copy(value = value.withNumberValues(transform), seconds = transform(seconds))
    is BlockDefinition.Repeat -> copy(times = transform(times))
    is BlockDefinition.If -> copy(condition = condition.withNumberValues(transform))
    BlockDefinition.Stop, BlockDefinition.RunParentEvent, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf,
    BlockDefinition.BounceOffOther, BlockDefinition.RemoveOther, is BlockDefinition.SetText, is BlockDefinition.SetToggle,
    is BlockDefinition.StopTimer, is BlockDefinition.GoToScene, BlockDefinition.RestartScene, is BlockDefinition.PauseGame,
    is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic, BlockDefinition.StopMusic, is BlockDefinition.SetColor, BlockDefinition.Else,
    BlockDefinition.End, is BlockDefinition.Log, is BlockDefinition.Kotlin,
    -> this
}

private fun ConditionDefinition.withNumberValues(transform: (NumberValue) -> NumberValue) = when (this) {
    is ConditionDefinition.Number -> copy(value = transform(value))
    is ConditionDefinition.Velocity -> copy(value = transform(value))
    is ConditionDefinition.Chance -> copy(percent = transform(percent))
    is ConditionDefinition.Text, is ConditionDefinition.Toggle, is ConditionDefinition.KeyHeld, is ConditionDefinition.Paused -> this
}

/**
 * The numbers of [this] animated value, in the order its fields are declared.
 */
public val AnimatedValue.numberValues: List<NumberValue>
    get() = when (this) {
        is AnimatedValue.Scale -> listOf(from, to)
        is AnimatedValue.Opacity -> listOf(from, to)
        is AnimatedValue.Position -> listOf(x, y)
        is AnimatedValue.Color -> emptyList()
    }

private fun AnimatedValue.withNumberValues(transform: (NumberValue) -> NumberValue) = when (this) {
    is AnimatedValue.Scale -> copy(from = transform(from), to = transform(to))
    is AnimatedValue.Opacity -> copy(from = transform(from), to = transform(to))
    is AnimatedValue.Position -> copy(x = transform(x), y = transform(y))
    is AnimatedValue.Color -> this
}

/**
 * The Properties [this] Block reads as numbers ([NumberValue.Property]), with or without a name chosen yet.
 */
public val BlockDefinition.numberPropertyReferences: List<PropertyReference>
    get() = numberValues.mapNotNull { it.propertyReference }
