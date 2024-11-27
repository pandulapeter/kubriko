package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.actor.body.Body
import com.pandulapeter.kubriko.actor.body.ComplexBody
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorText
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties

internal fun createBodyPropertyEditor(
    getActor: () -> Positionable,
    notifySelectedInstanceUpdate: () -> Unit,
): @Composable () -> Unit {
    return {
        BodyPropertyEditor(
            getActor = getActor,
            notifySelectedInstanceUpdate = notifySelectedInstanceUpdate,
        )
    }
}

@Composable
internal fun BodyPropertyEditor(
    getActor: () -> Positionable,
    notifySelectedInstanceUpdate: () -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth()
) {
    val actor = getActor()
    val bd = actor::class.memberProperties.firstOrNull { it.returnType.isSubtypeOf(Body::class.createType()) }
    val body = bd!!.getter.call(actor) as Body
    EditorText(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        text = "Position: ${body.position.x.raw};${body.position.y.raw}",
    )
    if (body is ComplexBody) {
        EditorText(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            text = "Pivot: ${body.pivot.x.raw};${body.pivot.y.raw}",
        )
        EditorText(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            text = "Size: ${body.size.width.raw};${body.size.height.raw}",
        )
        ScalePropertyEditor(
            name = "Scale",
            value = body.scale,
            onValueChanged = {
                body.scale = it
                notifySelectedInstanceUpdate()
            },
        )
        AngleRadiansPropertyEditor(
            name = "Rotation",
            value = body.rotation,
            onValueChanged = {
                body.rotation = it
                notifySelectedInstanceUpdate()
            },
        )
    }
}