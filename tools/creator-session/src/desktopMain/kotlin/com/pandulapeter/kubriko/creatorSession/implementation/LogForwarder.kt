/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch

/**
 * Sends every [Logger] entry to Kubriko Creator: the ones logged before the session connected, then each new one.
 */
internal class LogForwarder(
    private val send: (SessionMessage) -> Unit,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    fun start() = scope.launch {
        val alreadySentIds = HashSet<String>()
        Logger.latestEntry
            .onSubscription {
                Logger.logs.value.asReversed().forEach { entry ->
                    alreadySentIds.add(entry.id)
                    send(entry.toMessage())
                }
            }
            .collect { entry ->
                if (!alreadySentIds.remove(entry.id)) {
                    send(entry.toMessage())
                }
            }
    }

    private fun Logger.Entry.toMessage() = SessionMessage.Log(
        id = id,
        message = message,
        details = details,
        source = source,
        timestamp = timestamp,
        importance = importance.name,
    )
}
