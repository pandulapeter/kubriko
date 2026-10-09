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

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Produces update ticks for the Kubriko engine.
 *
 * Override this class to provide a custom timing source.
 */
abstract class TickSource {
    /**
     * Whether the [TickSource] has been initialized.
     */
    protected var isInitialized = false
        private set

    private val _isRunning = MutableStateFlow(false)

    /**
     * Whether the [TickSource] is currently emitting ticks.
     */
    protected val isRunning: Boolean
        get() = _isRunning.value
    internal val isRunningInternal: StateFlow<Boolean> get() = _isRunning
    @OptIn(ExperimentalAtomicApi::class)
    private val isApplyingTransition = AtomicBoolean(false)
    @Volatile
    private var isStartApplied = false
    private lateinit var _scope: CoroutineScope
    private lateinit var kubrikoImpl: KubrikoImpl

    /**
     * The [CoroutineScope] of the [Kubriko] instance this [TickSource] is attached to.
     * Only available after [onInitialize] has been called.
     * Lives until the Kubriko instance is disposed.
     */
    protected val scope
        get() = try {
            _scope
        } catch (_: RuntimeException) {
            throw IllegalStateException("Cannot use the scope of ${this::class.simpleName} until the TickSource has been initialized.")
        }

    internal fun initializeInternal(kubriko: Kubriko) {
        if (!isInitialized) {
            _scope = kubriko as CoroutineScope
            val kubrikoImpl = kubriko as? KubrikoImpl
                ?: throw IllegalStateException("Custom Kubriko implementations are not supported. Use Kubriko.newInstance() to instantiate Kubriko.")
            this.kubrikoImpl = kubrikoImpl
            isInitialized = true
            onInitialize(kubriko)
            log(
                message = "Initialized.",
                importance = Logger.Importance.MEDIUM,
            )
        }
    }

    /**
     * Called when the [TickSource] is being initialized.
     *
     * @param kubriko The [Kubriko] instance this [TickSource] is attached to.
     */
    protected open fun onInitialize(kubriko: Kubriko) = Unit

    /**
     * Initializes the attached [Kubriko] instance and starts this [TickSource].
     * Applies every actor operation queued before the first start before returning.
     *
     * Calling this function multiple times is safe. Safe to call from any thread; concurrent calls are applied in order
     * and [onStart]/[onStop] never overlap.
     */
    fun start() {
        kubrikoImpl.initializeInternal()
        if (_isRunning.compareAndSet(expect = false, update = true)) {
            log(
                message = "Starting...",
                importance = Logger.Importance.LOW,
            )
            applyRequestedState()
        }
    }

    /**
     * Stops this [TickSource] without disposing the attached [Kubriko] instance.
     *
     * Calling this function multiple times is safe. Safe to call from any thread; concurrent calls are applied in order
     * and [onStart]/[onStop] never overlap.
     */
    fun stop() {
        if (_isRunning.compareAndSet(expect = true, update = false)) {
            log(
                message = "Stopping...",
                importance = Logger.Importance.LOW,
            )
            applyRequestedState()
        }
    }

    /**
     * Brings the applied state in line with the requested one without blocking: whoever claims the transition flag
     * applies every pending transition, and the others leave theirs to it. After releasing the flag the claimer checks
     * once more, so a request that arrived while it was releasing is not lost.
     */
    @OptIn(ExperimentalAtomicApi::class)
    private fun applyRequestedState() {
        while (isApplyingTransition.compareAndSet(expectedValue = false, newValue = true)) {
            try {
                while (isStartApplied != _isRunning.value) {
                    if (_isRunning.value) {
                        isStartApplied = true
                        onStart()
                        log(
                            message = "Started.",
                            importance = Logger.Importance.MEDIUM,
                        )
                    } else {
                        isStartApplied = false
                        onStop()
                        log(
                            message = "Stopped.",
                            importance = Logger.Importance.MEDIUM,
                        )
                    }
                }
            } finally {
                isApplyingTransition.store(false)
            }
            if (isStartApplied == _isRunning.value) return
        }
    }

    /**
     * Called when this [TickSource] starts. Never called concurrently with itself or with [onStop].
     */
    protected open fun onStart() = Unit

    /**
     * Called when this [TickSource] stops. Never called concurrently with itself or with [onStart].
     */
    protected open fun onStop() = Unit

    internal fun onDisposeInternal() {
        if (isInitialized) {
            stop()
            log(
                message = "Disposing...",
                importance = Logger.Importance.LOW,
            )
            isInitialized = false
            onDispose()
            log(
                message = "Disposed.",
                importance = Logger.Importance.MEDIUM,
            )
        }
    }

    /**
     * Called when the [TickSource] is being disposed.
     * Should not be called manually, the dispose() function of Kubriko triggers the disposal of the [TickSource].
     * Use this to clean up any resources or subscriptions.
     */
    protected open fun onDispose() = Unit

    /**
     * Emits one engine tick.
     *
     * Must not be called concurrently with itself: ticks drive non-thread-safe engine state. A source should emit from
     * one thread or coroutine at a time.
     */
    protected fun emitTick(deltaTimeInMilliseconds: Int) {
        if (!isRunning) return
        kubrikoImpl.onTick(deltaTimeInMilliseconds)
    }

    /**
     * Logs a message with the TickSource's source information.
     */
    protected fun log(
        message: String,
        details: String? = null,
        importance: Logger.Importance = Logger.Importance.HIGH,
    ) {
        if (kubrikoImpl.isLoggingEnabled) {
            Logger.log(
                message = "TickSource: $message",
                details = details,
                source = kubrikoImpl.instanceNameForLogging,
                importance = importance,
            )
        }
    }

    companion object {
        /**
         * Creates a [ManualTickSource] for deterministic tests, editors, and replay systems.
         */
        fun manual() = ManualTickSource()

        /**
         * Creates the default viewport-frame based [TickSource].
         *
         * @param shouldPauseOnFocusLoss when true, ticks will only be emitted when the window is focused.
         *
         * A gap of more than two seconds between display frames (the app was in the background) restarts the timeline
         * instead of being emitted as one delta; that time is not added to `MetadataManager.totalRuntimeInMilliseconds`
         * either.
         */
        fun viewportFrames(
            shouldPauseOnFocusLoss: Boolean = true,
        ): TickSource = ViewportFrameTickSource(
            shouldPauseOnFocusLoss = shouldPauseOnFocusLoss,
        )

        /**
         * Creates a fixed-rate [TickSource] that can run without a mounted viewport.
         */
        fun fixedRate(
            intervalInMilliseconds: Long,
        ): TickSource = FixedRateTickSource(intervalInMilliseconds)

        /**
         * Creates a [TickSource] that tries to achieve the provided target number of ticks per second.
         */
        fun fixedFrequency(
            ticksPerSecond: Int,
        ): TickSource = FixedFrequencyTickSource(ticksPerSecond)
    }
}

