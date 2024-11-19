package com.pandulapeter.kubrikoPong

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubrikoPong.implementation.GameplayManager

@Composable
fun PongGame(
    modifier: Modifier = Modifier,
) {
    val kubriko = remember {
        Kubriko.newInstance(
            GameplayManager(),
        )
    }
    KubrikoCanvas(
        kubriko = kubriko,
    )
}