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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.Axis
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PauseAction
import com.pandulapeter.kubrikoCreator.ui.components.BufferedTextField
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import com.pandulapeter.kubrikoCreator.ui.components.RightClickMenu
import com.pandulapeter.kubrikoCreator.ui.components.accentEdge
import com.pandulapeter.kubrikoCreator.ui.components.flashBackground
import com.pandulapeter.kubrikoCreator.ui.components.hoverReveal
import com.pandulapeter.kubrikoCreator.ui.components.rememberHoverRevealState
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import com.pandulapeter.kubrikoCreator.ui.components.revealedWhen
import com.pandulapeter.kubrikoCreator.ui.components.rightClickMenu
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_drag_block
import kubriko.creator.generated.resources.action_remove
import kubriko.creator.generated.resources.audio_music_none_yet
import kubriko.creator.generated.resources.audio_sounds_none_yet
import kubriko.creator.generated.resources.block_else_stray
import kubriko.creator.generated.resources.block_end_stray
import kubriko.creator.generated.resources.block_music_choose
import kubriko.creator.generated.resources.block_parameter_by
import kubriko.creator.generated.resources.block_parameter_code
import kubriko.creator.generated.resources.block_parameter_looping
import kubriko.creator.generated.resources.block_parameter_message
import kubriko.creator.generated.resources.block_parameter_opacity
import kubriko.creator.generated.resources.block_parameter_relative
import kubriko.creator.generated.resources.block_parameter_repeating
import kubriko.creator.generated.resources.block_parameter_seconds
import kubriko.creator.generated.resources.block_parameter_speed
import kubriko.creator.generated.resources.block_parameter_times
import kubriko.creator.generated.resources.block_parameter_x
import kubriko.creator.generated.resources.block_parameter_y
import kubriko.creator.generated.resources.block_sound_choose
import kubriko.creator.generated.resources.block_velocity_unit
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_drag_handle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * One Block of an Event: its icon and name with the buttons that rearrange it, and its parameters under them.
 */
