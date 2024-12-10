package com.pandulapeter.kubriko.demoBuiltInShaders

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoBuiltInShaders.implementation.BuiltInShadersDemoManager
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.shader.ShaderManager

@Composable
fun BuiltInShadersDemo(
    modifier: Modifier = Modifier,
) {
    val kubriko = remember {
        Kubriko.newInstance(
            ShaderManager.newInstance(),
            ViewportManager.newInstance(
                aspectRatioMode = ViewportManager.AspectRatioMode.Stretched(1000.sceneUnit, 1000.sceneUnit),
            ),
            BuiltInShadersDemoManager()
        )
    }
    KubrikoViewport(
        modifier = modifier.background(Color.Black),
        kubriko = kubriko,
    )
}