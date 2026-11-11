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

import com.agentclientprotocol.model.SessionConfigGroupId
import com.agentclientprotocol.model.SessionConfigId
import com.agentclientprotocol.model.SessionConfigOption
import com.agentclientprotocol.model.SessionConfigOptionCategory
import com.agentclientprotocol.model.SessionConfigSelectGroup
import com.agentclientprotocol.model.SessionConfigSelectOption
import com.agentclientprotocol.model.SessionConfigSelectOptions
import com.agentclientprotocol.model.SessionConfigValueId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SmallerModelTest {
    private val claude = listOf(
        SessionConfigSelectOption(SessionConfigValueId("default"), "Default (recommended)", "Opus 4.6 · Most capable for complex work"),
        SessionConfigSelectOption(SessionConfigValueId("opus"), "Opus"),
        SessionConfigSelectOption(SessionConfigValueId("sonnet"), "Sonnet", "Sonnet 4.6 · Best for everyday tasks"),
        SessionConfigSelectOption(SessionConfigValueId("sonnet[1m]"), "Sonnet (1M context)"),
        SessionConfigSelectOption(SessionConfigValueId("haiku"), "Haiku", "Haiku 4.5 · Fastest for quick answers"),
    )

    private fun models(current: String, choices: List<SessionConfigSelectOption> = claude, category: SessionConfigOptionCategory? = SessionConfigOptionCategory.MODEL) =
        listOf(SessionConfigOption.select("model", "Model", current, SessionConfigSelectOptions.Flat(choices), category = category))

    private fun smaller(value: String, name: String) = SmallerModel(SessionConfigId("model"), SessionConfigValueId(value), name)

    @Test
    fun theModelATierBelowIsOfferedTheFirstOfItInTheAgentsOrder() {
        assertEquals(smaller("sonnet", "Sonnet"), SmallerModel.of(models("opus")))
        assertEquals(smaller("haiku", "Haiku"), SmallerModel.of(models("sonnet[1m]")))
    }

    @Test
    fun aDefaultIsKnownByTheModelItsDescriptionNames() {
        assertEquals(smaller("sonnet", "Sonnet"), SmallerModel.of(models("default")))
    }

    @Test
    fun nothingIsOfferedBelowTheSmallestOrForAModelOfNoKnownTier() {
        assertNull(SmallerModel.of(models("haiku")))
        assertNull(SmallerModel.of(models("fable", claude + SessionConfigSelectOption(SessionConfigValueId("fable"), "Fable"))))
        assertNull(SmallerModel.of(models("opus", claude.filter { "sonnet" !in it.value.value })))
        assertNull(SmallerModel.of(models("opus", category = null)))
        assertNull(SmallerModel.of(emptyList()))
    }

    @Test
    fun otherFamiliesAndGroupedOptionsAreRead() {
        val gemini = listOf(SessionConfigSelectOption(SessionConfigValueId("gemini-pro"), "Gemini 3 Pro"), SessionConfigSelectOption(SessionConfigValueId("gemini-flash"), "Gemini 3 Flash"))
        val grouped = SessionConfigSelectOptions.Grouped(listOf(SessionConfigSelectGroup(SessionConfigGroupId("gemini"), "Gemini", gemini)))
        val options = listOf(SessionConfigOption.select("model", "Model", "gemini-pro", grouped, category = SessionConfigOptionCategory.MODEL))
        assertEquals(smaller("gemini-flash", "Gemini 3 Flash"), SmallerModel.of(options))
    }
}
