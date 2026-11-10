/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * What Claude Agent alone is started with: the user's Anthropic API key, kept in [secrets].
 *
 * @param scope Where the key is looked up: the main thread, which the panel reads it on.
 */
internal class ClaudeApiKey(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val secrets: SecretStore,
) {
    private val _hasApiKey = MutableStateFlow(false)

    /**
     * Whether Claude Agent uses the user's Anthropic API key rather than a Claude Code login.
     */
    val hasApiKey = _hasApiKey.asStateFlow()

    /**
     * Whether this computer has somewhere to keep an API key.
     */
    val canKeepApiKey get() = secrets.isAvailable
    private val _apiKeyFailed = MutableStateFlow(false)

    /**
     * Whether the last API key given couldn't be kept.
     */
    val apiKeyFailed = _apiKeyFailed.asStateFlow()
    private val apiKeyLookup = CompletableDeferred<Unit>()
    private var hasLookedUpApiKey = false

    /**
     * Keeps [apiKey] in the secret store.
     *
     * @return Whether it was kept.
     */
    suspend fun keepApiKey(apiKey: String): Boolean {
        val isKept = withContext(ioDispatcher) { secrets.write(API_KEY, apiKey) }
        _apiKeyFailed.value = !isKept
        if (isKept) _hasApiKey.value = true
        return isKept
    }

    suspend fun forgetApiKey() {
        withContext(ioDispatcher) { secrets.delete(API_KEY) }
        _hasApiKey.value = false
    }

    /**
     * What Claude Agent is started with: the API key, if given.
     */
    suspend fun environment(): Map<String, String> {
        lookUpApiKey()
        apiKeyLookup.await()
        val apiKey = if (_hasApiKey.value) withContext(ioDispatcher) { secrets.read(API_KEY) } else null
        return listOfNotNull(apiKey?.let { ANTHROPIC_API_KEY to it }).toMap()
    }

    /**
     * Finds out whether a key is kept, once per run.
     */
    fun lookUpApiKey() {
        if (hasLookedUpApiKey) return
        hasLookedUpApiKey = true
        scope.launch {
            _hasApiKey.value = withContext(ioDispatcher) { secrets.read(API_KEY) } != null
            apiKeyLookup.complete(Unit)
        }
    }

    private companion object {
        const val API_KEY = "anthropic-api-key"
        const val ANTHROPIC_API_KEY = "ANTHROPIC_API_KEY"
    }
}
