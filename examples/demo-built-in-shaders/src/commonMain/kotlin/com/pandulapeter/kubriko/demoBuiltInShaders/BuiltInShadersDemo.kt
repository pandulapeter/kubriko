package com.pandulapeter.kubriko.demoBuiltInShaders

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoBuiltInShaders.implementation.BuiltInShadersDemoManager
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shared.ExampleStateHolder

@Composable
fun BuiltInShadersDemo(
    modifier: Modifier = Modifier,
    stateHolder: BuiltInShadersDemoStateHolder = createBuiltInShadersDemoStateHolder(),
) {
    stateHolder as BuiltInShadersDemoStateHolderImpl
    KubrikoViewport(
        modifier = modifier.background(Color.Black),
        kubriko = stateHolder.kubriko,
    )
}

sealed interface BuiltInShadersDemoStateHolder : ExampleStateHolder

fun createBuiltInShadersDemoStateHolder(): BuiltInShadersDemoStateHolder = BuiltInShadersDemoStateHolderImpl()

internal class BuiltInShadersDemoStateHolderImpl : BuiltInShadersDemoStateHolder {
    val kubriko = Kubriko.newInstance(
        ShaderManager.newInstance(),
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.Stretched(1000.sceneUnit, 1000.sceneUnit),
        ),
        BuiltInShadersDemoManager()
    )

    override fun dispose() = kubriko.dispose()
}