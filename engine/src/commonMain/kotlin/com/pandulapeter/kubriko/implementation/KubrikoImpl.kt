package com.pandulapeter.kubriko.implementation

import com.pandulapeter.kubriko.Kubriko
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

    init {
        managers.forEach { it.initializeInternal(this) }
    }

    private inline fun <reified T : Manager> Set<Manager>.addIfNeeded(creator: () -> T) = if (none { T::class.isInstance(it) }) this + creator() else this

    @Suppress("UNCHECKED_CAST")
    override fun <T : Manager> get(managerType: KClass<T>) =
        managers.firstOrNull { managerType.isInstance(it) } as? T ?: throw IllegalStateException("$managerType has not been registered as a Manager in Kubriko.newInstance()")
}