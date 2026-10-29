/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeyChoiceButtonTest {
    private fun title(key: KeyChoice) = if (key == KeyChoice.LEFT) "Left arrow" else readableKeyName(key.composeKeyName)

    @Test
    fun anEmptySearchShowsEveryKey() = assertEquals(KeyChoice.all, matchingKeys("  ", ::title))

    @Test
    fun exactMatchesComeFirstThenThoseStartingWithTheSearch() {
        val keys = matchingKeys("a", ::title)
        assertEquals(KeyChoice.A, keys.first())
        assertTrue(keys.indexOf(KeyChoice("AltLeft")) < keys.indexOf(KeyChoice.LEFT))
        assertTrue(KeyChoice.LEFT in keys)
    }

    @Test
    fun theSearchMatchesTitlesAndComposeNamesWithOrWithoutSpaces() {
        assertEquals(KeyChoice.LEFT, matchingKeys("left arrow", ::title).single())
        assertEquals(KeyChoice("PageUp"), matchingKeys("page up", ::title).first())
        assertEquals(KeyChoice("MediaPlayPause"), matchingKeys("mediaplaypause", ::title).single())
        assertEquals(emptyList(), matchingKeys("hyper", ::title))
    }

    @Test
    fun composeNamesAreMadeReadable() {
        assertEquals("Media Play Pause", readableKeyName("MediaPlayPause"))
        assertEquals("Num Pad5", readableKeyName("NumPad5"))
        assertEquals("F12", readableKeyName("F12"))
    }
}
