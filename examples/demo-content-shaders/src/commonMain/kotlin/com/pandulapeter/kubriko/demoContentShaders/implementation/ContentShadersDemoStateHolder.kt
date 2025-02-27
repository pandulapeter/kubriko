/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoContentShaders.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoContentShaders.implementation.managers.ContentShadersDemoManager
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kubriko.examples.demo_content_shaders.generated.resources.Res
import kubriko.examples.demo_content_shaders.generated.resources.blur
import kubriko.examples.demo_content_shaders.generated.resources.chromatic_aberration
import kubriko.examples.demo_content_shaders.generated.resources.collapse_controls
import kubriko.examples.demo_content_shaders.generated.resources.comic
import kubriko.examples.demo_content_shaders.generated.resources.description
import kubriko.examples.demo_content_shaders.generated.resources.expand_controls
import kubriko.examples.demo_content_shaders.generated.resources.ic_brush
import kubriko.examples.demo_content_shaders.generated.resources.ripple
import kubriko.examples.demo_content_shaders.generated.resources.shaders_not_supported
import kubriko.examples.demo_content_shaders.generated.resources.smooth_pixelation
import kubriko.examples.demo_content_shaders.generated.resources.vignette

sealed interface ContentShadersDemoStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areIconResourcesLoaded() && areStringResourcesLoaded()

        @Composable
        private fun areIconResourcesLoaded() = preloadedImageVector(Res.drawable.ic_brush).value != null

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.description).value.isNotBlank()
                && preloadedString(Res.string.shaders_not_supported).value.isNotBlank()
                && preloadedString(Res.string.expand_controls).value.isNotBlank()
                && preloadedString(Res.string.collapse_controls).value.isNotBlank()
                && preloadedString(Res.string.chromatic_aberration).value.isNotBlank()
                && preloadedString(Res.string.ripple).value.isNotBlank()
                && preloadedString(Res.string.comic).value.isNotBlank()
                && preloadedString(Res.string.blur).value.isNotBlank()
                && preloadedString(Res.string.vignette).value.isNotBlank()
                && preloadedString(Res.string.smooth_pixelation).value.isNotBlank()
    }
}

internal class ContentShadersDemoStateHolderImpl : ContentShadersDemoStateHolder {
    private val viewportManager = ViewportManager.newInstance(
        aspectRatioMode = ViewportManager.AspectRatioMode.Stretched(SceneSize(2000.sceneUnit, 2000.sceneUnit)),
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    val shaderManager = ShaderManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val contentShadersDemoManager = ContentShadersDemoManager()
    private val _kubriko = MutableStateFlow(
        Kubriko.newInstance(
            viewportManager,
            shaderManager,
            contentShadersDemoManager,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    )
    override val kubriko = _kubriko.asStateFlow()

    override fun dispose() = kubriko.value.dispose()

    override fun navigateBack(
        isInFullscreenMode: Boolean,
        onFullscreenModeToggled: () -> Unit,
    ) = contentShadersDemoManager.areControlsExpanded.value.also {
        if (it) {
            contentShadersDemoManager.toggleControlsExpanded()
        }
    }
}

private const val LOG_TAG = "ContentShaders"