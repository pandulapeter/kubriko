package com.pandulapeter.kubriko.demoParticles.implementation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.demoParticles.implementation.managers.ParticlesDemoManager
import com.pandulapeter.kubriko.uiComponents.LargeButton
import com.pandulapeter.kubriko.uiComponents.ShaderSlider
import kubriko.examples.demo_particles.generated.resources.Res
import kubriko.examples.demo_particles.generated.resources.burst
import kubriko.examples.demo_particles.generated.resources.emit_continuously
import kubriko.examples.demo_particles.generated.resources.lifespan
import kubriko.examples.demo_particles.generated.resources.rate
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun EmitterPropertiesPanel(
    modifier: Modifier,
    particlesDemoManager: ParticlesDemoManager,
) = Card(
    modifier = modifier,
    colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surface),
) {
    LazyColumn {
        item("type") {
            Type(
                emissionRate = particlesDemoManager.emissionRate.collectAsState().value,
                onEmissionRateChanged = particlesDemoManager::setEmissionRate,
                isEmittingContinuously = particlesDemoManager.isEmittingContinuously.collectAsState().value,
                onEmittingContinuouslyChanged = particlesDemoManager::onEmittingContinuouslyChanged,
                onBurstButtonPressed = particlesDemoManager::burst,
                lifespan = particlesDemoManager.lifespan.collectAsState().value,
                onLifespanChanged = particlesDemoManager::setLifespan,
            )
        }
    }
}

@Composable
private fun Type(
    emissionRate: Float,
    onEmissionRateChanged: (Float) -> Unit,
    isEmittingContinuously: Boolean,
    onEmittingContinuouslyChanged: () -> Unit,
    onBurstButtonPressed: () -> Unit,
    lifespan: Float,
    onLifespanChanged: (Float) -> Unit,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp),
) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = stringResource(Res.string.rate, emissionRate.formatToString()),
    )
    ShaderSlider(
        modifier = Modifier.padding(horizontal = 8.dp),
        value = emissionRate,
        onValueChanged = onEmissionRateChanged,
        valueRange = 0f..0.5f,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        text = stringResource(Res.string.lifespan, lifespan.roundToInt()),
    )
    ShaderSlider(
        modifier = Modifier.padding(horizontal = 8.dp),
        value = lifespan,
        onValueChanged = onLifespanChanged,
        valueRange = 100f..2000f,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxSize()
            .selectable(
                enabled = emissionRate > 0f,
                selected = isEmittingContinuously,
                onClick = onEmittingContinuouslyChanged,
            )
            .padding(vertical = 8.dp)
            .padding(
                start = 16.dp,
                end = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(Res.string.emit_continuously),
        )
        Switch(
            modifier = Modifier.scale(0.6f).height(24.dp),
            enabled = emissionRate > 0f,
            checked = isEmittingContinuously,
            onCheckedChange = { onEmittingContinuouslyChanged() },
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    LargeButton(
        modifier = Modifier.padding(horizontal = 16.dp).align(Alignment.End),
        title = Res.string.burst,
        isEnabled = !isEmittingContinuously && emissionRate > 0f,
        onButtonPressed = onBurstButtonPressed,
    )
}

private fun Float.formatToString() = toString().let {
    it.substringBefore('.') + "." + it.substringAfter('.').take(2)
}