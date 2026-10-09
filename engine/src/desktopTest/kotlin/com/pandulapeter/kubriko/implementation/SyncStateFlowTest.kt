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

import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncStateFlowTest {

    private fun newKubriko(viewportManager: ViewportManager = ViewportManager.newInstance()): Pair<KubrikoImpl, TickSource> {
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(viewportManager, tickSource = tickSource) as KubrikoImpl
        return kubriko to tickSource
    }

    @Test
    fun firstEmissionIsTheCurrentScale() {
        val (kubriko, tickSource) = newKubriko(ViewportManager.newInstance(initialScaleFactor = 2f))
        tickSource.start()
        runBlocking {
            withTimeout(2_000) {
                assertEquals(Scale(2f, 2f), kubriko.viewportManager.scaleFactor.first())
            }
        }
        kubriko.dispose()
    }

    @Test
    fun firstEmissionIsTheCurrentTopLeft() {
        val (kubriko, tickSource) = newKubriko()
        tickSource.start()
        kubriko.viewportManager.updateSize(Size(800f, 600f))
        kubriko.viewportManager.setCameraPosition(SceneOffset(100f.sceneUnit, 0f.sceneUnit))
        runBlocking {
            withTimeout(2_000) {
                assertEquals(kubriko.viewportManager.topLeft.value, kubriko.viewportManager.topLeft.first())
            }
        }
        kubriko.dispose()
    }

    @Test
    fun isRunningFirstEmissionMatchesValue() {
        val (kubriko, tickSource) = newKubriko()
        runBlocking(Dispatchers.Main) {
            tickSource.start()
            assertEquals(kubriko.stateManager.isRunning.value, kubriko.stateManager.isRunning.first())
        }
        kubriko.dispose()
    }

    @Test
    fun replayCacheHoldsTheCurrentValue() {
        val (kubriko, tickSource) = newKubriko(ViewportManager.newInstance(initialScaleFactor = 2f))
        tickSource.start()
        assertEquals(listOf(kubriko.viewportManager.scaleFactor.value), kubriko.viewportManager.scaleFactor.replayCache)
        kubriko.dispose()
    }
}
