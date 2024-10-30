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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorTextTitle
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.propertyEditors.AngleDegreesPropertyEditor
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.propertyEditors.ColorPropertyEditor
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.propertyEditors.FloatPropertyEditor
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.propertyEditors.WorldCoordinatesPropertyEditor
import com.pandulapeter.gameTemplate.engine.gameObject.editor.Editable
import com.pandulapeter.gameTemplate.engine.types.AngleDegrees
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import game.editor.generated.resources.Res
import game.editor.generated.resources.ic_collapse
import game.editor.generated.resources.ic_expand
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation


internal fun <T : Any> KMutableProperty<*>.toPropertyEditor(
    instance: T,
    notifySelectedInstanceUpdate: () -> Unit,
): (@Composable () -> Unit)? = setter.findAnnotation<Editable>()?.let { editableProperty ->
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

            WorldCoordinates::class.createType() -> {
                {
                    WorldCoordinatesPropertyEditor(
                        name = name,
                        value = getter.call(instance) as WorldCoordinates,
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
