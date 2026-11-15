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
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.ToggleValue
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_toggle
import kubriko.creator.generated.resources.toggle_value_fixed
import kubriko.creator.generated.resources.toggle_value_kind
import kubriko.creator.generated.resources.toggle_value_property

/**
 * Whether something is on, as a Block or a condition takes it: fixed, through [fixedField] with the current choice, or
 * a toggle Property's value.
 */
@Composable
internal fun ToggleValueField(
    value: ToggleValue,
    scope: BlockScope,
    onValueChange: (ToggleValue) -> Unit,
    fixedField: @Composable (Boolean) -> Unit,
) = FixedOrPropertyField(
    property = value.propertyReference,
    scope = scope,
    fixedIcon = Res.drawable.ic_toggle,
    kindLabel = Res.string.toggle_value_kind,
    fixedLabel = Res.string.toggle_value_fixed,
    propertyLabel = Res.string.toggle_value_property,
    onKindChange = { toProperty -> onValueChange(if (toProperty) ToggleValue.Property() else ToggleValue.Fixed(true)) },
    onPropertyChange = { onValueChange(ToggleValue.Property(property = it.name, manager = it.manager)) },
) {
    fixedField((value as ToggleValue.Fixed).isOn)
}
