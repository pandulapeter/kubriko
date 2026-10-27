/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.template.TemplateFiles.root
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemplateFilesTest {
    private val values = mapOf(
        "PROJECT_NAME" to "My Game",
        "PROJECT_ID" to "my-game",
        "PACKAGE" to "com.example.mygame",
        "KUBRIKO_VERSION" to "1.0.0",
        "KOTLIN_VERSION" to "2.4.21",
        "COMPOSE_VERSION" to "1.12.1",
        "EXTRA_REPOSITORIES" to "",
    )

    @Test
    fun everyTemplateListsExactlyTheFilesItShips() = ProjectTemplate.entries.forEach { template ->
        val directory = File(TemplateFiles::class.java.classLoader.getResource(template.root)!!.toURI())
        val shipped = directory.walkTopDown()
            .filter { it.isFile && it.name.endsWith(TemplateFiles.TEMPLATE_SUFFIX) }
            .map { it.relativeTo(directory).invariantSeparatorsPath.removeSuffix(TemplateFiles.TEMPLATE_SUFFIX) }
            .toSortedSet()
        assertEquals(shipped, TemplateFiles.load(template).map { it.path }.toSortedSet(), template.id)
    }

    @Test
    fun everyTemplateFileRendersWithTheValuesTheGeneratorProvides() = ProjectTemplate.entries.forEach { template ->
        TemplateFiles.load(template).forEach { file ->
            val rendered = TemplateRenderer.renderContent(file.content, values)
            assertTrue("{{" !in rendered, "${template.id}/${file.path}")
        }
    }

    @Test
    fun kotlinFilesDeclareThePackageOfTheFolderTheyAreWrittenTo() = ProjectTemplate.entries.forEach { template ->
        TemplateFiles.load(template).filter { it.path.endsWith(".kt") }.forEach { file ->
            val path = TemplateRenderer.renderPath(file.path, "com.example.mygame")
            val folderPackage = path.substringAfter("/kotlin/").substringBeforeLast('/').replace('/', '.')
            val declaredPackage = TemplateRenderer.renderContent(file.content, values).lines().first { it.startsWith("package ") }.removePrefix("package ")
            assertEquals(folderPackage, declaredPackage, "${template.id}/${file.path}")
        }
    }

    @Test
    fun everyTemplateRegistersAnEntryPointThatExists() = ProjectTemplate.entries.forEach { template ->
        val files = TemplateFiles.load(template)
        val registration = files.single { it.path == "src/desktopMain/resources/META-INF/services/com.pandulapeter.kubriko.creator.CreatorEntryPoint" }
        val entryPointClass = TemplateRenderer.renderContent(registration.content, values).trim()
        val entryPointPath = "src/commonMain/kotlin/" + entryPointClass.replace('.', '/') + ".kt"
        val entryPointFile = files.single { TemplateRenderer.renderPath(it.path, "com.example.mygame") == entryPointPath }
        assertTrue(": CreatorEntryPoint" in entryPointFile.content, entryPointPath)
    }
}
