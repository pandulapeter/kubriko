/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation

import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CameraAnimatorTest {

    private val manualKubriko = newManualKubriko()
    private val viewportManager = manualKubriko.kubriko.get<ViewportManager>()
    private val target = SceneOffset(100f.sceneUnit, (-50f).sceneUnit)

    @AfterTest
    fun tearDown() = manualKubriko.dispose()

    @Test
    fun reachesTheTargetAfterTheAnimationDuration() = runTest {
        val animator = CameraAnimator(this, viewportManager, testScheduler.timeSource)
        animator.animateCameraTo(target)
        runCurrent()
        assertEquals(SceneOffset.Zero, viewportManager.cameraPosition.value)
        advanceTimeBy(1_000)
        assertEquals(target, viewportManager.cameraPosition.value)
    }

    @Test
    fun stopsWhenTheCameraIsMovedExternally() = runTest {
        val animator = CameraAnimator(this, viewportManager, testScheduler.timeSource)
        animator.animateCameraTo(target)
        advanceTimeBy(100)
        val externalPosition = SceneOffset((-20f).sceneUnit, 30f.sceneUnit)
        viewportManager.setCameraPosition(externalPosition)
        advanceTimeBy(1_000)
        assertEquals(externalPosition, viewportManager.cameraPosition.value)
    }
}
