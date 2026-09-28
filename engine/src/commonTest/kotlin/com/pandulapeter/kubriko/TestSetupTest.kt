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
import com.pandulapeter.kubriko.manager.ActorManager
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TestSetupTest {

    @Test
    fun disposedInstanceRejectsManagerLookup() {
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(tickSource = tickSource)
        tickSource.start()
        tickSource.tick(16)
        kubriko.dispose()
        assertFailsWith<IllegalStateException> { kubriko.get<ActorManager>() }
    }
}
