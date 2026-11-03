/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import java.io.File

/**
 * What a game is called and which version it is, kept as `game.*` keys in `gradle.properties`, which the build file (from
 * template version 9) reads for the installers and the generated `GameInfo` object, so that there is one source.
 *
 * @param versionName The version players see, which installers require as one to three numbers (`1.2.0`).
 * @param versionNumber The number stores compare, raised by one with each release.
 * @param applicationId The reverse domain name the platforms tell the game apart by (`com.example.mygame`).
 */
public data class GameIdentity(
    val name: String,
    val versionName: String,
    val versionNumber: Int,
    val applicationId: String,
    val vendor: String,
    val description: String,
) {

    /**
     * Why this can't be the game's identity, with the arguments of the message, or null if it can.
     */
    public fun validate(): Pair<IdentityProblem, List<String>>? = when {
        name.isBlank() || name.length > MAXIMUM_NAME_LENGTH || name.hasLineBreak() -> IdentityProblem.NAME_INVALID to listOf(MAXIMUM_NAME_LENGTH.toString())
        !versionNamePattern.matches(versionName) -> IdentityProblem.VERSION_NAME_INVALID to listOf(versionName)
        versionNumber !in 1..MAXIMUM_VERSION_NUMBER -> IdentityProblem.VERSION_NUMBER_INVALID to listOf(MAXIMUM_VERSION_NUMBER.toString())
        !isValidApplicationId(applicationId) -> IdentityProblem.APPLICATION_ID_INVALID to listOf(applicationId)
        listOf(vendor, description).any { it.length > MAXIMUM_TEXT_LENGTH || it.hasLineBreak() } -> IdentityProblem.TEXT_INVALID to listOf(MAXIMUM_TEXT_LENGTH.toString())
        else -> null
    }

    /**
     * [text] (a `gradle.properties`) holding this identity, with every other line as it was.
     */
    public fun writtenTo(text: String): String = valuesByKey().entries.fold(text) { result, (key, value) -> GradleProperties.withValue(result, key, value) }

    private fun valuesByKey() = linkedMapOf(
        NAME to name,
        VERSION_NAME to versionName,
        VERSION_NUMBER to versionNumber.toString(),
        APPLICATION_ID to applicationId,
        VENDOR to vendor,
        DESCRIPTION to description,
    )

    private fun String.hasLineBreak() = '\n' in this || '\r' in this

    public companion object {
        public const val NAME: String = "game.name"
        public const val VERSION_NAME: String = "game.versionName"
        public const val VERSION_NUMBER: String = "game.versionNumber"
        public const val APPLICATION_ID: String = "game.applicationId"
        public const val VENDOR: String = "game.vendor"
        public const val DESCRIPTION: String = "game.description"
        public const val GRADLE_PROPERTIES_FILE: String = "gradle.properties"
        public const val BUILD_FILE: String = "build.gradle.kts"
        public const val DEFAULT_VERSION_NAME: String = "1.0.0"
        internal const val MAXIMUM_NAME_LENGTH = 100
        internal const val MAXIMUM_TEXT_LENGTH = 200
        public const val MAXIMUM_VERSION_NUMBER: Int = 2_100_000_000
        private val versionNamePattern = Regex("[1-9][0-9]{0,8}(\\.[0-9]{1,9}){0,2}")
        private val applicationIdSegmentPattern = Regex("[A-Za-z][A-Za-z0-9_]*")

        internal fun isValidApplicationId(value: String) = value.split('.').let { segments -> segments.size >= 2 && segments.all(applicationIdSegmentPattern::matches) }

        /**
         * The identity [text] (a `gradle.properties`) holds, or null if it lacks a key or holds a version number that isn't one.
         */
        public fun of(text: String): GameIdentity? {
            fun value(key: String) = GradleProperties.valueOf(text, key)
            return GameIdentity(
                name = value(NAME) ?: return null,
                versionName = value(VERSION_NAME) ?: return null,
                versionNumber = value(VERSION_NUMBER)?.trim()?.toIntOrNull() ?: return null,
                applicationId = value(APPLICATION_ID) ?: return null,
                vendor = value(VENDOR).orEmpty(),
                description = value(DESCRIPTION).orEmpty(),
            )
        }

        /**
         * The identity of the project in [rootDirectory], or null if its build doesn't read one (its template is older
         * than version 9, or it was changed by hand) or its `gradle.properties` lacks it.
         */
        public fun read(rootDirectory: File): GameIdentity? {
            if (!isReadByBuild(rootDirectory)) return null
            return File(rootDirectory, GRADLE_PROPERTIES_FILE).takeIf { it.isFile }?.let { of(it.readText()) }
        }

        @EditorMutation
        public fun write(rootDirectory: File, identity: GameIdentity) {
            val file = File(rootDirectory, GRADLE_PROPERTIES_FILE)
            val raw = if (file.isFile) file.readText() else ""
            file.writeTextAtomically(identity.writtenTo(raw.withUnixLineBreaks()).withLineBreaksOf(raw))
        }

        internal fun isReadByBuild(rootDirectory: File) = File(rootDirectory, BUILD_FILE).takeIf { it.isFile }?.readText()?.let { NAME in it } == true
    }
}
