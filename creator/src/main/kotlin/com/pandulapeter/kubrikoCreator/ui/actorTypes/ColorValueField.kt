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
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.ColorValue
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.PropertyReference
import com.pandulapeter.kubrikoCreator.ui.components.ColorField
import com.pandulapeter.kubrikoCreator.ui.components.MenuIconButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.color_value_fixed
import kubriko.creator.generated.resources.color_value_kind
import kubriko.creator.generated.resources.color_value_property
import kubriko.creator.generated.resources.ic_palette
import kubriko.creator.generated.resources.ic_property
import org.jetbrains.compose.resources.stringResource

/**
 * A color a Block takes: a fixed one, picked or typed, or a color Property's value, chosen beside it.
 *
 * @param fallback The color a fixed one starts as when a Property's is switched back to one.
 * @param isAlphaEnabled False for a field that takes only opaque colors, as [ColorField]'s.
 */
@Composable
internal fun ColorValueField(
    value: ColorValue,
    scope: BlockScope,
    onValueChange: (ColorValue) -> Unit,
    fallback: Long = 0xFFFFFFFF,
    isAlphaEnabled: Boolean = true,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    val isProperty = value is ColorValue.Property
    MenuIconButton(
        icon = if (isProperty) Res.drawable.ic_property else Res.drawable.ic_palette,
        label = Res.string.color_value_kind,
        options = listOf(false, true),
        optionText = { stringResource(if (it) Res.string.color_value_property else Res.string.color_value_fixed) },
        optionIcon = { if (it) Res.drawable.ic_property else Res.drawable.ic_palette },
        onSelected = { toProperty ->
            if (toProperty != isProperty) onValueChange(if (toProperty) ColorValue.Property() else ColorValue.Fixed(fallback))
        },
    )
    when (value) {
        is ColorValue.Fixed -> ColorField(color = value.argb, onColorChange = { onValueChange(ColorValue.Fixed(it)) }, isAlphaEnabled = isAlphaEnabled)
        is ColorValue.Property -> PropertyChoice(PropertyReference(value.property, PropertyKind.COLOR, value.manager), scope) {
            onValueChange(ColorValue.Property(property = it.name, manager = it.manager))
        }
    }
}
