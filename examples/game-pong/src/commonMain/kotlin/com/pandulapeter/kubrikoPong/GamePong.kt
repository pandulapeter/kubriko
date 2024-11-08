package com.pandulapeter.kubrikoPong

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.collection.RippleShader
import com.pandulapeter.kubrikoPong.implementation.GameplayManager

@Composable
fun GamePong(
    modifier: Modifier = Modifier,
) {
    val kubriko = remember {
        Kubriko.newInstance(
            ShaderManager.newInstance(RippleShader(canvasIndex = -1)),
            GameplayManager(),
        )
    }
    KubrikoCanvas(
        kubriko = kubriko,
    )
}