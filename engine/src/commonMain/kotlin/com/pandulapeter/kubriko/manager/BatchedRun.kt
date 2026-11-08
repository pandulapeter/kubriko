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
import com.pandulapeter.kubriko.helpers.TriangleBatch
import com.pandulapeter.kubriko.helpers.appendFrom
import com.pandulapeter.kubriko.helpers.truncate

/**
 * One run of batched actors drawn in a single call, kept from frame to frame in the same slot of its layer. While the
 * actors appended to it are the ones of the last frame, in the same order and with unchanged geometry, its batch is
 * not touched and draws last frame's arrays again; at the first difference it is cut back to where the matched members
 * end, which is exactly what they appended last frame, and filled on from there. Nothing is re-read from a member's
 * look, which a look shared between actors would have overwritten since.
 */
internal class BatchedRun {
    val batch = TriangleBatch()

    /** The vertices the run holds this frame, replayed or not. */
    var vertexCount = 0
        private set

    private val members = ArrayList<Batched?>()
    private var versions = IntArray(16)
    private var vertexEnds = IntArray(16)
    private var indexEnds = IntArray(16)
    private var memberCount = 0
    private var position = 0
    private var isReplaying = false

    /** Starts this frame's fill of the run with the texture and sampling of [geometry]. */
    fun begin(geometry: LookGeometry) {
        val hasSameKey = canHold(geometry)
        if (!hasSameKey) {
            batch.texture = geometry.texture
            batch.textureTileMode = geometry.tileMode
            batch.textureFilterQuality = geometry.filterQuality
            batch.defaultU = geometry.batch.defaultU
            batch.defaultV = geometry.batch.defaultV
        }
        isReplaying = hasSameKey && memberCount > 0
        if (!isReplaying) batch.reset()
        position = 0
        vertexCount = 0
    }

    /**
     * Whether [geometry] samples the texture this run does, the same way, with the same texel for its vertices that
     * sample none.
     */
    fun canHold(geometry: LookGeometry) = batch.texture === geometry.texture &&
        batch.textureTileMode == geometry.tileMode &&
        batch.textureFilterQuality == geometry.filterQuality &&
        batch.defaultU == geometry.batch.defaultU &&
        batch.defaultV == geometry.batch.defaultV

    fun append(actor: Batched, geometry: LookGeometry) {
        vertexCount += geometry.batch.vertexCount
        if (isReplaying) {
            if (position < memberCount && members[position] === actor && versions[position] == geometry.version) {
                position++
                return
            }
            isReplaying = false
            truncateTo(position)
        }
        if (position == members.size) members.add(actor) else members[position] = actor
        if (position == versions.size) {
            versions = versions.copyOf(versions.size * 2)
            vertexEnds = vertexEnds.copyOf(versions.size)
            indexEnds = indexEnds.copyOf(versions.size)
        }
        versions[position] = geometry.version
        batch.appendFrom(geometry.batch)
        vertexEnds[position] = batch.vertexCount
        indexEnds[position] = batch.indexCount
        position++
    }

    /** Ends this frame's fill and returns the batch to draw. */
    fun end(): TriangleBatch {
        if (isReplaying && position != memberCount) truncateTo(position)
        isReplaying = false
        memberCount = position
        for (i in position until members.size) members[i] = null
        return batch
    }

    /** Lets go of last frame's members, for a slot no run used this frame. */
    fun clear() {
        memberCount = 0
        for (i in members.indices) members[i] = null
        batch.reset()
    }

    /** Keeps what the first [count] members appended, dropping everything after. */
    private fun truncateTo(count: Int) = if (count == 0) {
        batch.reset()
    } else {
        batch.truncate(vertexEnds[count - 1], indexEnds[count - 1])
    }
}
