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

import com.pandulapeter.kubriko.creatorSession.SessionFailure
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlaySessionTest {
    private val targetClassName = "com.pandulapeter.kubriko.creatorSession.implementation.PlaySessionTargetKt"

    @AfterTest
    fun tearDown() {
        playSessionTargetArguments = null
        System.clearProperty(PLAY_SESSION_TARGET_FAILURE)
    }

    @Test
    fun theGamesMainFunctionRunsWithoutArguments() {
        PlaySession.run(SessionArguments.Play(targetClassName))
        assertContentEquals(emptyArray(), playSessionTargetArguments)
    }

    @Test
    fun anUnknownMainClassFailsWithAnExplanation() {
        assertFailsWith<SessionFailure> { PlaySession.run(SessionArguments.Play("com.example.Missing")) }
    }

    @Test
    fun aClassWithoutAMainFunctionFailsWithAnExplanation() {
        assertFailsWith<SessionFailure> { PlaySession.run(SessionArguments.Play(PlaySessionTest::class.java.name)) }
    }

    @Test
    fun anExceptionThrownByTheGameIsRethrownUnwrapped() {
        System.setProperty(PLAY_SESSION_TARGET_FAILURE, "true")
        val exception = assertFailsWith<IllegalStateException> { PlaySession.run(SessionArguments.Play(targetClassName)) }
        assertEquals(PLAY_SESSION_TARGET_FAILURE, exception.message)
    }
}
