package com.pandulapeter.kubrikoShaderTest

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubrikoShaderTest.implementation.DemoType
import com.pandulapeter.kubrikoShaderTest.implementation.GameplayManager
import org.jetbrains.compose.resources.painterResource

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
    }
}
