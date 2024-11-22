package com.pandulapeter.kubriko.demoPhysics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoManager
import com.pandulapeter.kubriko.physics.PhysicsManager

@Composable
fun PhysicsDemo(
    modifier: Modifier = Modifier,
) {
    val kubriko = remember {
        Kubriko.newInstance(
            PhysicsManager.newInstance(),
            PhysicsDemoManager(),
        )
    }
    KubrikoViewport(
        modifier = modifier,
        kubriko = kubriko,
    )
}