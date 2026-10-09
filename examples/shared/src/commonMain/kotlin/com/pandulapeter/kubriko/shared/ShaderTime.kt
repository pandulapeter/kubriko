/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shared

/**
 * Converts a shader clock in milliseconds to the seconds passed to a `time` uniform, wrapping once an hour.
 *
 * A `Float` in seconds stays precise to about a quarter of a millisecond up to an hour, while the shaders it drives are
 * not periodic: wrapping trades a visible jump once an hour for a precision loss that would otherwise grow without bound
 * and eventually freeze the animation.
 */
fun shaderTimeInSeconds(timeInMilliseconds: Long) = (timeInMilliseconds % SHADER_TIME_WRAP_IN_MILLISECONDS) / 1000f

private const val SHADER_TIME_WRAP_IN_MILLISECONDS = 3_600_000L
