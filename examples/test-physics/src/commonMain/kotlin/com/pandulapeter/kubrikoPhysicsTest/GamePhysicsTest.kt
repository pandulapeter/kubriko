package com.pandulapeter.kubrikoPhysicsTest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.physicsManager.PhysicsManager
import com.pandulapeter.kubriko.shaderManager.ShaderManager
import com.pandulapeter.kubriko.shaderManager.collection.SmoothPixelationShader
import com.pandulapeter.kubrikoPhysicsTest.implementation.BackgroundManager
import com.pandulapeter.kubrikoPhysicsTest.implementation.GameplayManager

@Composable
fun GamePhysicsTest(
    modifier: Modifier = Modifier,
) {
    val kubriko = remember {
        Kubriko.newInstance(
            BackgroundManager(),
            PhysicsManager.newInstance(),
            ShaderManager.newInstance(SmoothPixelationShader(canvasIndex = -1)),
            GameplayManager(),
        )
    }
    KubrikoCanvas(
        kubriko = kubriko,
    )
}