package com.pandulapeter.kubrikoShowcase.implementation.shaders

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun ShadersShowcase(
    modifier: Modifier = Modifier,
) {
    val gameplayManager = remember { ShadersShowcaseManager() }
    val kubriko = remember {
        Kubriko.newInstance(
            ShaderManager.newInstance(),
            gameplayManager,
        )
    }
    val selectedDemoType = gameplayManager.demoType.collectAsState()
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
                    onClick = { gameplayManager.setSelectedDemoType(demoType) }
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
                    ShaderDemoType.CLOUDS -> Unit
                    ShaderDemoType.FRACTAL -> Card(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val red = gameplayManager.red.collectAsState()
                            val green = gameplayManager.green.collectAsState()
                            val blue = gameplayManager.blue.collectAsState()
                            Slider(
                                modifier = Modifier.height(24.dp),
                                value = red.value.toFloat(),
                                onValueChange = { gameplayManager.setRed(it.roundToInt()) },
                                valueRange = 0f..20f,
                            )
                            Slider(
                                modifier = Modifier.height(24.dp),
                                value = green.value.toFloat(),
                                onValueChange = { gameplayManager.setGreen(it.roundToInt()) },
                                valueRange = 0f..20f,
                            )
                            Slider(
                                modifier = Modifier.height(24.dp),
                                value = blue.value.toFloat(),
                                onValueChange = { gameplayManager.setBlue(it.roundToInt()) },
                                valueRange = 0f..20f,
                            )
                        }
                    }
                }
            }
        }
    }
}