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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource

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
    private lateinit var _scope: CoroutineScope
    private lateinit var tickCallback: (Int) -> Unit

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
            log(
                message = "Initializing...",
                importance = Logger.Importance.LOW,
            )
            _scope = kubriko as CoroutineScope
            val kubrikoImpl = kubriko as? KubrikoImpl
                ?: throw IllegalStateException("Custom Kubriko implementations are not supported. Use Kubriko.newInstance() to instantiate Kubriko.")
            tickCallback = kubrikoImpl::onTick
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

    internal fun onDisposeInternal() {
        if (isInitialized) {
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
     */
    protected fun emitTick(deltaTimeInMilliseconds: Int) {
        if (!isInitialized) return
        tickCallback(deltaTimeInMilliseconds)
    }

    /**
     * Logs a message with the TickSource's source information.
     */
    protected fun log(
        message: String,
        details: String? = null,
        importance: Logger.Importance = Logger.Importance.HIGH,
    ) {
        val className = this::class.simpleName
        val instanceName = toString().substringAfterLast('@')
        Logger.log(
            message = message,
            details = details,
            source = "$className@$instanceName",
            importance = importance,
        )
    }

    companion object {
        /**
         * Creates a [ManualTickSource] for deterministic tests, editors, and replay systems.
         */
        fun manual() = ManualTickSource()

        /**
         * Creates the default viewport-frame based [TickSource].
         */
        fun viewportFrames(): TickSource = ViewportFrameTickSource()

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

/**
 * A [TickSource] that advances only when [tick] is called.
 */
class ManualTickSource : TickSource() {

    /**
     * Emits one engine tick with the provided delta time.
     */
    fun tick(deltaTimeInMilliseconds: Int) = emitTick(deltaTimeInMilliseconds)
}

internal class ViewportFrameTickSource : TickSource() {

    internal fun tick(deltaTimeInMilliseconds: Int) = emitTick(deltaTimeInMilliseconds)
}

internal class FixedRateTickSource(
    private val intervalInMilliseconds: Long,
) : TickSource() {
    private var job: Job? = null

    init {
        require(intervalInMilliseconds > 0L) { "intervalInMilliseconds must be greater than 0." }
    }

    override fun onInitialize(kubriko: Kubriko) {
        job = scope.launch {
            emitTick(0)
            while (isActive) {
                delay(intervalInMilliseconds)
                emitTick(intervalInMilliseconds.toInt())
            }
        }
    }

    override fun onDispose() {
        job?.cancel()
        job = null
    }
}

internal class FixedFrequencyTickSource(
    private val ticksPerSecond: Int,
) : TickSource() {
    private val targetInterval = (1_000_000_000L / ticksPerSecond).nanoseconds
    private var job: Job? = null

    init {
        require(ticksPerSecond > 0) { "ticksPerSecond must be greater than 0." }
    }

    override fun onInitialize(kubriko: Kubriko) {
        job = scope.launch {
            emitTick(0)
            var lastTickTime = TimeSource.Monotonic.markNow()
            var nextTickStart = lastTickTime
            while (isActive) {
                val remainingTime = targetInterval - nextTickStart.elapsedNow()
                if (remainingTime.isPositive()) {
                    delay(remainingTime.inWholeMilliseconds.coerceAtLeast(1L))
                }
                val currentTime = TimeSource.Monotonic.markNow()
                emitTick(lastTickTime.elapsedNow().inWholeMilliseconds.toInt())
                lastTickTime = currentTime
                nextTickStart += targetInterval
                if ((targetInterval - nextTickStart.elapsedNow()).isNegative()) {
                    nextTickStart = currentTime
                }
            }
        }
    }

    override fun onDispose() {
        job?.cancel()
        job = null
    }
}
