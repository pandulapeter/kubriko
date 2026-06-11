/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.animation

import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.shortestDeltaTo
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.Cuboid
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.CuboidAnimation
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.CuboidKeyframe
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.KeyframedColor

// Sorted parallel arrays for one float property. Null track = property has no keyframes → interpolation falls back to base value.
internal class SortedFloatTrack(
    val timestamps: IntArray,
    val values: FloatArray,
    val isCubic: BooleanArray,
)

// Same as SortedFloatTrack but for nullable color properties. Inner Color? null = "use texture" (a valid keyed state).
internal class SortedColorTrack(
    val timestamps: IntArray,
    val colors: Array<Color?>,
    val isCubic: BooleanArray,
)

// Per-cuboid container for all 15 property tracks. A null sub-track means that property is not keyed in this animation.
class PreparedCuboidTracks internal constructor(
    internal val positionX: SortedFloatTrack?, internal val positionY: SortedFloatTrack?, internal val positionZ: SortedFloatTrack?,
    internal val sizeX: SortedFloatTrack?, internal val sizeY: SortedFloatTrack?, internal val sizeZ: SortedFloatTrack?,
    internal val rotationX: SortedFloatTrack?, internal val rotationY: SortedFloatTrack?, internal val rotationZ: SortedFloatTrack?,
    internal val colorXPlus: SortedColorTrack?, internal val colorXMinus: SortedColorTrack?,
    internal val colorYPlus: SortedColorTrack?, internal val colorYMinus: SortedColorTrack?,
    internal val colorZPlus: SortedColorTrack?, internal val colorZMinus: SortedColorTrack?,
)

// Builds sorted, allocation-free tracks from the animation's keyframe map.
// Call once per animation selection or keyframe mutation — not per frame.
fun CuboidAnimation.buildTracks(): Map<String, PreparedCuboidTracks> =
    keyframes.mapValues { (_, keyframeMap) ->
        val sorted = keyframeMap.entries.sortedBy { it.key }
        PreparedCuboidTracks(
            positionX = buildFloatTrack(sorted) { it.positionX to it.positionXCubic },
            positionY = buildFloatTrack(sorted) { it.positionY to it.positionYCubic },
            positionZ = buildFloatTrack(sorted) { it.positionZ to it.positionZCubic },
            sizeX = buildFloatTrack(sorted) { it.sizeX to it.sizeXCubic },
            sizeY = buildFloatTrack(sorted) { it.sizeY to it.sizeYCubic },
            sizeZ = buildFloatTrack(sorted) { it.sizeZ to it.sizeZCubic },
            rotationX = buildFloatTrack(sorted) { it.rotationX to it.rotationXCubic },
            rotationY = buildFloatTrack(sorted) { it.rotationY to it.rotationYCubic },
            rotationZ = buildFloatTrack(sorted) { it.rotationZ to it.rotationZCubic },
            colorXPlus = buildColorTrack(sorted) { it.colorXPlus to it.colorXPlusCubic },
            colorXMinus = buildColorTrack(sorted) { it.colorXMinus to it.colorXMinusCubic },
            colorYPlus = buildColorTrack(sorted) { it.colorYPlus to it.colorYPlusCubic },
            colorYMinus = buildColorTrack(sorted) { it.colorYMinus to it.colorYMinusCubic },
            colorZPlus = buildColorTrack(sorted) { it.colorZPlus to it.colorZPlusCubic },
            colorZMinus = buildColorTrack(sorted) { it.colorZMinus to it.colorZMinusCubic },
        )
    }

