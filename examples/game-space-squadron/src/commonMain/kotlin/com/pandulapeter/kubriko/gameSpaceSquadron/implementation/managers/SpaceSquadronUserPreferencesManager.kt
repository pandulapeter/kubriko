package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class SpaceSquadronUserPreferencesManager(
    persistenceManager: PersistenceManager,
) : Manager() {
    private val audioManager by manager<SpaceSquadronAudioManager>()
    private val _areSoundEffectsEnabled = persistenceManager.boolean("areSoundEffectsEnabled", true)
    val areSoundEffectsEnabled = _areSoundEffectsEnabled.asStateFlow()

    private val _isMusicEnabled = persistenceManager.boolean("isMusicEnabled", true)
    val isMusicEnabled = _isMusicEnabled.asStateFlow()

    fun onAreSoundEffectsEnabledChanged() {
        _areSoundEffectsEnabled.update { !it }
        audioManager.playButtonToggleSoundEffect()
    }

    fun onIsMusicEnabledChanged() {
        _isMusicEnabled.update { !it }
        audioManager.playButtonToggleSoundEffect()
    }
}