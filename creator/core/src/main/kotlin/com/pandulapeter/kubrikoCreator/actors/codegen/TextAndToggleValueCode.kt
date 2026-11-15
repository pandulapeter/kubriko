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
import com.pandulapeter.kubrikoCreator.actors.TextValue
import com.pandulapeter.kubrikoCreator.actors.ToggleValue
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.stringLiteral
import com.pandulapeter.kubrikoCreator.actors.propertyReference

/**
 * Turns a [TextValue] or a [ToggleValue] into one Kotlin operand. A Property that doesn't exist (renamed, deleted, or not
 * chosen yet) reads as an empty text or as off, so the code still compiles.
 */
internal object TextAndToggleValueCode {

    fun text(value: TextValue, scope: BlockScope): String = when (value) {
        is TextValue.Fixed -> stringLiteral(value.text)
        is TextValue.Property -> value.propertyReference?.takeIf { scope.hasProperty(it) }?.let { BlockCodeGenerator.propertyExpression(it, scope) } ?: "\"\""
    }

    fun toggle(value: ToggleValue, scope: BlockScope): String = when (value) {
        is ToggleValue.Fixed -> value.isOn.toString()
        is ToggleValue.Property -> value.propertyReference?.takeIf { scope.hasProperty(it) }?.let { BlockCodeGenerator.propertyExpression(it, scope) } ?: "false"
    }
}
