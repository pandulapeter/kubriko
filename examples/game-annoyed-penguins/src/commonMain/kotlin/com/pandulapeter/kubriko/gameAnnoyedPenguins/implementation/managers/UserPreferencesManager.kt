/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers

import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class UserPreferencesManager : Manager() {
    private val audioManager by manager<AudioManager>()
    private val persistenceManager by manager<PersistenceManager>()
    private val _areSoundEffectsEnabled by lazy { persistenceManager.boolean("areSoundEffectsEnabled", true) }
    val areSoundEffectsEnabled by lazy { _areSoundEffectsEnabled.asStateFlow() }

    private val _isMusicEnabled by lazy { persistenceManager.boolean("isMusicEnabled", true) }
    val isMusicEnabled by lazy {_isMusicEnabled.asStateFlow() }

    fun onAreSoundEffectsEnabledChanged() {
        _areSoundEffectsEnabled.update { !it }
        audioManager.playButtonToggleSoundEffect()
    }

    fun onIsMusicEnabledChanged() {
        _isMusicEnabled.update { !it }
        audioManager.playButtonToggleSoundEffect()
    }
}