package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.actor.body.Body
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.body.ComplexBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.implementation.extensions.deg
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
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
            },
            xValueRange = 0f..body.size.width.raw,
            yValueRange = 0f..body.size.height.raw,
        )
        HorizontalDivider()
        if (body is RectangleBody) {
            SceneSizePropertyEditor(
                name = "size",
                value = body.size,
                onValueChanged = {
                    body.size = it
                    notifySelectedInstanceUpdate()
                }
            )
            HorizontalDivider()
        }
        if (body is CircleBody) {
            FloatPropertyEditor(
                name = "radius",
                value = body.radius.raw,
                onValueChanged = {
                    body.radius = it.scenePixel
                    notifySelectedInstanceUpdate()
                },
            )
        }
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