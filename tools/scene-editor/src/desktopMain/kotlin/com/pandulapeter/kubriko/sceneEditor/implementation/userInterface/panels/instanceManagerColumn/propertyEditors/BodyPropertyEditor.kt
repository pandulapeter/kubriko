package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.actor.body.Body
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.body.ComplexBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorText
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
    EditorText(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        text = "body",
        isBold = true,
    )
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
        SceneOffsetPropertyEditor(
            name = "pivot",
            value = body.pivot,
            onValueChanged = {
                body.pivot = it
                notifySelectedInstanceUpdate()
            },
            xValueRange = 0f..body.size.width.raw,
            yValueRange = 0f..body.size.height.raw,
            shouldShowCenterButton = true,
        )
        if (body is RectangleBody) {
            SceneSizePropertyEditor(
                name = "size",
                value = body.size,
                onValueChanged = {
                    body.size = it
                    notifySelectedInstanceUpdate()
                }
            )
        }
        if (body is CircleBody) {
            FloatPropertyEditor(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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