@Composable
internal fun BlockRow(
    block: BlockDefinition,
    nesting: BlockNesting,
    scope: BlockScope,
    index: Int,
    dragState: BlockDragState,
    onBlockChange: (BlockDefinition) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    flashKey: Any? = null,
) {
    val hover = rememberHoverRevealState()
    val menu = rememberRightClickMenuState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (nesting.depth * NESTING_INDENT).dp)
            .alpha(if ((dragState.dragged as? DraggedBlock.Existing)?.index == index) 0.4f else 1f)
            .clip(MaterialTheme.shapes.small)
            .background(if (nesting.role == BlockNesting.Role.PLAIN) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .accentEdge(BlockKind.of(block).category.accent.color)
            .flashBackground(flashKey, BlockKind.of(block).category.accent.color)
            .hoverReveal(hover)
            .rightClickMenu(menu)
            .padding(start = 8.dp, top = 2.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val kind = BlockKind.of(block)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(18.dp).listDragSource(dragState) { DraggedBlock.Existing(index, block) },
                painter = painterResource(Res.drawable.ic_drag_handle),
                contentDescription = stringResource(Res.string.action_drag_block),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(kind.icon),
                contentDescription = null,
                tint = kind.category.accent.color,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(kind.title),
                style = MaterialTheme.typography.bodyMedium,
            )
            Box {
                IconActionButton(
                    modifier = Modifier.revealedWhen(hover.isActive),
                    icon = Res.drawable.ic_close,
                    label = Res.string.action_remove,
                    onClick = onRemove,
                )
                RightClickMenu(menu, listOf(ContextAction(stringResource(Res.string.action_remove), Res.drawable.ic_close, isDestructive = true, onClick = onRemove)))
            }
        }
        if (kind.hasInlineParameters) {
            Box(
                modifier = Modifier.padding(start = 46.dp, end = 8.dp),
            ) {
                InlineParameters(block, scope, onBlockChange)
            }
        }
        if (nesting.role == BlockNesting.Role.STRAY) {
            Text(
                modifier = Modifier.padding(start = 46.dp, bottom = 4.dp),
                text = stringResource(if (block == BlockDefinition.Else) Res.string.block_else_stray else Res.string.block_end_stray),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (block is BlockDefinition.Kotlin) {
            BufferedTextField(
                modifier = Modifier.fillMaxWidth().padding(start = 46.dp, end = 8.dp, bottom = 4.dp).heightIn(min = 72.dp),
                value = block.code,
                onValueChange = { onBlockChange(block.copy(code = it)) },
                placeholder = stringResource(Res.string.block_parameter_code),
                isSingleLine = false,
                isWrapping = false,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineParameters(
    block: BlockDefinition,
    scope: BlockScope,
    onBlockChange: (BlockDefinition) -> Unit,
) = when (block) {
    is BlockDefinition.AddActor -> FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        ActorTypeChoice(block.actorType, scope.actorTypeNames) { onBlockChange(block.copy(actorType = it)) }
        OffsetParameters(block.x, block.y, scope, isVelocity = false) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
        if (scope.kind == BlockTypeKind.ACTOR) {
            LabeledSwitch(
                label = stringResource(Res.string.block_parameter_relative),
                checked = block.isRelative,
                onCheckedChange = { onBlockChange(block.copy(isRelative = it)) },
            )
        }
    }
    is BlockDefinition.MoveInDirection -> MoveInDirectionParameters(block, scope, onBlockChange)
    is BlockDefinition.MoveTowardsNearest -> FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        ActorTypeChoice(block.actorType, scope.actorTypeNames) { onBlockChange(block.copy(actorType = it)) }
        NumberValueField(
            label = Res.string.block_parameter_speed,
            value = block.speed,
            scope = scope,
            onValueChange = { onBlockChange(block.copy(speed = it)) },
            offersEdges = false,
        )
        UnitText(Res.string.block_velocity_unit)
    }
    is BlockDefinition.SetNumber, is BlockDefinition.ChangeNumber, is BlockDefinition.SetText, is BlockDefinition.SetToggle -> PropertyBlockParameters(block, scope, onBlockChange)
    is BlockDefinition.StartTimer -> FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        TimerChoice(block.timer, scope.timerNames, { onBlockChange(block.copy(timer = it)) })
        NumberValueField(
            label = Res.string.block_parameter_seconds,
            value = block.seconds,
            scope = scope,
            onValueChange = { onBlockChange(block.copy(seconds = it)) },
            accepts = { it >= 0f },
        )
        LabeledSwitch(
            label = stringResource(Res.string.block_parameter_repeating),
            checked = block.isRepeating,
            onCheckedChange = { onBlockChange(block.copy(isRepeating = it)) },
        )
    }
    is BlockDefinition.StopTimer -> TimerChoice(block.timer, scope.timerNames, { onBlockChange(block.copy(timer = it)) })
    is BlockDefinition.GoToScene -> SceneChoice(block.scene, scope.sceneNames) { onBlockChange(block.copy(scene = it)) }
    is BlockDefinition.PlaySound -> AudioFileChoice(block.sound, scope.audioFiles?.sounds, Res.string.block_sound_choose, Res.string.audio_sounds_none_yet) { onBlockChange(block.copy(sound = it)) }
    is BlockDefinition.PlayMusic -> FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        AudioFileChoice(block.music, scope.audioFiles?.music, Res.string.block_music_choose, Res.string.audio_music_none_yet) { onBlockChange(block.copy(music = it)) }
        LabeledSwitch(
            label = stringResource(Res.string.block_parameter_looping),
            checked = block.isLooping,
            onCheckedChange = { onBlockChange(block.copy(isLooping = it)) },
        )
    }
    is BlockDefinition.PauseGame -> MenuButton(
        label = stringResource(block.action.title),
        options = PauseAction.entries,
        optionText = { stringResource(it.title) },
        onSelected = { onBlockChange(block.copy(action = it)) },
    )
    is BlockDefinition.If -> ConditionParameters(block.condition, scope) { onBlockChange(block.copy(condition = it)) }
    is BlockDefinition.Repeat -> NumberValueField(
        label = Res.string.block_parameter_times,
        value = block.times,
        scope = scope,
        onValueChange = { onBlockChange(block.copy(times = it)) },
        isWhole = true,
        accepts = { it >= 0f },
    )
    is BlockDefinition.SetVelocity -> OffsetParameters(block.x, block.y, scope, isVelocity = true) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
    is BlockDefinition.AddToVelocity -> OffsetParameters(block.x, block.y, scope, isVelocity = true) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
    is BlockDefinition.JumpTo -> OffsetParameters(block.x, block.y, scope, isVelocity = false) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
    is BlockDefinition.SetFrame, is BlockDefinition.SetAnimation -> AnimationBlockParameters(block, scope, onBlockChange)
    is BlockDefinition.SetColor -> SetColorParameters(block, onBlockChange)
    is BlockDefinition.SetOpacity -> OpacityParameters(Res.string.block_parameter_opacity, block.percent, scope, { onBlockChange(block.copy(percent = it)) }, accepts = { it in 0f..100f })
    is BlockDefinition.ChangeOpacity -> OpacityParameters(Res.string.block_parameter_by, block.by, scope, { onBlockChange(block.copy(by = it)) })
    is BlockDefinition.Log -> BufferedTextField(
        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
        value = block.message,
        onValueChange = { onBlockChange(block.copy(message = it)) },
        placeholder = stringResource(Res.string.block_parameter_message),
    )
    BlockDefinition.Stop, BlockDefinition.RunParentEvent, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther, BlockDefinition.RemoveOther,
    BlockDefinition.Else, BlockDefinition.End, is BlockDefinition.Kotlin, BlockDefinition.RestartScene, BlockDefinition.StopMusic -> Unit
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OffsetParameters(
    x: NumberValue,
    y: NumberValue,
    scope: BlockScope,
    isVelocity: Boolean,
    onChange: (x: NumberValue, y: NumberValue) -> Unit,
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    NumberValueField(
        label = Res.string.block_parameter_x,
        value = x,
        scope = scope,
        axis = Axis.HORIZONTAL,
        onValueChange = { onChange(it, y) },
    )
    NumberValueField(
        label = Res.string.block_parameter_y,
        value = y,
        scope = scope,
        axis = Axis.VERTICAL,
        onValueChange = { onChange(x, it) },
    )
    if (isVelocity) UnitText(Res.string.block_velocity_unit)
}

/**
 * How far each section moves the Blocks in it to the right, in dp.
 */
private const val NESTING_INDENT = 20

/**
 * False for the Blocks whose title says it all; a Kotlin Block's code gets a field of its own below.
 */
private val BlockKind.hasInlineParameters
    get() = when (this) {
        BlockKind.STOP, BlockKind.RUN_PARENT_EVENT, BlockKind.BOUNCE_OFF_EDGE, BlockKind.REMOVE_SELF, BlockKind.BOUNCE_OFF_OTHER,
        BlockKind.REMOVE_OTHER, BlockKind.ELSE, BlockKind.END, BlockKind.KOTLIN, BlockKind.RESTART_SCENE, BlockKind.STOP_MUSIC,
        -> false
        BlockKind.SET_VELOCITY, BlockKind.ADD_TO_VELOCITY, BlockKind.MOVE_IN_DIRECTION, BlockKind.JUMP_TO, BlockKind.SET_NUMBER, BlockKind.CHANGE_NUMBER,
        BlockKind.SET_TEXT, BlockKind.SET_TOGGLE, BlockKind.ADD_ACTOR, BlockKind.MOVE_TOWARDS_NEAREST, BlockKind.START_TIMER,
        BlockKind.STOP_TIMER, BlockKind.GO_TO_SCENE, BlockKind.PAUSE_GAME, BlockKind.PLAY_SOUND, BlockKind.PLAY_MUSIC,
        BlockKind.IF, BlockKind.REPEAT, BlockKind.LOG, BlockKind.SET_FRAME, BlockKind.SET_ANIMATION, BlockKind.SET_COLOR,
        BlockKind.SET_OPACITY, BlockKind.CHANGE_OPACITY,
        -> true
    }