// Writes interpolated animation values to every cuboid that has at least one keyed property in the receiver.
// Cuboids absent from the receiver are left untouched — callers decide how to handle them.
// For properties with no sub-track, the value from [snapshots] is used as the constant base.
// [snapshots] should be Cuboid copies taken at the moment the animation was started (via cuboid.copy()).
fun Map<String, PreparedCuboidTracks>.applyTo(
    cuboids: Map<String, Cuboid>,
    snapshots: Map<String, Cuboid>,
    timestamp: Int,
) {
    forEach { (cuboidId, cuboidTracks) ->
        val cuboid = cuboids[cuboidId] ?: return@forEach
        val snapshot = snapshots[cuboidId] ?: return@forEach
        cuboid.positionX = interpolateSceneUnit(snapshot.positionX, cuboidTracks.positionX, timestamp)
        cuboid.positionY = interpolateSceneUnit(snapshot.positionY, cuboidTracks.positionY, timestamp)
        cuboid.positionZ = interpolateSceneUnit(snapshot.positionZ, cuboidTracks.positionZ, timestamp)
        cuboid.sizeX = interpolateSceneUnit(snapshot.sizeX, cuboidTracks.sizeX, timestamp)
        cuboid.sizeY = interpolateSceneUnit(snapshot.sizeY, cuboidTracks.sizeY, timestamp)
        cuboid.sizeZ = interpolateSceneUnit(snapshot.sizeZ, cuboidTracks.sizeZ, timestamp)
        cuboid.rotationX = interpolateAngle(snapshot.rotationX, cuboidTracks.rotationX, timestamp)
        cuboid.rotationY = interpolateAngle(snapshot.rotationY, cuboidTracks.rotationY, timestamp)
        cuboid.rotationZ = interpolateAngle(snapshot.rotationZ, cuboidTracks.rotationZ, timestamp)
        cuboid.colorXPlus = interpolateColor(snapshot.colorXPlus, cuboidTracks.colorXPlus, timestamp)
        cuboid.colorXMinus = interpolateColor(snapshot.colorXMinus, cuboidTracks.colorXMinus, timestamp)
        cuboid.colorYPlus = interpolateColor(snapshot.colorYPlus, cuboidTracks.colorYPlus, timestamp)
        cuboid.colorYMinus = interpolateColor(snapshot.colorYMinus, cuboidTracks.colorYMinus, timestamp)
        cuboid.colorZPlus = interpolateColor(snapshot.colorZPlus, cuboidTracks.colorZPlus, timestamp)
        cuboid.colorZMinus = interpolateColor(snapshot.colorZMinus, cuboidTracks.colorZMinus, timestamp)
    }
}

private fun buildFloatTrack(
    sortedEntries: List<Map.Entry<Int, CuboidKeyframe>>,
    extractor: (CuboidKeyframe) -> Pair<Float?, Boolean>,
): SortedFloatTrack? {
    val timestamps = mutableListOf<Int>()
    val values = mutableListOf<Float>()
    val cubics = mutableListOf<Boolean>()
    for ((t, kf) in sortedEntries) {
        val (v, cubic) = extractor(kf)
        if (v != null) { timestamps.add(t); values.add(v); cubics.add(cubic) }
    }
    if (timestamps.isEmpty()) return null
    return SortedFloatTrack(timestamps.toIntArray(), values.toFloatArray(), cubics.toBooleanArray())
}

private fun buildColorTrack(
    sortedEntries: List<Map.Entry<Int, CuboidKeyframe>>,
    extractor: (CuboidKeyframe) -> Pair<KeyframedColor?, Boolean>,
): SortedColorTrack? {
    val timestamps = mutableListOf<Int>()
    val colors = mutableListOf<Color?>()
    val cubics = mutableListOf<Boolean>()
    for ((t, kf) in sortedEntries) {
        val (kc, cubic) = extractor(kf)
        // Outer null = not keyed (skip). Non-null KeyframedColor with inner null = keyed as "use texture" (include).
        if (kc != null) { timestamps.add(t); colors.add(kc.color); cubics.add(cubic) }
    }
    if (timestamps.isEmpty()) return null
    return SortedColorTrack(timestamps.toIntArray(), colors.toTypedArray(), cubics.toBooleanArray())
}

