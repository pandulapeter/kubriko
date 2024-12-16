package com.pandulapeter.kubriko.demoContentShaders

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersDemoManager
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shared.ExampleStateHolder
import com.pandulapeter.kubriko.types.SceneSize

@Composable
fun ContentShadersDemo(
    modifier: Modifier = Modifier,
    stateHolder: ContentShadersDemoStateHolder = createContentShadersDemoStateHolder(),
) {
    stateHolder as ContentShadersDemoStateHolderImpl
    KubrikoViewport(
        modifier = modifier.background(Color.Black),
        kubriko = stateHolder.kubriko,
    )
}

sealed interface ContentShadersDemoStateHolder : ExampleStateHolder

fun createContentShadersDemoStateHolder(): ContentShadersDemoStateHolder = ContentShadersDemoStateHolderImpl()

internal class ContentShadersDemoStateHolderImpl : ContentShadersDemoStateHolder {
    val kubriko = Kubriko.newInstance(
        ShaderManager.newInstance(),
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.Stretched(SceneSize(1000.sceneUnit, 1000.sceneUnit)),
        ),
        ContentShadersDemoManager()
    )

    override fun dispose() = kubriko.dispose()
}