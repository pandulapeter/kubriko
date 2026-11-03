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

    @Test
    fun anEntryGoesWhereItKeepsItsSectionSorted() {
        val catalog = "[versions]\nkotlin = \"2.4.21\"\n\n[plugins]\ncompose = { id = \"a\" }\nkotlin-multiplatform = { id = \"b\" }\n"
        assertEquals(
            "[versions]\nkotlin = \"2.4.21\"\n\n[plugins]\nandroid-application = { id = \"c\" }\ncompose = { id = \"a\" }\nkotlin-multiplatform = { id = \"b\" }\n",
            VersionCatalog.withEntry(catalog, VersionCatalog.PLUGINS, "android-application", "{ id = \"c\" }"),
        )
    }

    @Test
    fun anEntryOfAnUnsortedSectionGoesLast() {
        val catalog = "[versions]\nkotlin = \"1\"\ncompose = \"2\"\n\n[plugins]\n"
        assertEquals(
            "[versions]\nkotlin = \"1\"\ncompose = \"2\"\nandroid = \"3\"\n\n[plugins]\n",
            VersionCatalog.withEntry(catalog, VersionCatalog.VERSIONS, "android", "\"3\""),
        )
    }

    @Test
    fun aMissingSectionIsAddedAtTheEnd() {
        assertEquals(
            "[versions]\nkotlin = \"1\"\n\n[plugins]\nandroid = { id = \"a\" }\n",
            VersionCatalog.withEntry("[versions]\nkotlin = \"1\"\n\n", VersionCatalog.PLUGINS, "android", "{ id = \"a\" }"),
        )
    }

    @Test
    fun anEntryThatIsThereAlreadyIsKeptAsItIs() {
        val catalog = "[versions]\nandroid = \"1\"\n"
        assertEquals(catalog, VersionCatalog.withEntry(catalog, VersionCatalog.VERSIONS, "android", "\"2\""))
    }

    @Test
    fun anEntryIsRemovedOnlyFromItsSection() {
        val catalog = "[versions]\nandroid = \"1\"\n\n[plugins]\nandroid = { id = \"a\" }\n"
        assertEquals("[versions]\nandroid = \"1\"\n\n[plugins]\n", VersionCatalog.withoutEntry(catalog, VersionCatalog.PLUGINS, "android"))
        assertEquals(false, VersionCatalog.hasEntry(VersionCatalog.withoutEntry(catalog, VersionCatalog.PLUGINS, "android"), VersionCatalog.PLUGINS, "android"))
        assertEquals(true, VersionCatalog.hasEntry(catalog, VersionCatalog.VERSIONS, "android"))
    }
}
