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

import kotlin.test.Test
import kotlin.test.assertContains
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
        assertContains(code, "        if (!isInSceneEditor) soundManager.play(uriOf(path))\n")
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
        assertContains(code, "kubriko.get<StateManager>().isFocused.filter { it }.onEach { isMusicPending = music != null }.launchIn(scope)\n")
        assertContains(code, "            music?.let { musicManager.play(uriOf(it), shouldLoop = isMusicLooping) }\n")
    }

    @Test
    fun aScenesMusicPlaysWhenItsSettingsChange() {
        assertContains(code, "        if (settings !== sceneSettings) {\n            sceneSettings = settings\n            if (settings != null && settings.music.isNotEmpty()) playMusic(settings.music)\n        }\n")
    }
}
