/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.AiPanelHeader
import com.pandulapeter.kubrikoCreator.ui.AiPanelView
import com.pandulapeter.kubrikoCreator.ui.DescriptionWriter
import com.pandulapeter.kubrikoCreator.ui.LocalDescriptionWriter
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ScreenplayOfferCardTest {

    @Test
    fun takingTheOfferSendsThePromptAndAnswersIt() = uiTest {
        val prompts = mutableListOf<String>()
        var answers = 0
        showing {
            CompositionLocalProvider(LocalDescriptionWriter provides DescriptionWriter(isBusy = false) { prompts += it }) {
                ScreenplayOfferCard(isShown = true, onAnswered = { answers++ })
            }
        }
        onNodeWithText("This game has no screenplay yet", substring = true).assertExists()
        saveSnapshot("screenplay-offer")
        onNodeWithText("Write the screenplay").performClick()
        assertEquals(1, prompts.size)
        assertEquals(1, answers)
        onNodeWithText("No, thanks").performClick()
        assertEquals(2, answers)
    }

    @Test
    fun theOfferNeedsAConversationThatTakesPrompts() = uiTest {
        showing { ScreenplayOfferCard(isShown = true, onAnswered = {}) }
        onNodeWithText("This game has no screenplay yet", substring = true).assertDoesNotExist()
    }

    @Test
    fun thePanelsMenuOffersToWriteTheScreenplay() = uiTest {
        val prompts = mutableListOf<String>()
        showing {
            CompositionLocalProvider(LocalDescriptionWriter provides DescriptionWriter(isBusy = false) { prompts += it }) {
                AiPanelHeader(view = AiPanelView.ASSISTANT, onViewSelected = {})
            }
        }
        onNodeWithContentDescription("More AI options").performClick()
        onNodeWithText("Write the screenplay").performClick()
        assertEquals(1, prompts.size)
    }
}
