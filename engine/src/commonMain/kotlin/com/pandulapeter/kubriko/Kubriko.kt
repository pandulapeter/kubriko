package com.pandulapeter.kubriko

import com.pandulapeter.kubriko.Kubriko.Companion.newInstance
import com.pandulapeter.kubriko.manager.Manager
import kotlin.reflect.KClass

/**
 * Holds references to the individual Manager classes that control the different aspects of a game.
 * See the documentations of the specific Managers for detailed information.
 * Use the static [newInstance] function to instantiate a [Kubriko] implementation.
 * Provide that instance to the [KubrikoViewport] Composable to draw the game world.
 */
sealed interface Kubriko {

    /**
     * TODO: Documentation + nullability
     */
    fun <T : Manager> get(managerType: KClass<T>): T

    /**
     * TODO: Documentation
     */
    fun dispose()

    companion object {
        /**
         * Creates a new [Kubriko] instance.
         *
         * TODO: Mention default Managers
         */
        fun newInstance(
            vararg manager: Manager,
            isLoggingEnabled: Boolean = false,
        ): Kubriko = KubrikoImpl(
            manager = manager,
            isLoggingEnabled = isLoggingEnabled,
        )
    }
}