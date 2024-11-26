package com.pandulapeter.kubriko.demoCollisions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.demoCollisions.implementation.CollisionsDemoManager

@Composable
fun CollisionsDemo(
    modifier: Modifier = Modifier,
) {
    val kubriko = remember {
        Kubriko.newInstance(
            CollisionManager.newInstance(),
            CollisionsDemoManager()
        )
    }
    DebugMenu(
        kubriko = kubriko,
    ) {
        KubrikoViewport(
            modifier = modifier,
            kubriko = kubriko,
        )
    }
}