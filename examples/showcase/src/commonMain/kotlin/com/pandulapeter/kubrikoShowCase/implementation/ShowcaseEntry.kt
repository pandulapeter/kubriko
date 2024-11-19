package com.pandulapeter.kubrikoShowcase.implementation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal enum class ShowcaseEntry(
    val title: String,
    val content: @Composable () -> Unit = {
        Text(
            modifier = Modifier.fillMaxSize().background(Color.Red).padding(16.dp),
            text = title,
        )
    },
) {
    KEYBOARD_INPUT(
        title = "Keyboard Input",
    ),
    PERFORMANCE_TEST(
        title = "Performance Test",
    ),
    POINTER_INPUT(
        title = "Pointer Input",
    ),
    PHYSICS(
        title = "Physics",
    ),
    SHADERS(
        title = "Shaders",
    ),
}