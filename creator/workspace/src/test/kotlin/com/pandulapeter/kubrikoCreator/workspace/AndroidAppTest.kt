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

import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AndroidAppTest {
    private val directory = testDirectory()

    @Test
    fun theAppIsTheOneTheBuildDescribesWithItsApplicationId() {
        File(directory, "androidApp-debug.apk").writeText("apk")
        File(directory, "output-metadata.json").writeText(METADATA)
        assertEquals(AndroidApp(File(directory, "androidApp-debug.apk"), "com.example.mygame"), AndroidApp.builtIn(directory))
    }

    @Test
    fun noAppIsFoundWithoutADescriptionOrItsFile() {
        assertNull(AndroidApp.builtIn(directory))
        File(directory, "output-metadata.json").writeText(METADATA)
        assertNull(AndroidApp.builtIn(directory))
        File(directory, "output-metadata.json").writeText("{ not json")
        File(directory, "androidApp-debug.apk").writeText("apk")
        assertNull(AndroidApp.builtIn(directory))
    }

    private companion object {
        val METADATA = """
            {
              "version": 3,
              "artifactType": { "type": "APK", "kind": "Directory" },
              "applicationId": "com.example.mygame",
              "variantName": "debug",
              "elements": [
                { "type": "SINGLE", "filters": [], "attributes": [], "versionCode": 1, "versionName": "1.0.0", "outputFile": "androidApp-debug.apk" }
              ],
              "elementType": "File"
            }
        """.trimIndent()
    }
}
