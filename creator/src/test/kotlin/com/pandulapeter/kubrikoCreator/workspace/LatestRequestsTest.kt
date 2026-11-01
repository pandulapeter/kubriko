/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LatestRequestsTest {
    private val requests = LatestRequests()

    @Test
    fun onlyTheNewestRequestsResultIsUsedWhateverOrderTheyFinishIn() {
        val older = requests.start()
        val newer = requests.start()
        assertTrue(requests.isLatest(newer))
        assertFalse(requests.isLatest(older))
    }

    @Test
    fun aRequestStaysTheLatestUntilAnotherOneStarts() {
        val request = requests.start()
        assertTrue(requests.isLatest(request))
        requests.start()
        assertFalse(requests.isLatest(request))
    }
}
