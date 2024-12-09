package com.pandulapeter.kubriko

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.implementation.InternalViewport

/**
 * This Composable should be embedded into applications to draw the game world and handle all related logic.
 *
 * @param kubriko - The [Kubriko] instance that will be used for the game within this Composable.
 */
@Composable
fun KubrikoViewport(
    modifier: Modifier = Modifier,
    kubriko: Kubriko,
    overlay: @Composable BoxScope.() -> Unit = {},
) = InternalViewport(
    modifier = modifier,
    getKubriko = { kubriko },
    overlay = overlay,
)