// Catmull-Rom cubic spline for a single float channel.
// P0/P3 are the "outer" control points (the keyframes before prev and after next).
private fun catmullRom(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float =
    0.5f * ((2f * p1) + (-p0 + p2) * t + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t * t + (-p0 + 3f * p1 - 3f * p2 + p3) * t * t * t)

// Returns the last index in [timestamps] whose value is <= [timestamp], or -1 if none.
// Uses a manual binary search to remain compatible with Kotlin/Native (IntArray.binarySearch is JVM-only).
private fun prevIndex(timestamps: IntArray, timestamp: Int): Int {
    var low = 0
    var high = timestamps.size - 1
    while (low <= high) {
        val mid = (low + high).ushr(1)
        when {
            timestamps[mid] < timestamp -> low = mid + 1
            timestamps[mid] > timestamp -> high = mid - 1
            else -> return mid
        }
    }
    return low - 1
}

// Interpolates between keyed float values. Before the first keyframe: base. After the last: hold last value.
// The isCubic flag on the next keyframe selects Catmull-Rom (true) or linear (false) for that interval.
private fun interpolateFloat(baseValue: Float, track: SortedFloatTrack?, timestamp: Int): Float {
    track ?: return baseValue
    val prevIdx = prevIndex(track.timestamps, timestamp)
    return when {
        prevIdx < 0 -> baseValue
        prevIdx == track.timestamps.size - 1 || track.timestamps[prevIdx] == timestamp -> track.values[prevIdx]
        else -> {
            val nextIdx = prevIdx + 1
            val prev = track.values[prevIdx]; val next = track.values[nextIdx]
            val t = (timestamp - track.timestamps[prevIdx]).toFloat() / (track.timestamps[nextIdx] - track.timestamps[prevIdx])
            if (track.isCubic[nextIdx]) {
                val p0 = if (prevIdx > 0) track.values[prevIdx - 1] else prev
                val p3 = if (nextIdx < track.timestamps.size - 1) track.values[nextIdx + 1] else next
                catmullRom(p0, prev, next, p3, t)
            } else {
                prev + t * (next - prev)
            }
        }
    }
}

private fun interpolateSceneUnit(base: SceneUnit, track: SortedFloatTrack?, timestamp: Int): SceneUnit =
    interpolateFloat(base.raw, track, timestamp).sceneUnit

// Both cubic and linear modes take the shortest arc across the [-π, π] wrap boundary.
// Cubic: control points are normalized relative to each other before Catmull-Rom so the spline
// never takes the long way around. Linear: shortestDeltaTo gives the correct signed delta directly.
private fun interpolateAngle(base: AngleRadians, track: SortedFloatTrack?, timestamp: Int): AngleRadians {
    track ?: return base
    val prevIdx = prevIndex(track.timestamps, timestamp)
    return when {
        prevIdx < 0 -> base
        prevIdx == track.timestamps.size - 1 || track.timestamps[prevIdx] == timestamp -> track.values[prevIdx].rad
        else -> {
            val nextIdx = prevIdx + 1
            val t = (timestamp - track.timestamps[prevIdx]).toFloat() / (track.timestamps[nextIdx] - track.timestamps[prevIdx])
            if (track.isCubic[nextIdx]) {
                val p1 = track.values[prevIdx]
                val p2Raw = track.values[nextIdx]
                val p0Raw = if (prevIdx > 0) track.values[prevIdx - 1] else track.values[prevIdx]
                val p3Raw = if (nextIdx < track.timestamps.size - 1) track.values[nextIdx + 1] else track.values[nextIdx]
                // Normalize each control point so consecutive pairs always use the shortest arc.
                val p2 = p1 + p1.rad.shortestDeltaTo(p2Raw.rad).raw
                val p0 = p1 - p0Raw.rad.shortestDeltaTo(p1.rad).raw
                val p3 = p2 + p2Raw.rad.shortestDeltaTo(p3Raw.rad).raw
                catmullRom(p0, p1, p2, p3, t).rad
            } else {
                val delta = track.values[prevIdx].rad.shortestDeltaTo(track.values[nextIdx].rad)
                (track.values[prevIdx] + t * delta.raw).rad
            }
        }
    }
}

// Colors are interpolated per RGBA channel. If one end is null ("use texture"), the non-null
// side is held to the midpoint then the other takes over — regardless of the cubic flag.
// When both ends are colors, cubic uses Catmull-Rom per channel; linear uses lerp.
private fun interpolateColor(baseColor: Color?, track: SortedColorTrack?, timestamp: Int): Color? {
    track ?: return baseColor
    val prevIdx = prevIndex(track.timestamps, timestamp)
    return when {
        prevIdx < 0 -> baseColor
        prevIdx == track.timestamps.size - 1 || track.timestamps[prevIdx] == timestamp -> track.colors[prevIdx]
        else -> {
            val nextIdx = prevIdx + 1
            val prevColor = track.colors[prevIdx]; val nextColor = track.colors[nextIdx]
            val t = (timestamp - track.timestamps[prevIdx]).toFloat() / (track.timestamps[nextIdx] - track.timestamps[prevIdx])
            if (prevColor == null || nextColor == null) {
                if (t < 0.5f) prevColor else nextColor
            } else if (track.isCubic[nextIdx]) {
                val p0 = (if (prevIdx > 0) track.colors[prevIdx - 1] else null) ?: prevColor
                val p3 = (if (nextIdx < track.timestamps.size - 1) track.colors[nextIdx + 1] else null) ?: nextColor
                Color(
                    red = catmullRom(p0.red, prevColor.red, nextColor.red, p3.red, t).coerceIn(0f, 1f),
                    green = catmullRom(p0.green, prevColor.green, nextColor.green, p3.green, t).coerceIn(0f, 1f),
                    blue = catmullRom(p0.blue, prevColor.blue, nextColor.blue, p3.blue, t).coerceIn(0f, 1f),
                    alpha = catmullRom(p0.alpha, prevColor.alpha, nextColor.alpha, p3.alpha, t).coerceIn(0f, 1f),
                )
            } else {
                Color(
                    red = prevColor.red + t * (nextColor.red - prevColor.red),
                    green = prevColor.green + t * (nextColor.green - prevColor.green),
                    blue = prevColor.blue + t * (nextColor.blue - prevColor.blue),
                    alpha = prevColor.alpha + t * (nextColor.alpha - prevColor.alpha),
                )
            }
        }
    }
}
