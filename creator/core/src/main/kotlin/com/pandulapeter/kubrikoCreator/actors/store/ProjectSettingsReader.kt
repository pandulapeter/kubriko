/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.store

import com.pandulapeter.kubrikoCreator.actors.CreatorProjectSettings
import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import com.pandulapeter.kubrikoCreator.project.UnreadableProjectFileException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.io.File
import java.io.IOException

/**
 * Reads `.kubriko/project.json`, telling what is wrong in one it can't use in words someone who never wrote JSON
 * follows, rather than in the parser's.
 */
internal object ProjectSettingsReader {
    private val offset = Regex("""at offset (\d+)""")

    /**
     * @throws UnreadableProjectFileException If [file] can't be read or doesn't hold valid settings.
     */
    fun read(file: File, json: Json): CreatorProjectSettings {
        val text = try {
            file.readText()
        } catch (exception: IOException) {
            throw UnreadableProjectFileException(ProjectProblem.PROJECT_FILE_NOT_READ, listOf(file.path, exception.message ?: exception.toString()), exception)
        }
        val element = try {
            json.parseToJsonElement(text)
        } catch (exception: SerializationException) {
            throw UnreadableProjectFileException(ProjectProblem.PROJECT_FILE_NOT_JSON, listOf(file.path, lineOfMistake(text, exception).toString()), exception)
        }
        if (element !is JsonObject) throw UnreadableProjectFileException(ProjectProblem.PROJECT_FILE_NOT_AN_OBJECT, listOf(file.path))
        checkValues(file, element)
        return try {
            json.decodeFromJsonElement(CreatorProjectSettings.serializer(), element)
        } catch (exception: IllegalArgumentException) {
            val reason = exception.message?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim() ?: exception.toString()
            throw UnreadableProjectFileException(ProjectProblem.PROJECT_FILE_NOT_DECODED, listOf(file.path, reason), exception)
        }
    }

    /**
     * Checks every value the settings hold against the kind their serializer expects, so the first wrong one is named.
     */
    private fun checkValues(file: File, settings: JsonObject) {
        val descriptor = CreatorProjectSettings.serializer().descriptor
        (0 until descriptor.elementsCount).forEach { index ->
            val name = descriptor.getElementName(index)
            val value = settings[name]
            val elementDescriptor = descriptor.getElementDescriptor(index)
            val problem = when {
                value == null || value == JsonNull -> ProjectProblem.PROJECT_FILE_VALUE_MISSING.takeUnless {
                    descriptor.isElementOptional(index) || (value == JsonNull && elementDescriptor.isNullable)
                }
                elementDescriptor.kind == PrimitiveKind.STRING -> ProjectProblem.PROJECT_FILE_VALUE_NOT_TEXT.takeUnless { value.isText }
                elementDescriptor.kind == PrimitiveKind.INT -> ProjectProblem.PROJECT_FILE_VALUE_NOT_NUMBER.takeUnless { value.isWholeNumber }
                else -> null
            }
            if (problem != null) throw UnreadableProjectFileException(problem, listOf(file.path, name))
        }
    }

    private val JsonElement.isText get() = this is JsonPrimitive && isString

    private val JsonElement.isWholeNumber get() = this is JsonPrimitive && !isString && intOrNull != null

    /**
     * The line of [text] the parser stopped at, counted from 1; the first line when its message names no place.
     */
    private fun lineOfMistake(text: String, exception: SerializationException): Int {
        val position = exception.message?.let(offset::find)?.groupValues?.get(1)?.toIntOrNull() ?: return 1
        return 1 + text.take(position).count { it == '\n' }
    }
}
