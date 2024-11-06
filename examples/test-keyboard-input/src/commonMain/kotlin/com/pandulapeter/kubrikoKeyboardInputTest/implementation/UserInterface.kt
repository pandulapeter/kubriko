package com.pandulapeter.kubrikoKeyboardInputTest.implementation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
internal fun UserInterface(
    modifier: Modifier = Modifier,
    text: String,
) = Box(
    modifier = modifier.fillMaxSize(),
) {
    Text(
        modifier = Modifier.align(Alignment.Center),
        text = text,
        textAlign = TextAlign.Center,
    )
}