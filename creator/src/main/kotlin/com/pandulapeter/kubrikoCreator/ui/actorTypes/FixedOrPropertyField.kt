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
import com.pandulapeter.kubrikoCreator.actors.PropertyReference
import com.pandulapeter.kubrikoCreator.ui.components.MenuIconButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_property
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A value a Block takes that is fixed or a Property's: a menu choosing which, then [fixedField] for a fixed one, or the
 * Property chosen beside it.
 *
 * @param property The Property the value reads, or null when it is fixed.
 * @param onKindChange Called with whether the value now reads a Property, when the menu changes that.
 */
@Composable
internal fun FixedOrPropertyField(
    property: PropertyReference?,
    scope: BlockScope,
    fixedIcon: DrawableResource,
    kindLabel: StringResource,
    fixedLabel: StringResource,
    propertyLabel: StringResource,
    onKindChange: (Boolean) -> Unit,
    onPropertyChange: (PropertyReference) -> Unit,
    fixedField: @Composable () -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    val isProperty = property != null
    MenuIconButton(
        icon = if (isProperty) Res.drawable.ic_property else fixedIcon,
        label = kindLabel,
        options = listOf(false, true),
        optionText = { stringResource(if (it) propertyLabel else fixedLabel) },
        optionIcon = { if (it) Res.drawable.ic_property else fixedIcon },
        onSelected = { toProperty -> if (toProperty != isProperty) onKindChange(toProperty) },
    )
    if (property == null) fixedField() else PropertyChoice(property, scope, onPropertyChange)
}
