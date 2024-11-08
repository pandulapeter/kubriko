package com.pandulapeter.kubrikoPhysicsTest

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.shaderManager.ShaderManager
import com.pandulapeter.kubrikoPhysicsTest.implementation.BackgroundManager
import com.pandulapeter.kubrikoPhysicsTest.implementation.GameplayManager

@Composable
fun GamePhysicsTest(
    modifier: Modifier = Modifier,
) {
    val kubrikoBackground = remember { Kubriko.newInstance(BackgroundManager(), ShaderManager.newInstance()) }
    val kubrikoGame = remember { Kubriko.newInstance(GameplayManager()) }
    KubrikoCanvas(
        modifier = Modifier.background(Color.Black).alpha(0.5f),
        kubriko = kubrikoBackground,
    )
    KubrikoCanvas(
        kubriko = kubrikoGame,
    )
}