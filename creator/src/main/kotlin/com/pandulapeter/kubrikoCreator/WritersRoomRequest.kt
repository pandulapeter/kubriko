/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator

import java.util.concurrent.atomic.AtomicReference

/**
 * The idea a project was created from ("Start from an idea"), which the writers' room starts from once its workspace is
 * shown: [take] hands it over only once, so a workspace shown again doesn't start another.
 */
internal class WritersRoomRequest(pitch: String) {
    private val pitch = AtomicReference<String?>(pitch)

    /**
     * The idea, the first time it is asked for, and null after.
     */
    fun take(): String? = pitch.getAndSet(null)
}
