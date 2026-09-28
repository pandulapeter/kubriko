/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
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
import com.pandulapeter.kubriko.helpers.extensions.deg
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorTextTitle
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.BooleanPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.ColorPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.FloatPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.IntPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.RotationPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.ScalePropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.SceneOffsetPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.SceneUnitPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.StringPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.AngleEditorMode
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.ColorEditorMode
import com.pandulapeter.kubriko.types.AngleDegrees
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.action_collapse
import kubriko.tools.scene_editor.generated.resources.action_expand
import kubriko.tools.scene_editor.generated.resources.ic_collapse
import kubriko.tools.scene_editor.generated.resources.ic_expand
import org.jetbrains.compose.resources.stringResource
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible

private val booleanType = Boolean::class.createType()
private val colorType = Color::class.createType()
private val angleDegreesType = AngleDegrees::class.createType()
private val angleRadiansType = AngleRadians::class.createType()
private val sceneOffsetType = SceneOffset::class.createType()
private val scaleType = Scale::class.createType()
private val floatType = Float::class.createType()
private val intType = Int::class.createType()
private val sceneUnitType = SceneUnit::class.createType()

internal enum class PropertyEditorKind {
    BOOLEAN,
    COLOR,
    ANGLE_DEGREES,
    ANGLE_RADIANS,
    SCENE_OFFSET,
    SCALE,
    FLOAT,
    INT,
    STRING,
    SCENE_UNIT,
}

/**
 * Picks the editor for a property type. Only [String] is matched regardless of nullability; every other type must
 * match exactly (non-null).
 */
internal fun KType.toPropertyEditorKind(): PropertyEditorKind? = if (classifier == String::class) {
    PropertyEditorKind.STRING
} else when (this) {
    booleanType -> PropertyEditorKind.BOOLEAN
    colorType -> PropertyEditorKind.COLOR
    angleDegreesType -> PropertyEditorKind.ANGLE_DEGREES
    angleRadiansType -> PropertyEditorKind.ANGLE_RADIANS
    sceneOffsetType -> PropertyEditorKind.SCENE_OFFSET
    scaleType -> PropertyEditorKind.SCALE
    floatType -> PropertyEditorKind.FLOAT
    intType -> PropertyEditorKind.INT
    sceneUnitType -> PropertyEditorKind.SCENE_UNIT
    else -> null
}

internal fun <T : Any> KMutableProperty<*>.toPropertyEditor(
    actor: T,
    onBeforeChange: (editKey: Any) -> Unit,
    notifySelectedInstanceUpdate: () -> Unit,
    colorEditorMode: ColorEditorMode,
    angleEditorMode: AngleEditorMode,
): (@Composable () -> Unit)? = setter.findAnnotation<Exposed>()?.let { editableProperty ->
    isAccessible = true
    val applyValue: (value: Any?) -> Unit = { value ->
        onBeforeChange(this)
        setter.call(actor, value)
        notifySelectedInstanceUpdate()
    }
    editableProperty.name.let { name ->
        when (returnType.toPropertyEditorKind()) {
            PropertyEditorKind.BOOLEAN -> {
                {
                    BooleanPropertyEditor(
                        name = name,
                        value = getter.call(actor) as Boolean,
                        onValueChanged = { boolean -> applyValue(boolean) },
                    )
                }
            }

            PropertyEditorKind.COLOR -> {
                {
                    ColorPropertyEditor(
                        name = name,
                        value = getter.call(actor) as Color,
                        onValueChanged = { color -> applyValue(color) },
                        colorEditorMode = colorEditorMode,
                    )
                }
            }

            PropertyEditorKind.ANGLE_DEGREES -> {
                {
                    RotationPropertyEditor(
                        name = name,
                        value = (getter.call(actor) as AngleDegrees).rad,
                        onValueChanged = { applyValue(it.deg.normalized) },
                        angleEditorMode = angleEditorMode,
                    )
                }
            }

            PropertyEditorKind.ANGLE_RADIANS -> {
                {
                    RotationPropertyEditor(
                        name = name,
                        value = getter.call(actor) as AngleRadians,
                        onValueChanged = { applyValue(it) },
                        angleEditorMode = angleEditorMode,
                    )
                }
            }

            PropertyEditorKind.SCENE_OFFSET -> {
                {
                    SceneOffsetPropertyEditor(
                        name = name,
                        value = getter.call(actor) as SceneOffset,
                        onValueChanged = { applyValue(it) }
                    )
                }
            }

            PropertyEditorKind.SCALE -> {
                {
                    ScalePropertyEditor(
                        name = name,
                        value = getter.call(actor) as Scale,
                        onValueChanged = { applyValue(it) }
                    )
                }
            }

            PropertyEditorKind.FLOAT -> {
                {
                    FloatPropertyEditor(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        name = name,
                        value = getter.call(actor) as Float,
                        onValueChanged = { applyValue(it) },
                    )
                }
            }

            PropertyEditorKind.INT -> {
                {
                    IntPropertyEditor(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        name = name,
                        value = getter.call(actor) as Int,
                        onValueChanged = { applyValue(it) },
                    )
                }
            }

            PropertyEditorKind.STRING -> {
                {
                    StringPropertyEditor(
                        name = name,
                        value = (getter.call(actor) as String?).orEmpty(),
                        onValueChanged = { applyValue(it) }
                    )
                }
            }

            PropertyEditorKind.SCENE_UNIT -> {
                {
                    SceneUnitPropertyEditor(
                        name = name,
                        value = getter.call(actor) as SceneUnit,
                        onValueChanged = { applyValue(it) },
                    )
                }
            }

            null -> null
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
                contentDescription = stringResource(if (isExpanded) Res.string.action_collapse else Res.string.action_expand)
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
