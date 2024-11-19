package com.pandulapeter.kubrikoShaderTest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubrikoShaderTest.implementation.DemoType
import com.pandulapeter.kubrikoShaderTest.implementation.GameplayManager
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

@Composable
fun GameShaderTest(
    modifier: Modifier = Modifier,
) {
    val gameplayManager = remember { GameplayManager() }
    val kubriko = remember {
        Kubriko.newInstance(
            ShaderManager.newInstance(),
            gameplayManager,
        )
    }
    val selectedDemoType = gameplayManager.demoType.collectAsState()
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            DemoType.entries.forEach { demoType ->
                item(
                    icon = {
                        Icon(
                            painter = painterResource(demoType.drawableResource),
                            contentDescription = demoType.label,
                        )
                    },
                    label = { Text(demoType.label) },
                    selected = demoType == selectedDemoType.value,
                    onClick = { gameplayManager.setSelectedDemoType(demoType) }
                )
            }
        }
    ) {
        KubrikoCanvas(
            kubriko = kubriko,
        )
        Box(
            modifier = modifier.fillMaxSize(),
        ) {
            when (selectedDemoType.value) {
                DemoType.CLOUDS -> {

                }

                DemoType.FRACTAL -> {
                    Card(
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
