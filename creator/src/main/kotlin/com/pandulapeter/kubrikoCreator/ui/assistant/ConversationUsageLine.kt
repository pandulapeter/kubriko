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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.ConversationUsage
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_usage_billed_to_key
import kubriko.creator.generated.resources.assistant_usage_context
import kubriko.creator.generated.resources.assistant_usage_cost
import org.jetbrains.compose.resources.stringResource

/**
 * One quiet line above the composer: how full the conversation's context is, what it cost so far, and whether it is
 * billed to the user's API key. Nothing while none of that is known.
 */
@Composable
internal fun ConversationUsageLine(
    usage: ConversationUsage?,
    isBilledToApiKey: Boolean,
) {
    val parts = listOfNotNull(
        usage?.takeIf { it.contextTokens > 0 }?.let {
            stringResource(Res.string.assistant_usage_context, "${contextPercent(it.usedTokens, it.contextTokens)}%", shortTokenCount(it.contextTokens))
        },
        usage?.cost?.let { stringResource(Res.string.assistant_usage_cost, formattedCost(it, usage.currency)) },
        stringResource(Res.string.assistant_usage_billed_to_key).takeIf { isBilledToApiKey },
    )
    if (parts.isEmpty()) return
    Text(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        text = parts.joinToString(SEPARATOR),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * How full a context is, as a whole percentage.
 */
internal fun contextPercent(used: Long, size: Long) = if (size <= 0) 0 else (used * 100 / size).toInt().coerceIn(0, 100)

/**
 * A token count as people say it: `950`, `84k`, or `1.2M`.
 */
internal fun shortTokenCount(tokens: Long) = when {
    tokens >= 1_000_000 -> "%.1fM".format(tokens / 1_000_000.0).replace(".0M", "M")
    tokens >= 1_000 -> "${tokens / 1_000}k"
    else -> tokens.toString()
}

/**
 * A cost with its currency: dollars with a sign, anything else followed by its code.
 */
internal fun formattedCost(amount: Double, currency: String?) = when (currency?.uppercase()) {
    null, "USD" -> "$" + "%.2f".format(amount)
    else -> "%.2f".format(amount) + " " + currency
}

private const val SEPARATOR = " · "
