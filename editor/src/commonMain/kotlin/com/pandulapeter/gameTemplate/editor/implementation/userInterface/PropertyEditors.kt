package com.pandulapeter.gameTemplate.editor.implementation.userInterface

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.EditorController
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Colorful
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible

@Composable
internal fun ColorfulPropertyEditors(
    data: Pair<Colorful, Boolean>,
) = data.first.let { rotatable ->
    PropertyEditorSection("Colorful") {
        PropertyTitle("color")
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color = rotatable.color)
                .clickable {
                    rotatable.color = Color.hsv((0..360).random().toFloat(), 0.5f, 0.9f)
                    EditorController.notifyGameObjectUpdate()
                }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
internal fun RotatablePropertyEditors(
    data: Pair<Rotatable, Boolean>,
) = data.first.let { rotatable ->
    PropertyEditorSection("Rotatable") {
        SliderWithTitle(
            title = "rotationDegrees",
            value = rotatable.rotationDegrees,
            onValueChange = {
                rotatable.rotationDegrees = it
                EditorController.notifyGameObjectUpdate()
            },
            valueRange = 0f..360f
        )
    }
}

@Composable
internal fun VisiblePropertyEditors(
    data: Pair<Visible, Boolean>,
) = data.first.let { visible ->
    PropertyEditorSection("Visible") {
        SliderWithTitle(
            title = "bounds.width",
            value = visible.bounds.width,
            onValueChange = {
                visible.bounds = Size(it, visible.bounds.height)
                EditorController.notifyGameObjectUpdate()
            },
            valueRange = 50f..250f
        )
        SliderWithTitle(
            title = "bounds.height",
            value = visible.bounds.height,
            onValueChange = {
                visible.bounds = Size(visible.bounds.width, it)
                EditorController.notifyGameObjectUpdate()
            },
            valueRange = 50f..250f
        )
    }
}
