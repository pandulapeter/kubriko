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

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.Block
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class GameplayManager : Manager() {

    private val actorManager by manager<ActorManager>()
    private val _currentLevel = MutableStateFlow<String?>(null)
    val currentLevel = _currentLevel.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        _currentLevel
            .onEach {
                actorManager.removeAll()
                when (it) {
                    "1" -> actorManager.add(Block())
                }
            }
            .launchIn(scope)
    }

    fun setCurrentLevel(level: String) = _currentLevel.update { level }
}