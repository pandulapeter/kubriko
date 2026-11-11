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
            SessionMessage.CommandDone(id = 1),
            SessionMessage.Screenshot(id = 2, png = "iVBORw0KGgo="),
            SessionMessage.CommandFailed(id = 3, message = "No window."),
            SessionMessage.ActorsLocated(id = 4, left = 0.25f, top = 0.5f, right = 0.75f, bottom = 1f),
        ).forEach { message ->
            val line = SessionProtocol.encode(message)
            assertFalse('\n' in line)
            assertEquals(message, SessionProtocol.decode(line))
        }
    }

    @Test
    fun everyCommandSurvivesARoundTripOnASingleLine() {
        listOf(
            SessionCommand.BringToFront(id = 1),
            SessionCommand.PressKeys(id = 2, keys = listOf("LEFT", "SPACE"), durationInMilliseconds = 250),
            SessionCommand.CaptureScreen(id = 3, maximumWidth = 1280),
            SessionCommand.AwaitActorsDrawn(id = 4, timeoutInMilliseconds = 5000),
            SessionCommand.LocateActors(id = 5, actorType = "Ship"),
        ).forEach { command ->
            val line = SessionProtocol.encodeCommand(command)
            assertFalse('\n' in line)
            assertEquals(command, SessionProtocol.decodeCommand(line))
        }
    }

    @Test
    fun theWireFormatOfEveryVersionStaysTheSame() {
        mapOf(
            """{"type":"hello","protocolVersion":3,"mode":"edit"}""" to SessionMessage.Hello(protocolVersion = 3, mode = SessionMode.EDIT),
            """{"type":"hello","protocolVersion":3,"mode":"play"}""" to SessionMessage.Hello(protocolVersion = 3, mode = SessionMode.PLAY),
            """{"type":"log","id":"1","message":"m","details":null,"source":"Game","timestamp":42,"importance":"HIGH"}""" to
                SessionMessage.Log(id = "1", message = "m", details = null, source = "Game", timestamp = 42L, importance = "HIGH"),
            """{"type":"failure","message":"x"}""" to SessionMessage.Failure(message = "x"),
            """{"type":"commandDone","id":1}""" to SessionMessage.CommandDone(id = 1),
            """{"type":"screenshot","id":2,"png":"iVBORw0KGgo="}""" to SessionMessage.Screenshot(id = 2, png = "iVBORw0KGgo="),
            """{"type":"commandFailed","id":3,"message":"No window."}""" to SessionMessage.CommandFailed(id = 3, message = "No window."),
            """{"type":"actorsLocated","id":4,"left":0.25,"top":0.5,"right":0.75,"bottom":1.0}""" to
                SessionMessage.ActorsLocated(id = 4, left = 0.25f, top = 0.5f, right = 0.75f, bottom = 1f),
        ).forEach { (line, message) ->
            assertEquals(message, SessionProtocol.decode(line))
            assertEquals(line, SessionProtocol.encode(message))
        }
        mapOf(
            """{"type":"bringToFront","id":1}""" to SessionCommand.BringToFront(id = 1),
            """{"type":"pressKeys","id":2,"keys":["LEFT","SPACE"],"durationInMilliseconds":250}""" to
                SessionCommand.PressKeys(id = 2, keys = listOf("LEFT", "SPACE"), durationInMilliseconds = 250),
            """{"type":"captureScreen","id":3,"maximumWidth":1280}""" to SessionCommand.CaptureScreen(id = 3, maximumWidth = 1280),
            """{"type":"awaitActorsDrawn","id":4,"timeoutInMilliseconds":5000}""" to
                SessionCommand.AwaitActorsDrawn(id = 4, timeoutInMilliseconds = 5000),
            """{"type":"locateActors","id":5,"actorType":"Ship"}""" to SessionCommand.LocateActors(id = 5, actorType = "Ship"),
        ).forEach { (line, command) ->
            assertEquals(command, SessionProtocol.decodeCommand(line))
            assertEquals(line, SessionProtocol.encodeCommand(command))
        }
    }

    @Test
    fun theIdsOfCommandsFromNewerVersionsCanStillBeRead() {
        assertEquals(7, SessionProtocol.decodeCommandId("""{"type":"addedLater","id":7}"""))
        assertEquals(null, SessionProtocol.decodeCommandId("""{"type":"addedLater"}"""))
        assertEquals(null, SessionProtocol.decodeCommandId("not json"))
    }

    @Test
    fun unknownFieldsFromNewerSessionsAreIgnored() {
        assertEquals(
            SessionMessage.Failure(message = "x"),
            SessionProtocol.decode("""{"type":"failure","message":"x","addedLater":true}"""),
        )
    }
}
