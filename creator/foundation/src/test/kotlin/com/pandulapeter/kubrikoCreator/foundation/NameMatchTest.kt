/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import com.pandulapeter.kubrikoCreator.foundation.NameMatch.Quality
import kotlin.test.Test
import kotlin.test.assertEquals

class NameMatchTest {

    @Test
    fun matchesAreOrderedByQualityThenByFewerPiecesThenByAnEarlierStart() = assertEquals(
        listOf(
            NameMatch(Quality.PREFIX, listOf(0..1)),
            NameMatch(Quality.SCATTERED, listOf(0..1, 4..4)),
            NameMatch(Quality.SCATTERED, listOf(2..3, 6..6)),
            NameMatch(Quality.SCATTERED, listOf(0..0, 3..3, 6..6)),
        ),
        listOf(
            NameMatch(Quality.SCATTERED, listOf(0..0, 3..3, 6..6)),
            NameMatch(Quality.SCATTERED, listOf(2..3, 6..6)),
            NameMatch(Quality.PREFIX, listOf(0..1)),
            NameMatch(Quality.SCATTERED, listOf(0..1, 4..4)),
        ).sorted(),
    )
}
