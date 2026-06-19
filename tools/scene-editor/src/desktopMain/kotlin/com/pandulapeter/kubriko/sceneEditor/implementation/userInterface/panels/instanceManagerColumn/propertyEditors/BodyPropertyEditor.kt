/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorText
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.AngleEditorMode
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.property_body
import kubriko.tools.scene_editor.generated.resources.property_pivot
import kubriko.tools.scene_editor.generated.resources.property_position
import kubriko.tools.scene_editor.generated.resources.property_rotation
import kubriko.tools.scene_editor.generated.resources.property_scale
import kubriko.tools.scene_editor.generated.resources.property_size
import org.jetbrains.compose.resources.stringResource
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties

internal fun createBodyPropertyEditor(
    getActor: () -> Positionable,
    onBeforeChange: (editKey: Any) -> Unit,
    notifySelectedInstanceUpdate: () -> Unit,
    angleEditorMode: AngleEditorMode,
): @Composable () -> Unit {
    return {
        BodyPropertyEditor(
            getActor = getActor,
            onBeforeChange = onBeforeChange,
            notifySelectedInstanceUpdate = notifySelectedInstanceUpdate,
            angleEditorMode = angleEditorMode,
        )
    }
}

@Composable
internal fun BodyPropertyEditor(
    getActor: () -> Positionable,
    onBeforeChange: (editKey: Any) -> Unit,
    notifySelectedInstanceUpdate: () -> Unit,
    angleEditorMode: AngleEditorMode,
) {
    EditorText(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        text = stringResource(Res.string.property_body),
        isBold = true,
    )
    val actor = getActor()
    val bd = actor::class.memberProperties.firstOrNull { it.returnType.isSubtypeOf(PointBody::class.createType()) }
    val body = bd!!.getter.call(actor) as PointBody
    SceneOffsetPropertyEditor(
        name = stringResource(Res.string.property_position),
        value = body.position,
        onValueChanged = {
            onBeforeChange("body.position")
            body.position = it
            notifySelectedInstanceUpdate()
        }
    )
    if (body is BoxBody) {
        SceneOffsetPropertyEditor(
            name = stringResource(Res.string.property_pivot),
            value = body.pivot,
            onValueChanged = {
                onBeforeChange("body.pivot")
                body.pivot = it
                notifySelectedInstanceUpdate()
            },
            xValueRange = 0f..body.size.width.raw,
            yValueRange = 0f..body.size.height.raw,
            shouldShowCenterButton = true,
        )
        SceneSizePropertyEditor(
            name = stringResource(Res.string.property_size),
            value = body.size,
            onValueChanged = {
                onBeforeChange("body.size")
                body.size = it
                notifySelectedInstanceUpdate()
            }
        )
        ScalePropertyEditor(
            name = stringResource(Res.string.property_scale),
            value = body.scale,
            onValueChanged = {
                onBeforeChange("body.scale")
                body.scale = it
                notifySelectedInstanceUpdate()
            },
        )
        RotationPropertyEditor(
            name = stringResource(Res.string.property_rotation),
            value = body.rotation,
            onValueChanged = {
                onBeforeChange("body.rotation")
                body.rotation = it
                notifySelectedInstanceUpdate()
            },
            angleEditorMode = angleEditorMode,
        )
    }
}