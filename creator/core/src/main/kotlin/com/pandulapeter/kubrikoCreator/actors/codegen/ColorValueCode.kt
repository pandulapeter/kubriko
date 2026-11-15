/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.ColorValue
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.colorLiteral
import com.pandulapeter.kubrikoCreator.actors.propertyReference

/**
 * Turns a [ColorValue] into one Kotlin operand of the type `Color`, an inline class, so reading one allocates nothing.
 */
internal object ColorValueCode {

    /**
     * What every color value's code names, which a Block taking one imports, since a Manager type may have no color of its own.
     */
    const val IMPORT = "androidx.compose.ui.graphics.Color"

    /**
     * What a color Property that doesn't exist (renamed, deleted, or not chosen yet) reads as, so the code still
     * compiles.
     */
    private const val MISSING_PROPERTY_COLOR = 0xFFFFFFFF

    fun color(value: ColorValue, scope: BlockScope): String = when (value) {
        is ColorValue.Fixed -> "Color(${colorLiteral(value.argb)})"
        is ColorValue.Property -> value.propertyReference?.takeIf { scope.hasProperty(it) }?.let { BlockCodeGenerator.propertyExpression(it, scope) }
            ?: "Color(${colorLiteral(MISSING_PROPERTY_COLOR)})"
    }
}
