package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.actor.body.Body
import com.pandulapeter.kubriko.actor.body.ComplexBody
import com.pandulapeter.kubriko.actor.traits.Positionable
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties

internal fun createBodyPropertyEditor(
    getActor: () -> Positionable,
    notifySelectedInstanceUpdate: () -> Unit,
    rotationEditorMode: RotationEditorMode,
    onRotationEditorModeChanged: (RotationEditorMode) -> Unit,
): @Composable () -> Unit {
    return {
        BodyPropertyEditor(
            getActor = getActor,
            notifySelectedInstanceUpdate = notifySelectedInstanceUpdate,
            rotationEditorMode = rotationEditorMode,
            onRotationEditorModeChanged = onRotationEditorModeChanged,
        )
    }
}

@Composable
internal fun BodyPropertyEditor(
    getActor: () -> Positionable,
    notifySelectedInstanceUpdate: () -> Unit,
    rotationEditorMode: RotationEditorMode,
    onRotationEditorModeChanged: (RotationEditorMode) -> Unit,
) {
    val actor = getActor()
    val bd = actor::class.memberProperties.firstOrNull { it.returnType.isSubtypeOf(Body::class.createType()) }
    val body = bd!!.getter.call(actor) as Body
    SceneOffsetPropertyEditor(
        name = "position",
        value = body.position,
        onValueChanged = {
            body.position = it
            notifySelectedInstanceUpdate()
        }
    )
    if (body is ComplexBody) {
        HorizontalDivider()
        SceneOffsetPropertyEditor(
            name = "pivot",
            value = body.pivot,
            onValueChanged = {
                body.pivot = it
                notifySelectedInstanceUpdate()
            }
        )
        HorizontalDivider()
        SceneSizePropertyEditor(
            name = "size",
            value = body.size,
            onValueChanged = {
                body.size = it
                notifySelectedInstanceUpdate()
            }
        )
        HorizontalDivider()
        ScalePropertyEditor(
            name = "scale",
            value = body.scale,
            onValueChanged = {
                body.scale = it
                notifySelectedInstanceUpdate()
            },
        )
        HorizontalDivider()
        RotationPropertyEditor(
            name = "rotation",
            value = body.rotation,
            onValueChanged = {
                body.rotation = it
                notifySelectedInstanceUpdate()
            },
            rotationEditorMode = rotationEditorMode,
            onRotationEditorModeChanged = onRotationEditorModeChanged,
        )
    }
}