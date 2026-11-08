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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.UserQuestions
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_answer
import kubriko.creator.generated.resources.question_other_answer
import org.jetbrains.compose.resources.stringResource

/**
 * The questions an AI asks the user ([UserQuestions]) that wait for an answer, each with its answers as buttons and a
 * field for any other answer, which [onAnswer] sends.
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
                QuestionCard(question = question, onAnswer = { onAnswer(question.id, it) })
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: UserQuestions.Question,
    onAnswer: (String) -> Unit,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
        .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text(text = question.text, style = MaterialTheme.typography.bodyMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        question.answers.forEach { answer ->
            OutlinedButton(onClick = { onAnswer(answer) }) { Text(answer) }
        }
    }
    var other by remember(question.id) { mutableStateOf("") }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactTextField(
            modifier = Modifier.weight(1f),
            value = other,
            onValueChange = { other = it },
            placeholder = stringResource(Res.string.question_other_answer),
        )
        TextButton(
            enabled = other.isNotBlank(),
            onClick = { onAnswer(other.trim()) },
        ) {
            Text(stringResource(Res.string.action_answer))
        }
    }
}
