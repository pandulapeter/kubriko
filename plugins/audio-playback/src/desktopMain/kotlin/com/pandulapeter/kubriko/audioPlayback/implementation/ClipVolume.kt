/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/。
 */
package com.pandulapeter.kubriko.audioPlayback.implementation

import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.log10

/**
 * Sets the gain of a pooled clip for its next play, since it keeps the gain of its last one. A mixer that offers no
 * master gain plays it as recorded.
 */
internal fun Clip.applyVolume(volume: Float) {
    if (!isControlSupported(FloatControl.Type.MASTER_GAIN)) return
    val gain = getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
    gain.value = gainInDecibelsOf(volume, gain.minimum, gain.maximum)
}

/**
 * The gain that plays a sound at [volume] (0 to 1, a factor of its amplitude) within the range a gain control takes:
 * its [minimum], the quietest it goes, for silence.
 */
internal fun gainInDecibelsOf(volume: Float, minimum: Float, maximum: Float): Float =
    if (volume <= 0f) minimum else (20f * log10(volume)).coerceIn(minimum, maximum)
