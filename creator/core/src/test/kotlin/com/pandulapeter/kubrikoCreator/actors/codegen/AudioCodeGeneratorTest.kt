/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.SoundValue
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioCodeGeneratorTest {
    private val code = AudioCodeGenerator.generate("com.example.game", setOf("files/sounds/pop.wav", "files/sounds/hit.wav"), emptySet())

    @Test
    fun itIsAManagerThatPlaysNothingInTheSceneEditor() {
        assertTrue(GeneratedFile.isUnchanged(code))
        assertContains(code, "package com.example.game.generated\n")
        assertContains(code, "import com.example.game.resources.Res\n")
        assertContains(code, "class GeneratedAudio(\n    private val isInSceneEditor: Boolean = false,\n) : Manager() {\n")
        assertContains(code, "    override fun onInitialize(kubriko: Kubriko) {\n        if (isInSceneEditor) return\n")
        assertContains(code, "    fun playSound(path: String) {\n        if (!isInSceneEditor && path.isNotEmpty()) soundManager.play(uriOf(path))\n")
    }

    @Test
    fun everyFileItPlaysIsPreloadedAndItsUriLookedUpOnce() {
        assertContains(code, "        val SOUNDS = listOf(\n            \"files/sounds/hit.wav\",\n            \"files/sounds/pop.wav\",\n        )\n")
        assertContains(code, "        val MUSIC = emptyList<String>()\n")
        assertContains(code, "        (SOUNDS + MUSIC).forEach { uris[it] = Res.getUri(it) }\n")
        assertContains(code, "        soundManager.preload(SOUNDS.map(::uriOf))\n        musicManager.preload(MUSIC.map(::uriOf))\n")
    }

    @Test
    fun musicStartsOnTheNextFrameAndAgainWhenTheGameIsFocused() {
        assertContains(code, "        if (path != music) music?.let { musicManager.stop(uriOf(it)) }\n")
        assertContains(code, "        val playableVolume = if (volume.isNaN()) 0f else volume.coerceIn(0f, 1f)\n")
        assertContains(code, "        if (path != music || playableVolume != musicVolume) {\n            musicVolume = playableVolume\n            musicManager.setVolume(uriOf(path), playableVolume, playableVolume)\n        }\n")
        assertContains(code, "kubriko.get<StateManager>().isFocused.filter { it }.onEach { isMusicPending = music != null }.launchIn(scope)\n")
        assertContains(code, "            music?.let { musicManager.play(uriOf(it), shouldLoop = isMusicLooping) }\n")
    }

    @Test
    fun aScenesMusicPlaysWhenItsSettingsChange() {
        assertContains(code, "        if (settings !== sceneSettings) {\n            sceneSettings = settings\n            if (settings != null && settings.music.isNotEmpty()) playMusic(settings.music)\n        }\n")
    }

    @Test
    fun aSoundsVolumeIsPassedOnOnlyInAGameThatPlaysOneAtLessThanFull() {
        val withVolume = AudioCodeGenerator.generate("com.example.game", setOf("files/sounds/hit.wav"), emptySet(), usesSoundVolume = true)
        assertContains(withVolume, "    fun playSound(path: String, volume: Float = 1f) {\n        if (!isInSceneEditor && path.isNotEmpty()) soundManager.play(uriOf(path), volume)\n")
        assertFalse("soundManager.play(uriOf(path), volume)" in code)
        val loud = ActorTypeDefinition(name = "Ball", events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.PlaySound(SoundValue.Fixed("files/sounds/hit.wav"))))))
        val quiet = loud.copy(events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.PlaySound(SoundValue.Fixed("files/sounds/hit.wav"), NumberValue.Fixed(50f))))))
        assertFalse(PluginManagers.of(listOf(loud)).usesSoundVolume)
        assertTrue(PluginManagers.of(listOf(quiet)).usesSoundVolume)
    }
}
