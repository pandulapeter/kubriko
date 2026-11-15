/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.Axis
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.Comparison
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionKind
import com.pandulapeter.kubrikoCreator.actors.NumberLimits
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.PropertyReference
import com.pandulapeter.kubrikoCreator.actors.ToggleValue
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_parameter_value
import kubriko.creator.generated.resources.condition_axis_horizontal
import kubriko.creator.generated.resources.condition_axis_vertical
import kubriko.creator.generated.resources.condition_comparison_greater
import kubriko.creator.generated.resources.condition_comparison_greater_or_equal
import kubriko.creator.generated.resources.condition_comparison_less
import kubriko.creator.generated.resources.condition_comparison_less_or_equal
import kubriko.creator.generated.resources.condition_is
import kubriko.creator.generated.resources.condition_is_as
import kubriko.creator.generated.resources.condition_is_held
import kubriko.creator.generated.resources.condition_is_not
import kubriko.creator.generated.resources.condition_is_not_held
import kubriko.creator.generated.resources.condition_is_not_paused
import kubriko.creator.generated.resources.condition_is_off
import kubriko.creator.generated.resources.condition_is_on
import kubriko.creator.generated.resources.condition_is_paused
import kubriko.creator.generated.resources.condition_percent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * What an if Block checks, or what a text is shown while, read as a sentence: the kind of condition, then its parts.
 *
 * @param isForText True for the condition a Manager type's text is shown while, which offers only the conditions and
 *   numbers a text can follow ([com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition.isShownWhenAllowed]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConditionParameters(
    condition: ConditionDefinition,
    scope: BlockScope,
    isForText: Boolean = false,
    onConditionChange: (ConditionDefinition) -> Unit,
): Unit = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    // A combined condition's list starts beside its kind, rather than the kind floating beside the list's middle.
    itemVerticalAlignment = if (condition is ConditionDefinition.Combined) Alignment.Top else Alignment.CenterVertically,
) {
    MenuButton(
        label = stringResource(ConditionKind.of(condition).title),
        options = ConditionKind.entries.filter { it.isAvailableIn(scope.kind) && (!isForText || it.isAvailableForTexts) },
        optionText = { stringResource(it.title) },
        onSelected = { kind ->
            when {
                kind == ConditionKind.of(condition) -> Unit
                // Combining keeps the condition there is, as the first of those combined.
                kind == ConditionKind.COMBINED -> onConditionChange(ConditionDefinition.Combined(listOf(condition, ConditionDefinition.Toggle())))
                else -> onConditionChange(kind.createDefault())
            }
        },
    )
    when (condition) {
        is ConditionDefinition.Number -> {
            PropertyChoice(PropertyReference(condition.property, PropertyKind.NUMBER, condition.manager), scope) { onConditionChange(condition.copy(property = it.name, manager = it.manager)) }
            ComparisonChoice(condition.comparison) { onConditionChange(condition.copy(comparison = it)) }
            NumberValueField(
                label = Res.string.block_parameter_value,
                value = condition.value,
                scope = scope,
                onValueChange = { onConditionChange(condition.copy(value = it)) },
                offersEdges = !isForText,
                offersRandom = !isForText,
            )
        }
        is ConditionDefinition.Text -> {
            PropertyChoice(PropertyReference(condition.property, PropertyKind.TEXT, condition.manager), scope) { onConditionChange(condition.copy(property = it.name, manager = it.manager)) }
            YesNoChoice(condition.isEqual, Res.string.condition_is, Res.string.condition_is_not) { onConditionChange(condition.copy(isEqual = it)) }
            TextValueField(condition.value, scope, fieldWidth = 140.dp) { onConditionChange(condition.copy(value = it)) }
        }
        is ConditionDefinition.Toggle -> {
            PropertyChoice(PropertyReference(condition.property, PropertyKind.TOGGLE, condition.manager), scope) { onConditionChange(condition.copy(property = it.name, manager = it.manager)) }
            if (condition.isOn is ToggleValue.Property) UnitText(Res.string.condition_is_as)
            ToggleValueField(condition.isOn, scope, onValueChange = { onConditionChange(condition.copy(isOn = it)) }) { isOn ->
                YesNoChoice(isOn, Res.string.condition_is_on, Res.string.condition_is_off) { onConditionChange(condition.copy(isOn = ToggleValue.Fixed(it))) }
            }
        }
        is ConditionDefinition.KeyHeld -> {
            KeyChoiceButton(
                key = condition.key,
                onSelected = { onConditionChange(condition.copy(key = it)) },
            )
            YesNoChoice(condition.isHeld, Res.string.condition_is_held, Res.string.condition_is_not_held) { onConditionChange(condition.copy(isHeld = it)) }
        }
        is ConditionDefinition.Velocity -> {
            MenuButton(
                label = stringResource(condition.axis.title),
                options = Axis.entries,
                optionText = { stringResource(it.title) },
                onSelected = { onConditionChange(condition.copy(axis = it)) },
            )
            ComparisonChoice(condition.comparison) { onConditionChange(condition.copy(comparison = it)) }
            NumberValueField(
                label = Res.string.block_parameter_value,
                value = condition.value,
                scope = scope,
                onValueChange = { onConditionChange(condition.copy(value = it)) },
            )
        }
        is ConditionDefinition.Distance -> {
            ActorTypeValueField(condition.actorType, scope) { onConditionChange(condition.copy(actorType = it)) }
            ComparisonChoice(condition.comparison) { onConditionChange(condition.copy(comparison = it)) }
            NumberValueField(
                label = Res.string.block_parameter_value,
                value = condition.value,
                scope = scope,
                onValueChange = { onConditionChange(condition.copy(value = it)) },
                offersEdges = false,
            )
        }
        is ConditionDefinition.ActorCount -> {
            ActorTypeChoice(
                actorType = condition.actorType,
                actorTypeNames = scope.actorTypeNames,
                onSelected = { onConditionChange(condition.copy(actorType = it)) },
            )
            ComparisonChoice(condition.comparison) { onConditionChange(condition.copy(comparison = it)) }
            NumberValueField(
                label = Res.string.block_parameter_value,
                value = condition.value,
                scope = scope,
                onValueChange = { onConditionChange(condition.copy(value = it)) },
                offersEdges = false,
            )
        }
        is ConditionDefinition.Chance -> NumberValueField(
            label = Res.string.condition_percent,
            value = condition.percent,
            scope = scope,
            onValueChange = { onConditionChange(condition.copy(percent = it)) },
            limits = NumberLimits.PERCENT,
        )
        is ConditionDefinition.Combined -> CombinedConditionParameters(condition, scope, isForText, onConditionChange)
        is ConditionDefinition.Paused -> YesNoChoice(condition.isPaused, Res.string.condition_is_paused, Res.string.condition_is_not_paused) { onConditionChange(condition.copy(isPaused = it)) }
    }
}

