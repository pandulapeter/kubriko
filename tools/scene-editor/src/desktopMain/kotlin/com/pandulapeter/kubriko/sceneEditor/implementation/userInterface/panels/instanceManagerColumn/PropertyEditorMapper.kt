/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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
import com.pandulapeter.kubriko.extensions.deg
import com.pandulapeter.kubriko.extensions.rad
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorTextTitle
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.BooleanPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.ColorPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.FloatPropertyEditor
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
import kubriko.tools.scene_editor.generated.resources.ic_collapse
import kubriko.tools.scene_editor.generated.resources.ic_expand
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.isAccessible

private val booleanType = Boolean::class.createType()
private val colorType = Color::class.createType()
private val angleDegreesType = AngleDegrees::class.createType()
private val angleRadiansType = AngleRadians::class.createType()
private val sceneOffsetType = SceneOffset::class.createType()
private val scaleType = Scale::class.createType()
private val floatType = Float::class.createType()
private val stringType = String::class.starProjectedType.withNullability(true)
private val sceneUnitType = SceneUnit::class.createType()

internal fun <T : Any> KMutableProperty<*>.toPropertyEditor(
    actor: T,
    notifySelectedInstanceUpdate: () -> Unit,
    colorEditorMode: ColorEditorMode,
    angleEditorMode: AngleEditorMode,
): (@Composable () -> Unit)? = setter.findAnnotation<Exposed>()?.let { editableProperty ->
    isAccessible = true
    editableProperty.name.let { name ->
        when (returnType) {
            booleanType -> {
                {
                    BooleanPropertyEditor(
                        name = name,
                        value = getter.call(actor) as Boolean,
                        onValueChanged = { boolean ->
                            setter.call(actor, boolean)
                            notifySelectedInstanceUpdate()
                        },
                    )
                }
            }

            colorType -> {
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

            angleDegreesType -> {
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

            angleRadiansType -> {
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

            sceneOffsetType -> {
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

            scaleType -> {
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

            floatType -> {
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

            stringType -> {
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

            sceneUnitType -> {
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
