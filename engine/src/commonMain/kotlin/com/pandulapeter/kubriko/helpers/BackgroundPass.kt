/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * A heavy field a game recomputes now and then (a lighting grid, a cloud density lattice), computed off the frame
 * thread.
 *
 * It fits work that takes milliseconds of pure arithmetic, depends on nothing the frame is doing, and has nothing in
 * the frame waiting for it: the frame keeps drawing from the last finished result, and a pass in flight only decides
 * what the next one looks like. The frame thread captures everything the pass reads into the subclass, calls [submit],
 * and picks the result up through [collect] a frame or two later.
 *
 * Everything a pass reads is the subclass' to capture before it submits, so that nothing the frame goes on changing is
 * read while it runs. The result's storage is the one thing shared: the frame thread must neither read it nor hand it
 * to anything else until [collect] reports the pass finished. A pass whose result would no longer describe anything is
 * [abandon]ed rather than cancelled: letting it run out on a thread nobody waits for costs less than the bookkeeping
 * that would stop it.
 *
 * [run] should yield between slices of its work. That costs nothing where [Dispatchers.Default] is a real thread
 * pool, and keeps a target without threads (the web) spreading a pass across frames instead of stalling one.
 *
 * [submit], [collect], and [abandon] belong to one thread, the one drawing.
 */
abstract class BackgroundPass {

    /** True from the moment a pass is [submit]ted until [collect] picks it up; nothing is submitted meanwhile. */
    var isBusy = false
        private set

    private val requests = Channel<Unit>(capacity = 1)
    private val results = Channel<Boolean>(capacity = 1)
    private var isAbandoned = false

    /**
     * Starts the worker on [scope] (typically the Kubriko instance's own, so the worker ends with it), running passes
     * on [dispatcher]. No pass runs before this is called. A pass whose [run] throws is dropped like an [abandon]ed one,
     * frees this for the next, and its exception is rethrown on [scope], where any other failure of the instance goes;
     * the worker carries on.
     */
    fun start(scope: CoroutineScope, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        scope.launch(dispatcher) {
            for (request in requests) {
                val failure = try {
                    run()
                    null
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Throwable) {
                    exception
                }
                results.send(failure == null)
                if (failure != null) scope.launch { throw failure }
            }
        }
    }

    /**
     * Hands the pass the subclass has just captured to the worker. Ignored while [isBusy]: the subclass checks that
     * before capturing anything.
     */
    protected fun submit() {
        if (isBusy) return
        isAbandoned = false
        isBusy = true
        requests.trySend(Unit)
    }

    /**
     * Whether a finished pass is waiting, which also frees this for the next one. False while one is still running,
     * and false for one that was [abandon]ed or failed, whose result is dropped here.
     */
    fun collect(): Boolean {
        if (!isBusy) return false
        val hasSucceeded = results.tryReceive().getOrNull() ?: return false
        isBusy = false
        val wasAbandoned = isAbandoned
        isAbandoned = false
        return hasSucceeded && !wasAbandoned
    }

    /** Drops the pass in flight, if there is one: what it was started from no longer describes anything. */
    fun abandon() {
        if (isBusy) isAbandoned = true
    }

    /** The work itself, over what the subclass captured. Should yield between slices of it. */
    protected abstract suspend fun run()
}
