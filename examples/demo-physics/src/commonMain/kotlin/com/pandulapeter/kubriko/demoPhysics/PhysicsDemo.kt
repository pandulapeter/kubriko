package com.pandulapeter.kubriko.demoPhysics

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.DebugMenu
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
    DebugMenu(
        modifier = modifier,
        debugMenuModifier = modifier.windowInsetsPadding(windowInsets),
        kubriko = stateHolder.kubriko,
        buttonAlignment = null,
    ) {
        KubrikoViewport(
            kubriko = stateHolder.kubriko,
            windowInsets = windowInsets,
        )
    }
}