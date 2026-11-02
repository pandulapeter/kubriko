/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Keeps one value in a JSON [file], written through [writeTextAtomically] (readable by its owner only when [isPrivate]).
 * Every call blocks.
 *
 * A write never replaces a file it can't read: one whose content can't be decoded is first moved aside to `<name>.bak`
 * next to it (replacing an older one), and one that can't be read at all (locked by another program, say) isn't written.
 */
internal class JsonFileStore<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val isPrivate: Boolean = false,
    private val json: Json = defaultJson,
    private val readText: (File) -> String = File::readText,
) {
    private val backup get() = File(file.parentFile, "${file.name}.bak")

    fun read(): Read<T> = try {
        if (file.isFile) Read.Found(json.decodeFromString(serializer, readText(file))) else Read.Missing
    } catch (exception: IOException) {
        Read.Unreadable(exception.reason(), isContentBroken = false)
    } catch (exception: SerializationException) {
        Read.Unreadable(exception.reason(), isContentBroken = true)
    } catch (exception: IllegalArgumentException) {
        Read.Unreadable(exception.reason(), isContentBroken = true)
    }

    /**
     * Writes [value], returning false when the file was left as it was.
     */
    fun write(value: T): Boolean {
        val current = read()
        if (current is Read.Unreadable && (!current.isContentBroken || !setAside())) return false
        return try {
            file.parentFile?.mkdirs()
            file.writeTextAtomically(json.encodeToString(serializer, value), isPrivate)
            true
        } catch (_: IOException) {
            false
        }
    }

    private fun setAside() = try {
        moveReplacing(file.toPath(), backup.toPath())
        if (isPrivate) backup.restrictToOwner()
        true
    } catch (_: IOException) {
        false
    }

    private fun Exception.reason() = message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: javaClass.simpleName

    /**
     * What [read] found.
     */
    sealed interface Read<out T> {
        data object Missing : Read<Nothing>

        data class Found<T>(val value: T) : Read<T>

        /**
         * The file is there but gave no value: its content is broken when [isContentBroken], otherwise it couldn't be read
         * now and may be fine later.
         */
        data class Unreadable(val reason: String, val isContentBroken: Boolean) : Read<Nothing>
    }

    companion object {
        val defaultJson = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    }
}

/**
 * The value [JsonFileStore.read] found, or null when the file is missing or can't be read.
 */
internal fun <T> JsonFileStore.Read<T>.valueOrNull() = (this as? JsonFileStore.Read.Found<T>)?.value
