/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.INDENT
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.stringLiteral

/**
 * The Manager that plays sounds and music for the audio Blocks and each Scene's music. Generated only into projects
 * that play something ([PluginManagers.usesAudio]), so that the others don't depend on the audio plugin.
 */
internal object AudioCodeGenerator {

    const val AUDIO_NAME = "GeneratedAudio"

    /**
     * @param sounds The sound files the game plays, by their path from `composeResources`.
     * @param music The music files the game plays, by their path from `composeResources`.
     */
    fun generate(packageName: String, sounds: Collection<String>, music: Collection<String>) = GeneratedFile.withHeader("", buildString {
        appendLine("package $packageName.${ActorTypeCodeGenerator.GENERATED_PACKAGE}")
        appendLine()
        listOf(
            "com.pandulapeter.kubriko.Kubriko",
            "com.pandulapeter.kubriko.audioPlayback.MusicManager",
            "com.pandulapeter.kubriko.audioPlayback.SoundManager",
            "com.pandulapeter.kubriko.helpers.extensions.get",
            "com.pandulapeter.kubriko.manager.Manager",
            "com.pandulapeter.kubriko.manager.StateManager",
            "kotlin.concurrent.Volatile",
            "kotlinx.coroutines.flow.filter",
            "kotlinx.coroutines.flow.launchIn",
            "kotlinx.coroutines.flow.onEach",
            "$packageName.resources.Res",
        ).forEach { appendLine("import $it") }
        appendLine()
        appendLine(
            """
            |/**
            | * Plays the sounds and music of the game: those of the Blocks that play them, and each Scene's music, which starts
            | * when the Scene does, unless it is the music playing already.
            | *
            | * @param isInSceneEditor True in the Scene editor, where nothing plays.
            | */
            |class $AUDIO_NAME(
            |    private val isInSceneEditor: Boolean = false,
            |) : Manager() {
            |    private lateinit var musicManager: MusicManager
            |    private lateinit var soundManager: SoundManager
            |    private lateinit var generatedScenes: ${ScenesCodeGenerator.SCENES_NAME}
            |    private val uris = HashMap<String, String>()
            |    private var music: String? = null
            |    private var isMusicLooping = true
            |    private var sceneSettings: ${SceneSettingsCodeGenerator.SETTINGS_NAME}? = null
            |
            |    /**
            |     * Whether [music] has to be played (again) on the next frame, once the music player exists.
            |     */
            |    @Volatile
            |    private var isMusicPending = false
            |
            |    override fun onInitialize(kubriko: Kubriko) {
            |        if (isInSceneEditor) return
            |        musicManager = kubriko.get()
            |        soundManager = kubriko.get()
            |        generatedScenes = kubriko.get()
            |        (SOUNDS + MUSIC).forEach { uris[it] = Res.getUri(it) }
            |        soundManager.preload(SOUNDS.map(::uriOf))
            |        musicManager.preload(MUSIC.map(::uriOf))
            |        // The music manager pauses the music when the game loses focus, and leaves resuming it to the game.
            |        kubriko.get<StateManager>().isFocused.filter { it }.onEach { isMusicPending = music != null }.launchIn(scope)
            |    }
            |
            |    override fun onUpdate(deltaTimeInMilliseconds: Int) {
            |        if (isInSceneEditor) return
            |        val settings = generatedScenes.settings
            |        if (settings !== sceneSettings) {
            |            sceneSettings = settings
            |            if (settings != null && settings.music.isNotEmpty()) playMusic(settings.music)
            |        }
            |        if (isMusicPending) {
            |            isMusicPending = false
            |            music?.let { musicManager.play(uriOf(it), shouldLoop = isMusicLooping) }
            |        }
            |    }
            |
            |    /**
            |     * Plays the sound at [path] (from `composeResources`) once.
            |     */
            |    fun playSound(path: String) {
            |        if (!isInSceneEditor) soundManager.play(uriOf(path))
            |    }
            |
            |    /**
            |     * Plays the music at [path] (from `composeResources`) instead of the music playing now, or goes on with it if it
            |     * is the same.
            |     */
            |    fun playMusic(path: String, isLooping: Boolean = true) {
            |        if (isInSceneEditor) return
            |        if (path != music) music?.let { musicManager.stop(uriOf(it)) }
            |        music = path
            |        isMusicLooping = isLooping
            |        isMusicPending = true
            |    }
            |
            |    fun stopMusic() {
            |        if (isInSceneEditor) return
            |        music?.let { musicManager.stop(uriOf(it)) }
            |        music = null
            |        isMusicPending = false
            |    }
            |
            |    private fun uriOf(path: String) = uris[path] ?: Res.getUri(path)
            |
            |    private companion object {
            """.trimMargin(),
        )
        appendFileList("SOUNDS", sounds)
        appendFileList("MUSIC", music)
        appendLine("    }")
        appendLine("}")
    })

    private fun StringBuilder.appendFileList(name: String, paths: Collection<String>) {
        if (paths.isEmpty()) {
            appendLine("$INDENT${INDENT}val $name = emptyList<String>()")
        } else {
            appendLine("$INDENT${INDENT}val $name = listOf(")
            paths.sorted().forEach { appendLine("$INDENT$INDENT$INDENT${stringLiteral(it)},") }
            appendLine("$INDENT$INDENT)")
        }
    }
}
