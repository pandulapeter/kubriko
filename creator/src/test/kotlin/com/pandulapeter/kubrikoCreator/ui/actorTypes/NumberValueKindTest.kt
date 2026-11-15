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

import com.pandulapeter.kubrikoCreator.actors.Axis
import com.pandulapeter.kubrikoCreator.actors.NumberLimits
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.SceneEdge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumberValueKindTest {

    @Test
    fun aNumberBecomesARandomOneBetweenZeroAndItAndBack() {
        assertEquals(NumberValue.Random(0f, 200f), NumberValueKind.RANDOM.of(NumberValue.Fixed(200f)))
        assertEquals(NumberValue.Random(-5f, 0f, isWhole = true), NumberValueKind.RANDOM_WHOLE.of(NumberValue.Fixed(-5f)))
        assertEquals(NumberValue.Random(0f, 1f), NumberValueKind.RANDOM.of(NumberValue.Fixed(0f)))
        assertEquals(NumberValue.Fixed(3f), NumberValueKind.FIXED.of(NumberValue.Random(3f, 9f)))
        assertEquals(NumberValue.Fixed(1.5f), NumberValueKind.FIXED.of(NumberValue.Random(1.5f, 4f)))
        assertEquals(NumberValue.Fixed(2f), NumberValueKind.FIXED.of(NumberValue.Random(1.5f, 4f), isWholeField = true))
        assertEquals(NumberValue.Random(3f, 9f, isWhole = true), NumberValueKind.RANDOM_WHOLE.of(NumberValue.Random(3f, 9f)))
    }

    @Test
    fun aNewKindStartsWithinTheLimitsOfItsField() {
        val zoom = NumberLimits.ZOOM
        assertEquals(NumberValue.Random(1f, 100f), NumberValueKind.RANDOM.of(NumberValue.Fixed(100f), limits = zoom))
        assertEquals(NumberValue.Random(1f, 2f), NumberValueKind.RANDOM.of(NumberValue.Property("zoom"), limits = zoom))
        assertEquals(NumberValue.Random(1f, 2f, isWhole = true), NumberValueKind.RANDOM_WHOLE.of(NumberValue.Fixed(0f), limits = zoom))
        assertEquals(NumberValue.Fixed(1f), NumberValueKind.FIXED.of(NumberValue.Property("zoom"), limits = zoom))
        assertEquals(NumberValue.Fixed(1f), NumberValueKind.FIXED.of(NumberValue.Random(0f, 5f), limits = zoom))
        assertEquals(NumberValue.Random(1f, 2f), NumberValueKind.RANDOM.of(NumberValue.Fixed(1f), limits = zoom))
        assertEquals(NumberValue.Random(-1f, 0f), NumberValueKind.RANDOM.of(NumberValue.Property("x"), limits = NumberLimits(maximum = 0f)))
        assertEquals(NumberValue.Random(1f, 2.5f, isWhole = true), NumberValueKind.RANDOM_WHOLE.of(NumberValue.Random(0f, 2.5f), limits = zoom))
        listOf(NumberValue.Fixed(100f), NumberValue.Fixed(1f), NumberValue.Random(1f, 2f), NumberValue.Property("zoom")).forEach { value ->
            NumberValueKind.offeredBy(isWholeField = false, offersEdges = false).forEach { kind ->
                assertTrue(kind.of(value, limits = zoom) in zoom, "$kind of $value")
            }
        }
    }

    @Test
    fun aPropertyStartsUnchosenAndLeavesZeroBehind() {
        assertEquals(NumberValue.Property(), NumberValueKind.PROPERTY.of(NumberValue.Fixed(5f)))
        assertEquals(NumberValue.Fixed(0f), NumberValueKind.FIXED.of(NumberValue.Property("speed")))
        assertEquals(NumberValue.Random(), NumberValueKind.RANDOM.of(NumberValue.Property("speed")))
    }

    @Test
    fun aWholeFieldShowsAndOffersOneRandomKind() {
        assertEquals(NumberValueKind.RANDOM, NumberValueKind.of(NumberValue.Random(isWhole = true), isWholeField = true))
        assertEquals(NumberValueKind.RANDOM_WHOLE, NumberValueKind.of(NumberValue.Random(isWhole = true), isWholeField = false))
        assertEquals(listOf(NumberValueKind.FIXED, NumberValueKind.RANDOM, NumberValueKind.PROPERTY), NumberValueKind.offeredBy(isWholeField = true))
        assertEquals(NumberValueKind.entries, NumberValueKind.offeredBy(isWholeField = false))
        assertEquals(
            listOf(NumberValueKind.FIXED, NumberValueKind.RANDOM, NumberValueKind.RANDOM_WHOLE, NumberValueKind.PROPERTY),
            NumberValueKind.offeredBy(isWholeField = false, offersEdges = false),
        )
        assertEquals(listOf(NumberValueKind.FIXED, NumberValueKind.PROPERTY), NumberValueKind.offeredBy(isWholeField = false, offersEdges = false, offersRandom = false))
    }

    @Test
    fun anEdgeBecomesOneEndOfARandomNumberBetweenItAndTheEdgeAcrossAndBack() {
        assertEquals(NumberValue.Edge(), NumberValueKind.EDGE.of(NumberValue.Fixed(5f)))
        assertEquals(
            NumberValue.BetweenEdges(SceneEdge.TOP, 10f, SceneEdge.BOTTOM, 0f),
            NumberValueKind.BETWEEN_EDGES.of(NumberValue.Edge(SceneEdge.TOP, 10f)),
        )
        assertEquals(NumberValue.Edge(SceneEdge.RIGHT, -4f), NumberValueKind.EDGE.of(NumberValue.BetweenEdges(SceneEdge.RIGHT, -4f, SceneEdge.LEFT)))
        assertEquals(NumberValue.BetweenEdges(), NumberValueKind.BETWEEN_EDGES.of(NumberValue.Random()))
        assertEquals(NumberValue.Fixed(0f), NumberValueKind.FIXED.of(NumberValue.Edge(SceneEdge.LEFT, 3f)))
        assertEquals(NumberValue.Random(), NumberValueKind.RANDOM.of(NumberValue.BetweenEdges()))
        assertEquals(NumberValueKind.EDGE, NumberValueKind.of(NumberValue.Edge(), isWholeField = true))
        assertEquals(NumberValueKind.BETWEEN_EDGES, NumberValueKind.of(NumberValue.BetweenEdges(), isWholeField = false))
    }

    @Test
    fun aNewEdgeStartsOnTheAxisOfItsField() {
        assertEquals(NumberValue.Edge(SceneEdge.TOP), NumberValueKind.EDGE.of(NumberValue.Fixed(1f), axis = Axis.VERTICAL))
        assertEquals(NumberValue.Edge(SceneEdge.LEFT), NumberValueKind.EDGE.of(NumberValue.Fixed(1f), axis = Axis.HORIZONTAL))
        assertEquals(NumberValue.BetweenEdges(SceneEdge.TOP, to = SceneEdge.BOTTOM), NumberValueKind.BETWEEN_EDGES.of(NumberValue.Random(), axis = Axis.VERTICAL))
    }
}
