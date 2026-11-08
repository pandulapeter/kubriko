/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.actor.looks.RectangleLook
import com.pandulapeter.kubriko.actor.looks.SpriteLook
import com.pandulapeter.kubriko.actor.traits.Batched
import com.pandulapeter.kubriko.testFixtures.measureAllocatedBytesPerRun
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunGroupingTest {

    private val grouping = RunGrouping()

    private fun groupsOf(actors: List<Batched>, start: Int = 0, end: Int = actors.size): List<List<Int>> {
        actors.forEach { it.look.geometry.update(it.look, it.body, pixelsPerUnit = 1f, isTextureSampledPerVertex = true) }
        grouping.group(actors, start, end)
        return List(grouping.groupCount) { group ->
            generateSequence(grouping.firstMember(group)) { grouping.memberAfter(it).takeIf { next -> next >= 0 } }.toList()
        }
    }

    @Test
    fun `groups keep the order of their first members, and members their own order`() {
        val first = ImageBitmap(2, 2)
        val second = ImageBitmap(2, 2)
        val actors = listOf(
            TestBatched(SpriteLook(second)),
            TestBatched(RectangleLook()),
            TestBatched(SpriteLook(first)),
            TestBatched(SpriteLook(second)),
            TestBatched(SpriteLook(second, filterQuality = FilterQuality.None)),
            TestBatched(RectangleLook()),
        )

        assertEquals(listOf(listOf(0, 3), listOf(1, 5), listOf(2), listOf(4)), groupsOf(actors))
    }

    @Test
    fun `a thousand textures each form a group of their own, in order`() {
        val textures = List(1_000) { ImageBitmap(1, 1) }
        val actors = List(2_000) { TestBatched(SpriteLook(textures[it % 1_000])) }

        val groups = groupsOf(actors)

        assertEquals(1_000, groups.size)
        assertTrue(groups.withIndex().all { (index, members) -> members == listOf(index, index + 1_000) })
    }

    @Test
    fun `a small range after a large one starts from an empty table`() {
        val textures = List(500) { ImageBitmap(1, 1) }
        val actors = List(500) { TestBatched(SpriteLook(textures[it])) }
        groupsOf(actors)

        assertEquals(listOf(listOf(10), listOf(11)), groupsOf(actors, start = 10, end = 12))
    }

    @Test
    fun `grouping allocates nothing once its arrays have grown`() {
        val textures = List(50) { ImageBitmap(1, 1) }
        val actors = List(400) { TestBatched(SpriteLook(textures[it % 50])) }
        actors.forEach { it.look.geometry.update(it.look, it.body, pixelsPerUnit = 1f, isTextureSampledPerVertex = true) }

        val bytes = measureAllocatedBytesPerRun(warmUpRuns = 2_000, measuredRuns = 500) { grouping.group(actors, 0, actors.size) }

        assertTrue(bytes < 1, "$bytes B per grouping")
    }
}
