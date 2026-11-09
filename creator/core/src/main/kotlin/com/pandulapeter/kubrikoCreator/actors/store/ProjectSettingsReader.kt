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
        val settings = parse(file, json)
        checkValues(file, settings)
        return try {
            json.decodeFromJsonElement(CreatorProjectSettings.serializer(), settings)
        } catch (exception: IllegalArgumentException) {
            throw UnreadableProjectFileException(ProjectProblem.PROJECT_FILE_NOT_DECODED, listOf(file.path), exception)
        }
    }

    /**
     * The JSON object [file] holds, whatever its values, for what reads the settings as plain JSON (`ProjectFormat`), so
     * that it refuses the same files [read] does.
     *
     * @throws UnreadableProjectFileException If [file] can't be read or doesn't hold a JSON object.
     */
    fun parse(file: File, json: Json): JsonObject {
        val text = try {
            file.readText()
        } catch (exception: IOException) {
            throw UnreadableProjectFileException(ProjectProblem.PROJECT_FILE_NOT_READ, listOf(file.path, reasonOf(file, exception)), exception)
        }
        if (text.isBlank()) throw UnreadableProjectFileException(ProjectProblem.PROJECT_FILE_EMPTY, listOf(file.path))
        val element = try {
            json.parseToJsonElement(text)
        } catch (exception: SerializationException) {
            val line = lineOfMistake(text, exception)
            throw if (line == null) {
                UnreadableProjectFileException(ProjectProblem.PROJECT_FILE_NOT_JSON_SOMEWHERE, listOf(file.path), exception)
            } else {
                UnreadableProjectFileException(ProjectProblem.PROJECT_FILE_NOT_JSON, listOf(file.path, line.toString()), exception)
            }
        }
        return element as? JsonObject ?: throw UnreadableProjectFileException(ProjectProblem.PROJECT_FILE_NOT_AN_OBJECT, listOf(file.path))
    }

    /**
     * Checks every value the settings hold against the kind their serializer expects, so the first wrong one is named. A
     * value written as `null` is of the wrong kind unless it may be null, since the key is there.
     */
    private fun checkValues(file: File, settings: JsonObject) {
        val descriptor = CreatorProjectSettings.serializer().descriptor
        (0 until descriptor.elementsCount).forEach { index ->
            val name = descriptor.getElementName(index)
            val value = settings[name]
            val elementDescriptor = descriptor.getElementDescriptor(index)
            val problem = when {
                value == null -> ProjectProblem.PROJECT_FILE_VALUE_MISSING.takeUnless { descriptor.isElementOptional(index) }
                value == JsonNull && elementDescriptor.isNullable -> null
                elementDescriptor.kind == PrimitiveKind.STRING -> ProjectProblem.PROJECT_FILE_VALUE_NOT_TEXT.takeUnless { value.isText }
                elementDescriptor.kind == PrimitiveKind.INT -> problemOfWholeNumber(value)
                else -> null
            }
            if (problem != null) throw UnreadableProjectFileException(problem, listOf(file.path, name))
        }
    }

    private val JsonElement.isText get() = this is JsonPrimitive && isString

    /**
     * What is wrong with [value] as a whole number. Like Kotlin's decoder, a number between quotation marks counts.
     */
    private fun problemOfWholeNumber(value: JsonElement): ProjectProblem? {
        val digits = (value as? JsonPrimitive)?.takeUnless { it == JsonNull }?.content?.toBigIntegerOrNull() ?: return ProjectProblem.PROJECT_FILE_VALUE_NOT_NUMBER
        return ProjectProblem.PROJECT_FILE_VALUE_TOO_LARGE.takeUnless { digits.bitLength() < Int.SIZE_BITS }
    }

    /**
     * What the operating system said about [file], without the path it usually starts with, which the message names anyway.
     */
    private fun reasonOf(file: File, exception: IOException): String {
        val reason = exception.message.orEmpty().removePrefix(file.path).trim().removeSurrounding("(", ")").trim()
        return reason.ifEmpty { exception::class.simpleName.orEmpty() }
    }

    /**
     * The line of [text] the parser stopped at, counted from 1, or null when its message names no place.
     */
    internal fun lineOfMistake(text: String, exception: SerializationException): Int? {
        val position = exception.message?.let(offset::find)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..text.length } ?: return null
        return 1 + text.take(position).count { it == '\n' }
    }
}
