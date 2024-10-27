package com.pandulapeter.gameTemplate.editor.implementation.userInterface

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.EditorController
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorSlider
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorTextLabel
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorTextTitle
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Colorful
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Scalable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toHSV
import game.editor.generated.resources.Res
import game.editor.generated.resources.ic_collapse
import game.editor.generated.resources.ic_expand

@Composable
internal fun LazyItemScope.ColorfulPropertyEditors(
    data: Pair<Colorful, Boolean>,
    isExpanded: Boolean,
    onExpandedChanged: () -> Unit,
) = data.first.let { colorful ->
    PropertyEditorSection(
        title = "Colorful",
        isExpanded = isExpanded,
        onExpandedChanged = onExpandedChanged,
    ) {
        EditorTextLabel(text = "color")
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color = colorful.color),
        )
        Spacer(modifier = Modifier.height(8.dp))
        val (hue, saturation, value) = colorful.color.toHSV()
        EditorSlider(
            title = "color.hue",
            value = hue,
            onValueChange = {
                colorful.color = Color.hsv(it, saturation, value)
                EditorController.notifyGameObjectUpdate()
            },
            valueRange = 0f..359.5f,
            enabled = saturation >0 && value > 0,
        )
        EditorSlider(
            title = "color.saturation",
            value = saturation,
            onValueChange = {
                colorful.color = Color.hsv(hue, it, value)
                EditorController.notifyGameObjectUpdate()
            },
            valueRange = 0f..1f,
            enabled = value > 0,
        )
        EditorSlider(
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
internal fun LazyItemScope.RotatablePropertyEditors(
    data: Pair<Rotatable, Boolean>,
    isExpanded: Boolean,
    onExpandedChanged: () -> Unit,
) = data.first.let { rotatable ->
    PropertyEditorSection(
        title = "Rotatable",
        isExpanded = isExpanded,
        onExpandedChanged = onExpandedChanged,
    ) {
        EditorSlider(
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
internal fun LazyItemScope.ScalablePropertyEditors(
    data: Pair<Scalable, Boolean>,
    isExpanded: Boolean,
    onExpandedChanged: () -> Unit,
) = data.first.let { scalable ->
    PropertyEditorSection(
        title = "Scalable",
        isExpanded = isExpanded,
        onExpandedChanged = onExpandedChanged,
    ) {
        EditorSlider(
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
internal fun LazyItemScope.VisiblePropertyEditors(
    data: Pair<Visible, Boolean>,
    isExpanded: Boolean,
    onExpandedChanged: () -> Unit,
) = data.first.let { visible ->
    PropertyEditorSection(
        title = "Visible",
        isExpanded = isExpanded,
        onExpandedChanged = onExpandedChanged,
    ) {
        EditorTextLabel(
            text = "x: ${visible.position.x}",
        )
        EditorTextLabel(
            text = "y: ${visible.position.y}",
        )
        EditorSlider(
            title = "pivot.x",
            value = visible.pivot.x,
            onValueChange = {
                visible.pivot = Offset(it, visible.pivot.y)
                EditorController.notifyGameObjectUpdate()
            },
            valueRange = 0f..visible.bounds.width,
            enabled = visible.bounds.width > 0,
        )
        EditorSlider(
            title = "pivot.y",
            value = visible.pivot.y,
            onValueChange = {
                visible.pivot = Offset(visible.pivot.x, it)
                EditorController.notifyGameObjectUpdate()
            },
            valueRange = 0f..visible.bounds.height,
            enabled = visible.bounds.height > 0,
        )
        EditorSlider(
            title = "bounds.width",
            value = visible.bounds.width,
            onValueChange = {
                visible.bounds = Size(it, visible.bounds.height)
                EditorController.notifyGameObjectUpdate()
            },
            valueRange = 0f..250f
        )
        EditorSlider(
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

@Composable
private fun LazyItemScope.PropertyEditorSection(
    title: String,
    isExpanded: Boolean,
    onExpandedChanged: () -> Unit,
    controls: @Composable () -> Unit,
) = Column(
    modifier = Modifier.animateItem().fillMaxWidth(),
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .clickable(onClick = onExpandedChanged)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorTextTitle(
            modifier = Modifier.weight(1f),
            text = title,
        )
        EditorIcon(
            drawableResource = if (isExpanded) Res.drawable.ic_collapse else Res.drawable.ic_expand,
            contentDescription = if (isExpanded) "Collapse" else "Expand"
        )
    }
    AnimatedVisibility(
        visible = isExpanded
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            controls()
        }
    }
}
