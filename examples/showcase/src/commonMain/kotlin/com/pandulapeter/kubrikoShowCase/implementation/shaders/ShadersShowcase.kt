package com.pandulapeter.kubrikoShowcase.implementation.shaders

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ui.CloudControls
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ui.FractalControls
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ShadersShowcase(
    modifier: Modifier = Modifier,
) {
    val shadersShowcaseManager = remember { ShadersShowcaseManager() }
    val kubriko = remember {
        Kubriko.newInstance(
            ShaderManager.newInstance(),
            shadersShowcaseManager,
        )
    }
    val selectedDemoType = shadersShowcaseManager.demoType.collectAsState()
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TabRow(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            selectedTabIndex = selectedDemoType.value.ordinal,
        ) {
            ShaderDemoType.entries.forEach { demoType ->
                Tab(
                    text = { Text(stringResource(demoType.nameStringResource)) },
                    selected = demoType == selectedDemoType.value,
                    onClick = { shadersShowcaseManager.setSelectedDemoType(demoType) }
                )
            }
        }
        Crossfade(
            targetState = selectedDemoType.value
        ) { demoType ->
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                KubrikoCanvas(
                    kubriko = kubriko,
                )
                when (demoType) {
                    ShaderDemoType.CLOUDS -> CloudControls(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        properties = shadersShowcaseManager.cloudProperties.collectAsState().value,
                        onPropertiesChanged = shadersShowcaseManager::setCloudProperties,
                    )

                    ShaderDemoType.FRACTAL -> FractalControls(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        properties = shadersShowcaseManager.fractalProperties.collectAsState().value,
                        onPropertiesChanged = shadersShowcaseManager::setFractalProperties,
                    )
                }
            }
        }
    }
}