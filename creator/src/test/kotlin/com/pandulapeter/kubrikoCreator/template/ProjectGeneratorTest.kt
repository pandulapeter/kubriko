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

import com.pandulapeter.kubrikoCreator.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.git.JGitRepository
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_project_folder_not_empty
import kubriko.creator.generated.resources.error_project_not_created
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProjectGeneratorTest {
    private val parentDirectory = createTempDirectory().toFile()
    private val configuration = CreatorConfiguration(kubrikoVersion = "1.0.0", kotlinVersion = "2.4.21", composeVersion = "1.12.1", gradleVersion = "9.7.1", useMavenLocal = false)
    private val request = NewProjectRequest(name = "My Game", packageName = "com.example.mygame", parentDirectory = parentDirectory)
    private val wrapperInstallations = mutableListOf<Pair<File, String>>()
    private val template = listOf(
        TemplateFile("settings.gradle.kts", "rootProject.name = \"{{PROJECT_ID}}\"\n{{EXTRA_REPOSITORIES}}"),
        TemplateFile("dot-gitignore", "build/"),
        TemplateFile("src/__PACKAGE__/Game.kt", "package {{PACKAGE}} // {{PROJECT_NAME}}, Kubriko {{KUBRIKO_VERSION}}, Kotlin {{KOTLIN_VERSION}}, Compose {{COMPOSE_VERSION}}"),
    )

    private val versionControlStarts = mutableListOf<Pair<File, String>>()

    private fun generator(
        configuration: CreatorConfiguration = this.configuration,
        wrapperInstaller: WrapperInstaller = WrapperInstaller { directory, version -> wrapperInstallations += directory to version },
    ) = ProjectGenerator(configuration, wrapperInstaller, loadTemplate = { template }, startVersionControl = { directory, message -> versionControlStarts += directory to message })

    @AfterTest
    fun tearDown() {
        parentDirectory.deleteRecursively()
    }

    @Test
    fun theProjectIsWrittenToAFolderNamedAfterItsId() {
        val directory = generator().create(request)
        assertEquals(File(parentDirectory, "my-game"), directory)
        assertEquals("rootProject.name = \"my-game\"\n", File(directory, "settings.gradle.kts").readText())
        assertEquals("build/", File(directory, ".gitignore").readText())
        assertEquals(
            "package com.example.mygame // My Game, Kubriko 1.0.0, Kotlin 2.4.21, Compose 1.12.1",
            File(directory, "src/com/example/mygame/Game.kt").readText(),
        )
    }

    @Test
    fun theWrapperIsInstalledLastWithTheBundledGradleVersion() {
        val directory = generator(wrapperInstaller = { installedDirectory, version ->
            assertTrue(File(installedDirectory, "settings.gradle.kts").isFile)
            wrapperInstallations += installedDirectory to version
        }).create(request)
        assertEquals(listOf(directory to "9.7.1"), wrapperInstallations)
    }

    @Test
    fun mavenLocalIsAddedOnlyWhenAskedFor() {
        val directory = generator(configuration.copy(useMavenLocal = true)).create(request)
        assertTrue("mavenLocal" in File(directory, "settings.gradle.kts").readText())
    }

    @Test
    fun anExistingEmptyFolderIsUsed() {
        File(parentDirectory, "my-game").mkdirs()
        assertTrue(File(generator().create(request), "settings.gradle.kts").isFile)
    }

    @Test
    fun aFolderWithFilesIsNeverOverwritten() {
        val existing = File(parentDirectory, "my-game/notes.txt").apply { parentFile.mkdirs(); writeText("Mine") }
        val exception = assertFailsWith<ProjectCreationException> { generator().create(request) }
        assertEquals(Res.string.error_project_folder_not_empty, exception.reason)
        assertEquals("Mine", existing.readText())
        assertFalse(File(parentDirectory, "my-game/settings.gradle.kts").exists())
        assertTrue(wrapperInstallations.isEmpty())
    }

    @Test
    fun aFileInTheProjectsPlaceIsNeverOverwritten() {
        File(parentDirectory, "my-game").writeText("Mine")
        assertFailsWith<ProjectCreationException> { generator().create(request) }
    }

    @Test
    fun aFailingWrapperInstallationIsReportedAsAFailedCreation() {
        val exception = assertFailsWith<ProjectCreationException> {
            generator(wrapperInstaller = { _, _ -> throw IOException("Offline.") }).create(request)
        }
        assertEquals(Res.string.error_project_not_created, exception.reason)
        assertFalse(File(parentDirectory, "my-game").exists(), "what was written is removed, so trying again works")
    }

    @Test
    fun aFailedCreationInAnExistingEmptyFolderLeavesItEmpty() {
        val directory = File(parentDirectory, "my-game").apply { mkdirs() }
        assertFailsWith<ProjectCreationException> { generator(wrapperInstaller = { _, _ -> throw IOException("Offline.") }).create(request) }
        assertEquals(emptyList(), directory.list()?.toList())
    }

    @Test
    fun theStarterTemplateGeneratesARunnableLayout() {
        val directory = ProjectGenerator(configuration, wrapperInstaller = { _, _ -> }).create(request)
        val repository = assertNotNull(JGitRepository.open(directory))
        assertEquals(listOf(ProjectGenerator.FIRST_COMMIT_MESSAGE), repository.history(10).map { it.message })
        assertTrue(repository.status().changes.isEmpty(), repository.status().changes.toString())
        listOf(
            "settings.gradle.kts",
            "build.gradle.kts",
            "gradle/libs.versions.toml",
            ".gitignore",
            "src/commonMain/kotlin/com/example/mygame/GameEntryPoint.kt",
            "src/commonMain/composeResources/files/scenes/main.json",
            ".kubriko/project.json",
            ".kubriko/actors/Ball.json",
            "src/commonMain/kotlin/com/example/mygame/generated/Ball.kt",
            "src/commonMain/kotlin/com/example/mygame/generated/GeneratedActorTypes.kt",
        ).forEach { assertTrue(File(directory, it).isFile, it) }
        assertEquals(
            "com.example.mygame.GameEntryPoint",
            File(directory, "src/desktopMain/resources/META-INF/services/com.pandulapeter.kubriko.creator.CreatorEntryPoint").readText().trim(),
        )
        assertTrue("kubriko = \"1.0.0\"" in File(directory, "gradle/libs.versions.toml").readText())
    }

    @Test
    fun theProjectStartsUnderVersionControlAfterEverythingIsWritten() {
        val directory = generator(wrapperInstaller = { installed, version -> wrapperInstallations += installed to version }).create(request)
        assertEquals(listOf(directory to ProjectGenerator.FIRST_COMMIT_MESSAGE), versionControlStarts)
        assertEquals(1, wrapperInstallations.size)
    }

    @Test
    fun versionControlCanBeLeftOut() {
        generator().create(request.copy(isUnderVersionControl = false))
        assertTrue(versionControlStarts.isEmpty())
    }
}
