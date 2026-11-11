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
    fun aModelsNameOutweighsALargerOneItsDescriptionNames() {
        val choices = claude.map { if (it.value.value == "sonnet") it.copy(description = "Sonnet 4.6 · Use Opus for harder tasks") else it }
        assertEquals(smaller("haiku", "Haiku"), SmallerModel.of(models("sonnet", choices)))
    }

    @Test
    fun theFamilysPlainModelIsOfferedRatherThanAVariant() {
        val gemini = listOf("Gemini 2.5 Pro", "Gemini 2.5 Flash-Lite", "Gemini 2.5 Flash").map { SessionConfigSelectOption(SessionConfigValueId(it.lowercase().replace(' ', '-')), it) }
        assertEquals(smaller("gemini-2.5-flash", "Gemini 2.5 Flash"), SmallerModel.of(models("gemini-2.5-pro", gemini)))
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
    fun ofClaudesManyModelsTheAliasOfTheNewestIsOffered() {
        val choices = listOf(
            "default" to "Default (recommended)" to "Opus 5.5",
            "opus" to "Opus 5.5" to "For complex work and everyday tasks",
            "claude-fable-5-2" to "Fable 5.2" to "For your toughest challenges",
            "sonnet" to "Sonnet 5.5" to "Most efficient for simpler tasks",
            "haiku" to "Haiku 5.5" to "Fastest for quick answers",
            "claude-haiku-4-5-20251001" to "Haiku 4.5" to "Fastest for quick answers",
            "claude-sonnet-5" to "Sonnet 5" to "Efficient for routine tasks",
            "claude-opus-5" to "Opus 5" to "Best for everyday, complex tasks",
            "claude-sonnet-4-6" to "Sonnet 4.6" to "Efficient for routine tasks",
        ).map { (valueAndName, description) -> SessionConfigSelectOption(SessionConfigValueId(valueAndName.first), valueAndName.second, description) }
        assertEquals(smaller("sonnet", "Sonnet 5.5"), SmallerModel.of(models("opus", choices)))
        assertEquals(smaller("sonnet", "Sonnet 5.5"), SmallerModel.of(models("default", choices)))
        assertEquals(smaller("claude-sonnet-5", "Sonnet 5"), SmallerModel.of(models("claude-opus-5", choices)))
        assertNull(SmallerModel.of(models("claude-fable-5-2", choices)))
    }

    @Test
    fun ofModelsWithoutAnAliasOneOfTheCurrentVersionIsOffered() {
        val choices = listOf("Opus 5", "Sonnet 4.6", "Sonnet 5").map { SessionConfigSelectOption(SessionConfigValueId(it.lowercase().replace(' ', '-')), it) }
        assertEquals(smaller("sonnet-5", "Sonnet 5"), SmallerModel.of(models("opus-5", choices)))
    }

    @Test
    fun aCurrentModelNamedOnlyByItsValueIsKnownByIt() {
        val choices = listOf(
            SessionConfigSelectOption(SessionConfigValueId("claude-opus-4-5"), "Recommended"),
            SessionConfigSelectOption(SessionConfigValueId("claude-sonnet-4-0"), "Older"),
            SessionConfigSelectOption(SessionConfigValueId("claude-sonnet-4-5"), "Newer"),
        )
        assertEquals(smaller("claude-sonnet-4-5", "Newer"), SmallerModel.of(models("claude-opus-4-5", choices)))
    }

    @Test
    fun aContextSizeIsntAVersionAndAVersionMayBeInTheDescription() {
        val choices = listOf(
            SessionConfigSelectOption(SessionConfigValueId("opus[1m]"), "Opus (1M context)", "Opus 4.6 with a long context"),
            SessionConfigSelectOption(SessionConfigValueId("claude-sonnet-4-5"), "Sonnet", "Sonnet 4.5"),
            SessionConfigSelectOption(SessionConfigValueId("claude-sonnet-4-6"), "Sonnet", "Sonnet 4.6"),
        )
        assertEquals(smaller("claude-sonnet-4-6", "Sonnet"), SmallerModel.of(models("opus[1m]", choices)))
    }

    @Test
    fun otherFamiliesAndGroupedOptionsAreRead() {
        val gemini = listOf(SessionConfigSelectOption(SessionConfigValueId("gemini-pro"), "Gemini 3 Pro"), SessionConfigSelectOption(SessionConfigValueId("gemini-flash"), "Gemini 3 Flash"))
        val grouped = SessionConfigSelectOptions.Grouped(listOf(SessionConfigSelectGroup(SessionConfigGroupId("gemini"), "Gemini", gemini)))
        val options = listOf(SessionConfigOption.select("model", "Model", "gemini-pro", grouped, category = SessionConfigOptionCategory.MODEL))
        assertEquals(smaller("gemini-flash", "Gemini 3 Flash"), SmallerModel.of(options))
    }
}
