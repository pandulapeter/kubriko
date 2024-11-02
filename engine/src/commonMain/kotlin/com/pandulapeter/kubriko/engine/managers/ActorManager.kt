package com.pandulapeter.kubriko.engine.managers

import com.pandulapeter.kubriko.engine.traits.Visible
import com.pandulapeter.kubriko.engine.types.SceneOffset
import kotlinx.coroutines.flow.StateFlow
/**
 * TODO: Documentation
 */
interface ActorManager {

    val allActors: StateFlow<List<Any>>
    val visibleActorsWithinViewport: StateFlow<List<Visible>>

    fun add(vararg actors: Any)

    fun remove(vararg actors: Any)

    fun removeAll()

    suspend fun serializeState(): String

    suspend fun deserializeState(json: String)

    // TODO: No need for this
    fun findVisibleInstancesWithBoundsInPosition(position: SceneOffset): List<Any>

    // TODO: No need for this
    fun findVisibleInstancesWithPivotsAroundPosition(position: SceneOffset, range: Float): List<Any>
}