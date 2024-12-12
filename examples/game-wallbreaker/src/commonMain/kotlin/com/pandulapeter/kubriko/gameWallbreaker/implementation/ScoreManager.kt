package com.pandulapeter.kubriko.gameWallbreaker.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class ScoreManager  : Manager() {

    private lateinit var persistenceManager: PersistenceManager

    private val _score = MutableStateFlow(0)
    val score = _score.asStateFlow()
    private val _highScore = MutableStateFlow(0)
    val highScore = _highScore.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        persistenceManager = kubriko.require()
        _highScore.update { persistenceManager.getInt(KEY_HIGH_SCORE) }
        _score.onEach { score ->
            if (score > highScore.value) {
                _highScore.update { score }
            }
        }.launchIn(scope)
        _highScore.onEach { highScore ->
            persistenceManager.putInt(KEY_HIGH_SCORE, highScore)
        }.launchIn(scope)
    }

    fun incrementScore() = _score.update { currentValue -> currentValue + 1 }

    companion object {
        private const val KEY_HIGH_SCORE = "highScore"
    }
}