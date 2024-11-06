package com.pandulapeter.kubrikoKeyboardInputTest

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas

@Composable
fun GameKeyboardInputTest(
    modifier: Modifier = Modifier,
) = MaterialTheme {
    KubrikoCanvas(
        kubriko = Kubriko.newInstance(),
    )
}