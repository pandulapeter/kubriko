package com.pandulapeter.kubriko.debugMenu

import com.pandulapeter.kubriko.debugMenu.implementation.InternalDebugMenu

object DebugMenu {

    val isVisible = InternalDebugMenu.isVisible

    fun toggleVisibility() = InternalDebugMenu.toggleVisibility()
}