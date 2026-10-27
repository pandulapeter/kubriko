/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

internal var playSessionTargetArguments: Array<String>? = null

/**
 * Stands in for a game's main function in [PlaySessionTest].
 */
fun main(args: Array<String>) {
    playSessionTargetArguments = args
    if (System.getProperty(PLAY_SESSION_TARGET_FAILURE) != null) throw IllegalStateException(PLAY_SESSION_TARGET_FAILURE)
}

internal const val PLAY_SESSION_TARGET_FAILURE = "kubriko.creatorSession.test.failure"
