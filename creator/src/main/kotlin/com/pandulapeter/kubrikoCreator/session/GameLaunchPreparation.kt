/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

/**
 * What building the project for a play session gave.
 */
internal sealed interface GameLaunchPreparation {

    /**
     * Built, and the game can be started without Gradle.
     */
    data class Ready(val launch: GameLaunch) : GameLaunchPreparation

    /**
     * Built, but the build didn't say how to start the game, so Gradle starts it.
     */
    data object Unavailable : GameLaunchPreparation

    /**
     * The build failed, which it reported.
     */
    data object Failed : GameLaunchPreparation
}
