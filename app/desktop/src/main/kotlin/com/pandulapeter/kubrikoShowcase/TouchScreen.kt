/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeScenePointer
import java.awt.AWTEvent
import java.awt.EventQueue
import java.awt.Toolkit
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap
import javax.swing.SwingUtilities
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Makes a finger on a touchscreen a touch to Compose rather than a mouse wheel, so that the menu's lists are dragged and
 * flung the way they are on a phone.
 *
 * The JetBrains Runtime takes a touchscreen's input on Windows and X11 itself and hands it on as mouse wheel events
 * of three scroll types of its own (`sun.awt.event.TouchEvent`): one where the finger comes down, one for every move
 * past a small radius, carrying how far it moved in the wheel rotation, and one where it lifts — followed, for a
 * finger that never left the radius, by a mouse click at that point. Compose Desktop reads the moves as wheel notches,
 * which on Windows are a twentieth of the scrolled area each and animated, so a finger that moves a hundred pixels
 * scrolls five screens, lifting it flings nothing, and nothing can be dragged at all. Every event of those scroll types
 * still carries where the finger is, which is all a touch pointer needs: an event queue takes them before Compose sees
 * them and sends the window's scene a press, the moves, and a release of a touch pointer instead, dropping the click
 * that follows a tap, which the press and the release already were.
 *
 * Compose Desktop has no public way to the scene a window renders, so it is found among the window's own objects by
 * its type (see [findComposeScene]). A Compose version that keeps it elsewhere, or keeps another scene there, leaves
 * the events to Compose, which is the behavior of a window without this.
 *
 * @return What stops translating the events.
 */
internal fun ComposeWindow.translateTouchScreenInput(): () -> Unit {
    val translator = TouchScreenTranslator(this)
    TouchScreenEventQueue.add(translator)
    return { TouchScreenEventQueue.remove(translator) }
}

/**
 * One queue for every window, pushed once and never popped: [EventQueue.pop] removes the queue on top, whoever pushed
 * it, so a window popping its own after the next window has pushed one (the Windows fullscreen toggle recreates the
 * window) would take the new window's instead. Only the event dispatch thread touches it.
 */
private object TouchScreenEventQueue : EventQueue() {
    private val translators = mutableListOf<TouchScreenTranslator>()
    private var isPushed = false

    fun add(translator: TouchScreenTranslator) {
        translators += translator
        if (!isPushed) {
            Toolkit.getDefaultToolkit().systemEventQueue.push(this)
            isPushed = true
        }
    }

    fun remove(translator: TouchScreenTranslator) {
        translators -= translator
    }

    override fun dispatchEvent(event: AWTEvent) {
        if (event is MouseEvent) {
            for (translator in translators) {
                if (translator.isInWindow(event)) {
                    if (translator.translate(event)) return
                    break
                }
            }
        }
        super.dispatchEvent(event)
    }
}

@OptIn(InternalComposeUiApi::class)
private class TouchScreenTranslator(private val window: ComposeWindow) {
    private var scene: ComposeScene? = null
    private var isSceneSearched = false
    private var pointerId = TOUCH_POINTER_ID_BASE
    private var lastPosition: Offset? = null
    private var hasMoved = false
    private var isSkippingTapClick = false

    fun isInWindow(event: MouseEvent) = event.component?.let { it === window || SwingUtilities.getWindowAncestor(it) === window } == true

    /** @return Whether the event was taken, so Compose must not see it. */
    fun translate(event: MouseEvent): Boolean {
        if (event is MouseWheelEvent && event.scrollType in TOUCH_BEGIN..TOUCH_END) {
            isSkippingTapClick = false
            // AWT can still deliver events to a window that has been disposed, whose scene is closed.
            if (!window.isDisplayable) return false
            val scene = scene() ?: return false
            val position = event.positionInScene()
            when (event.scrollType) {
                TOUCH_BEGIN -> {
                    // X11 gives a touch up after a second without an end, starting the next one without it.
                    lastPosition?.let { scene.sendTouch(PointerEventType.Release, it, isPressed = false, timeMillis = event.`when`) }
                    pointerId++
                    hasMoved = false
                    lastPosition = position
                    scene.sendTouch(PointerEventType.Press, position, isPressed = true, timeMillis = event.`when`)
                }
                TOUCH_UPDATE -> {
                    // A diagonal move comes as two events at the same point, one for each axis.
                    if (lastPosition != null && position != lastPosition) {
                        hasMoved = true
                        lastPosition = position
                        scene.sendTouch(PointerEventType.Move, position, isPressed = true, timeMillis = event.`when`)
                    }
                }
                TOUCH_END -> if (lastPosition != null) {
                    lastPosition = null
                    scene.sendTouch(PointerEventType.Release, position, isPressed = false, timeMillis = event.`when`)
                    isSkippingTapClick = !hasMoved
                }
            }
            return true
        }
        // The runtime marks the click it makes of a tap on Windows only, and not where X11 is concerned, so it is
        // recognized by coming right after a touch that never moved: a move, on X11 a drag, the press, the release,
        // and the click, posted together.
        if (isSkippingTapClick) {
            when (event.id) {
                MouseEvent.MOUSE_MOVED, MouseEvent.MOUSE_DRAGGED, MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED -> return true
                MouseEvent.MOUSE_CLICKED -> {
                    isSkippingTapClick = false
                    return true
                }
                else -> isSkippingTapClick = false
            }
        }
        return false
    }