@Composable
private fun ComparisonChoice(
    comparison: Comparison,
    onSelected: (Comparison) -> Unit,
) = MenuButton(
    label = stringResource(comparison.title),
    options = Comparison.entries,
    optionText = { stringResource(it.title) },
    onSelected = onSelected,
)

@Composable
private fun YesNoChoice(
    value: Boolean,
    yes: StringResource,
    no: StringResource,
    onSelected: (Boolean) -> Unit,
) = MenuButton(
    label = stringResource(if (value) yes else no),
    options = listOf(true, false),
    optionText = { stringResource(if (it) yes else no) },
    onSelected = onSelected,
)

private val Comparison.title
    get() = when (this) {
        Comparison.LESS -> Res.string.condition_comparison_less
        Comparison.LESS_OR_EQUAL -> Res.string.condition_comparison_less_or_equal
        Comparison.EQUAL -> Res.string.condition_is
        Comparison.NOT_EQUAL -> Res.string.condition_is_not
        Comparison.GREATER_OR_EQUAL -> Res.string.condition_comparison_greater_or_equal
        Comparison.GREATER -> Res.string.condition_comparison_greater
    }

private val Axis.title
    get() = when (this) {
        Axis.HORIZONTAL -> Res.string.condition_axis_horizontal
        Axis.VERTICAL -> Res.string.condition_axis_vertical
    }
