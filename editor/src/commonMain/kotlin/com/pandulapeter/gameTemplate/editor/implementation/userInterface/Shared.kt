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
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import game.editor.generated.resources.Res
import game.editor.generated.resources.ic_collapse
import game.editor.generated.resources.ic_expand
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun GameObjectTypeRadioButton(
    gameObjectType: Class<out GameObject>,
    selectedGameObjectType: Class<out GameObject>,
    onSelected: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onSelected),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    RadioButton(
        selected = gameObjectType == selectedGameObjectType,
        onClick = onSelected,
    )
    Text(
        text = gameObjectType.simpleName,
    )
}

@Composable
internal fun LazyItemScope.PropertyEditorSection(
    title: String,
    controls: @Composable () -> Unit,
) = Column(
    modifier = Modifier.animateItem().fillMaxWidth(),
) {
    val isExpanded = remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .clickable { isExpanded.value = !isExpanded.value }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.subtitle1,
            text = title,
        )
        Icon(
            painter = painterResource(if (isExpanded.value) Res.drawable.ic_collapse else Res.drawable.ic_expand),
            contentDescription = if (isExpanded.value) "Collapse" else "Expand"
        )
    }
    AnimatedVisibility(
        visible = isExpanded.value
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            controls()
        }
    }
}

@Composable
internal fun SliderWithTitle(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
) {
    PropertyTitle(
        text = "$title: ${"%.2f".format(value)}",
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
    )
}

@Composable
internal fun PropertyTitle(
    text: String,
) = Text(
    style = MaterialTheme.typography.caption,
    text = text,
)