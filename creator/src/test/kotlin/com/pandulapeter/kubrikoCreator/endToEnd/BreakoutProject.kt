/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(EditorMutation::class)

package com.pandulapeter.kubrikoCreator.endToEnd

import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeStore
import com.pandulapeter.kubrikoCreator.context.BreakoutFixture
import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.git.JGitRepository
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFiles
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.template.NewProjectRequest
import com.pandulapeter.kubrikoCreator.template.ProjectGenerator
import com.pandulapeter.kubrikoCreator.template.TemplateFile
import com.pandulapeter.kubrikoCreator.template.WrapperInstaller
import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * The project the end-to-end checks open: [BreakoutFixture]'s game, as the user's Breakout was when Kubriko Creator last
 * committed it, so that opening it runs what an older project meets. Its template files are template version 7's
 * (`src/test/resources/endToEnd/breakout`), and its `.kubriko/project.json` records no format version.
 */
internal object BreakoutProject {

    const val PACKAGE_NAME = "com.example.breakout"
    const val TEMPLATE_VERSION = 7
    const val FIRST_SCENE = "main"
    const val GAME_OVER_SCENE = "game-over"
    const val COMMIT_MESSAGE = "Breakout as Kubriko Creator last committed it."
    val sounds = listOf("files/sounds/wall.wav", "files/sounds/hit.wav")
    private const val TEMPLATE_ROOT = "endToEnd/breakout"

    /**
     * Creates the project in [parentDirectory], built against [configuration]'s Kubriko, and commits it into a new git
     * repository, so that a scenario's changes show in its diff. [wrapperInstaller] is Gradle's by default; a project
     * created without one can't be built.
     *
     * @return The project's folder.
     */
    fun create(parentDirectory: File, configuration: CreatorConfiguration, wrapperInstaller: WrapperInstaller? = null): File {
        val generator = if (wrapperInstaller == null) {
            ProjectGenerator(configuration, loadTemplate = { templateFiles() })
        } else {
            ProjectGenerator(configuration, wrapperInstaller = wrapperInstaller, loadTemplate = { templateFiles() })
        }
        val directory = generator.create(NewProjectRequest(name = "Breakout", packageName = PACKAGE_NAME, parentDirectory = parentDirectory, isUnderVersionControl = false))
        val store = checkNotNull(BlockTypeStore.open(directory)) { "The Breakout template has no .kubriko/project.json." }
        BreakoutFixture.types.forEach { store.save(it) }
        store.regenerateAll()
        File(directory, SceneStore.DEFAULT_SCENE_DIRECTORY).apply {
            mkdirs()
            File(this, "$FIRST_SCENE.json").writeText(SceneDocument.format(BreakoutFixture.level))
            File(this, "$GAME_OVER_SCENE.json").writeText(SceneDocument.format(emptyList()))
        }
        sounds.forEach { path -> writeSilence(File(directory, "src/commonMain/composeResources/$path")) }
        KubrikoBuildFiles(directory, configuration.kubrikoVersion, configuration.useMavenLocal).sync()
        JGitRepository.create(directory, COMMIT_MESSAGE)
        return directory
    }

    private fun templateFiles() = resource("files.txt")
        .lines()
        .filter { it.isNotBlank() }
        .map { path -> TemplateFile(path = path, content = resource("$path.template")) }

    private fun resource(path: String) = BreakoutProject::class.java.classLoader.getResourceAsStream("$TEMPLATE_ROOT/$path")
        ?.use { it.readBytes().decodeToString() }
        ?: error("$TEMPLATE_ROOT/$path is missing from the test resources.")

    /**
     * A tenth of a second of silence, as the WAV files the game's sounds are.
     */
    private fun writeSilence(file: File) {
        file.parentFile.mkdirs()
        val format = AudioFormat(44_100f, 16, 1, true, false)
        val frames = 4_410L
        AudioInputStream(ByteArrayInputStream(ByteArray((frames * format.frameSize).toInt())), format, frames).use { audio ->
            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, file)
        }
    }
}
