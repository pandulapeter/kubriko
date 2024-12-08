package com.pandulapeter.kubriko.demoPhysics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubriko.demoPhysics.implementation.ActionType
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoKubrikoWrapper
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoManager
import com.pandulapeter.kubriko.demoPhysics.implementation.PlatformSpecificContent
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.PhysicsManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import kubriko.examples.demo_physics.generated.resources.Res
import kubriko.examples.demo_physics.generated.resources.chain
import kubriko.examples.demo_physics.generated.resources.explosion
import kubriko.examples.demo_physics.generated.resources.ic_chain
import kubriko.examples.demo_physics.generated.resources.ic_explosion
import kubriko.examples.demo_physics.generated.resources.ic_shape
import kubriko.examples.demo_physics.generated.resources.shape
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PhysicsDemo(
    modifier: Modifier = Modifier,
) {
    val physicsDemoKubrikoWrapper = remember { PhysicsDemoKubrikoWrapper() }
    val selectedActionType = physicsDemoKubrikoWrapper.physicsDemoManager.actionType.collectAsState()
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        DebugMenu(
            modifier = modifier,
            kubriko = physicsDemoKubrikoWrapper.kubriko,
        ) {
            KubrikoViewport(
                modifier = Modifier.fillMaxSize(),
                kubriko = physicsDemoKubrikoWrapper.kubriko,
            )
            PlatformSpecificContent()
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                FloatingActionButton(
                    modifier = Modifier.size(40.dp).align(Alignment.BottomEnd),
                    containerColor = MaterialTheme.colorScheme.primary,
                    onClick = physicsDemoKubrikoWrapper.physicsDemoManager::changeSelectedActionType,
                ) {
                    Icon(
                        painter = painterResource(
                            when (selectedActionType.value) {
                                ActionType.SHAPE -> Res.drawable.ic_shape
                                ActionType.CHAIN -> Res.drawable.ic_chain
                                ActionType.EXPLOSION -> Res.drawable.ic_explosion
                            }
                        ),
                        contentDescription = stringResource(
                            when (selectedActionType.value) {
                                ActionType.SHAPE -> Res.string.shape
                                ActionType.CHAIN -> Res.string.chain
                                ActionType.EXPLOSION -> Res.string.explosion
                            }
                        ),
                    )
                }
            }
        }
    }
}