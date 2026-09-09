/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers

/**
 * Whether this device draws a [TriangleBatch] carrying both per-vertex colours and a tiling pattern the way
 * every other one does.
 *
 * The call is the same everywhere and almost every device answers it correctly, but some drivers quietly drop
 * the texture coordinates when the same draw also hands them colours, and sample the whole batch at one texel
 * instead. Nothing in the API reports that and it does not follow the OS version, so the only honest test is to
 * draw the smallest batch that would show the difference and read the result back.
 *
 * It is worth asking because a batch is the only way a pattern reaches this geometry: on a device that drops
 * the coordinates every patterned surface comes out flat whatever is done to it, so the pattern is better
 * turned off than paid for.
 */
object TriangleBatchSupport {

    private var cachedResult: Boolean? = null

    /**
     * Draws the probe and reports whether the pattern varied across it. The answer belongs to the device, so it
     * is settled once per process and every later caller is given that one. A probe that cannot be run at all
     * reads as supported: a device that refuses the question is far likelier to be one of the many that work
     * than one of the few that do not.
     */
    suspend fun isTextureSampledPerVertex() = cachedResult ?: probeTextureSampling().also { cachedResult = it }
}

// Draws a batch whose two halves name texels of different brightness and reports whether they came back
// different. Called at most once per process. @see TriangleBatchSupport
internal expect suspend fun probeTextureSampling(): Boolean
