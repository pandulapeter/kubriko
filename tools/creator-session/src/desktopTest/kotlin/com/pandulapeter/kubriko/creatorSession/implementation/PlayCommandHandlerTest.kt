/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import com.pandulapeter.kubriko.creatorSession.protocol.SessionCommand
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayCommandHandlerTest {
    private val received = mutableListOf<String>()
    private val sleeps = mutableListOf<Long>()
    private val listener = AWTEventListener { event ->
        if (event is KeyEvent) received += "${if (event.id == KeyEvent.KEY_PRESSED) "press" else "release"} ${KeyEvent.getKeyText(event.keyCode)}"
    }
    private val handler = PlayCommandHandler(send = {}, findWindow = { null }, sleep = { sleeps += it; received += "wait $it" })

    init {
        Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.KEY_EVENT_MASK)
    }

    @AfterTest
    fun tearDown() = Toolkit.getDefaultToolkit().removeAWTEventListener(listener)

    @Test
    fun keysAreNamedAsInKeyEvent() {
        assertEquals(KeyEvent.VK_LEFT, PlayCommandHandler.keyCodeOf("LEFT"))
        assertEquals(KeyEvent.VK_SPACE, PlayCommandHandler.keyCodeOf("space"))
        assertEquals(KeyEvent.VK_A, PlayCommandHandler.keyCodeOf("A"))
        assertNull(PlayCommandHandler.keyCodeOf("NOPE"))
    }

    @Test
    fun pressedKeysReachKubrikosKeyboardInputHeldTogetherAndReleasedInReverse() {
        handler.pressKeys(object : Component() {}, listOf(KeyEvent.VK_LEFT, KeyEvent.VK_SPACE), 250)
        assertEquals(
            listOf("press ${KeyEvent.getKeyText(KeyEvent.VK_LEFT)}", "press ${KeyEvent.getKeyText(KeyEvent.VK_SPACE)}", "wait 250", "release ${KeyEvent.getKeyText(KeyEvent.VK_SPACE)}", "release ${KeyEvent.getKeyText(KeyEvent.VK_LEFT)}"),
            received,
        )
    }

    @Test
    fun commandsThatCantBeCarriedOutSayWhy() {
        assertEquals(SessionMessage.CommandFailed(1, "There's no key called NOPE."), handler.carryOut(SessionCommand.PressKeys(1, listOf("LEFT", "NOPE"), 100)))
        assertEquals(SessionMessage.CommandFailed(2, "The game has no window yet."), handler.carryOut(SessionCommand.PressKeys(2, listOf("LEFT"), 100)))
        assertEquals(SessionMessage.CommandFailed(3, "The game has no window yet."), handler.carryOut(SessionCommand.CaptureScreen(3, 800)))
        assertEquals(SessionMessage.CommandFailed(4, "The game has no window yet."), handler.carryOut(SessionCommand.BringToFront(4)))
        assertEquals(emptyList(), sleeps)
    }
}
