/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.settings

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Creator settings ([CreatorSettings]) as they are now, saved after every change.
 */
internal class CreatorSettingsController(
    private val store: CreatorSettingsStore,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val _settings = MutableStateFlow(store.load())
    val settings = _settings.asStateFlow()
    private val _isAiEnabled = MutableStateFlow(_settings.value.isAiEnabled)

    /**
     * The one check every AI feature reads: the AI panel, the AI's activity and highlights, the MCP server, and the
     * assistant's agents.
     */
    val isAiEnabled: StateFlow<Boolean> = _isAiEnabled.asStateFlow()
    private val saves = Mutex()
    private var lastSave: Job? = null

    fun setAiEnabled(isEnabled: Boolean) = update { it.copy(isAiEnabled = isEnabled) }

    fun setTheme(theme: ThemeChoice) = update { it.copy(theme = theme) }

    fun setInterfaceScale(scale: Float) = update { it.copy(interfaceScale = CreatorSettingsStore.closestInterfaceScale(scale)) }

    /**
     * Creates new projects in [folder] by default, or in the home folder when it is null.
     */
    fun setDefaultProjectFolder(folder: File?) = update { it.copy(defaultProjectFolder = folder?.absolutePath) }

    /**
     * Waits until the last change is saved, which quitting does so that a setting changed just before it is kept. Saves
     * write the newest settings one at a time, so the last one started is the last one needed.
     */
    suspend fun awaitSaves() {
        lastSave?.join()
    }

    private fun update(change: (CreatorSettings) -> CreatorSettings) {
        val settings = change(_settings.value).takeIf { it != _settings.value } ?: return
        _settings.value = settings
        _isAiEnabled.value = settings.isAiEnabled
        // Saves can finish in any order, so each one writes the settings as they are by then.
        lastSave = scope.launch(ioDispatcher) { saves.withLock { store.save(_settings.value) } }
    }
}
