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

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.actors.ActorTypeValue
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_value_fixed
import kubriko.creator.generated.resources.actor_type_value_kind
import kubriko.creator.generated.resources.actor_type_value_property
import kubriko.creator.generated.resources.ic_actor_type

/**
 * The Actor type a Block works with: one chosen by its name, or the one an Actor type reference Property names.
 */
@Composable
internal fun ActorTypeValueField(
    value: ActorTypeValue,
    scope: BlockScope,
    onValueChange: (ActorTypeValue) -> Unit,
) = FixedOrPropertyField(
    property = value.propertyReference,
    scope = scope,
    fixedIcon = Res.drawable.ic_actor_type,
    kindLabel = Res.string.actor_type_value_kind,
    fixedLabel = Res.string.actor_type_value_fixed,
    propertyLabel = Res.string.actor_type_value_property,
    onKindChange = { toProperty -> onValueChange(if (toProperty) ActorTypeValue.Property() else ActorTypeValue.Fixed("")) },
    onPropertyChange = { onValueChange(ActorTypeValue.Property(property = it.name, manager = it.manager)) },
) {
    ActorTypeChoice((value as ActorTypeValue.Fixed).name, scope.actorTypeNames) { onValueChange(ActorTypeValue.Fixed(it)) }
}
