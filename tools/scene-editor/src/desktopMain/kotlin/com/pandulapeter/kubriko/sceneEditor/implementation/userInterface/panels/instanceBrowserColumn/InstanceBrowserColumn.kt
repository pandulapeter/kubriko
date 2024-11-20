package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceBrowserColumn

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.actor.traits.Identifiable
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorSurface
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorText
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.ic_visible_only_off
import kubriko.tools.scene_editor.generated.resources.ic_visible_only_on
import kotlin.reflect.KClass

@Composable
internal fun InstanceBrowserColumn(
    shouldShowVisibleOnly: Boolean,
    allInstances: List<Editable<*>>,
    visibleInstances: List<Editable<*>>,
    selectedUpdatableInstance: Pair<Editable<*>?, Boolean>,
    onShouldShowVisibleOnlyToggled: () -> Unit,
    selectInstance: (Editable<*>) -> Unit,
    resolveTypeId: (KClass<out Editable<*>>) -> String?,
) = EditorSurface(
    modifier = Modifier.fillMaxHeight().width(150.dp),
    isElevated = false,
) {
    Column {
        HeaderRow(
            shouldShowVisibleOnly = shouldShowVisibleOnly,
            onShouldShowVisibleOnlyToggled = onShouldShowVisibleOnlyToggled,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(if (shouldShowVisibleOnly) visibleInstances else allInstances) { instance ->
                EditorText(
                    modifier = Modifier.fillMaxWidth().clickable { selectInstance(instance) }.padding(
                        horizontal = 8.dp,
                        vertical = 2.dp,
                    ),
                    text = instance.getName(resolveTypeId(instance::class)),
                    isBold = instance == selectedUpdatableInstance.first,
                )
            }
        }
    }
}

private fun Editable<*>.getName(typeId: String?): String {
    val type = typeId ?: "Unknown Actor type"
    val id = (this as? Identifiable)?.name
    return if (id == null) type else "$type [$id]"
}

@Composable
private fun HeaderRow(
    shouldShowVisibleOnly: Boolean,
    onShouldShowVisibleOnlyToggled: () -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 2.dp,
            )
            .padding(
                start = 8.dp,
                end = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        EditorIcon(
            drawableResource = if (shouldShowVisibleOnly) Res.drawable.ic_visible_only_on else Res.drawable.ic_visible_only_off,
            contentDescription = "Toggle visible only",
            onClick = onShouldShowVisibleOnlyToggled,
        )
    }
    HorizontalDivider()
}