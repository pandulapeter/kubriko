/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.ui.actorTypes.ActorTypeHighlight
import com.pandulapeter.kubrikoCreator.ui.scenes.SceneHighlight
import com.pandulapeter.kubrikoCreator.workspace.Reveal
import com.pandulapeter.kubrikoCreator.workspace.RevealedPlace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RevealHighlightsTest {

    @Test
    fun aPlaceInATypeLightsUpItsBlockOrPropertyInThatTypesEditorOnly() {
        val block = Reveal(RevealedPlace.InType("Ball", eventIndex = 1, blockIndex = 2))
        assertEquals(ActorTypeHighlight(key = block, eventIndex = 1, blockIndex = 2), block.highlightIn("Ball"))
        val property = Reveal(RevealedPlace.InType("Ball", property = "speed"))
        assertEquals(ActorTypeHighlight(key = property, eventIndex = null, blockIndex = null, property = "speed"), property.highlightIn("Ball"))
        assertNull(block.highlightIn("Paddle"))
        assertNull(Reveal(RevealedPlace.InScene("level", listOf("a"))).highlightIn("Ball"))
    }

    @Test
    fun actorsOfASceneLightUpAndTheFirstIsSelectedInThatSceneOnly() {
        val reveal = Reveal(RevealedPlace.InScene("level", listOf("a", "b")))
        assertEquals(SceneHighlight(key = reveal, actorIds = setOf("a", "b"), startedAt = 5L, isSelecting = true), reveal.highlightInScene("level", startedAt = 5L))
        assertNull(reveal.highlightInScene("other"))
        assertNull(Reveal(RevealedPlace.InScene("level", emptyList())).highlightInScene("level"))
    }

    @Test
    fun whicheverLitUpLastIsShown() {
        val newest = NewestHighlight()
        val reveal = Reveal(RevealedPlace.InType("Ball", eventIndex = 0))
        assertNull(newest.revealIfNewer(null, aiStepKey = 1))
        assertEquals(reveal, newest.revealIfNewer(reveal, aiStepKey = 1), "the user went somewhere after the AI's step")
        assertNull(newest.revealIfNewer(reveal, aiStepKey = 2), "the AI took a step after that")
        assertNull(newest.revealIfNewer(reveal, aiStepKey = 2))
        val again = Reveal(RevealedPlace.InType("Ball", eventIndex = 0))
        assertEquals(again, newest.revealIfNewer(again, aiStepKey = 2), "going there again lights it up again")
        assertEquals(again, newest.revealIfNewer(again, aiStepKey = null), "no AI step shown any more")
    }
}
