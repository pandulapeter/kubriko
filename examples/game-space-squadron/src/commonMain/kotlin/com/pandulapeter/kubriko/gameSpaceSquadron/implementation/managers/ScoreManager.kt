/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

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
    private val _score = MutableStateFlow(0)
    val score = _score.asStateFlow()
    private val _highScore = persistenceManager.int("highScore")
    val highScore = _highScore.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        _score.onEach { score ->
            if (score > highScore.value) {
                _highScore.update { score }
            }
        }.launchIn(scope)
    }

    fun resetScore() = _score.update { 0 }

    fun incrementScore() = _score.update { currentValue -> currentValue + 1 }
}