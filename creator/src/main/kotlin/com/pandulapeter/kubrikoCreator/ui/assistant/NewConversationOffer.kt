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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_new_conversation_offer
import kubriko.creator.generated.resources.ic_add
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A way out of a conversation the user is done with, above the composer while the agent isn't working: after its turn
 * ends, or while it waits for an answer the user may not want to give. Starting the next conversation should never take a
 * Stop, or an answer to a follow-up question nobody needed.
 */
@Composable
internal fun NewConversationOffer(
    isShown: Boolean,
    onStartNew: () -> Unit,
) = AnimatedVisibility(
    visible = isShown,
    enter = fadeIn(CreatorMotion.medium()) + expandVertically(CreatorMotion.medium()),
    exit = fadeOut(CreatorMotion.short()) + shrinkVertically(CreatorMotion.short()),
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onStartNew) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(Res.drawable.ic_add),
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = stringResource(Res.string.assistant_new_conversation_offer),
            )
        }
    }
}

/**
 * Whether [NewConversationOffer] shows: once the conversation holds something, and its agent isn't working, though it may
 * wait for an answer.
 */
internal fun isNewConversationOffered(isEmpty: Boolean, isBusy: Boolean, isWaitingForAnswer: Boolean) = !isEmpty && (!isBusy || isWaitingForAnswer)
