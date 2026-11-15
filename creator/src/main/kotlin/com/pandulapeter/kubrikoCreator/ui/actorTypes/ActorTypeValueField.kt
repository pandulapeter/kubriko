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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorTypeValue
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.PropertyReference
import com.pandulapeter.kubrikoCreator.ui.components.MenuIconButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_value_fixed
import kubriko.creator.generated.resources.actor_type_value_kind
import kubriko.creator.generated.resources.actor_type_value_property
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.ic_property
import org.jetbrains.compose.resources.stringResource

/**
 * The Actor type a Block works with: one chosen by its name, or the one an Actor type reference Property names.
 */
@Composable
internal fun ActorTypeValueField(
    value: ActorTypeValue,
    scope: BlockScope,
    onValueChange: (ActorTypeValue) -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    val isProperty = value is ActorTypeValue.Property
    MenuIconButton(
        icon = if (isProperty) Res.drawable.ic_property else Res.drawable.ic_actor_type,
        label = Res.string.actor_type_value_kind,
        options = listOf(false, true),
        optionText = { stringResource(if (it) Res.string.actor_type_value_property else Res.string.actor_type_value_fixed) },
        optionIcon = { if (it) Res.drawable.ic_property else Res.drawable.ic_actor_type },
        onSelected = { toProperty ->
            if (toProperty != isProperty) onValueChange(if (toProperty) ActorTypeValue.Property() else ActorTypeValue.Fixed(""))
        },
    )
    when (value) {
        is ActorTypeValue.Fixed -> ActorTypeChoice(value.name, scope.actorTypeNames) { onValueChange(ActorTypeValue.Fixed(it)) }
        is ActorTypeValue.Property -> PropertyChoice(PropertyReference(value.property, PropertyKind.ACTOR_TYPE, value.manager), scope) {
            onValueChange(ActorTypeValue.Property(property = it.name, manager = it.manager))
        }
    }
}
