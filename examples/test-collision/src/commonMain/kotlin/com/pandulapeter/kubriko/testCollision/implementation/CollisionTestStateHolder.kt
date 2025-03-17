/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testCollision.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.testCollision.implementation.managers.CollisionTestManager
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kubriko.examples.test_collision.generated.resources.Res
import kubriko.examples.test_collision.generated.resources.description

sealed interface CollisionTestStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areStringResourcesLoaded()

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.description).value.isNotBlank()
    }
}

internal class CollisionTestStateHolderImpl(
    isLoggingEnabled: Boolean,
) : CollisionTestStateHolder {

    private val collisionManager = CollisionManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val pointerInputManager = PointerInputManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val viewportManager = ViewportManager.newInstance(
        aspectRatioMode = ViewportManager.AspectRatioMode.FitVertical(
            height = CollisionTestManager.AREA_LIMIT.sceneUnit,
        ),
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    val collisionTestManager = CollisionTestManager()
    private val _kubriko = MutableStateFlow(
        Kubriko.newInstance(
            collisionManager,
            pointerInputManager,
            collisionTestManager,
            viewportManager,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    )
    override val kubriko = _kubriko.asStateFlow()

    override fun dispose() = kubriko.value.dispose()
}

private const val LOG_TAG = "Collision"