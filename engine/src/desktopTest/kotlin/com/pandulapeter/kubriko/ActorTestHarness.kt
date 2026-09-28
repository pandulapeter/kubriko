/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko

import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.helpers.ManualTickSource
import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager

/**
 * Creates a started [KubrikoImpl] driven by a [ManualTickSource], with a 1920×1080 viewport, for the engine's own
 * tests that need its internals.
 */
internal fun newTestKubriko(
    vararg managers: Manager,
    actorManager: ActorManager = ActorManager.newInstance(),
): Pair<KubrikoImpl, ManualTickSource> {
    val tickSource = TickSource.manual()
    val kubriko = Kubriko.newInstance(
        actorManager,
        *managers,
        tickSource = tickSource,
    ) as KubrikoImpl
    tickSource.start()
    kubriko.viewportManager.updateSize(Size(1920f, 1080f))
    return kubriko to tickSource
}
