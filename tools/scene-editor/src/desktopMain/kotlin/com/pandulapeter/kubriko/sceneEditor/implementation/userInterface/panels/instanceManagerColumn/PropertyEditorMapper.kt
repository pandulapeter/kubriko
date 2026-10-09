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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.helpers.extensions.deg
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.sceneEditor.Exposed
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
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

internal fun exposedMutableProperties(type: KClass<*>): List<KMutableProperty<*>> = type.memberProperties
    .filterIsInstance<KMutableProperty<*>>()
    .filter { it.setter.findAnnotation<Exposed>() != null }
    .sortedBy { it.name }

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
