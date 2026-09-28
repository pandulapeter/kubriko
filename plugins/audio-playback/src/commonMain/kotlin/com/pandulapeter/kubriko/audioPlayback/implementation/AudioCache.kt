/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlin.concurrent.Volatile

/**
 * Loaded audio values of a manager, keyed by URI, with at most one load in flight per URI shared by `preload()` and
 * `play()`.
 *
 * Every per-URI state lives in one map that is only changed through `compareAndSet`, so each decision (who starts a
 * load, whether a finished load is stored or discarded, what [remove] and [clear] return) is taken from the snapshot
 * that won. Every loaded value is handed out for disposal exactly once: by [remove] / [clear], or to the
 * `onDiscarded` callback passed to [attach].
 *
 * A load that returns `null` or throws marks its URI as failed. A failed URI counts as settled in [entries], and the
 * next [preload] or [get] retries it while it keeps counting as settled.
 */
internal class AudioCache {

    private sealed interface Slot {
        /** [deferred] is null while no loader is attached. */
        class Loading(val deferred: Deferred<Any?>?) : Slot
        class Loaded(val value: Any) : Slot
        class Failed(val retry: Deferred<Any?>?) : Slot
    }

    private object LoadFailed

    private class Attachment(
        val scope: CoroutineScope,
        val loader: suspend (String) -> Any?,
        val onFailed: (String) -> Unit,
        val onDiscarded: (Any) -> Unit,
    )

    private val slots = MutableStateFlow(persistentMapOf<String, Slot>())

    @Volatile
    private var attachment: Attachment? = null

    /**
     * Every known URI mapped to `null` while it is loading, or to a non-null value once it is settled: its loaded
     * value, or an opaque marker when its load failed. Only [loaded] hands out values that can be played.
     */
    val entries: Flow<Map<String, Any?>> = slots.map { slots ->
        slots.mapValues { (_, slot) ->
            when (slot) {
                is Slot.Loading -> null
                is Slot.Loaded -> slot.value
                is Slot.Failed -> LoadFailed
            }
        }
    }

    val uris: Set<String> get() = slots.value.keys

    fun loaded(uri: String) = (slots.value[uri] as? Slot.Loaded)?.value

    fun attach(
        scope: CoroutineScope,
        loader: suspend (String) -> Any?,
        onFailed: (String) -> Unit = {},
        onDiscarded: (Any) -> Unit,
    ) {
        attachment = Attachment(scope, loader, onFailed, onDiscarded)
        slots.value.keys.forEach { uri -> acquire(uri, shouldAddIfMissing = false) }
    }

    fun preload(uri: String) {
        acquire(uri, shouldAddIfMissing = true)
    }

    suspend fun get(uri: String): Any? {
        if (attachment == null) return null
        return when (val slot = acquire(uri, shouldAddIfMissing = true)) {
            is Slot.Loaded -> slot.value
            is Slot.Loading -> slot.deferred?.await()
            is Slot.Failed -> slot.retry?.await()
            null -> null
        }
    }

    fun remove(uri: String): Any? {
        while (true) {
            val current = slots.value
            val slot = current[uri] ?: return null
            if (slots.compareAndSet(current, current.removing(uri))) return (slot as? Slot.Loaded)?.value
        }
    }

    fun clear(): List<Any> {
        while (true) {
            val current = slots.value
            if (slots.compareAndSet(current, persistentMapOf())) return current.values.mapNotNull { (it as? Slot.Loaded)?.value }
        }
    }

    /** Makes sure a load is in flight for [uri] (when a loader is attached) and returns the slot it ended up in. */
    private fun acquire(uri: String, shouldAddIfMissing: Boolean): Slot? {
        while (true) {
            val current = slots.value
            val slot = current[uri]
            when {
                slot is Slot.Loaded -> return slot
                slot is Slot.Loading && slot.deferred != null -> return slot
                slot is Slot.Failed && slot.retry != null -> return slot
                slot == null && !shouldAddIfMissing -> return null
            }
            val attachment = attachment
            if (attachment == null && slot != null) return slot
            val deferred = attachment?.let { load(uri, it) }
            val newSlot = if (slot is Slot.Failed) Slot.Failed(deferred) else Slot.Loading(deferred)
            if (slots.compareAndSet(current, current.putting(uri, newSlot))) {
                deferred?.start()
                return newSlot
            }
            deferred?.cancel()
        }
    }

    private fun load(uri: String, attachment: Attachment) = attachment.scope.async(start = CoroutineStart.LAZY) {
        val value = try {
            attachment.loader(uri)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
        if (settle(uri, coroutineContext.job, value)) {
            if (value == null) {
                attachment.onFailed(uri)
            }
            value
        } else {
            value?.let(attachment.onDiscarded)
            null
        }
    }

    private fun settle(uri: String, load: Any, value: Any?): Boolean {
        while (true) {
            val current = slots.value
            val isCurrentLoad = when (val slot = current[uri]) {
                is Slot.Loading -> slot.deferred === load
                is Slot.Failed -> slot.retry === load
                else -> false
            }
            if (!isCurrentLoad) return false
            val settled = if (value == null) Slot.Failed(null) else Slot.Loaded(value)
            if (slots.compareAndSet(current, current.putting(uri, settled))) return true
        }
    }
}
