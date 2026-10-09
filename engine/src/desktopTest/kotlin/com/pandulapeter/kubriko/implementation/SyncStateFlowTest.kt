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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncStateFlowTest {

    private val delegate = MutableStateFlow(0)
    @Volatile
    private var syncValue = 1
    private val syncStateFlow = SyncStateFlow(delegate) { syncValue }

    private fun newStartedKubriko(viewportManager: ViewportManager): KubrikoImpl {
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(viewportManager, tickSource = tickSource) as KubrikoImpl
        tickSource.start()
        return kubriko
    }

    @Test
    fun valueIsComputedSynchronouslyInsteadOfReadFromTheDelegate() {
        syncValue = 5

        assertEquals(5, syncStateFlow.value)
        assertEquals(listOf(5), syncStateFlow.replayCache)
    }

    @Test
    fun collectorsReceiveTheSynchronousValueInsteadOfTheDelegatesPlaceholder() {
        val firstEmission = runBlocking { withTimeout(2_000) { syncStateFlow.first() } }

        assertEquals(1, firstEmission)
    }

    @Test
    fun delegateEmissionsTriggerNewValuesWithoutRepeats() {
        val emissions = runBlocking(Dispatchers.Default) {
            withTimeout(2_000) {
                val collected = async(start = CoroutineStart.UNDISPATCHED) { syncStateFlow.take(2).toList() }
                delegate.value = 1
                syncValue = 2
                delegate.value = 2
                collected.await()
            }
        }

        assertEquals(listOf(1, 2), emissions)
    }

    @Test
    fun viewportScaleFactorEmitsTheCurrentScaleFirst() {
        val kubriko = newStartedKubriko(ViewportManager.newInstance(initialScaleFactor = 2f))
        try {
            val firstEmission = runBlocking { withTimeout(2_000) { kubriko.viewportManager.scaleFactor.first() } }

            assertEquals(Scale(2f, 2f), firstEmission)
        } finally {
            kubriko.dispose()
        }
    }

    @Test
    fun viewportTopLeftEmitsTheCurrentBoundsFirst() {
        val kubriko = newStartedKubriko(ViewportManager.newInstance())
        try {
            kubriko.viewportManager.updateSize(Size(800f, 600f))
            kubriko.viewportManager.setCameraPosition(SceneOffset(100f.sceneUnit, 0f.sceneUnit))

            val firstEmission = runBlocking { withTimeout(2_000) { kubriko.viewportManager.topLeft.first() } }

            assertEquals(kubriko.viewportManager.topLeft.value, firstEmission)
        } finally {
            kubriko.dispose()
        }
    }
}
