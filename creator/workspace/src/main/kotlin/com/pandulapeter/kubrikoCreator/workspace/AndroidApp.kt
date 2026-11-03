/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

/**
 * The debug app a build of the `androidApp` module made, as the Android Gradle plugin describes it next to the APK, so
 * the application ID is the one the build used, however the build file sets it.
 */
internal data class AndroidApp(
    val file: File,
    val applicationId: String,
) {
    companion object {
        /**
         * Where the debug build puts the app, under the project.
         */
        val OUTPUT_PATH: String = listOf("androidApp", "build", "outputs", "apk", "debug").joinToString(File.separator)
        private const val METADATA_FILE = "output-metadata.json"

        /**
         * The app the build left in [directory], or null if there is none, or its description can't be read. Blocks.
         */
        fun builtIn(directory: File): AndroidApp? = try {
            val metadata = Json.parseToJsonElement(File(directory, METADATA_FILE).readText()) as? JsonObject
            val applicationId = (metadata?.get("applicationId") as? JsonPrimitive)?.content
            val outputFile = ((metadata?.get("elements") as? JsonArray)?.firstOrNull() as? JsonObject)?.get("outputFile") as? JsonPrimitive
            val file = outputFile?.content?.let { File(directory, it) }
            if (applicationId != null && file != null && file.isFile) AndroidApp(file, applicationId) else null
        } catch (_: Exception) {
            null
        }
    }
}
