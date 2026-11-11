/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.protocol

import kotlinx.serialization.Serializable

/**
 * A region of a captured image, as fractions of its size from 0 to 1 (its left edge is 0, its right edge 1).
 */
@Serializable
data class ImageRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)
