package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorTextTitle
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.AngleDegreesPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.AngleRadiansPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.ColorPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.FloatPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.ScalePropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.SceneOffsetPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.ScenePixelPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.StringPropertyEditor
import com.pandulapeter.kubriko.sceneSerializer.integration.EditableProperty
import com.pandulapeter.kubriko.types.AngleDegrees
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.ic_collapse
import kubriko.tools.scene_editor.generated.resources.ic_expand
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability


internal fun <T : Any> KMutableProperty<*>.toPropertyEditor(
    instance: T,
    notifySelectedInstanceUpdate: () -> Unit,
): (@Composable () -> Unit)? = setter.findAnnotation<EditableProperty>()?.let { editableProperty ->
    editableProperty.name.let { name ->
        when (returnType) {
            Color::class.createType() -> {
                {
                    ColorPropertyEditor(
                        name = name,
                        value = getter.call(instance) as Color,
                        onValueChanged = { color ->
                            setter.call(instance, color)
                            notifySelectedInstanceUpdate()
                        }
                    )
                }
            }

            AngleDegrees::class.createType() -> {
                {
                    AngleDegreesPropertyEditor(
                        name = name,
                        value = getter.call(instance) as AngleDegrees,
                        onValueChanged = {
                            setter.call(instance, it)
                            notifySelectedInstanceUpdate()
                        }
                    )
                }
            }

            AngleRadians::class.createType() -> {
                {
                    AngleRadiansPropertyEditor(
                        name = name,
                        value = getter.call(instance) as AngleRadians,
                        onValueChanged = {
                            setter.call(instance, it)
                            notifySelectedInstanceUpdate()
                        }
                    )
                }
            }

            SceneOffset::class.createType() -> {
                {
                    SceneOffsetPropertyEditor(
                        name = name,
                        value = getter.call(instance) as SceneOffset,
                        onValueChanged = {
                            setter.call(instance, it)
                            notifySelectedInstanceUpdate()
                        }
                    )
                }
            }

            Scale::class.createType() -> {
                {
                    ScalePropertyEditor(
                        name = name,
                        value = getter.call(instance) as Scale,
                        onValueChanged = {
                            setter.call(instance, it)
                            notifySelectedInstanceUpdate()
                        }
                    )
                }
            }

            Float::class.createType() -> {
                {
                    FloatPropertyEditor(
                        name = name,
                        value = getter.call(instance) as Float,
                        onValueChanged = {
                            setter.call(instance, it)
                            notifySelectedInstanceUpdate()
                        }
                    )
                }
            }

            String::class.starProjectedType.withNullability(true) -> {
                {
                    StringPropertyEditor(
                        name = name,
                        value = getter.call(instance) as String,
                        onValueChanged = {
                            setter.call(instance, it)
                            notifySelectedInstanceUpdate()
                        }
                    )
                }
            }

            ScenePixel::class.createType() -> {
                {
                    ScenePixelPropertyEditor(
                        name = name,
                        value = getter.call(instance) as ScenePixel,
                        onValueChanged = {
                            setter.call(instance, it)
                            notifySelectedInstanceUpdate()
                        }
                    )
                }
            }

            else -> null
        }
    }
}

@Composable
private fun LazyItemScope.EditorCategory(
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
