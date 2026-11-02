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

import kotlin.test.Test
import kotlin.test.assertEquals

class VersionCatalogTest {

    @Test
    fun aLibraryDeclaredWithModuleIsFound() {
        val catalog = """
            |[versions]
            |kubriko = "0.9.0"
            |
            |[libraries]
            |kubriko-engine = { module = "io.github.pandulapeter.kubriko:engine", version.ref = "kubriko" }
            |
        """.trimMargin()
        assertEquals(mapOf(KubrikoArtifact.ENGINE to "kubriko-engine"), VersionCatalog.kubrikoLibraries(catalog))
    }
}
