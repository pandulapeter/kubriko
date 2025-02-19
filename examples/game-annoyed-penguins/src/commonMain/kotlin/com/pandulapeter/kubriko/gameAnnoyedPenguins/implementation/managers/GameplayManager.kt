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
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.FogShader
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager

internal class GameplayManager : Manager() {

    private val actorManager by manager<ActorManager>()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(FogShader())
    }
}