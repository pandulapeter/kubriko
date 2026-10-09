/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import com.pandulapeter.kubriko.types.TargetFrameRate

/**
 * The single hint that satisfies every viewport sharing a window; [TargetFrameRate.DisplayDefault] (release) when none
 * requests one. A [TargetFrameRate.DisplayDefault] or [TargetFrameRate.DisplayDivider] request needs the panel's
 * native rate, so it releases the hint; otherwise the fastest [TargetFrameRate.Limit] wins, since the slowest mode
 * covering it covers every other request.
 */
internal fun combineFrameRateHints(requests: List<TargetFrameRate>): TargetFrameRate {
    var fastestLimit: TargetFrameRate.Limit? = null
    for (index in requests.indices) {
        when (val request = requests[index]) {
            TargetFrameRate.DisplayDefault, is TargetFrameRate.DisplayDivider -> return TargetFrameRate.DisplayDefault
            is TargetFrameRate.Limit -> if (fastestLimit == null || request.framesPerSecond > fastestLimit.framesPerSecond) {
                fastestLimit = request
            }
        }
    }
    return fastestLimit ?: TargetFrameRate.DisplayDefault
}
