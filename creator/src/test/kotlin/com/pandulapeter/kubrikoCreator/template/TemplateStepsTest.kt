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

import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.project.withUnixLineBreaks
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TemplateStepsTest {
    private val parentDirectory = createTempDirectory().toFile()
    private val configuration = CreatorConfiguration(kubrikoVersion = "1.0.0", kotlinVersion = "2.4.21", composeVersion = "1.12.1", gradleVersion = "9.7.1", useMavenLocal = false)
    private val directory = ProjectGenerator(configuration, wrapperInstaller = { _, _ -> }, startVersionControl = { _, _ -> })
        .create(NewProjectRequest(name = "My Game", packageName = "com.example.mygame", parentDirectory = parentDirectory))
    private val context = TemplateContext("com.example.mygame", "src/commonMain/kotlin", { path -> File(directory, path).takeIf { it.isFile }?.readText()?.withUnixLineBreaks() }) { "my-game" }

    @AfterTest
    fun tearDown() {
        parentDirectory.deleteRecursively()
    }

    @Test
    fun everyVersionFromTwoToTheCurrentHasAStep() {
        assertEquals((2..ProjectUpgrader.CURRENT_VERSION).toSet(), TemplateSteps.FILES.flatMap { it.steps }.mapTo(HashSet()) { it.version })
        TemplateSteps.FILES.forEach { file ->
            val versions = file.steps.map { it.version }
            assertEquals(versions.sorted().distinct(), versions, "the steps of ${file.path(context)}")
        }
    }

    @Test
    fun everyStepIsDoneForANewProject() {
        TemplateSteps.FILES.forEach { file ->
            val path = file.path(context)
            val text = context.read(path) ?: return@forEach
            file.steps.forEach { step ->
                assertTrue(step.isDone(text, context), "${step::class.simpleName} (version ${step.version}) isn't done for a new project's $path")
            }
        }
    }

    @Test
    fun aFileTheUserChangedTooMuchIsLeftToThemOnce() {
        val main = File(directory, context.mainPath)
        main.writeText("package com.example.mygame\n\nfun main() {\n    application { Game() }\n}\n")
        File(directory, ".kubriko/project.json").writeText("""{ "packageName": "com.example.mygame", "sourceDirectory": "src/commonMain/kotlin", "templateVersion": 4 }""")
        val upgrade = assertIs<TemplateUpgrade.Needed>(ProjectUpgrader(directory).check())
        assertEquals(listOf(context.mainPath), upgrade.byHand)
        assertEquals(emptyList(), upgrade.changes.map { it.path })
    }
}
