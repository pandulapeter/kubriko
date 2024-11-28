package com.pandulapeter.kubriko.demoPhysics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoManager
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoType
import com.pandulapeter.kubriko.physics.PhysicsManager
import org.jetbrains.compose.resources.stringResource

@Composable
fun PhysicsDemo(
    modifier: Modifier = Modifier,
) {
    val physicsDemoManager = remember { PhysicsDemoManager() }
    val kubriko = remember {
        Kubriko.newInstance(
            PhysicsManager.newInstance(),
            physicsDemoManager,
        )
    }
    val selectedDemoType = physicsDemoManager.demoType.collectAsState()
    DebugMenu(
        modifier = modifier,
        kubriko = kubriko,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            TabRow(
                modifier = Modifier.fillMaxWidth(),
                selectedTabIndex = selectedDemoType.value.ordinal,
            ) {
                PhysicsDemoType.entries.forEach { demoType ->
                    Tab(
                        modifier = Modifier.height(42.dp),
                        text = { Text(stringResource(demoType.nameStringResource)) },
                        selected = demoType == selectedDemoType.value,
                        onClick = { physicsDemoManager.setSelectedDemoType(demoType) }
                    )
                }
            }
            KubrikoViewport(
                modifier = Modifier.fillMaxSize(),
                kubriko = kubriko,
            )
        }
    }
}