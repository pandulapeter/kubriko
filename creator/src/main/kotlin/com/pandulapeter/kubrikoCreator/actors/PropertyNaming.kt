/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_property_name_invalid
import kubriko.creator.generated.resources.error_property_name_reserved
import kubriko.creator.generated.resources.error_property_name_taken
import org.jetbrains.compose.resources.StringResource

internal object PropertyNaming {

    private val namePattern = Regex("[a-z][A-Za-z0-9]*")
    private val generatedTimerField = Regex("timer[0-9]+(Remaining|Period)InMilliseconds")

    /**
     * Kotlin's keywords, and the names the generated class (and the Kubriko traits it implements) already uses.
     */
    private val reservedNames = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null", "object",
        "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while",
        "actorManager", "body", "bottomRight", "collidableTypes", "collidables", "collisionMask", "color", "deltaTimeInMilliseconds",
        "drawingOrder", "halfSize", "index", "isAlwaysActive", "isVisible", "key", "kubriko", "layerIndex", "metadata", "other",
        "shouldClip", "state", "topLeft", "velocity", "viewportManager",
    )

    /**
     * [base], or the first of base2, base3, … that can be a Property's name among [existingNames].
     */
    fun freeName(base: String, existingNames: Collection<String>) = generateSequence(1) { it + 1 }
        .map { if (it == 1) base else "$base$it" }
        .first { validate(it, existingNames) == null }

    /**
     * Why [name] can't be the name of a Property among [existingNames], or null if it can. Names that differ only in
     * case are taken too, like those of Actor types.
     */
    fun validate(name: String, existingNames: Collection<String>): StringResource? = when {
        !namePattern.matches(name) -> Res.string.error_property_name_invalid
        name in reservedNames || name.startsWith("isTouching") || generatedTimerField.matches(name) -> Res.string.error_property_name_reserved
        existingNames.any { it.equals(name, ignoreCase = true) } -> Res.string.error_property_name_taken
        else -> null
    }
}
