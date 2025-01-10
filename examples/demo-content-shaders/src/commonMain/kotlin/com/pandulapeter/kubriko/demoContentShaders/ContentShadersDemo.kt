package com.pandulapeter.kubriko.demoContentShaders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersDemoManager
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.shared.ExampleStateHolder
import com.pandulapeter.kubriko.types.SceneSize
import kubriko.examples.demo_content_shaders.generated.resources.Res
import kubriko.examples.demo_content_shaders.generated.resources.shaders_not_supported
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContentShadersDemo(
    modifier: Modifier = Modifier,
    stateHolder: ContentShadersDemoStateHolder = createContentShadersDemoStateHolder(),
) {
    stateHolder as ContentShadersDemoStateHolderImpl
    if (stateHolder.shaderManager.areShadersSupported) {
        KubrikoViewport(
            modifier = Modifier.background(Color.Black),
            kubriko = stateHolder.kubriko,
        )
    } else {
        Box(
            modifier = modifier.fillMaxSize().padding(16.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(0.75f).align(Alignment.Center),
                textAlign = TextAlign.Center,
                text = stringResource(Res.string.shaders_not_supported),
            )
        }
    }
}

sealed interface ContentShadersDemoStateHolder : ExampleStateHolder

fun createContentShadersDemoStateHolder(): ContentShadersDemoStateHolder = ContentShadersDemoStateHolderImpl()

internal class ContentShadersDemoStateHolderImpl : ContentShadersDemoStateHolder {
    val shaderManager = ShaderManager.newInstance()
    val kubriko = Kubriko.newInstance(
        shaderManager,
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.Stretched(SceneSize(2000.sceneUnit, 2000.sceneUnit)),
        ),
        ContentShadersDemoManager(),
        isLoggingEnabled = true,
        instanceNameForLogging = "ContentShaders",
    )

    override fun dispose() = kubriko.dispose()
}