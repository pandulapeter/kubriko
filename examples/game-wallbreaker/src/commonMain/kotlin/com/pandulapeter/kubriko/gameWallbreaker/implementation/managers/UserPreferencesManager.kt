package com.pandulapeter.kubriko.gameWallbreaker.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class UserPreferencesManager(
    persistenceManager: PersistenceManager,
) : Manager() {

    private var persistedAreSoundEffectsEnabled by persistenceManager.boolean("areSoundEffectsEnabled", true)
    private val _areSoundEffectsEnabled = MutableStateFlow(persistedAreSoundEffectsEnabled)
    val areSoundEffectsEnabled = _areSoundEffectsEnabled.asStateFlow()

    private var persistedIsMusicEnabled by persistenceManager.boolean("isMusicEnabled", true)
    private val _isMusicEnabled = MutableStateFlow(persistedIsMusicEnabled)
    val isMusicEnabled = _isMusicEnabled.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        areSoundEffectsEnabled.onEach { persistedAreSoundEffectsEnabled = it }.launchIn(scope)
        isMusicEnabled.onEach { persistedIsMusicEnabled = it }.launchIn(scope)
    }

    fun onAreSoundEffectsEnabledChanged() = _areSoundEffectsEnabled.update { !it }

    fun onIsMusicEnabledChanged() = _isMusicEnabled.update { !it }
}