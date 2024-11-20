package com.pandulapeter.kubrikoShowcase.implementation.physics

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.physics.PhysicsManager

@Composable
fun PhysicsShowcase(
    modifier: Modifier = Modifier,
) {
    val kubriko = remember {
        Kubriko.newInstance(
            PhysicsManager.newInstance(),
            PhysicsShowcaseManager(),
        )
    }
    KubrikoCanvas(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        kubriko = kubriko,
    )
}