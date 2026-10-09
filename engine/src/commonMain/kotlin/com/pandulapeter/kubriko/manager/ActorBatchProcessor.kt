/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Disposable
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.actor.traits.Identifiable
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class ActorBatchProcessor(
    private val shouldComposeLayers: Boolean,
    private val log: (message: String, details: String?) -> Unit,
) {
    private val _allActors = MutableStateFlow<ImmutableList<Actor>>(persistentListOf())
    val allActors: StateFlow<ImmutableList<Actor>> = _allActors.asStateFlow()
    private lateinit var kubrikoImpl: KubrikoImpl
    private lateinit var scope: CoroutineScope
    private val operationChannel = Channel<Operation>(Channel.UNLIMITED)
    private var isProcessingStarted = false
    private var processorJob: Job? = null
    @OptIn(ExperimentalAtomicApi::class)
    private val isDisposing = AtomicBoolean(false)
    /**
     * This and the two lists below are derived by the batch processor itself and published before [allActors] (so
     * before any removal callback), rather than through a main-thread hop that would keep feeding removed actors to
     * the tick loop for a while.
     */
    private val _dynamicActors = MutableStateFlow<ImmutableList<Dynamic>>(persistentListOf())
    private val _visibleActors = MutableStateFlow<ImmutableList<Visible>>(persistentListOf())
    private val _overlayActors = MutableStateFlow<ImmutableList<Overlay>>(persistentListOf())
    val dynamicActors: StateFlow<ImmutableList<Dynamic>> = _dynamicActors.asStateFlow()
    val visibleActors: StateFlow<ImmutableList<Visible>> = _visibleActors.asStateFlow()
    val overlayActors: StateFlow<ImmutableList<Overlay>> = _overlayActors.asStateFlow()

    /**
     * Applies every operation queued so far synchronously on the calling thread, round after round until the
     * callbacks enqueue nothing more, then starts the background batch processor. Runs once per instance, when the
     * Kubriko instance is first started, so that `onAdded` always sees every Manager initialized and the first tick
     * sees the initial scene.
     */
    fun start(kubrikoImpl: KubrikoImpl, scope: CoroutineScope) {
        if (isProcessingStarted) return
        isProcessingStarted = true
        this.kubrikoImpl = kubrikoImpl
        this.scope = scope
        while (true) {
            val firstOperation = operationChannel.tryReceive().getOrNull() ?: break
            processBatchStartingWith(firstOperation)
        }
        processorJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                processBatchStartingWith(operationChannel.receive())
            }
        }
    }

    /**
     * Releases the resources of the actors still in the scene: [Disposable.dispose] only, since [Actor.onRemoved]
     * carries game logic that must not fire on teardown.
     */
    @OptIn(ExperimentalAtomicApi::class)
    fun dispose() {
        isDisposing.store(true)
        processorJob?.cancel()
        processorJob = null
        for (actor in _allActors.value) {
            if (actor is Disposable) {
                runActorCallback(actor, "dispose", null) { actor.dispose() }
            }
        }
    }

    private fun processBatchStartingWith(firstOperation: Operation) {
        try {
            val batch = mutableListOf(firstOperation)
            while (true) {
                val operation = operationChannel.tryReceive().getOrNull() ?: break
                batch.add(operation)
            }
            processBatch(batch)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(
                "Actor batch processing failed.",
                e.stackTraceToString(),
            )
        }
    }

    private fun flattenActors(initialActors: List<Actor>): List<Actor> {
        val result = ArrayList<Actor>()
        val visited = HashSet<Actor>()
        val queue = ArrayDeque(initialActors)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue
            result.add(current)
            if (current is Group) {
                for (child in current.actors) {
                    if (child !in visited) {
                        queue.addLast(child)
                    }
                }
            }
        }
        return result
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalAtomicApi::class)
    private fun processBatch(batch: List<Operation>) {
        val publishedList = _allActors.value
        // One mutable working copy plus a membership index for the whole batch, so a batch of individual calls stays
        // linear and bulk removal never scans the whole list per removed actor.
        val workingList = ArrayList<Actor>(publishedList.size)
        workingList.addAll(publishedList)
        val workingSet = HashSet<Actor>(workingList.size * 2)
        workingSet.addAll(workingList)
        var didChange = false
        val newlyAdded = LinkedHashSet<Actor>()
        val newlyRemoved = LinkedHashSet<Actor>()
        for (op in batch) {
            when (op) {
                is Operation.Add -> {
                    val flattened = flattenActors(op.actors)
                    val latestUniqueByClass = LinkedHashMap<KClass<out Actor>, Actor>()
                    val nonUnique = ArrayList<Actor>(flattened.size)
                    for (a in flattened) {
                        if (a is Unique) latestUniqueByClass[a::class] = a
                        else nonUnique.add(a)
                    }
                    val newActors = ArrayList<Actor>(nonUnique.size + latestUniqueByClass.size).apply {
                        addAll(nonUnique)
                        addAll(latestUniqueByClass.values)
                    }
                    for (a in newActors) {
                        if (a is Identifiable && a.name == null) a.name = Uuid.random().toString()
                    }
                    if (latestUniqueByClass.isNotEmpty()) {
                        val uniqueTypesToReplace = latestUniqueByClass.keys
                        val iterator = workingList.iterator()
                        while (iterator.hasNext()) {
                            val actor = iterator.next()
                            if (actor::class in uniqueTypesToReplace && actor !== latestUniqueByClass[actor::class]) {
                                iterator.remove()
                                workingSet.remove(actor)
                                newlyRemoved.add(actor)
                                didChange = true
                            }
                        }
                    }
                    for (a in newActors) {
                        if (workingSet.add(a)) {
                            workingList.add(a)
                            didChange = true
                            if (!newlyRemoved.remove(a)) {
                                newlyAdded.add(a)
                            }
                        }
                    }
                }

                is Operation.Remove -> {
                    val flattenedActors = flattenActors(op.actors).asReversed()
                    val validRemovals = flattenedActors.filter { it in workingSet }
                    if (validRemovals.isNotEmpty()) {
                        val removalSet = validRemovals.toHashSet()
                        workingList.removeAll(removalSet)
                        workingSet.removeAll(removalSet)
                        didChange = true
                        newlyRemoved.addAll(validRemovals)
                    }
                }

                is Operation.RemoveAll -> {
                    if (workingList.isNotEmpty()) {
                        newlyRemoved.addAll(workingList)
                        workingList.clear()
                        workingSet.clear()
                        didChange = true
                    }
                }
            }
        }
        var firstFailure: Exception? = null
        for (actor in newlyAdded) {
            firstFailure = runActorCallback(actor, "onAdded", firstFailure) { actor.onAdded(kubrikoImpl) }
        }
        if (didChange) {
            publishDerivedActorLists(workingList)
            _allActors.value = workingList.toImmutableList()
        }
        if (isDisposing.load()) return
        for (actor in newlyRemoved) {
            if (actor is Disposable) {
                firstFailure = runActorCallback(actor, "dispose", firstFailure) { actor.dispose() }
            }
            firstFailure = runActorCallback(actor, "onRemoved", firstFailure) { actor.onRemoved() }
        }
        val failure = firstFailure
        if (failure != null) {
            scope.launch { throw failure }
        }
    }

    private fun publishDerivedActorLists(actors: List<Actor>) {
        val dynamics = ArrayList<Dynamic>()
        val visibles = ArrayList<Visible>()
        val overlays = ArrayList<Overlay>()
        for (actor in actors) {
            if (actor is Dynamic) dynamics.add(actor)
            if (actor is Visible) visibles.add(actor)
            if (shouldComposeLayers && actor is Overlay) overlays.add(actor)
        }
        if (!_dynamicActors.value.contentEquals(dynamics)) _dynamicActors.value = dynamics.toImmutableList()
        if (!_visibleActors.value.contentEquals(visibles)) _visibleActors.value = visibles.toImmutableList()
        if (!_overlayActors.value.contentEquals(overlays)) _overlayActors.value = overlays.toImmutableList()
    }

    /**
     * Runs one actor callback in isolation, so that a throwing actor never keeps the rest of the batch from being
     * applied. Returns the first failure of the batch: [firstFailure] if there already was one, otherwise the
     * exception thrown by [callback] (if any).
     */
    private inline fun runActorCallback(
        actor: Actor,
        callbackName: String,
        firstFailure: Exception?,
        callback: () -> Unit,
    ): Exception? = try {
        callback()
        firstFailure
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log(
            "Actor callback failed: $callbackName of $actor.",
            e.stackTraceToString(),
        )
        firstFailure ?: e
    }

    /**
     * This and the other enqueuing functions below send on the caller's own thread rather than from a coroutine, which
     * is what makes operations reach the processor in the order they were issued: a coroutine each would leave that
     * order to the dispatcher, and a removal arriving before the addition it undoes would find nothing to remove. The
     * unbounded channel is what allows it - enqueueing never suspends. Only the ordering is the caller's; the
     * processing stays on the processor.
     */
    fun add(actors: List<Actor>) {
        operationChannel.trySend(Operation.Add(actors))
    }

    fun remove(actors: List<Actor>) {
        operationChannel.trySend(Operation.Remove(actors))
    }

    fun removeAll() {
        operationChannel.trySend(Operation.RemoveAll)
    }

    private sealed class Operation {
        class Add(val actors: List<Actor>) : Operation()
        class Remove(val actors: List<Actor>) : Operation()
        data object RemoveAll : Operation()
    }
}
