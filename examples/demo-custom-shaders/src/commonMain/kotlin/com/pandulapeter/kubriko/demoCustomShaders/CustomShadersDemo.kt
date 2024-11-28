package com.pandulapeter.kubriko.demoCustomShaders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoCustomShaders.implementation.CustomShaderDemoType
import com.pandulapeter.kubriko.demoCustomShaders.implementation.CustomShadersDemoManager
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.ControlsContainer
import org.jetbrains.compose.resources.stringResource

@Composable
fun CustomShadersDemo(
    modifier: Modifier = Modifier,
) {
    val customShadersDemoManager = remember { CustomShadersDemoManager() }
    val kubriko = remember {
        Kubriko.newInstance(
            ShaderManager.newInstance(),
            customShadersDemoManager,
        )
    }
    val selectedDemoType = customShadersDemoManager.demoType.collectAsState()
    val areControlsExpanded = remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TabRow(
            modifier = Modifier.fillMaxWidth(),
            selectedTabIndex = selectedDemoType.value.ordinal,
        ) {
            CustomShaderDemoType.entries.forEach { demoType ->
                Tab(
                    modifier = Modifier.height(42.dp),
                    text = { Text(stringResource(demoType.nameStringResource)) },
                    selected = demoType == selectedDemoType.value,
                    onClick = { customShadersDemoManager.setSelectedDemoType(demoType) }
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            KubrikoViewport(
                kubriko = kubriko,
            )
            ControlsContainer(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                state = selectedDemoType.value to areControlsExpanded.value,
                onIsExpandedChanged = { areControlsExpanded.value = it },
                customShadersDemoManager = customShadersDemoManager,
            )
        }
    }
}