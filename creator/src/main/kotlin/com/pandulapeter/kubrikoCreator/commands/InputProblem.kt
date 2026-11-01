/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

/**
 * Turns the message of a failed decoding of a command's input into one sentence that says what to change, without
 * kotlinx.serialization's advice to the programmer (registering serializers, ignoring unknown keys) and the echoed input.
 */
internal object InputProblem {

    private val offsetPrefix = Regex("""^Unexpected JSON token at offset \d+: """)
    private val unknownType = Regex("""Serializer for subclass '([^']*)' is not found in the polymorphic scope of '[^']*' at path: (\S+)""")
    private val missingType = Regex("""Class discriminator was missing .* at path: (\S+)""")
    private val unknownKey = Regex("""Encountered an unknown key '([^']*)' at path: (\S+)""")
    private val wrongValue = Regex("""Failed to parse literal '(.*)' as an? (\w+) value at path: (\S+)""")
    private val missingField = Regex("""Fields? (.*) (?:is|are) required for type with serial name '[^']*', but (?:it was|they were) missing at path: (\S+)""")

    fun describe(message: String?): String {
        val line = message.orEmpty().lineSequence().first().replace(offsetPrefix, "").trim()
        unknownType.find(line)?.let { return "There is no type called '${it.groupValues[1]}'${at(it.groupValues[2])}." }
        missingType.find(line)?.let { return "A type is missing${at(it.groupValues[1])}." }
        unknownKey.find(line)?.let { return "There is no property called '${it.groupValues[1]}'${at(it.groupValues[2])}." }
        wrongValue.find(line)?.let { return "${it.groupValues[1]}${at(it.groupValues[3])} isn't a ${it.groupValues[2]}." }
        missingField.find(line)?.let { return "Missing${at(it.groupValues[2])}: ${it.groupValues[1].removePrefix("[").removeSuffix("]").removeSurrounding("'")}." }
        return line
    }

    private fun at(path: String) = if (path == "$") "" else " at '${path.removePrefix("$.")}'"
}