    /**
     * Looked for once: a window whose scene is not found, or not trusted, is left to Compose for good. Reflection that
     * fails in a way the search did not foresee (a field whose type cannot be loaded, a module that refuses access)
     * counts as not finding it, rather than breaking the window's input.
     */
    private fun scene(): ComposeScene? {
        if (!isSceneSearched) {
            isSceneSearched = true
            scene = try {
                window.findComposeScene()?.takeIf { it.fillsContentPane() }
            } catch (_: LinkageError) {
                null
            } catch (_: RuntimeException) {
                null
            }
            if (scene == null) println("Touchscreen input is left to Compose: the window's scene was not found")
        }
        return scene
    }

    /**
     * Whether the scene found is the one the window's content is drawn in, rather than one of a popup or a dialog that
     * a later Compose version might keep closer to the window, since touches sent there would land elsewhere.
     */
    private fun ComposeScene.fillsContentPane(): Boolean {
        val size = size ?: return false
        val scale = window.graphicsConfiguration.defaultTransform.scaleX
        val contentPane = window.contentPane
        return abs(size.width - (contentPane.width * scale).roundToInt()) <= SCENE_SIZE_TOLERANCE &&
            abs(size.height - (contentPane.height * scale).roundToInt()) <= SCENE_SIZE_TOLERANCE
    }

    /** Where Compose's own mouse handling puts the event: in pixels, from the corner of the window's content. */
    private fun MouseEvent.positionInScene(): Offset {
        val point = SwingUtilities.convertPoint(component, point, window.contentPane)
        val scale = window.graphicsConfiguration.defaultTransform.scaleX.toFloat()
        return Offset(point.x * scale, point.y * scale)
    }

    private fun ComposeScene.sendTouch(eventType: PointerEventType, position: Offset, isPressed: Boolean, timeMillis: Long) {
        sendPointerEvent(
            eventType = eventType,
            pointers = listOf(
                ComposeScenePointer(
                    id = PointerId(pointerId),
                    position = position,
                    pressed = isPressed,
                    type = PointerType.Touch,
                ),
            ),
            buttons = PointerButtons(isPrimaryPressed = isPressed),
            timeMillis = timeMillis,
        )
    }
}

/**
 * Walks the window's fields, and theirs, for the [ComposeScene] its content is in: a few levels down, behind the
 * window's panel, its container, and the mediator between that and the scene, none of them public. Only Compose's own
 * objects are followed, a lazy value only once it has been made, and the walk is by type rather than by the fields'
 * names, which are private and change between versions.
 */
@OptIn(InternalComposeUiApi::class)
private fun ComposeWindow.findComposeScene(): ComposeScene? {
    val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
    var level = listOf<Any>(this)
    repeat(SCENE_SEARCH_DEPTH) {
        level = level.flatMap { it.composeFieldValues() }.filter(visited::add)
        level.firstNotNullOfOrNull { it as? ComposeScene }?.let { return it }
    }
    return null
}

private fun Any.composeFieldValues(): List<Any> = generateSequence(javaClass) { it.superclass }
    .takeWhile { it.name.startsWith(COMPOSE_PACKAGE) }
    .flatMap { it.declaredFields.asSequence() }
    .filter { !Modifier.isStatic(it.modifiers) && !it.type.isPrimitive }
    .mapNotNull { field ->
        try {
            field.isAccessible = true
            when (val value = field.get(this)) {
                is Lazy<*> -> if (value.isInitialized()) value.value else null
                else -> value
            }
        } catch (_: Exception) {
            null
        }
    }
    .filter { it.javaClass.name.startsWith(COMPOSE_PACKAGE) }
    .toList()

/** The scroll types `sun.awt.event.TouchEvent` declares, which the JDK itself never uses. */
private const val TOUCH_BEGIN = 2
private const val TOUCH_UPDATE = 3
private const val TOUCH_END = 4

/** Far from the ids Compose gives the mouse, so that a touch is never taken for the mouse's pointer. */
private const val TOUCH_POINTER_ID_BASE = 1L shl 32

private const val SCENE_SEARCH_DEPTH = 6
private const val SCENE_SIZE_TOLERANCE = 2
private const val COMPOSE_PACKAGE = "androidx.compose."
