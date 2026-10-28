/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.creatorSession.protocol.SessionCommand
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.SkiaLayer
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Container
import java.awt.Desktop
import java.awt.EventQueue
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.KeyEvent
import java.util.Base64
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import java.util.concurrent.Executors

/**
 * Carries out the [SessionCommand]s Kubriko Creator sends while the game runs, one at a time and in order, answering
 * each through [send].
 *
 * Key presses go straight to the AWT event listeners, which is where Kubriko's keyboard input reads them, so they
 * reach the game whichever window has the keyboard focus. Screenshots come from the game's own rendering, so they
 * need no screen recording permission.
 */
internal class PlayCommandHandler(
    private val send: (SessionMessage) -> Unit,
    private val findWindow: () -> Window? = ::findGameWindow,
    private val sleep: (Long) -> Unit = Thread::sleep,
    private val hasActorsInView: () -> Boolean? = ::hasEntryPointsActorsInView,
) {
    private val worker = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "kubriko-creator-commands").apply { isDaemon = true } }

    fun handle(command: SessionCommand) {
        worker.execute { send(carryOut(command)) }
    }

    internal fun carryOut(command: SessionCommand): SessionMessage = try {
        when (command) {
            is SessionCommand.BringToFront -> {
                val window = requireWindow()
                onEventThread { bringToFront(window) }
                awaitFocus { window.isFocused }
                SessionMessage.CommandDone(command.id)
            }
            is SessionCommand.PressKeys -> {
                val keyCodes = command.keys.map { name -> keyCodeOf(name) ?: throw CommandException("There's no key called $name.") }
                val window = requireWindow()
                if (!awaitFocus { window.isFocused }) throw CommandException(
                    "The game's window didn't get the keyboard focus, so no keys were pressed. Another app may have taken it; try again.",
                )
                pressKeys(window, keyCodes, command.durationInMilliseconds)
                SessionMessage.CommandDone(command.id)
            }
            is SessionCommand.CaptureScreen -> {
                val window = requireWindow()
                SessionMessage.Screenshot(
                    id = command.id,
                    png = Base64.getEncoder().encodeToString(onEventThread { capture(window, command.maximumWidth) }),
                )
            }
            is SessionCommand.AwaitActorsDrawn -> {
                val isDrawing = hasActorsInView() ?: throw CommandException("The game's CreatorEntryPoint doesn't report its Kubriko instances.")
                if (!isDrawing && poll(command.timeoutInMilliseconds) { true.takeIf { hasActorsInView() == true } } == null) throw CommandException(
                    "The game drew no Actors within ${command.timeoutInMilliseconds} ms.",
                )
                // Actors are culled into view on a tick, and drawn on a frame after it.
                sleep(DRAW_SETTLE_TIME_IN_MILLISECONDS)
                SessionMessage.CommandDone(command.id)
            }
        }
    } catch (exception: CommandException) {
        SessionMessage.CommandFailed(command.id, exception.message.orEmpty())
    }

    /**
     * Presses [keyCodes] together on [source], holds them for [durationInMilliseconds], and releases them in reverse.
     */
    internal fun pressKeys(source: Component, keyCodes: List<Int>, durationInMilliseconds: Long) {
        keyCodes.forEach { onEventThread { dispatchKey(source, KeyEvent.KEY_PRESSED, it) } }
        sleep(durationInMilliseconds.coerceAtLeast(0))
        keyCodes.asReversed().forEach { onEventThread { dispatchKey(source, KeyEvent.KEY_RELEASED, it) } }
    }

    /**
     * Waits up to [FOCUS_TIMEOUT_IN_MILLISECONDS] for [isFocused], since focus arrives some time after it's requested and
     * Kubriko ignores keys while the window isn't focused.
     *
     * @return Whether the window has the focus.
     */
    internal fun awaitFocus(isFocused: () -> Boolean): Boolean {
        if (isFocused()) return true
        if (poll(FOCUS_TIMEOUT_IN_MILLISECONDS) { true.takeIf { isFocused() } } == null) return false
        // AWT knows about the focus a frame or two before Compose passes it on to Kubriko's StateManager.
        sleep(FOCUS_SETTLE_TIME_IN_MILLISECONDS)
        return true
    }

    /**
     * What [find] finds, checking again every [POLL_INTERVAL_IN_MILLISECONDS] for up to [timeoutInMilliseconds], or null.
     */
    internal fun <T : Any> poll(timeoutInMilliseconds: Long, find: () -> T?): T? {
        repeat((timeoutInMilliseconds / POLL_INTERVAL_IN_MILLISECONDS).toInt()) {
            sleep(POLL_INTERVAL_IN_MILLISECONDS)
            find()?.let { return it }
        }
        return null
    }

    /**
     * The game's window, waiting up to [WINDOW_TIMEOUT_IN_MILLISECONDS] for it, since a session reports that it started
     * before the game has opened it.
     */
    private fun requireWindow() = findWindow() ?: poll(WINDOW_TIMEOUT_IN_MILLISECONDS, findWindow) ?: throw CommandException("The game has no window yet.")

    private fun bringToFront(window: Window) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) {
            Desktop.getDesktop().requestForeground(true)
        }
        window.toFront()
        window.requestFocus()
    }

    private fun dispatchKey(source: Component, id: Int, keyCode: Int) {
        val event = KeyEvent(source, id, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED)
        Toolkit.getDefaultToolkit().getAWTEventListeners(AWTEvent.KEY_EVENT_MASK).forEach { it.eventDispatched(event) }
    }

    private fun capture(window: Window, maximumWidth: Int): ByteArray {
        val layer = window.descendants().filterIsInstance<SkiaLayer>().maxByOrNull { it.width * it.height }
            ?: throw CommandException("The game's window has nothing to capture.")
        val image = Image.makeFromBitmap(layer.screenshot() ?: throw CommandException("The game hasn't drawn anything yet."))
        val scale = (maximumWidth.toFloat() / image.width).coerceAtMost(1f)
        val scaled = if (scale < 1f) {
            val width = (image.width * scale).toInt().coerceAtLeast(1)
            val height = (image.height * scale).toInt().coerceAtLeast(1)
            Surface.makeRasterN32Premul(width, height).run {
                canvas.drawImageRect(image, Rect.makeWH(width.toFloat(), height.toFloat()))
                makeImageSnapshot()
            }
        } else {
            image
        }
        return scaled.encodeToData(EncodedImageFormat.PNG)?.bytes ?: throw CommandException("The screenshot couldn't be encoded.")
    }

    private fun <T> onEventThread(action: () -> T): T {
        if (EventQueue.isDispatchThread()) return action()
        var result: Result<T>? = null
        EventQueue.invokeAndWait { result = runCatching(action) }
        return checkNotNull(result).getOrThrow()
    }

    private class CommandException(message: String) : Exception(message)

    companion object {
        private const val WINDOW_TIMEOUT_IN_MILLISECONDS = 5000L
        private const val FOCUS_TIMEOUT_IN_MILLISECONDS = 2000L
        private const val POLL_INTERVAL_IN_MILLISECONDS = 20L
        private const val FOCUS_SETTLE_TIME_IN_MILLISECONDS = 100L
        private const val DRAW_SETTLE_TIME_IN_MILLISECONDS = 100L

        /**
         * The code of the `java.awt.event.KeyEvent` key called `VK_[name]`, or null if there is none.
         */
        internal fun keyCodeOf(name: String) = try {
            KeyEvent::class.java.getField("VK_${name.uppercase()}").getInt(null)
        } catch (_: NoSuchFieldException) {
            null
        }

        private val entryPoint by lazy {
            try {
                ServiceLoader.load(CreatorEntryPoint::class.java).toList().singleOrNull()
            } catch (_: ServiceConfigurationError) {
                null
            }
        }

        /**
         * Whether a Kubriko instance the game's [CreatorEntryPoint] reports has Actors within its viewport, or null if it
         * reports none.
         */
        private fun hasEntryPointsActorsInView() = entryPoint?.getRunningInstances()?.any { it.hasActorsInView() }

        private fun Kubriko.hasActorsInView() = try {
            get<ActorManager>().visibleActorsWithinViewport.value.isNotEmpty()
        } catch (_: IllegalStateException) {
            false
        }

        private fun findGameWindow() = Window.getWindows().filter { it.isShowing }.maxByOrNull { it.width * it.height }

        private fun Component.descendants(): Sequence<Component> = sequence {
            yield(this@descendants)
            if (this@descendants is Container) components.forEach { yieldAll(it.descendants()) }
        }
    }
}
