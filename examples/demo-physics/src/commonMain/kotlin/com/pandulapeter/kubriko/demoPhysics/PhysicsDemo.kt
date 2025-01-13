package com.pandulapeter.kubriko.demoPhysics

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoStateHolder
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoStateHolderImpl

fun createPhysicsDemoStateHolder(): PhysicsDemoStateHolder = PhysicsDemoStateHolderImpl()

@Composable
fun PhysicsDemo(
    modifier: Modifier = Modifier,
    stateHolder: PhysicsDemoStateHolder = createPhysicsDemoStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as PhysicsDemoStateHolderImpl
    KubrikoViewport(
        modifier = modifier,
        kubriko = stateHolder.kubriko.collectAsState().value,
        windowInsets = windowInsets,
    )
}