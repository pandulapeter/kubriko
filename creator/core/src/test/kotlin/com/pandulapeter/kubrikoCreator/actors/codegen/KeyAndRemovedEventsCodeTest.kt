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
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.project.KubrikoArtifact
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The key released, key held, and removed from the Scene Events.
 */
class KeyAndRemovedEventsCodeTest {
    private val log = listOf(BlockDefinition.Log("Hello"))

    private fun actor(vararg events: EventDefinition) = ActorTypeCodeGenerator.generateActorType(ActorTypeDefinition(name = "Ship", events = events.toList()), "com.example.game", listOf("Ship"))

    private fun manager(vararg events: EventDefinition) = ManagerTypeDefinition(name = "Game", events = events.toList()).let { game ->
        ManagerTypeCodeGenerator.generateManagerType(game, "com.example.game", listOf("Ship"), listOf(game))
    }

    @Test
    fun anActorHearsAboutAKeyLetGoOfOnlyWhileTheGameRuns() {
        val code = actor(EventDefinition(EventType.KEY_RELEASED, key = KeyChoice.SPACE, blocks = log))
        assertContains(code, "KeyboardInputAware")
        assertContains(code, "    override fun onKeyReleased(key: Key) {\n        if (!stateManager.isRunning.value) return\n        when (key) {\n            Key.Spacebar -> onKeyReleasedSpacebar()\n")
        assertFalse("override fun onKeyPressed" in code)
    }

    @Test
    fun aHeldKeyIsAskedAboutEveryFrameWithoutTheTrait() {
        val code = actor(EventDefinition(EventType.KEY_HELD, key = KeyChoice.A, blocks = log), EventDefinition(EventType.KEY_HELD, key = KeyChoice.SPACE, blocks = log))
        assertFalse("KeyboardInputAware" in code)
        assertContains(code, "        if (keyboardInputManager.isKeyPressed(Key.Spacebar)) whileKeyHeldSpacebar()\n        if (keyboardInputManager.isKeyPressed(Key.A)) whileKeyHeldA()\n")
        assertContains(code, "lateinit var keyboardInputManager: KeyboardInputManager")
    }

    @Test
    fun aFamilyWhoseHeldKeyHasNoBlocksStillLooksUpTheKeyboardItsHookAsksAbout() {
        val ship = ActorTypeDefinition(name = "Ship", events = listOf(EventDefinition(EventType.KEY_HELD, key = KeyChoice.A), EventDefinition(EventType.EVERY_FRAME, blocks = log)))
        val fighter = ActorTypeDefinition(name = "Fighter", parent = "Ship")
        val code = ActorTypeCodeGenerator.generateActorType(ship, "com.example.game", listOf("Ship", "Fighter"), actorTypes = listOf(ship, fighter))
        assertContains(code, "if (keyboardInputManager.isKeyPressed(Key.A)) whileKeyHeldA()")
        assertContains(code, "lateinit var keyboardInputManager: KeyboardInputManager")
    }

    @Test
    fun theRemovedEventRunsOnceWhenABlockFirstMarksTheActorRemoved() {
        val code = actor(EventDefinition(EventType.REMOVED_FROM_SCENE, blocks = log))
        assertContains(code, "    var isRemoved = false\n        set(value) {\n            val wasRemoved = field\n            field = value\n            if (value && !wasRemoved) onRemovedFromScene()\n        }\n")
        assertFalse("set(value)" in actor(EventDefinition(EventType.REMOVED_FROM_SCENE)))
    }

    @Test
    fun aManagerHearsAboutKeysLetGoOfEvenWhilePausedAndAsksAboutHeldOnesOnlyWhileRunning() {
        val code = manager(EventDefinition(EventType.KEY_RELEASED, key = KeyChoice.SPACE, blocks = log), EventDefinition(EventType.KEY_HELD, key = KeyChoice.A, blocks = log))
        assertContains(code, "    override fun onKeyReleased(key: Key) {\n        when (key) {\n            Key.Spacebar -> onKeyReleasedSpacebar()\n")
        assertContains(code, "        actorManager.add(this)\n")
        assertContains(code, "        if (isInSceneEditor || !stateManager.isRunning.value) return\n        if (keyboardInputManager.isKeyPressed(Key.A)) whileKeyHeldA()\n")
        assertFalse("onRemovedFromScene" in manager(EventDefinition(EventType.REMOVED_FROM_SCENE, blocks = log)))
    }

    @Test
    fun everyKeyEventUsesTheKeyboard() {
        val game = ManagerTypeDefinition(name = "Game", events = listOf(EventDefinition(EventType.KEY_RELEASED, key = KeyChoice.SPACE), EventDefinition(EventType.KEY_HELD, key = KeyChoice.A)))
        assertEquals(
            listOf(PluginUse(KubrikoArtifact.KEYBOARD_INPUT, PluginReason.KEY_EVENT, "Game", "Spacebar"), PluginUse(KubrikoArtifact.KEYBOARD_INPUT, PluginReason.KEY_EVENT, "Game", "A")),
            PluginUse.of(listOf(game)),
        )
    }
}
