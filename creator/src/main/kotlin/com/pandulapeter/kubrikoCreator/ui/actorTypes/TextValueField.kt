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

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.TextValue
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import com.pandulapeter.kubrikoCreator.ui.components.BufferedTextField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_parameter_text
import kubriko.creator.generated.resources.ic_text
import kubriko.creator.generated.resources.text_value_fixed
import kubriko.creator.generated.resources.text_value_kind
import kubriko.creator.generated.resources.text_value_property
import org.jetbrains.compose.resources.stringResource

/**
 * A text a Block or a condition takes: a fixed one, typed in a field [fieldWidth] wide, or a text Property's value.
 */
@Composable
internal fun TextValueField(
    value: TextValue,
    scope: BlockScope,
    fieldWidth: Dp,
    onValueChange: (TextValue) -> Unit,
) = FixedOrPropertyField(
    property = value.propertyReference,
    scope = scope,
    fixedIcon = Res.drawable.ic_text,
    kindLabel = Res.string.text_value_kind,
    fixedLabel = Res.string.text_value_fixed,
    propertyLabel = Res.string.text_value_property,
    onKindChange = { toProperty -> onValueChange(if (toProperty) TextValue.Property() else TextValue.Fixed("")) },
    onPropertyChange = { onValueChange(TextValue.Property(property = it.name, manager = it.manager)) },
) {
    BufferedTextField(
        modifier = Modifier.width(fieldWidth),
        value = (value as TextValue.Fixed).text,
        onValueChange = { onValueChange(TextValue.Fixed(it)) },
        placeholder = stringResource(Res.string.block_parameter_text),
    )
}
