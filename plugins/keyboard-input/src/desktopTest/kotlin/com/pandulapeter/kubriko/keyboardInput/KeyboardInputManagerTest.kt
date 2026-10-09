/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.keyboardInput

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlinx.collections.immutable.ImmutableSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Focus loss (which releases every held key) is not covered: a headless instance is never shown, so it stays focused
 * and there is no public way to drive `StateManager.isFocused` from a test.
 */
class KeyboardInputManagerTest {

    @Test
    fun keyTappedBetweenTwoTicksIsReportedForExactlyOneTick() = withKeyboard { kubriko, manager, actor ->
        manager.onKeyPressed(Key.A)
        manager.onKeyReleased(Key.A)
        kubriko.tick(count = 3)

        assertEquals(listOf(setOf(Key.A), emptySet()), actor.activeKeySets)
    }

    @Test
    fun heldKeyIsReportedEveryTickAndItsReleaseOnce() = withKeyboard { kubriko, manager, actor ->
        manager.onKeyPressed(Key.A)
        kubriko.tick(count = 3)
        manager.onKeyReleased(Key.A)
        kubriko.tick(count = 3)

        assertEquals(listOf(setOf(Key.A), setOf(Key.A), setOf(Key.A), emptySet()), actor.activeKeySets)
    }

    @Test
    fun pressingAHeldKeyAgainDoesNotReportItTwice() = withKeyboard { kubriko, manager, actor ->
        manager.onKeyPressed(Key.A)
        kubriko.tick()
        manager.onKeyPressed(Key.A)
        kubriko.tick()

        assertEquals(listOf(Key.A), actor.pressedKeys)
    }

    @Test
    fun isKeyPressedIsLiveBetweenTicks() = withKeyboard { _, manager, _ ->
        manager.onKeyPressed(Key.A)
        assertTrue(manager.isKeyPressed(Key.A))

        manager.onKeyReleased(Key.A)
        assertFalse(manager.isKeyPressed(Key.A))
    }

    private fun withKeyboard(block: (ManualKubriko, KeyboardInputManagerImpl, RecordingActor) -> Unit) {
        val manager = KeyboardInputManager.newInstance() as KeyboardInputManagerImpl
        val kubriko = newManualKubriko(
            ActorManager.newInstance(shouldComposeLayers = false),
            manager,
        )
        try {
            val actor = RecordingActor()
            kubriko.actorManager.add(actor)
            kubriko.tickUntil {
                manager.onKeyPressed(PROBE_KEY)
                manager.onKeyReleased(PROBE_KEY)
                actor.pressedKeys.isNotEmpty()
            }
            kubriko.tick(count = 3)
            actor.activeKeySets.clear()
            actor.pressedKeys.clear()
            block(kubriko, manager, actor)
        } finally {
            kubriko.dispose()
        }
    }

    private class RecordingActor : KeyboardInputAware {
        val activeKeySets = mutableListOf<Set<Key>>()
        val pressedKeys = mutableListOf<Key>()

        override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) {
            activeKeySets.add(activeKeys.toSet())
        }

        override fun onKeyPressed(key: Key) {
            pressedKeys.add(key)
        }
    }

    private companion object {
        val PROBE_KEY = Key.Z
    }
}
