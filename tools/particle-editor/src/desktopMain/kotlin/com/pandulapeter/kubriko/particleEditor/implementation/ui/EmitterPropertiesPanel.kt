package com.pandulapeter.kubriko.particleEditor.implementation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.particleEditor.implementation.manager.ParticleEditorManager
import com.pandulapeter.kubriko.uiComponents.LargeButton
import com.pandulapeter.kubriko.uiComponents.ShaderSlider
import kubriko.tools.particle_editor.generated.resources.Res
import kubriko.tools.particle_editor.generated.resources.burst
import kubriko.tools.particle_editor.generated.resources.emit_continuously
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EmitterPropertiesPanel(
    modifier: Modifier,
    particleEditorManager: ParticleEditorManager,
) = LazyColumn(
    modifier = modifier,
) {
    item("type") {
        Type(
            emissionRate = particleEditorManager.emissionRate.collectAsState().value,
            onEmissionRateChanged = particleEditorManager::setEmissionRate,
            isEmittingContinuously = particleEditorManager.isEmittingContinuously.collectAsState().value,
            onEmittingContinuouslyChanged = particleEditorManager::onEmittingContinuouslyChanged,
            onBurstButtonPressed = particleEditorManager::burst,
        )
    }
}

@Composable
private fun Type(
    emissionRate: Float,
    onEmissionRateChanged: (Float) -> Unit,
    isEmittingContinuously: Boolean,
    onEmittingContinuouslyChanged: () -> Unit,
    onBurstButtonPressed: () -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    ShaderSlider(
        modifier = Modifier.padding(horizontal = 8.dp),
        value = emissionRate,
        onValueChanged = onEmissionRateChanged,
        valueRange = 0f..20f,
    )
    Row(
        modifier = Modifier.fillMaxSize()
            .selectable(
                selected = isEmittingContinuously,
                onClick = onEmittingContinuouslyChanged,
            ).padding(start = 8.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(Res.string.emit_continuously),
        )
        Switch(
            modifier = Modifier.scale(0.6f).height(24.dp),
            checked = isEmittingContinuously,
            onCheckedChange = { onEmittingContinuouslyChanged() },
        )
    }
    LargeButton(
        modifier = Modifier.padding(horizontal = 8.dp),
        title = Res.string.burst,
        isEnabled = !isEmittingContinuously && emissionRate > 0f,
        onButtonPressed = onBurstButtonPressed,
    )
}