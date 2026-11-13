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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeStore
import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.git.JGitRepository
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.project.ProjectFormat
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.testing.testConfiguration
import org.junit.Assume.assumeFalse
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
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
    private val configuration = testConfiguration()
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
    fun aProjectIsntCreatedOnAKubrikoVersionThatCantBeDownloaded() {
        val generator = ProjectGenerator(configuration, { _, _ -> wrapperInstallations += parentDirectory to "" }, loadTemplate = { template }, lookUpKubriko = { KubrikoAvailability.Lookup.MISSING })
        val exception = assertFailsWith<ProjectCreationException> { generator.create(request) }
        assertEquals(TemplateProblem.KUBRIKO_UNAVAILABLE, exception.reason)
        assertEquals(listOf("1.0.0"), exception.arguments)
        assertFalse(File(parentDirectory, "my-game").exists())
        assertTrue(wrapperInstallations.isEmpty())
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
        assertEquals(TemplateProblem.FOLDER_NOT_EMPTY, exception.reason)
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
        assertEquals(TemplateProblem.NOT_CREATED, exception.reason)
        assertEquals(listOf("Offline."), exception.arguments)
        assertFalse(File(parentDirectory, "my-game").exists(), "what was written is removed, so trying again works")
    }

    @Test
    fun aWrapperDownloadWithoutANetworkSaysSo() {
        val exception = assertFailsWith<ProjectCreationException> {
            generator(wrapperInstaller = { _, _ -> throw IllegalStateException("Could not install Gradle distribution.", UnknownHostException("services.gradle.org")) }).create(request)
        }
        assertEquals(TemplateProblem.NOT_CREATED_OFFLINE, exception.reason)
        assertFalse(File(parentDirectory, "my-game").exists())
    }

    @Test
    fun aKubrikoVersionThatCantBeLookedUpSaysTheNetworkIsMissing() {
        val generator = ProjectGenerator(configuration, { _, _ -> }, loadTemplate = { template }, lookUpKubriko = { KubrikoAvailability.Lookup.UNREACHABLE })
        assertEquals(TemplateProblem.NOT_CREATED_OFFLINE, assertFailsWith<ProjectCreationException> { generator.create(request) }.reason)
        assertFalse(File(parentDirectory, "my-game").exists())
    }

    @Test
    fun aRepositoryAnsweringWithAnErrorIsToldApartFromAMissingNetwork() {
        val generator = ProjectGenerator(configuration, { _, _ -> }, loadTemplate = { template }, lookUpKubriko = { KubrikoAvailability.Lookup.REFUSED })
        assertEquals(TemplateProblem.KUBRIKO_REFUSED, assertFailsWith<ProjectCreationException> { generator.create(request) }.reason)
    }

    @Test
    fun aLocationKubrikoCreatorMayNotWriteInIsNamedBeforeAnythingIsWritten() {
        val location = File(parentDirectory, "locked").apply { mkdirs() }
        location.setWritable(false)
        try {
            assumeFalse("the folder still takes new files", runCatching { File(location, "probe").createNewFile() }.getOrDefault(false))
            val exception = assertFailsWith<ProjectCreationException> { generator().create(request.copy(parentDirectory = File(location, "games"))) }
            assertEquals(TemplateProblem.NOT_CREATED_NOT_ALLOWED, exception.reason)
            assertEquals(listOf(location.absolutePath), exception.arguments)
            assertTrue(wrapperInstallations.isEmpty())
        } finally {
            location.setWritable(true)
        }
    }

    @Test
    fun aCreationInterruptedDuringTheWrapperDownloadRemovesWhatItWrote() {
        listOf(
            WrapperInstaller { _, _ -> throw InterruptedException() },
            WrapperInstaller { _, _ -> Thread.currentThread().interrupt() },
            WrapperInstaller { _, _ ->
                Thread.currentThread().interrupt()
                throw IOException("Closed by interrupt.")
            },
        ).forEach { installer ->
            try {
                assertFailsWith<InterruptedException> { generator(wrapperInstaller = installer).create(request) }
                assertFalse(File(parentDirectory, "my-game").exists())
            } finally {
                Thread.interrupted()
            }
        }
    }

    @Test
    fun aCreationInterruptedWhileItsTemplateIsWrittenStopsBeforeTheNextStep() {
        val generator = ProjectGenerator(
            configuration,
            wrapperInstaller = { directory, version -> wrapperInstallations += directory to version },
            loadTemplate = { template.also { Thread.currentThread().interrupt() } },
        )
        try {
            assertFailsWith<InterruptedException> { generator.create(request) }
            assertFalse(File(parentDirectory, "my-game").exists())
            assertTrue(wrapperInstallations.isEmpty())
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun aCreationInterruptedWhileCommittingRemovesWhatItWrote() {
        val generator = ProjectGenerator(configuration, { _, _ -> }, loadTemplate = { template }, startVersionControl = { _, _ -> Thread.currentThread().interrupt() })
        try {
            assertFailsWith<InterruptedException> { generator.create(request) }
            assertFalse(File(parentDirectory, "my-game").exists())
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun aCreationInterruptedBeforeItStartsWritesNothing() {
        val existing = File(parentDirectory, "my-game").apply { mkdirs() }
        Thread.currentThread().interrupt()
        try {
            assertFailsWith<InterruptedException> { generator().create(request) }
            assertEquals(emptyList(), existing.list()?.toList(), "a folder that was there stays, empty")
            assertTrue(wrapperInstallations.isEmpty())
        } finally {
            Thread.interrupted()
        }
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
        assertEquals(ProjectFormatStatus.Writable, ProjectFormat(directory, creatorVersion = "1.0.0").prepare())
        assertTrue("\"formatVersion\": ${ProjectFormat.CURRENT_VERSION}," in File(directory, ".kubriko/project.json").readText())
        assertTrue("\"templateVersion\": ${ProjectUpgrader.CURRENT_VERSION}," in File(directory, ".kubriko/project.json").readText())
    }

    @Test
    fun theStartersBallBouncesInsideTheScene() {
        val directory = ProjectGenerator(configuration, wrapperInstaller = { _, _ -> }).create(request)
        val ball = assertNotNull(BlockTypeStore.open(directory)).load().types.single { it.name == "Ball" } as ActorTypeDefinition
        assertTrue(ball.staysInsideScene)
        assertTrue(ball.events.any { event -> event.type == EventType.EDGE_TOUCHED && BlockDefinition.BounceOffEdge in event.blocks })
    }

    @Test
    fun aNameWithAccentsReachesWhatPlayersSeeAndIsFoldedEverywhereElse() {
        val name = "Kalandjáték"
        val directory = ProjectGenerator(configuration, wrapperInstaller = { _, _ -> }, startVersionControl = { _, _ -> })
            .create(NewProjectRequest(name = name, packageName = ProjectNaming.defaultPackageName(name), parentDirectory = parentDirectory))
        assertEquals(File(parentDirectory, "kalandjatek"), directory)
        assertTrue("rootProject.name = \"kalandjatek\"" in File(directory, "settings.gradle.kts").readText())
        assertTrue(File(directory, "src/commonMain/kotlin/com/example/kalandjatek/GameEntryPoint.kt").isFile)
        assertEquals(name, GameIdentity.read(directory)?.name)
        assertTrue(File(directory, "README.md").readText().startsWith("# $name\n"))
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

    private val noTargetsHost = object : TargetHost {
        override fun androidSdk(rootDirectory: File): File? = null

        override val canBuildIos: Boolean = false
    }

    private fun starterGenerator() = ProjectGenerator(
        configuration = configuration,
        wrapperInstaller = { _, _ -> },
        startVersionControl = { _, _ -> },
        targetHost = noTargetsHost,
    )

    @Test
    fun aProjectIsCreatedForTheTargetsAskedFor() {
        val directory = starterGenerator().create(request.copy(targets = emptySet()))
        val targets = GameTargets(directory, configuration.android, noTargetsHost).read()!!
        assertEquals(listOf(GameTarget.DESKTOP), targets.filter { it.isOn }.map { it.target })
    }

    @Test
    fun aTargetThisComputerCantBuildForCreatesNoProject() {
        val exception = assertFailsWith<ProjectCreationException> { starterGenerator().create(request.copy(targets = setOf(GameTarget.ANDROID))) }
        assertEquals(TemplateProblem.TARGET_NOT_SET_UP, exception.reason)
        assertEquals(TargetProblem.ANDROID_SDK_MISSING, exception.targetProblem)
        assertFalse(File(parentDirectory, "my-game").exists())
    }
}
