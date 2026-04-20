/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko

import com.pandulapeter.kubriko.Kubriko.Companion.newInstance
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import kotlin.reflect.KClass

/**
 * Holds references to the individual [Manager] classes that control the different aspects of the game.
 * See the documentations of the specific managers for detailed information.
 * Use the static [newInstance] function to instantiate a [Kubriko] implementation.
 * Provide that instance to the [KubrikoViewport] Composable to draw the game world.
 */
sealed interface Kubriko {

    /**
     * Whether logging is enabled for this [Kubriko] instance and its Managers.
     */
    val isLoggingEnabled: Boolean

    /**
     * The name of this [Kubriko] instance, used for logging.
     */
    val instanceName: String

    /**
     * Retrieves a [Manager] of the specified type.
     *
     * @param managerType The class of the [Manager] to retrieve.
     * @return The [Manager] instance.
     * @throws IllegalStateException if the [Manager] has not been registered or if the [Kubriko] instance has been disposed.
     */
    fun <T : Manager> get(managerType: KClass<T>): T

    /**
     * Disposes of this [Kubriko] instance and all its [Manager]s.
     * This should be called when the game engine is no longer needed.
     */
    fun dispose()

    companion object {
        /**
         * Creates a new [Kubriko] instance.
         *
         * If not provided, the default [ActorManager], [MetadataManager], [StateManager], and [ViewportManager]
         * implementations will be automatically added.
         *
         * @param manager Optional custom [Manager] implementations.
         * @param isLoggingEnabled Whether to enable logging for this instance.
         * @param instanceNameForLogging Optional name to use for this instance in log messages.
         */
        fun newInstance(
            vararg manager: Manager,
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): Kubriko = KubrikoImpl(
            manager = manager,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}