package com.pandulapeter.kubriko.demoParticles.implementation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.demoParticles.implementation.managers.ParticlesDemoManager
import com.pandulapeter.kubriko.uiComponents.LargeButton
import com.pandulapeter.kubriko.uiComponents.Panel
import com.pandulapeter.kubriko.uiComponents.SmallSliderWithTitle
import com.pandulapeter.kubriko.uiComponents.SmallSwitch
import kubriko.examples.demo_particles.generated.resources.Res
import kubriko.examples.demo_particles.generated.resources.burst
import kubriko.examples.demo_particles.generated.resources.emit_continuously
import kubriko.examples.demo_particles.generated.resources.lifespan
import kubriko.examples.demo_particles.generated.resources.rate
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EmitterPropertiesPanel(
    modifier: Modifier,
    particlesDemoManager: ParticlesDemoManager,
) = Panel(
    modifier = modifier,
) {
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
        .verticalScroll(rememberScrollState())
        .padding(vertical = 8.dp),
) {
    SmallSliderWithTitle(
        title = stringResource(Res.string.rate),
        modifier = Modifier.padding(horizontal = 16.dp),
        value = emissionRate,
        onValueChanged = onEmissionRateChanged,
        valueRange = 0f..0.5f,
    )
    SmallSliderWithTitle(
        title = stringResource(Res.string.lifespan),
        modifier = Modifier.padding(horizontal = 16.dp),
        value = lifespan,
        onValueChanged = onLifespanChanged,
        valueRange = 100f..2000f,
    )
    SmallSwitch(
        title = stringResource(Res.string.emit_continuously),
        isEnabled = emissionRate > 0f,
        isChecked = isEmittingContinuously,
        onCheckedChanged = onEmittingContinuouslyChanged,
    )
    Spacer(modifier = Modifier.height(4.dp))
    LargeButton(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = Res.string.burst,
        isEnabled = !isEmittingContinuously && emissionRate > 0f,
        onButtonPressed = onBurstButtonPressed,
    )
    Spacer(modifier = Modifier.height(4.dp))
}