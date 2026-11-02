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

import com.pandulapeter.kubrikoCreator.workspace.CLOSE_WRITE_TIMEOUT_IN_MILLISECONDS
import kotlin.test.Test
import kotlin.test.assertTrue

class ClosingTimeoutTest {

    @Test
    fun quittingOutlastsTheOpenProjectsWaitForItsWrites() {
        assertTrue(CLOSING_TIMEOUT_IN_MILLISECONDS > CLOSE_WRITE_TIMEOUT_IN_MILLISECONDS)
    }
}
