/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import java.util.concurrent.atomic.AtomicLong

/**
 * Numbers requests whose results may land out of order (reads in the background that overlap), so that a result is
 * only used while no newer request was made, and an older one can't bring back what a newer one no longer has.
 */
internal class LatestRequests {
    private val latest = AtomicLong()

    /**
     * Starts a request, returning its number.
     */
    fun start() = latest.incrementAndGet()

    fun isLatest(request: Long) = latest.get() == request
}
