package com.pandulapeter.kubriko.gameWallbreaker

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.WallbreakerGameManager
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager

@Composable
fun WallbreakerGame(
    modifier: Modifier = Modifier,
) {
    val kubriko = remember {
        Kubriko.newInstance(
            ViewportManager.newInstance(
                aspectRatioMode = ViewportManager.AspectRatioMode.Fixed(
                    ratio = 16f / 9f,
                    defaultWidth = 1200.sceneUnit,
                )
            ),
            CollisionManager.newInstance(),
            WallbreakerGameManager()
        )
    }
    KubrikoViewport(
        modifier = modifier.background(Color.LightGray),
        kubriko = kubriko,
    )
}