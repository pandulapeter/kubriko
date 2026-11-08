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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.LocalDescriptionWriter
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_no_thanks
import kubriko.creator.generated.resources.action_write_screenplay
import kubriko.creator.generated.resources.assistant_prompt_write_screenplay
import kubriko.creator.generated.resources.screenplay_offer_text
import org.jetbrains.compose.resources.stringResource

/**
 * The assistant's one offer, while [isShown], to write the screenplay of a game that has none: taking it sends the prompt
 * to the conversation shown, and either answer is final ([onAnswered]). It shows only while a conversation takes prompts.
 */
@Composable
internal fun ScreenplayOfferCard(
    isShown: Boolean,
    onAnswered: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val writer = LocalDescriptionWriter.current
    AnimatedVisibility(
        modifier = modifier,
        visible = isShown && writer != null && LocalIsAiEnabled.current,
        enter = fadeIn(CreatorMotion.medium()) + expandVertically(CreatorMotion.medium()),
        exit = fadeOut(CreatorMotion.short()) + shrinkVertically(CreatorMotion.short()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Accent.SCREENPLAY.container, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = stringResource(Res.string.screenplay_offer_text), style = MaterialTheme.typography.bodySmall)
            val prompt = stringResource(Res.string.assistant_prompt_write_screenplay)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = writer?.isBusy == false,
                    onClick = {
                        writer?.write(prompt)
                        onAnswered()
                    },
                ) {
                    Text(stringResource(Res.string.action_write_screenplay))
                }
                TextButton(onClick = onAnswered) { Text(stringResource(Res.string.action_no_thanks)) }
            }
        }
    }
}
