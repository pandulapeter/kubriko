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
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Scalable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toHSV

@Composable
internal fun ColorfulPropertyEditors(
    data: Pair<Colorful, Boolean>,
) = data.first.let { colorful ->
    PropertyEditorSection("Colorful") {
        PropertyTitle("color")
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color = colorful.color)
                .clickable {
                    colorful.color = Color.hsv((0..360).random().toFloat(), 0.5f, 0.9f)
                    EditorController.notifyGameObjectUpdate()
                }
        )
        Spacer(modifier = Modifier.height(8.dp))
        val (hue, saturation, value) = colorful.color.toHSV()
        SliderWithTitle(
            title = "color.hue",
            value = hue,
            onValueChange = {
                colorful.color = Color.hsv(it, saturation, value)
                EditorController.notifyGameObjectUpdate()
            },
            valueRange = 0f..359.5f,
            enabled = value > 0,
        )
        SliderWithTitle(
            title = "color.saturation",
            value = saturation,
            onValueChange = {
                colorful.color = Color.hsv(hue, it, value)
                EditorController.notifyGameObjectUpdate()
            },
            valueRange = 0f..1f,
            enabled = value > 0,
        )
        SliderWithTitle(
            title = "color.value",
            value = value,
            onValueChange = {
                colorful.color = Color.hsv(hue, saturation, it)
                EditorController.notifyGameObjectUpdate()
            },
            valueRange = 0f..1f
        )
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
internal fun ScalablePropertyEditors(
    data: Pair<Scalable, Boolean>,
) = data.first.let { scalable ->
    PropertyEditorSection("Scalable") {
        SliderWithTitle(
            title = "scaleFactor",
            value = scalable.scaleFactor,
            onValueChange = {
                scalable.scaleFactor = it
                EditorController.notifyGameObjectUpdate()
            },
            valueRange = 0f..10f
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
            valueRange = 0f..250f
        )
        SliderWithTitle(
            title = "bounds.height",
            value = visible.bounds.height,
            onValueChange = {
                visible.bounds = Size(visible.bounds.width, it)
                EditorController.notifyGameObjectUpdate()
            },
            valueRange = 0f..250f
        )
    }
}
