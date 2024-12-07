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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.implementation.extensions.deg
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorTextTitle
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.ColorPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.FloatPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.RotationPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.ScalePropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.SceneOffsetPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.SceneUnitPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.StringPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.ColorEditorMode
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.AngleEditorMode
import com.pandulapeter.kubriko.types.AngleDegrees
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.ic_collapse
import kubriko.tools.scene_editor.generated.resources.ic_expand
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.isAccessible

internal fun <T : Any> KMutableProperty<*>.toPropertyEditor(
    actor: T,
    notifySelectedInstanceUpdate: () -> Unit,
    colorEditorMode: ColorEditorMode,
    angleEditorMode: AngleEditorMode,
): (@Composable () -> Unit)? = setter.findAnnotation<Exposed>()?.let { editableProperty ->
    isAccessible = true
    editableProperty.name.let { name ->
        when (returnType) {
            Color::class.createType() -> {
                {
                    ColorPropertyEditor(
                        name = name,
                        value = getter.call(actor) as Color,
                        onValueChanged = { color ->
                            setter.call(actor, color)
                            notifySelectedInstanceUpdate()
                        },
                        colorEditorMode = colorEditorMode,
                    )
                }
            }

            AngleDegrees::class.createType() -> {
                {
                    RotationPropertyEditor(
                        name = name,
                        value = (getter.call(actor) as AngleDegrees).rad,
                        onValueChanged = {
                            setter.call(actor, it.deg)
                            notifySelectedInstanceUpdate()
                        },
                        angleEditorMode = angleEditorMode,
                    )
                }
            }

            AngleRadians::class.createType() -> {
                {
                    RotationPropertyEditor(
                        name = name,
                        value = getter.call(actor) as AngleRadians,
                        onValueChanged = {
                            setter.call(actor, it)
                            notifySelectedInstanceUpdate()
                        },
                        angleEditorMode = angleEditorMode,
                    )
                }
            }

            SceneOffset::class.createType() -> {
                {
                    SceneOffsetPropertyEditor(
                        name = name,
                        value = getter.call(actor) as SceneOffset,
                        onValueChanged = {
                            setter.call(actor, it)
                            notifySelectedInstanceUpdate()
                        }
                    )
                }
            }

            Scale::class.createType() -> {
                {
                    ScalePropertyEditor(
                        name = name,
                        value = getter.call(actor) as Scale,
                        onValueChanged = {
                            setter.call(actor, it)
                            notifySelectedInstanceUpdate()
                        }
                    )
                }
            }

            Float::class.createType() -> {
                {
                    FloatPropertyEditor(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        name = name,
                        value = getter.call(actor) as Float,
                        onValueChanged = {
                            setter.call(actor, it)
                            notifySelectedInstanceUpdate()
                        },
                    )
                }
            }

            String::class.starProjectedType.withNullability(true) -> {
                {
                    StringPropertyEditor(
                        name = name,
                        value = getter.call(actor) as String,
                        onValueChanged = {
                            setter.call(actor, it)
                            notifySelectedInstanceUpdate()
                        }
                    )
                }
            }

            SceneUnit::class.createType() -> {
                {
                    SceneUnitPropertyEditor(
                        name = name,
                        value = getter.call(actor) as SceneUnit,
                        onValueChanged = {
                            setter.call(actor, it)
                            notifySelectedInstanceUpdate()
                        },
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
            .background(MaterialTheme.colorScheme.surface)
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
