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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetMovement
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_movement
import kubriko.creator.generated.resources.actor_type_stays_inside_scene
import kubriko.creator.generated.resources.actor_type_stays_inside_scene_off
import kubriko.creator.generated.resources.actor_type_stays_inside_scene_on
import kubriko.creator.generated.resources.ic_velocity
import org.jetbrains.compose.resources.stringResource

/**
 * How an Actor type's Actors move by themselves: whether they stay inside the Scene's bounds, with what that means.
 */
@Composable
internal fun MovementColumn(
    definition: ActorTypeDefinition,
    onChange: (SetMovement.Input) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    PanelHeader(
        icon = Res.drawable.ic_velocity,
        accent = Accent.CAST,
        title = stringResource(Res.string.actor_type_movement),
    )
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LabeledSwitch(
            label = stringResource(Res.string.actor_type_stays_inside_scene),
            checked = definition.staysInsideScene,
            onCheckedChange = { onChange(SetMovement.Input(definition.name, staysInsideScene = it)) },
        )
        Text(
            text = stringResource(if (definition.staysInsideScene) Res.string.actor_type_stays_inside_scene_on else Res.string.actor_type_stays_inside_scene_off),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
