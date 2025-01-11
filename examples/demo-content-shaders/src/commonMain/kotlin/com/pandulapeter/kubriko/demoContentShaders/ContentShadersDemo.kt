package com.pandulapeter.kubriko.demoContentShaders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersDemoStateHolderImpl
import kubriko.examples.demo_content_shaders.generated.resources.Res
import kubriko.examples.demo_content_shaders.generated.resources.shaders_not_supported
import org.jetbrains.compose.resources.stringResource

fun createContentShadersDemoStateHolder(): ContentShadersDemoStateHolder = ContentShadersDemoStateHolderImpl()

@Composable
fun ContentShadersDemo(
    modifier: Modifier = Modifier,
    stateHolder: ContentShadersDemoStateHolder = createContentShadersDemoStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as ContentShadersDemoStateHolderImpl
    DebugMenu(
        modifier = modifier,
        debugMenuModifier = modifier.windowInsetsPadding(windowInsets),
        kubriko = stateHolder.kubriko,
        buttonAlignment = null,
    ) {
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
}