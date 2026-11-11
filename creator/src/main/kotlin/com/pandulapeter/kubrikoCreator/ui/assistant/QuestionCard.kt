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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.UserQuestions
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_answer
import kubriko.creator.generated.resources.question_late
import kubriko.creator.generated.resources.question_other_answer
import org.jetbrains.compose.resources.stringResource

/**
 * An AI's question ([UserQuestions]) with its answers as buttons and, unless the composer takes it, a field for any other
 * answer, which [onAnswer] sends.
 *
 * @param isLate Whether the agent stopped waiting for the answer, which then goes to it as the user's next message.
 * @param isEnabled Whether an answer can be sent now: a late one only while the conversation takes a message.
 * @param hasOwnAnswerField Whether the card has its field for another answer: not where the conversation's composer
 *   answers the question while the agent waits, since two fields for the same answer would leave the user guessing.
 */
@Composable
internal fun QuestionCard(
    question: UserQuestions.Question,
    onAnswer: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLate: Boolean = false,
    isEnabled: Boolean = true,
    hasOwnAnswerField: Boolean = true,
) = Column(
    modifier = modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
        .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text(text = question.text, style = MaterialTheme.typography.bodyMedium)
    if (isLate) {
        Text(
            text = stringResource(Res.string.question_late),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        question.answers.forEach { answer ->
            OutlinedButton(enabled = isEnabled, onClick = { onAnswer(answer) }) { Text(answer) }
        }
    }
    if (hasOwnAnswerField) {
        OtherAnswerField(
            questionId = question.id,
            isEnabled = isEnabled,
            onAnswer = onAnswer,
        )
    }
}

@Composable
private fun OtherAnswerField(
    questionId: Long,
    isEnabled: Boolean,
    onAnswer: (String) -> Unit,
) {
    var other by remember(questionId) { mutableStateOf("") }
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
            enabled = isEnabled && other.isNotBlank(),
            onClick = { onAnswer(other.trim()) },
        ) {
            Text(stringResource(Res.string.action_answer))
        }
    }
}
