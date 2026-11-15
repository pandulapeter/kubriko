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
 * What [withValues] makes of each value a Block, a condition, or an animated value takes, by its kind; each kind is left
 * as it is unless overridden. Every value field of every Block is reached through the one exhaustive `when` of
 * [withValues], so a new field, or a new kind of value, is added in one place and every reader and replacer follows.
 */
public interface ValueTransform {
    public fun number(value: NumberValue): NumberValue = value

    public fun color(value: ColorValue): ColorValue = value

    public fun actorType(value: ActorTypeValue): ActorTypeValue = value
}

/**
 * [this] Block with each value it takes, in the order its fields are declared, replaced by what [transform] makes of it.
 */
public fun BlockDefinition.withValues(transform: ValueTransform): BlockDefinition = when (this) {
    is BlockDefinition.SetVelocity -> copy(x = transform.number(x), y = transform.number(y))
    is BlockDefinition.AddToVelocity -> copy(x = transform.number(x), y = transform.number(y))
    is BlockDefinition.MoveInDirection -> copy(direction = transform.number(direction), speed = transform.number(speed))
    is BlockDefinition.MoveWithKeys -> copy(speed = transform.number(speed))
    is BlockDefinition.JumpTo -> copy(x = transform.number(x), y = transform.number(y))
    is BlockDefinition.SetNumber -> copy(value = transform.number(value))
    is BlockDefinition.ChangeNumber -> copy(by = transform.number(by))
    is BlockDefinition.SetColorProperty -> copy(value = transform.color(value))
    is BlockDefinition.AddActor -> copy(actorType = transform.actorType(actorType), x = transform.number(x), y = transform.number(y))
    is BlockDefinition.MoveTowardsNearest -> copy(actorType = transform.actorType(actorType), speed = transform.number(speed))
    is BlockDefinition.StartTimer -> copy(seconds = transform.number(seconds))
    is BlockDefinition.SetFrame -> copy(frame = transform.number(frame))
    is BlockDefinition.SetAnimation -> copy(speed = transform.number(speed))
    is BlockDefinition.SetColor -> copy(colors = colors.map(transform::color))
    is BlockDefinition.SetOpacity -> copy(percent = transform.number(percent))
    is BlockDefinition.ChangeOpacity -> copy(by = transform.number(by))
    is BlockDefinition.Animate -> copy(value = value.withValues(transform), seconds = transform.number(seconds))
    is BlockDefinition.Repeat -> copy(times = transform.number(times))
    is BlockDefinition.If -> copy(condition = condition.withValues(transform))
    BlockDefinition.Stop, BlockDefinition.RunParentEvent, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf,
    BlockDefinition.BounceOffOther, BlockDefinition.RemoveOther, is BlockDefinition.SetText, is BlockDefinition.SetToggle,
    is BlockDefinition.StopTimer, is BlockDefinition.GoToScene, BlockDefinition.RestartScene, is BlockDefinition.PauseGame,
    is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic, BlockDefinition.StopMusic, BlockDefinition.Else,
    BlockDefinition.End, is BlockDefinition.Log, is BlockDefinition.Kotlin,
    -> this
}

/**
 * [this] condition with each value it takes, those of the conditions it combines too, replaced by what [transform] makes
 * of it.
 */
public fun ConditionDefinition.withValues(transform: ValueTransform): ConditionDefinition = when (this) {
    is ConditionDefinition.Number -> copy(value = transform.number(value))
    is ConditionDefinition.Velocity -> copy(value = transform.number(value))
    is ConditionDefinition.ActorCount -> copy(value = transform.number(value))
    is ConditionDefinition.Chance -> copy(percent = transform.number(percent))
    is ConditionDefinition.Combined -> copy(conditions = conditions.map { it.withValues(transform) })
    is ConditionDefinition.Text, is ConditionDefinition.Toggle, is ConditionDefinition.KeyHeld, is ConditionDefinition.Paused -> this
}

private fun AnimatedValue.withValues(transform: ValueTransform) = when (this) {
    is AnimatedValue.Scale -> copy(from = transform.number(from), to = transform.number(to))
    is AnimatedValue.Opacity -> copy(from = transform.number(from), to = transform.number(to))
    is AnimatedValue.Position -> copy(x = transform.number(x), y = transform.number(y))
    is AnimatedValue.Color -> copy(from = transform.color(from), to = transform.color(to))
}

/**
 * Every value [withValues] reaches, by kind, in order.
 */
private class ValueCollector : ValueTransform {
    val numbers = ArrayList<NumberValue>()
    val colors = ArrayList<ColorValue>()
    val actorTypes = ArrayList<ActorTypeValue>()

