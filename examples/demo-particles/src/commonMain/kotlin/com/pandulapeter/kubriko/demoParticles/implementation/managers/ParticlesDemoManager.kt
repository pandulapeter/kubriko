/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoParticles.implementation.managers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.demoParticles.implementation.PlatformSpecificContent
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class ParticlesDemoManager : Manager(), Unique {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()

    override fun onInitialize(kubriko: Kubriko) {
        stateManager.isFocused
            .onEach(stateManager::updateIsRunning)
            .launchIn(scope)
        actorManager.add(this)
    }

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) = Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
    ) {
        PlatformSpecificContent()
    }
}