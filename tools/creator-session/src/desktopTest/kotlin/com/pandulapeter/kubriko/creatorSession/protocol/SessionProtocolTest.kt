/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SessionProtocolTest {

    @Test
    fun everyMessageSurvivesARoundTripOnASingleLine() {
        listOf(
            SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.EDIT),
            SessionMessage.Log(id = "1", message = "Line\nbreak", details = null, source = "Game", timestamp = 42L, importance = "HIGH"),
            SessionMessage.Failure(message = "Something went wrong."),
        ).forEach { message ->
            val line = SessionProtocol.encode(message)
            assertFalse('\n' in line)
            assertEquals(message, SessionProtocol.decode(line))
        }
    }

    @Test
    fun unknownFieldsFromNewerSessionsAreIgnored() {
        assertEquals(
            SessionMessage.Failure(message = "x"),
            SessionProtocol.decode("""{"type":"failure","message":"x","addedLater":true}"""),
        )
    }
}
