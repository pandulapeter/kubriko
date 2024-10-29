package com.pandulapeter.gameTemplate.editor.implementation.userInterface

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.gameObject.editor.VisibleInEditor
import game.editor.generated.resources.Res
import game.editor.generated.resources.ic_collapse
import game.editor.generated.resources.ic_expand
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

@Composable
internal fun <T : Trait<out T>> LazyItemScope.GenericTraitEditor(
    data: Pair<T, Boolean>,
    visibleInEditor: VisibleInEditor,
    isExpanded: Boolean,
    onExpandedChanged: () -> Unit,
) {
    TraitEditorSection(
        title = visibleInEditor.typeId,
        isExpanded = isExpanded,
        onExpandedChanged = onExpandedChanged,
        controls = data.first::class.declaredMemberProperties
            .filter { it.findAnnotation<VisibleInEditor>() != null && it is KMutableProperty<*> }
            .mapNotNull { property ->
                property.findAnnotation<VisibleInEditor>()!!.let { editableProperty ->
                    when (property.returnType) {
                        Color::class.createType() -> {
                            { EditorTextLabel(text = "colorType: ${editableProperty.typeId}") }
                        }

                        Float::class.createType() -> {
                            {
                                EditorSlider(
                                    title = editableProperty.typeId,
                                    value = property.getter.call(data.first) as Float,
                                    onValueChange = {
                                        (property as KMutableProperty<*>).setter.call(data.first, it)
                                        EditorController.notifyGameObjectUpdate()
                                    },
                                    valueRange = 0f..1f
                                )
                                 }
                        }

                        Size::class.createType() -> {
                            { EditorTextLabel(text = "sizeType: ${editableProperty.typeId}") }
                        }

                        Offset::class.createType() -> {
                            { EditorTextLabel(text = "offsetType: ${editableProperty.typeId}") }
                        }

                        else -> null
                    }
                }
            }
    )
}

//@Composable
//internal fun LazyItemScope.ColorfulTraitEditor(
//    data: Pair<Colorful, Boolean>,
//    isExpanded: Boolean,
//    onExpandedChanged: () -> Unit,
//) = data.first.let { colorful ->
//    TraitEditorSection(
//        title = "Colorful",
//        isExpanded = isExpanded,
//        onExpandedChanged = onExpandedChanged,
//    ) {
//        EditorTextLabel(text = "color")
//        Spacer(modifier = Modifier.height(8.dp))
//        Box(
//            modifier = Modifier
//                .size(48.dp)
//                .background(color = colorful.color),
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//        val (hue, saturation, value) = colorful.color.toHSV()
//        EditorSlider(
//            title = "color.hue",
//            value = hue,
//            onValueChange = {
//                colorful.color = Color.hsv(it, saturation, value)
//                EditorController.notifyGameObjectUpdate()
//            },
//            valueRange = 0f..359.5f,
//            enabled = saturation > 0 && value > 0,
//        )
//        EditorSlider(
//            title = "color.saturation",
//            value = saturation,
//            onValueChange = {
//                colorful.color = Color.hsv(hue, it, value)
//                EditorController.notifyGameObjectUpdate()
//            },
//            valueRange = 0f..1f,
//            enabled = value > 0,
//        )
//    }
//}
//
//@Composable
//internal fun LazyItemScope.UniqueTraitEditor() = TraitEditorSection(
//    title = "Unique",
//)
//
//@Composable
//internal fun LazyItemScope.VisibleTraitEditor(
//    data: Pair<Visible, Boolean>,
//    isExpanded: Boolean,
//    onExpandedChanged: () -> Unit,
//) = data.first.let { visible ->
//    TraitEditorSection(
//        title = "Visible",
//        isExpanded = isExpanded,
//        onExpandedChanged = onExpandedChanged,
//    ) {
//        EditorTextLabel(
//            text = "position.x: ${visible.position.x}",
//        )
//        EditorTextLabel(
//            text = "position.y: ${visible.position.y}",
//        )
//        EditorSlider(
//            title = "pivot.x",
//            value = visible.pivot.x,
//            onValueChange = {
//                visible.pivot = Offset(it, visible.pivot.y)
//                EditorController.notifyGameObjectUpdate()
//            },
//            valueRange = 0f..visible.bounds.width,
//            enabled = visible.bounds.width > 0,
//        )
//        EditorSlider(
//            title = "pivot.y",
//            value = visible.pivot.y,
//            onValueChange = {
//                visible.pivot = Offset(visible.pivot.x, it)
//                EditorController.notifyGameObjectUpdate()
//            },
//            valueRange = 0f..visible.bounds.height,
//            enabled = visible.bounds.height > 0,
//        )
//        EditorSlider(
//            title = "bounds.width",
//            value = visible.bounds.width,
//            onValueChange = {
//                visible.bounds = Size(it, visible.bounds.height)
//                EditorController.notifyGameObjectUpdate()
//            },
//            valueRange = 0f..250f
//        )
//        EditorSlider(
//            title = "bounds.height",
//            value = visible.bounds.height,
//            onValueChange = {
//                visible.bounds = Size(visible.bounds.width, it)
//                EditorController.notifyGameObjectUpdate()
//            },
//            valueRange = 0f..250f
//        )
//        EditorSlider(
//            title = "scale.width",
//            value = visible.scale.width,
//            onValueChange = {
//                visible.scale = Size(it, visible.scale.height)
//                EditorController.notifyGameObjectUpdate()
//            },
//            valueRange = 0f..10f
//        )
//        EditorSlider(
//            title = "scale.height",
//            value = visible.scale.height,
//            onValueChange = {
//                visible.scale = Size(visible.scale.width, it)
//                EditorController.notifyGameObjectUpdate()
//            },
//            valueRange = 0f..10f
//        )
//        EditorSlider(
//            title = "rotationDegrees",
//            value = visible.rotationDegrees,
//            onValueChange = {
//                visible.rotationDegrees = it
//                EditorController.notifyGameObjectUpdate()
//            },
//            valueRange = 0f..360f
//        )
//    }
//}

@Composable
private fun LazyItemScope.TraitEditorSection(
    title: String,
    isExpanded: Boolean = false,
    onExpandedChanged: () -> Unit = {},
    controls: List<@Composable () -> Unit> = emptyList(),
) = Column(
    modifier = Modifier.animateItem().fillMaxWidth(),
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .clickable(
                enabled = controls.isNotEmpty(),
                onClick = onExpandedChanged,
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorTextTitle(
            modifier = Modifier.weight(1f),
            text = title,
        )
        if (controls.isNotEmpty()) {
            EditorIcon(
                drawableResource = if (isExpanded) Res.drawable.ic_collapse else Res.drawable.ic_expand,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
    }
    if (controls.isNotEmpty()) {
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
                controls.forEach { it.invoke() }
            }
        }
    }
}
