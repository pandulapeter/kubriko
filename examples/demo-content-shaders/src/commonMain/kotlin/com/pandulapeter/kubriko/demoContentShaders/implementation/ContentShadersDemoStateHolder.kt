package com.pandulapeter.kubriko.demoContentShaders.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Disposable
import com.pandulapeter.kubriko.demoContentShaders.implementation.managers.ContentShadersDemoManager
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.types.SceneSize

sealed interface ContentShadersDemoStateHolder : Disposable

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
    val kubriko = Kubriko.newInstance(
        viewportManager,
        shaderManager,
        contentShadersDemoManager,
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )

    override fun dispose() = kubriko.dispose()
}

private const val LOG_TAG = "ContentShaders"