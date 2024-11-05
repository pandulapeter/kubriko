package com.pandulapeter.kubriko.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.manager.ActorManagerImpl
import com.pandulapeter.kubriko.implementation.manager.MetadataManagerImpl
import com.pandulapeter.kubriko.implementation.manager.StateManagerImpl
import com.pandulapeter.kubriko.implementation.manager.ViewportManagerImpl
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.reflect.KClass

internal class KubrikoImpl(
    vararg manager: Manager,
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
        managers.forEach { it.initializeInternal(this) }
    }

    private inline fun <reified T : Manager> Set<Manager>.addIfNeeded(creator: () -> T) =
        if (none { T::class.isInstance(it) }) this + creator() else this

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Manager, TI : T> requireAndVerify(name: String) =
        require<T>() as? TI ?: throw IllegalStateException("Custom implementations of the $name interface are not supported. Use $name.newInstance() to instantiate $name.")

    @Suppress("UNCHECKED_CAST")
    override fun <T : Manager> get(managerType: KClass<T>) =
        managers.firstOrNull { managerType.isInstance(it) } as? T

    override fun <T : Manager> require(managerType: KClass<T>) =
        get(managerType) ?: throw IllegalStateException("$managerType has not been registered as a Manager in Kubriko.newInstance()")
}