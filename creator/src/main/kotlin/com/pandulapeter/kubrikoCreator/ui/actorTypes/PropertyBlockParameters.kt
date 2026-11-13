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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import com.pandulapeter.kubrikoCreator.actors.withPropertyReference
import com.pandulapeter.kubrikoCreator.ui.components.BufferedTextField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_parameter_by
import kubriko.creator.generated.resources.block_parameter_text
import kubriko.creator.generated.resources.block_parameter_value
import org.jetbrains.compose.resources.stringResource

/**
 * The parameters of a Block that changes a Property: which of the Properties in [scope] (of the kind the Block needs)
 * it changes, and the value.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PropertyBlockParameters(
    block: BlockDefinition,
    scope: BlockScope,
    onBlockChange: (BlockDefinition) -> Unit,
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    val reference = block.propertyReference ?: return@FlowRow
    PropertyChoice(reference, scope) { onBlockChange(block.withPropertyReference(it.manager, it.name)) }
    if (propertyChoices(scope, reference.kind).isEmpty()) return@FlowRow
    when (block) {
        is BlockDefinition.SetNumber -> NumberValueField(
            label = Res.string.block_parameter_value,
            value = block.value,
            scope = scope,
            onValueChange = { onBlockChange(block.copy(value = it)) },
        )
        is BlockDefinition.ChangeNumber -> NumberValueField(
            label = Res.string.block_parameter_by,
            value = block.by,
            scope = scope,
            onValueChange = { onBlockChange(block.copy(by = it)) },
        )
        is BlockDefinition.SetText -> BufferedTextField(
            modifier = Modifier.width(180.dp),
            value = block.value,
            onValueChange = { onBlockChange(block.copy(value = it)) },
            placeholder = stringResource(Res.string.block_parameter_text),
        )
        is BlockDefinition.SetToggle -> Switch(
            checked = block.value,
            onCheckedChange = { onBlockChange(block.copy(value = it)) },
        )
        is BlockDefinition.SetVelocity, is BlockDefinition.AddToVelocity, is BlockDefinition.MoveInDirection, BlockDefinition.Stop, BlockDefinition.RunParentEvent,
        is BlockDefinition.JumpTo, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther,
        BlockDefinition.RemoveOther, is BlockDefinition.AddActor, is BlockDefinition.MoveTowardsNearest,
        is BlockDefinition.StartTimer, is BlockDefinition.StopTimer, is BlockDefinition.GoToScene, BlockDefinition.RestartScene,
        is BlockDefinition.PauseGame, is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic, BlockDefinition.StopMusic,
        is BlockDefinition.SetFrame, is BlockDefinition.SetAnimation,
        is BlockDefinition.If, BlockDefinition.Else, is BlockDefinition.Repeat, BlockDefinition.End, is BlockDefinition.Log,
        is BlockDefinition.Kotlin,
        -> Unit
    }
}
