package com.pandulapeter.kubrikoWallbreaker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubrikoWallbreaker.implementation.WallbreakerGameplayManager

@Composable
fun WallbreakerGame(
    modifier: Modifier = Modifier,
) {
    val kubriko = remember {
        Kubriko.newInstance(
            WallbreakerGameplayManager(),
        )
    }
    KubrikoCanvas(
        kubriko = kubriko,
    )
}