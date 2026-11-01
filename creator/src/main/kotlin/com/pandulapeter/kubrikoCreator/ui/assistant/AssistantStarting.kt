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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentChoice
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentDownload
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_downloading
import kubriko.creator.generated.resources.assistant_downloading_of
import kubriko.creator.generated.resources.assistant_starting
import kubriko.creator.generated.resources.assistant_starting_download
import org.jetbrains.compose.resources.stringResource

/**
 * What the panel shows while the agent starts: the download of its archive on its first start, with how far it got.
 */
@Composable
internal fun AssistantStarting(
    download: AgentDownload?,
) = Column(
    modifier = Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    if (download == null) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = listOfNotNull(
                stringResource(Res.string.assistant_starting, LocalAgent.current.name),
                stringResource(Res.string.assistant_starting_download).takeIf { LocalAgent.current is AgentChoice.Registry },
            ).joinToString(" "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        val total = download.totalBytes
        if (total == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { downloadFraction(download.downloadedBytes, total) },
            )
        }
        Text(
            text = if (total == null) {
                stringResource(Res.string.assistant_downloading, download.agentName, megabytes(download.downloadedBytes))
            } else {
                stringResource(Res.string.assistant_downloading_of, download.agentName, megabytes(download.downloadedBytes), megabytes(total))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * How much of a download is done, from 0 to 1.
 */
internal fun downloadFraction(downloaded: Long, total: Long) = if (total <= 0) 0f else (downloaded.toFloat() / total).coerceIn(0f, 1f)

/**
 * [bytes] in megabytes with one decimal, as the panel shows a download's size.
 */
internal fun megabytes(bytes: Long) = "%.1f".format(bytes / BYTES_IN_MEGABYTE)

private const val BYTES_IN_MEGABYTE = 1_000_000.0
