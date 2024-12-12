package com.pandulapeter.kubriko.gameWallbreaker.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class ScoreManager(
    persistenceManager: PersistenceManager,
) : Manager() {
    private var persistedHighScore by persistenceManager.int("highScore")
    private val _score = MutableStateFlow(0)
    val score = _score.asStateFlow()
    private val _highScore = MutableStateFlow(persistedHighScore)
    val highScore = _highScore.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        _score.onEach { score ->
            if (score > highScore.value) {
                _highScore.update { score }
            }
        }.launchIn(scope)
        _highScore.onEach { persistedHighScore = it }.launchIn(scope)
    }

    fun incrementScore() = _score.update { currentValue -> currentValue + 1 }
}