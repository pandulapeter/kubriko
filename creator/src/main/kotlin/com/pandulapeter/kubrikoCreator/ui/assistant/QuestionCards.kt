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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.UserQuestions
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion

/**
 * The questions outside AI clients ask the user ([UserQuestions]) that wait for an answer, each with its answers as buttons
 * and a field for any other answer, which [onAnswer] sends. The assistant's own questions are shown in its conversation.
 */
@Composable
internal fun QuestionCards(
    questions: List<UserQuestions.Question>,
    onAnswer: (id: Long, answer: String) -> Unit,
    modifier: Modifier = Modifier,
) = AnimatedVisibility(
    modifier = modifier,
    visible = questions.isNotEmpty() && LocalIsAiEnabled.current,
    enter = fadeIn(CreatorMotion.medium()) + expandVertically(CreatorMotion.medium()),
    exit = fadeOut(CreatorMotion.short()) + shrinkVertically(CreatorMotion.short()),
) {
    Column {
        questions.forEach { question ->
            key(question.id) {
                QuestionCard(modifier = Modifier.padding(8.dp), question = question, onAnswer = { onAnswer(question.id, it) })
            }
        }
    }
}
