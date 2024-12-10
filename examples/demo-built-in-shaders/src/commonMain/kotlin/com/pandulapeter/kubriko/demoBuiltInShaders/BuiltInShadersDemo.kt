package com.pandulapeter.kubriko.demoBuiltInShaders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoBuiltInShaders.implementation.BuiltInShadersDemoManager
import com.pandulapeter.kubriko.shader.ShaderManager

@Composable
fun BuiltInShadersDemo(
    modifier: Modifier = Modifier,
) {
    val kubriko = remember {
        Kubriko.newInstance(
            ShaderManager.newInstance(),
            BuiltInShadersDemoManager()
        )
    }
    KubrikoViewport(
        modifier = modifier,
        kubriko = kubriko,
    )
}