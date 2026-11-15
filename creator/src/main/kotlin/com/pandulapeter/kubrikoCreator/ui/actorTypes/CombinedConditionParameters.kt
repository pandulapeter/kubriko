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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_condition
import kubriko.creator.generated.resources.action_remove_condition
import kubriko.creator.generated.resources.condition_all
import kubriko.creator.generated.resources.condition_any
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_close
import org.jetbrains.compose.resources.stringResource

/**
 * The conditions a combined one holds, each on a row of its own that can be removed while another stays, and whether all
 * of them or any of them has to hold.
 */
@Composable
internal fun CombinedConditionParameters(
    condition: ConditionDefinition.Combined,
    scope: BlockScope,
    isForText: Boolean,
    onConditionChange: (ConditionDefinition) -> Unit,
): Unit = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    MenuButton(
        label = stringResource(if (condition.isAll) Res.string.condition_all else Res.string.condition_any),
        options = listOf(true, false),
        optionText = { stringResource(if (it) Res.string.condition_all else Res.string.condition_any) },
        onSelected = { onConditionChange(condition.copy(isAll = it)) },
    )
    condition.conditions.forEachIndexed { index, part ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConditionParameters(part, scope, isForText) { changed ->
                onConditionChange(condition.copy(conditions = condition.conditions.toMutableList().also { it[index] = changed }))
            }
            if (condition.conditions.size > 1) {
                IconActionButton(
                    icon = Res.drawable.ic_close,
                    label = Res.string.action_remove_condition,
                    onClick = { onConditionChange(condition.copy(conditions = condition.conditions.filterIndexed { other, _ -> other != index })) },
                )
            }
        }
    }
    IconActionButton(
        icon = Res.drawable.ic_add,
        label = Res.string.action_add_condition,
        onClick = { onConditionChange(condition.copy(conditions = condition.conditions + ConditionDefinition.Toggle())) },
    )
}
