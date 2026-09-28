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

import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import kotlin.test.Test
import kotlin.test.assertSame

class ManagerDeduplicationTest {

    private class M : Manager()

    @Test
    fun firstManagerOfAClassWins() {
        val a = M()
        val b = M()
        val kubriko = Kubriko.newInstance(a, b, tickSource = TickSource.manual())
        assertSame(a, kubriko.get<M>())
        kubriko.dispose()
    }

    @Test
    fun userBuiltInManagerReplacesTheDefault() {
        val viewportManager = ViewportManager.newInstance(initialScaleFactor = 3f)
        val kubriko = Kubriko.newInstance(viewportManager, tickSource = TickSource.manual())
        assertSame(viewportManager, kubriko.get<ViewportManager>())
        kubriko.dispose()
    }
}
