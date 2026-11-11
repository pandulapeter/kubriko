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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.CreatorProjectSettings
import com.pandulapeter.kubrikoCreator.actors.DefinitionPaths
import com.pandulapeter.kubrikoCreator.actors.EveryBlockTypes
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeStore
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDescriptions
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins the files of a project as each format version writes them, with one fixture project per version in
 * `src/test/resources/formats/v<version>`: the current version's decodes to [EveryBlockTypes] and is written back byte for
 * byte, and every older one is brought up to it. The family is in a folder, and two types have a description, so that every
 * field is written. [PersistedShapeTest] pins the shape; these pin names, values, and defaults.
 *
 * A released version's folder is never edited ("released" meaning present on `main` when a Kubriko Creator release was cut;
 * until releases are tagged, every folder on `main`). Raising the format version copies `v<N>` to `v<N+1>`, and edits the
 * copy to show what the raise added. A test that reads a fixture of an older version reads it through
 * [ProjectFormat.prepare]. Version 0 is kept because [ProjectFormat.MIGRATIONS] still reads it.
 */
class FormatFixturesTest {
    private val fixtures = File(assertNotNull(javaClass.getResource("/formats"), "The format fixtures are missing.").toURI()).also {
        assertTrue(it.isDirectory, "${it.path} isn't a folder.")
    }
    private val knownTypes = EveryBlockTypes.all.map { type ->
        when (type) {
            in EveryBlockTypes.family -> type.withFolder("family")
            EveryBlockTypes.everyBlockInEveryEvent -> EveryBlockTypes.everyBlockInEveryEvent.copy(description = "Uses \"every\" Block,\nin every Event.")
            EveryBlockTypes.everyManager -> EveryBlockTypes.everyManager.copy(description = "Keeps the score.")
            else -> type
        }
    }.sortedBy { it.name }
    private val settingsJson = Json { ignoreUnknownKeys = true }

    @Test
    fun theCurrentFormatDecodesToTheKnownTypes() {
        val project = copyOfFixture(ProjectFormat.CURRENT_VERSION)
        val loaded = BlockTypeStore.open(project)!!.load()
        assertEquals(emptyList(), loaded.unreadableFiles)
        assertEquals(knownTypes, loaded.types)
        val textsBefore = textsOf(project)
        assertEquals(ProjectFormatStatus.Writable, ProjectFormat(project, "test").prepare())
        assertEquals(textsBefore, textsOf(project))
    }

    @Test
    fun theCurrentFormatIsWrittenByteForByte() {
        val fixture = copyOfFixture(ProjectFormat.CURRENT_VERSION)
        knownTypes.forEach { type ->
            val project = testDirectory()
            File(fixture, BlockTypeStore.SETTINGS_FILE).copyTo(File(project, BlockTypeStore.SETTINGS_FILE))
            BlockTypeStore.open(project)!!.save(type)
            val path = DefinitionPaths.definitionPath(type)
            assertEquals(File(fixture, path).readText(), File(project, path).readText(), "$path is written differently.")
        }
    }

    @Test
    fun projectSettingsKeepTheirFieldNames() {
        val text = File(copyOfFixture(ProjectFormat.CURRENT_VERSION), BlockTypeStore.SETTINGS_FILE).readText()
        assertEquals(
            CreatorProjectSettings(
                packageName = "com.example.formats",
                templateVersion = 9,
                formatVersion = ProjectFormat.CURRENT_VERSION,
                minimumCreatorVersion = "0.0.0-fixture",
            ),
            settingsJson.decodeFromString(CreatorProjectSettings.serializer(), text),
        )
    }

    @Test
    fun sceneDescriptionsAndScenesKeepTheirShape() {
        val project = copyOfFixture(ProjectFormat.CURRENT_VERSION)
        assertEquals(
            mapOf(
                "level" to "The first level, with \"quotes\".",
                "menu" to "The title screen.",
            ),
            SceneDescriptions(project, OwnWrites()).all(),
        )
        val levelText = File(project, "src/commonMain/composeResources/files/scenes/level.json").readText()
        val parsed = SceneDocument.parse(levelText)
        assertEquals(
            listOf(
                SceneActorEntry(
                    typeId = "EveryBlock",
                    state = """{"body":{"position":{"x":10.0,"y":-20.5},"size":{"width":64.0,"height":32.0}}}""",
                    id = "a1b2c3",
                ),
                SceneActorEntry(
                    typeId = "wall",
                    state = """{"body":{"position":{"x":0.0,"y":100.0}},"isSolid":true}""",
                    id = "i1",
                ),
            ),
            parsed,
        )
        assertEquals(levelText, SceneDocument.format(parsed))
    }

    @Test
    fun everyOlderFormatIsBroughtUpToTheCurrentOne() {
        (0 until ProjectFormat.CURRENT_VERSION).forEach { version ->
            val project = copyOfFixture(version)
            assertEquals(ProjectFormatStatus.Writable, ProjectFormat(project, "test").prepare())
            val loaded = BlockTypeStore.open(project)!!.load()
            assertEquals(emptyList(), loaded.unreadableFiles, "v$version has unreadable files.")
            assertEquals(knownTypes.map { it.asOf(version) }, loaded.types, "v$version isn't brought up to the current format.")
            val settings = settingsJson.decodeFromString(
                CreatorProjectSettings.serializer(),
                File(project, BlockTypeStore.SETTINGS_FILE).readText(),
            )
            assertEquals(ProjectFormat.CURRENT_VERSION, settings.formatVersion)
        }
    }

    @Test
    fun thereIsAFixtureForEveryFormatVersion() {
        assertEquals(
            (0..ProjectFormat.CURRENT_VERSION).mapTo(sortedSetOf()) { "v$it" },
            fixtures.listFiles().orEmpty().filter { it.isDirectory }.mapTo(sortedSetOf()) { it.name },
            "Raising the format version copies the folder of the previous version to a new one, edited to show what it adds; " +
                "a released version's folder is never edited.",
        )
    }

    /**
     * [this] type as a project of format [version] could hold it, without what later versions added, which bringing it
     * up to the current format doesn't make up.
     */
    private fun BlockTypeDefinition.asOf(version: Int): BlockTypeDefinition {
        if (version >= 4) return this
        val withoutAnimation = withEvents(events.map { event -> event.copy(blocks = event.blocks.filterNot { it is BlockDefinition.SetFrame || it is BlockDefinition.SetAnimation }) })
        return if (withoutAnimation is ActorTypeDefinition) withoutAnimation.copy(tintsSprite = false) else withoutAnimation
    }

    private fun copyOfFixture(version: Int): File {
        val fixture = File(fixtures, "v$version")
        assertTrue(fixture.isDirectory, "The fixture of format version $version is missing.")
        return testDirectory().also { fixture.copyRecursively(it, overwrite = true) }
    }

    private fun textsOf(directory: File) = directory.walkTopDown()
        .filter { it.isFile }
        .associate { it.relativeTo(directory).invariantSeparatorsPath to it.readText() }
}
