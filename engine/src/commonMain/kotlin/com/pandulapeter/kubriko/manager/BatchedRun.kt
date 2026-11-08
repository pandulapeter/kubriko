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

/**
 * One run of batched actors drawn in a single call, kept from frame to frame in the same slot of its layer. While the
 * actors appended to it are the ones of the last frame, in the same order and with unchanged geometry, its batch is
 * not touched and draws last frame's arrays again; the first difference refills it from the members matched so far
 * onwards.
 */
internal class BatchedRun {
    val batch = TriangleBatch()

    /** The vertices the run holds this frame, replayed or not. */
    var vertexCount = 0
        private set

    private val members = ArrayList<Batched?>()
    private var versions = IntArray(16)
    private var memberCount = 0
    private var position = 0
    private var isReplaying = false

    /** Starts this frame's fill of the run with the texture and sampling of [geometry]. */
    fun begin(geometry: LookGeometry) {
        val hasSameKey = canHold(geometry) &&
            batch.defaultU == geometry.batch.defaultU &&
            batch.defaultV == geometry.batch.defaultV
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

    /** Whether [geometry] samples the texture this run does, the same way. */
    fun canHold(geometry: LookGeometry) = batch.texture === geometry.texture &&
        batch.textureTileMode == geometry.tileMode &&
        batch.textureFilterQuality == geometry.filterQuality

    fun append(actor: Batched, geometry: LookGeometry) {
        vertexCount += geometry.batch.vertexCount
        if (isReplaying) {
            if (position < memberCount && members[position] === actor && versions[position] == geometry.version) {
                position++
                return
            }
            isReplaying = false
            refillUpTo(position)
        }
        if (position == members.size) members.add(actor) else members[position] = actor
        if (position == versions.size) versions = versions.copyOf(versions.size * 2)
        versions[position] = geometry.version
        batch.appendFrom(geometry.batch)
        position++
    }

    /** Ends this frame's fill and returns the batch to draw. */
    fun end(): TriangleBatch {
        if (isReplaying && position != memberCount) refillUpTo(position)
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

    private fun refillUpTo(count: Int) {
        batch.reset()
        for (i in 0 until count) batch.appendFrom(members[i]!!.look.geometry.batch)
    }
}
