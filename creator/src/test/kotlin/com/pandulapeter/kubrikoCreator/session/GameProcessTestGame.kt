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

import kotlin.system.exitProcess

/**
 * A game for [GameProcessTest] to play, which does what its system property says.
 */
object GameProcessTestGame {
    const val MODE_PROPERTY = "kubriko.creator.test.mode"

    @JvmStatic
    fun main(args: Array<String>) {
        when (System.getProperty(MODE_PROPERTY)) {
            "fail" -> {
                (1..40).forEach { println("Line $it") }
                exitProcess(3)
            }
            "sleep" -> Thread.sleep(60_000)
            else -> println("Played.")
        }
    }
}
