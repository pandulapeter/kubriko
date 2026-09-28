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
 */
internal class AudioCache {

    private sealed interface Slot {
        /** [deferred] is null while no loader is attached. */
        class Loading(val deferred: Deferred<Any?>?) : Slot
        class Loaded(val value: Any) : Slot
    }

    private class Attachment(
        val scope: CoroutineScope,
        val loader: suspend (String) -> Any?,
        val onDiscarded: (Any) -> Unit,
    )

    private val slots = MutableStateFlow(persistentMapOf<String, Slot>())

    @Volatile
    private var attachment: Attachment? = null

    /** Every known URI mapped to its loaded value, or to `null` while it is loading. */
    val entries: Flow<Map<String, Any?>> = slots.map { slots -> slots.mapValues { (_, slot) -> (slot as? Slot.Loaded)?.value } }

    val uris: Set<String> get() = slots.value.keys

    fun loaded(uri: String) = (slots.value[uri] as? Slot.Loaded)?.value

    fun attach(
        scope: CoroutineScope,
        loader: suspend (String) -> Any?,
        onDiscarded: (Any) -> Unit,
    ) {
        attachment = Attachment(scope, loader, onDiscarded)
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
                slot == null && !shouldAddIfMissing -> return null
            }
            val attachment = attachment
            if (attachment == null && slot != null) return slot
            val newSlot = Slot.Loading(attachment?.let { load(uri, it) })
            if (slots.compareAndSet(current, current.putting(uri, newSlot))) {
                newSlot.deferred?.start()
                return newSlot
            }
            newSlot.deferred?.cancel()
        }
    }

    private fun load(uri: String, attachment: Attachment) = attachment.scope.async(start = CoroutineStart.LAZY) {
        val value = attachment.loader(uri)
        if (value == null || store(uri, coroutineContext.job, value)) {
            value
        } else {
            attachment.onDiscarded(value)
            null
        }
    }

    private fun store(uri: String, load: Any, value: Any): Boolean {
        while (true) {
            val current = slots.value
            val slot = current[uri]
            if (slot !is Slot.Loading || slot.deferred !== load) return false
            if (slots.compareAndSet(current, current.putting(uri, Slot.Loaded(value)))) return true
        }
    }
}
