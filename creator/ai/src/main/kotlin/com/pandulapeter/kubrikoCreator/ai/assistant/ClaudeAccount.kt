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

import com.pandulapeter.kubrikoCreator.mcp.clients.ProcessRunner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * What Claude Agent alone is started with: the Claude Code configuration it signs in with (the default one, or one of
 * a shell alias's) and the user's Anthropic API key, kept in [secrets].
 *
 * @param scope Where the configurations are found and the key looked up: the main thread, which the panel reads them on.
 * @param rememberedAlias The alias of the configuration chosen in an earlier run, resolved once the aliases are found.
 */
internal class ClaudeAccount(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val runner: ProcessRunner,
    private val secrets: SecretStore,
    private val home: String,
    rememberedAlias: String?,
) {
    private val _configurations = MutableStateFlow(listOf(ClaudeConfiguration.default))

    /**
     * The Claude Code configurations found on this computer, the default one first.
     */
    val configurations = _configurations.asStateFlow()
    private val _configuration = MutableStateFlow(rememberedAlias?.let(ClaudeConfiguration::ofAlias) ?: ClaudeConfiguration.default)

    /**
     * The Claude Code configuration Claude Agent uses.
     */
    val configuration = _configuration.asStateFlow()
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
    private var hasFoundConfigurations = false
    private val discovery = CompletableDeferred<Unit>()

    /**
     * Finds the Claude Code configurations of the user's shell aliases, once per run. A remembered configuration whose
     * alias is gone falls back to the default one.
     */
    fun findConfigurations() {
        lookUpApiKey()
        if (hasFoundConfigurations) return
        hasFoundConfigurations = true
        scope.launch {
            val found = ClaudeConfiguration.discover(withContext(ioDispatcher) { runner.aliases() }, home)
            _configurations.value = found
            _configuration.value.alias?.let { alias -> _configuration.value = found.firstOrNull { it.alias == alias } ?: ClaudeConfiguration.default }
            discovery.complete(Unit)
        }
    }

    /**
     * Uses [configuration] from now on.
     *
     * @return Whether it is another one than before.
     */
    fun select(configuration: ClaudeConfiguration): Boolean {
        if (configuration == _configuration.value) return false
        _configuration.value = configuration
        return true
    }

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
     * What Claude Agent is started with: the chosen Claude Code configuration's variables and the API key, if given.
     */
    suspend fun environment(): Map<String, String> {
        if (_configuration.value.alias != null) {
            findConfigurations()
            discovery.await()
        }
        lookUpApiKey()
        apiKeyLookup.await()
        val apiKey = if (_hasApiKey.value) withContext(ioDispatcher) { secrets.read(API_KEY) } else null
        return _configuration.value.environment + listOfNotNull(apiKey?.let { ANTHROPIC_API_KEY to it })
    }

    private fun lookUpApiKey() {
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
