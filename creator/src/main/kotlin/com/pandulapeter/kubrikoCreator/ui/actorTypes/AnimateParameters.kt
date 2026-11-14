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
import com.pandulapeter.kubrikoCreator.actors.AnimatedValue
import com.pandulapeter.kubrikoCreator.actors.AnimationEasing
import com.pandulapeter.kubrikoCreator.actors.AnimationTarget
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.ui.components.ColorField
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_animate_easing_ease_in
import kubriko.creator.generated.resources.block_animate_easing_ease_in_out
import kubriko.creator.generated.resources.block_animate_easing_ease_out
import kubriko.creator.generated.resources.block_animate_easing_linear
import kubriko.creator.generated.resources.block_animate_removes_actor
import kubriko.creator.generated.resources.block_animate_target_color
import kubriko.creator.generated.resources.block_animate_target_opacity
import kubriko.creator.generated.resources.block_animate_target_position
import kubriko.creator.generated.resources.block_animate_target_scale
import kubriko.creator.generated.resources.block_opacity_unit
import kubriko.creator.generated.resources.block_parameter_end
import kubriko.creator.generated.resources.block_parameter_relative
import kubriko.creator.generated.resources.block_parameter_seconds
import kubriko.creator.generated.resources.block_parameter_start
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Animate Block read as a sentence: what it animates, its start and its end, how long it takes, its easing, and
 * whether it removes the Actor when it ends. Choosing another value starts from that value's defaults.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AnimateParameters(
    block: BlockDefinition.Animate,
    scope: BlockScope,
    onBlockChange: (BlockDefinition) -> Unit,
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    MenuButton(
        label = stringResource(block.value.target.title),
        options = AnimationTarget.entries,
        optionText = { stringResource(it.title) },
        onSelected = { if (it != block.value.target) onBlockChange(block.copy(value = defaultValueOf(it))) },
    )
    AnimatedValueParameters(block.value, scope) { onBlockChange(block.copy(value = it)) }
    NumberValueField(
        label = Res.string.block_parameter_seconds,
        value = block.seconds,
        scope = scope,
        onValueChange = { onBlockChange(block.copy(seconds = it)) },
        offersEdges = false,
        accepts = { it >= 0f },
        fieldWidth = SECONDS_FIELD_WIDTH,
    )
    MenuButton(
        label = stringResource(block.easing.title),
        options = AnimationEasing.entries,
        optionText = { stringResource(it.title) },
        onSelected = { onBlockChange(block.copy(easing = it)) },
    )
    LabeledSwitch(
        label = stringResource(Res.string.block_animate_removes_actor),
        checked = block.removesActor,
        onCheckedChange = { onBlockChange(block.copy(removesActor = it)) },
    )
}

/**
 * The fields of [value]: two numbers or two colors for its start and end, or where the position goes.
 */
@Composable
private fun AnimatedValueParameters(
    value: AnimatedValue,
    scope: BlockScope,
    onValueChange: (AnimatedValue) -> Unit,
) = when (value) {
    is AnimatedValue.Scale -> EndsParameters(value.from, value.to, scope, accepts = { it >= 0f }) { from, to -> onValueChange(value.copy(from = from, to = to)) }
    is AnimatedValue.Opacity -> {
        EndsParameters(value.from, value.to, scope, accepts = { it in 0f..100f }) { from, to -> onValueChange(value.copy(from = from, to = to)) }
        UnitText(Res.string.block_opacity_unit)
    }
    is AnimatedValue.Color -> {
        UnitText(Res.string.block_parameter_start)
        ColorField(color = value.from, onColorChange = { onValueChange(value.copy(from = it)) }, isAlphaEnabled = false)
        UnitText(Res.string.block_parameter_end)
        ColorField(color = value.to, onColorChange = { onValueChange(value.copy(to = it)) }, isAlphaEnabled = false)
    }
    is AnimatedValue.Position -> {
        OffsetParameters(value.x, value.y, scope, isVelocity = false) { x, y -> onValueChange(value.copy(x = x, y = y)) }
        LabeledSwitch(
            label = stringResource(Res.string.block_parameter_relative),
            checked = value.isRelative,
            onCheckedChange = { onValueChange(value.copy(isRelative = it)) },
        )
    }
}

@Composable
private fun EndsParameters(
    from: NumberValue,
    to: NumberValue,
    scope: BlockScope,
    accepts: (Float) -> Boolean,
    onChange: (from: NumberValue, to: NumberValue) -> Unit,
) {
    NumberValueField(
        label = Res.string.block_parameter_start,
        value = from,
        scope = scope,
        onValueChange = { onChange(it, to) },
        offersEdges = false,
        accepts = accepts,
    )
    NumberValueField(
        label = Res.string.block_parameter_end,
        value = to,
        scope = scope,
        onValueChange = { onChange(from, it) },
        offersEdges = false,
        accepts = accepts,
    )
}

private fun defaultValueOf(target: AnimationTarget): AnimatedValue = when (target) {
    AnimationTarget.SCALE -> AnimatedValue.Scale()
    AnimationTarget.OPACITY -> AnimatedValue.Opacity()
    AnimationTarget.COLOR -> AnimatedValue.Color()
    AnimationTarget.POSITION -> AnimatedValue.Position()
}

private val AnimationTarget.title: StringResource
    get() = when (this) {
        AnimationTarget.SCALE -> Res.string.block_animate_target_scale
        AnimationTarget.OPACITY -> Res.string.block_animate_target_opacity
        AnimationTarget.COLOR -> Res.string.block_animate_target_color
        AnimationTarget.POSITION -> Res.string.block_animate_target_position
    }

private val AnimationEasing.title: StringResource
    get() = when (this) {
        AnimationEasing.LINEAR -> Res.string.block_animate_easing_linear
        AnimationEasing.EASE_IN -> Res.string.block_animate_easing_ease_in
        AnimationEasing.EASE_OUT -> Res.string.block_animate_easing_ease_out
        AnimationEasing.EASE_IN_OUT -> Res.string.block_animate_easing_ease_in_out
    }
