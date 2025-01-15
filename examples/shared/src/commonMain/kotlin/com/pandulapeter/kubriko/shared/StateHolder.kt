package com.pandulapeter.kubriko.shared

import com.pandulapeter.kubriko.Kubriko
import kotlinx.coroutines.flow.Flow

interface StateHolder {

    /**
     * Used for the debug menu
     */
    val kubriko: Flow<Kubriko?>

    /**
     * The music would stop after dispose() is called, but, due to the crossfade transition, that might be just a bit too late.
     */
    fun stopMusic() = Unit

    /**
     * Can be used to free up all resources related to the current demo.
     */
    fun dispose()
}