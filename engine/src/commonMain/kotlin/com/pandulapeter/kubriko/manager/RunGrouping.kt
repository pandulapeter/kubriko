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

import com.pandulapeter.kubriko.actor.looks.LookGeometry
import com.pandulapeter.kubriko.actor.traits.Batched
import com.pandulapeter.kubriko.implementation.samplingKey

/**
 * Groups a range of batched actors by what lets them share a run (the texture, its sampling, and whether sprites are
 * drawn one by one), each group in the order its first member appears and its members in their own order. One pass
 * through an open-addressing table of reused arrays, so it costs the same whether the range holds one texture or a
 * thousand, and allocates nothing once the arrays have grown. Only the slots the last call filled are cleared, so many
 * small ranges after one large one stay cheap.
 */
internal class RunGrouping {
    private var table = IntArray(64)
    private var nextMember = IntArray(64)
    private var groupFirst = IntArray(16)
    private var groupLast = IntArray(16)
    private var groupSlot = IntArray(16)

    /** How many groups the last [group] call found. */
    var groupCount = 0
        private set

    /** Groups `actors[start until end]`; read the result through [firstMember] and [memberAfter]. */
    fun group(actors: List<Batched>, start: Int, end: Int) {
        val count = end - start
        for (group in 0 until groupCount) table[groupSlot[group]] = 0
        groupCount = 0
        var capacity = table.size
        while (capacity < count * 2) capacity *= 2
        if (capacity != table.size) table = IntArray(capacity)
        if (nextMember.size < actors.size) nextMember = IntArray(maxOf(actors.size, nextMember.size * 2))
        val mask = capacity - 1
        for (i in start until end) {
            val geometry = actors[i].look.geometry
            var slot = hashOf(geometry) and mask
            while (true) {
                val entry = table[slot]
                if (entry == 0) {
                    addGroup(i, slot)
                    table[slot] = groupCount
                    break
                }
                val group = entry - 1
                if (isSameGroup(actors[groupFirst[group]].look.geometry, geometry)) {
                    nextMember[groupLast[group]] = i
                    groupLast[group] = i
                    break
                }
                slot = (slot + 1) and mask
            }
            nextMember[i] = NONE
        }
    }

    /** The first member of [group], as an index into the list last grouped. */
    fun firstMember(group: Int) = groupFirst[group]

    /** The member after [member] in its group, or a negative number after the last one. */
    fun memberAfter(member: Int) = nextMember[member]

    private fun addGroup(member: Int, slot: Int) {
        if (groupCount == groupFirst.size) {
            groupFirst = groupFirst.copyOf(groupCount * 2)
            groupLast = groupLast.copyOf(groupCount * 2)
            groupSlot = groupSlot.copyOf(groupCount * 2)
        }
        groupFirst[groupCount] = member
        groupLast[groupCount] = member
        groupSlot[groupCount] = slot
        groupCount++
    }

    private fun hashOf(geometry: LookGeometry): Int {
        var hash = geometry.texture?.hashCode() ?: 0
        hash = hash * 31 + samplingKey(geometry.tileMode, geometry.filterQuality)
        hash = hash * 31 + if (geometry.isDrawnIndividually) 1 else 0
        hash = hash * 31 + geometry.batch.defaultU.toRawBits()
        hash = hash * 31 + geometry.batch.defaultV.toRawBits()
        return hash xor (hash ushr 16)
    }

    private fun isSameGroup(first: LookGeometry, second: LookGeometry) =
        first.isDrawnIndividually == second.isDrawnIndividually && first.canShareRunWith(second)

    private companion object {
        const val NONE = -1
    }
}
