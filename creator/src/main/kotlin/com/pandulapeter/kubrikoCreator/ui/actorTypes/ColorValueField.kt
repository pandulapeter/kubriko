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
import com.pandulapeter.kubrikoCreator.actors.ColorValue
import com.pandulapeter.kubrikoCreator.actors.fixedArgb
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import com.pandulapeter.kubrikoCreator.ui.components.ColorField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.color_value_fixed
import kubriko.creator.generated.resources.color_value_kind
import kubriko.creator.generated.resources.color_value_property
import kubriko.creator.generated.resources.ic_palette

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
) = FixedOrPropertyField(
    property = value.propertyReference,
    scope = scope,
    fixedIcon = Res.drawable.ic_palette,
    kindLabel = Res.string.color_value_kind,
    fixedLabel = Res.string.color_value_fixed,
    propertyLabel = Res.string.color_value_property,
    onKindChange = { toProperty -> onValueChange(if (toProperty) ColorValue.Property() else ColorValue.Fixed(fallback)) },
    onPropertyChange = { onValueChange(ColorValue.Property(property = it.name, manager = it.manager)) },
) {
    ColorField(color = checkNotNull(value.fixedArgb), onColorChange = { onValueChange(ColorValue.Fixed(it)) }, isAlphaEnabled = isAlphaEnabled)
}
