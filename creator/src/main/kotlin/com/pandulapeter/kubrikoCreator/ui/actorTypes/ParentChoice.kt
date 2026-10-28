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
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_parent
import kubriko.creator.generated.resources.actor_type_parent_none
import org.jetbrains.compose.resources.stringResource

/**
 * A choice of the Actor type an Actor type inherits from, or none.
 */
@Composable
internal fun ParentChoice(
    parent: String?,
    choices: List<String>,
    onParentChange: (String?) -> Unit,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
) {
    val none = stringResource(Res.string.actor_type_parent_none)
    Text(
        text = stringResource(Res.string.actor_type_parent),
        style = MaterialTheme.typography.labelMedium,
    )
    MenuButton(
        label = parent ?: none,
        options = listOf("") + choices,
        optionText = { it.ifEmpty { none } },
        onSelected = { onParentChange(it.ifEmpty { null }) },
    )
}
