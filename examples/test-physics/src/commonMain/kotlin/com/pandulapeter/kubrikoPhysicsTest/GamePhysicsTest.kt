package com.pandulapeter.kubrikoPhysicsTest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.physics.PhysicsManager
import com.pandulapeter.kubrikoPhysicsTest.implementation.GameplayManager

@Composable
fun GamePhysicsTest(
    modifier: Modifier = Modifier,
) {
    val kubriko = remember {
        Kubriko.newInstance(
            PhysicsManager.newInstance(),
            GameplayManager(),
        )
    }
    KubrikoCanvas(
        kubriko = kubriko,
    )
}