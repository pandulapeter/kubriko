package com.pandulapeter.kubriko

import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.logger.Logger
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ActorManagerImpl
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.MetadataManagerImpl
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.StateManagerImpl
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.manager.ViewportManagerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.reflect.KClass

internal class KubrikoImpl(
    vararg manager: Manager,
    override var isLoggingEnabled: Boolean,
) : Kubriko, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val managers = manager.toSet()
        .addIfNeeded { ActorManager.newInstance() }
        .addIfNeeded { MetadataManager.newInstance() }
        .addIfNeeded { StateManager.newInstance() }
        .addIfNeeded { ViewportManager.newInstance() }
        .distinctBy { it::class }
    val actorManager = requireAndVerify<ActorManager, ActorManagerImpl>("ActorManager")
    val metadataManager = requireAndVerify<MetadataManager, MetadataManagerImpl>("MetadataManager")
    val stateManager = requireAndVerify<StateManager, StateManagerImpl>("StateManager")
    val viewportManager = requireAndVerify<ViewportManager, ViewportManagerImpl>("ViewportManager")

    init {
        // Internal Managers must be initialized first so that custom ones could get a stable state
        actorManager.initializeInternal(this)
        metadataManager.initializeInternal(this)
        stateManager.initializeInternal(this)
        viewportManager.initializeInternal(this)

        log(
            message = "Kubriko instance created with ${managers.size} Managers.",
            details = managers.joinToString { it::class.simpleName.orEmpty() },
        )
    }

    internal fun initialize() {
        log("Initializing Manager instances...")
        managers.forEach { it.initializeInternal(this) }
        if (stateManager.shouldAutoStart) {
            stateManager.updateIsRunning(true)
        }
        log("Kubriko initialization completed.")
    }

    private inline fun <reified T : Manager> Collection<Manager>.addIfNeeded(creator: () -> T) =
        if (none { T::class.isInstance(it) }) toMutableList().apply { add(0, creator()) } else this

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Manager, TI : T> requireAndVerify(name: String) = get<T>() as? TI
        ?: throw IllegalStateException("Custom implementations of the $name interface are not supported. Use $name.newInstance() to instantiate $name.")

    @Suppress("UNCHECKED_CAST")
    override fun <T : Manager> get(managerType: KClass<T>) = managers.firstOrNull { managerType.isInstance(it) } as? T
        ?: throw IllegalStateException("$managerType has not been registered as a Manager in Kubriko.newInstance()")

    override fun dispose() {
        log("Disposing Manager instances...")
        managers.forEach { it.onDisposeInternal() }
        cancel()
        log("Kubriko disposal completed.")
    }

    private fun log(
        message: String,
        details: String? = null,
    ) {
        if (isLoggingEnabled) {
            Logger.log(
                message = message,
                details = details,
                source = "Kubriko_$this"
            )
        }
    }
}