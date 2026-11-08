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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pandulapeter.kubrikoCreator.commands.UserQuestions
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class QuestionCardsTest {

    @Test
    fun aQuestionIsAnsweredByAButtonOrByTypingAnotherAnswer() = uiTest {
        val answers = mutableListOf<Pair<Long, String>>()
        showing {
            QuestionCards(
                questions = listOf(UserQuestions.Question(1, "How does the hero get around?", listOf("Runs and jumps", "Flies", "Swims"))),
                onAnswer = { id, answer -> answers += id to answer },
            )
        }
        saveSnapshot("question-card")
        onNodeWithText("Flies").performClick()
        onNode(hasSetTextAction()).performTextInput("Rides a dragon")
        onNodeWithText("Answer").performClick()
        assertEquals(listOf(1L to "Flies", 1L to "Rides a dragon"), answers)
    }
}
