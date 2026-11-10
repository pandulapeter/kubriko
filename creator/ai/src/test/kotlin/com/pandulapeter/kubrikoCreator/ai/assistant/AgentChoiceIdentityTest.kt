/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AgentChoiceIdentityTest {
    private val withKey = AgentChoice.Custom("OPENAI_API_KEY=sk-secret goose acp")

    @Test
    fun aRegistryAgentIsKnownByItsId() {
        assertEquals(RegistryAgent.claude.id, AgentChoice.Registry(RegistryAgent.claude).identity)
    }

    @Test
    fun aCustomAgentIsKnownByItsCommandWithoutKeepingIt() {
        assertFalse("sk-secret" in withKey.identity)
        assertTrue(AgentChoice.Custom.isIdentity(withKey.identity))
        assertFalse(AgentChoice.Custom.isIdentity(RegistryAgent.claude.id))
        assertNotEquals(withKey.identity, AgentChoice.Custom("goose acp").identity)
    }

    @Test
    fun spacesDontMakeAnotherCustomAgent() {
        assertEquals(AgentChoice.Custom("goose acp").identity, AgentChoice.Custom("  goose   acp ").identity)
    }

    @Test
    fun markingACustomAgentForgetsTheCustomAgentMarkedBefore() {
        val settings = AssistantSettings()
            .withChangingFilesUnasked(RegistryAgent.claude.id)
            .withChangingFilesUnasked(withKey.identity)
            .withChangingFilesUnasked(AgentChoice.Custom("goose acp").identity)
        assertEquals(setOf(RegistryAgent.claude.id, AgentChoice.Custom("goose acp").identity), settings.agentsChangingFilesUnasked)
    }
}