    override fun number(value: NumberValue) = value.also(numbers::add)

    override fun color(value: ColorValue) = value.also(colors::add)

    override fun actorType(value: ActorTypeValue) = value.also(actorTypes::add)
}

private fun BlockDefinition.collectedValues() = ValueCollector().also { withValues(it) }

/**
 * Every number [this] Block takes, in the order its fields are declared: the same ones [withNumberValues] replaces.
 */
public val BlockDefinition.numberValues: List<NumberValue>
    get() = collectedValues().numbers

public fun BlockDefinition.withNumberValues(transform: (NumberValue) -> NumberValue): BlockDefinition = withValues(
    object : ValueTransform {
        override fun number(value: NumberValue) = transform(value)
    },
)

/**
 * Every number [this] condition takes: the same ones [withNumberValues] replaces.
 */
public val ConditionDefinition.numberValues: List<NumberValue>
    get() = ValueCollector().also { withValues(it) }.numbers

public fun ConditionDefinition.withNumberValues(transform: (NumberValue) -> NumberValue): ConditionDefinition = withValues(
    object : ValueTransform {
        override fun number(value: NumberValue) = transform(value)
    },
)

/**
 * The numbers of [this] animated value, in the order its fields are declared.
 */
public val AnimatedValue.numberValues: List<NumberValue>
    get() = ValueCollector().also { withValues(it) }.numbers

/**
 * Every color [this] Block takes, in the order its fields are declared: the same ones [withColorValues] replaces.
 */
public val BlockDefinition.colorValues: List<ColorValue>
    get() = collectedValues().colors

public fun BlockDefinition.withColorValues(transform: (ColorValue) -> ColorValue): BlockDefinition = withValues(
    object : ValueTransform {
        override fun color(value: ColorValue) = transform(value)
    },
)

/**
 * The Actor type [this] Block adds or moves towards, if it is one of those.
 */
public val BlockDefinition.actorTypeValue: ActorTypeValue?
    get() = collectedValues().actorTypes.firstOrNull()

public fun BlockDefinition.withActorTypeValues(transform: (ActorTypeValue) -> ActorTypeValue): BlockDefinition = withValues(
    object : ValueTransform {
        override fun actorType(value: ActorTypeValue) = transform(value)
    },
)

/**
 * The Properties [this] Block reads as numbers ([NumberValue.Property]), with or without a name chosen yet.
 */
public val BlockDefinition.numberPropertyReferences: List<PropertyReference>
    get() = numberValues.mapNotNull { it.propertyReference }

/**
 * The Properties [this] condition reads as numbers ([NumberValue.Property]), with or without a name chosen yet.
 */
public val ConditionDefinition.numberPropertyReferences: List<PropertyReference>
    get() = numberValues.mapNotNull { it.propertyReference }

/**
 * The color Properties [this] Block reads as colors ([ColorValue.Property]), with or without a name chosen yet.
 */
public val BlockDefinition.colorPropertyReferences: List<PropertyReference>
    get() = colorValues.mapNotNull { it.propertyReference }

/**
 * The Properties [this] Block reads as values of any kind, with or without a name chosen yet.
 */
public val BlockDefinition.valuePropertyReferences: List<PropertyReference>
    get() = collectedValues().let { values ->
        values.numbers.mapNotNull { it.propertyReference } + values.colors.mapNotNull { it.propertyReference } + values.actorTypes.mapNotNull { it.propertyReference }
    }

/**
 * What every value a Block or condition takes becomes once the Property [oldName] of [manager] (the type's own one, if
 * null) is called [newName].
 */
internal fun propertyRenaming(oldName: String, newName: String, manager: String?): ValueTransform = object : ValueTransform {
    override fun number(value: NumberValue) = value.withPropertyRenamed(oldName, newName, manager)

    override fun color(value: ColorValue) = value.withPropertyRenamed(oldName, newName, manager)

    override fun actorType(value: ActorTypeValue) = value.withPropertyRenamed(oldName, newName, manager)
}

/**
 * What every value a Block or condition takes becomes once the Manager type [oldName] is called [newName].
 */
internal fun managerRenaming(oldName: String, newName: String): ValueTransform = object : ValueTransform {
    override fun number(value: NumberValue) = value.withManagerRenamed(oldName, newName)

    override fun color(value: ColorValue) = value.withManagerRenamed(oldName, newName)

    override fun actorType(value: ActorTypeValue) = value.withManagerRenamed(oldName, newName)
}
