<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Pointer Input Plugin

The `pointer-input` plugin provides a unified way to handle touch, mouse, and other pointing device events in Kubriko. It supports tracking multiple pointers, hovering, dragging, and zooming gestures.

## Features

- **Multi-pointer Tracking**: Track the positions and states of multiple simultaneous touch points.
- **Hover Support**: Detect the position of a hovering pointer (e.g., a mouse cursor) even when not pressed.
- **Trait Integration**: Simple integration using the `PointerInputAware` trait for actors.
- **Gesture Support**: Built-in support for drag and zoom (pinch/scroll) gestures.
- **StateFlow API**: Access pointer positions as observable `StateFlow`s for reactive UI or logic.

## Usage

### 1. Register the Manager

Add the `PointerInputManager` to your `Kubriko` instance:

```kotlin
val kubriko = Kubriko.newInstance(
    PointerInputManager.newInstance(),
    // ... other managers
)
```

### 2. Use the PointerInputAware trait

Implement the `PointerInputAware` interface in your actor to react to specific events:

```kotlin
class ButtonActor : Actor(), PointerInputAware {

    override fun onPointerPressed(pointerId: PointerId, screenOffset: Offset) {
        // Handle press
    }

    override fun onPointerZoom(position: Offset, factor: Float) {
        // Handle zoom (e.g., scale the actor)
    }
}
```

### 3. Access Pointer State Manually

You can query the current state of all pointers directly from the manager:

```kotlin
val pointerManager = kubriko.get<PointerInputManager>()

// Observe all pressed pointers
val pressedPointers = pointerManager.pressedPointerPositions.value

// Check for hovering pointer
val hoveringPosition = pointerManager.hoveringPointerPosition.value
```

## Technical Details

### Platform Limitations
- **Multitouch**: Multitouch support is fully available on Android, iOS, and Web (Wasm). On Desktop (JVM) only a single pointer (the mouse) is handled, and touchscreens are not properly supported (see below).
- **Web**: touch pinch gestures are not reported as zoom yet (Compose Multiplatform issue CMP-6957); mouse-wheel zoom works.
- **Desktop touchscreens**: Compose Multiplatform has no touchscreen support on Desktop ([CMP-1609](https://youtrack.jetbrains.com/issue/CMP-1609/Desktop.-Support-touch-screens-natively), [CMP-2209](https://youtrack.jetbrains.com/issue/CMP-2209/Desktop.-Unable-to-zoom-using-detectTransformGestures-in-Modifier.pointerInput-using-touch-screens)), so a game cannot rely on one there. See [Touchscreens on Desktop](#touchscreens-on-desktop).
- **Pointer Movement**: The `tryToMoveHoveringPointer` function is only supported on Desktop and may require specific platform permissions. On desktop it needs the engine's `windowState` (`com.pandulapeter.kubriko.implementation.windowState`) to be set, `windowState = rememberWindowState(...)`; while it is unset the function returns `false`.

### Touchscreens on Desktop

On Windows and Linux (X11) the JetBrains Runtime, which Compose Desktop apps commonly ship with (the Showcase does), takes the touchscreen's input itself and never reports it to Compose as touches. Compose only sees the mouse events the runtime makes of it, so the plugin receives exactly that:

- **A tap becomes a mouse click.** The runtime clicks the mouse at the finger's position once it lifts, so a tap arrives as `onPointerPressed` immediately followed by `onPointerReleased`, with the mouse's pointer id. No press is reported while the finger is down, so a press cannot be held, and `pressedPointerPositions` and `handleActivePointers` see the tap for a single tick at most.
- **A drag becomes mouse wheel scrolling.** The finger's movement arrives as wheel notches, which the plugin reports as zoom (`onPointerZoom`, the same way it treats a mouse wheel) rather than as `onPointerDrag`, with no press, move, or release around it.
- **Only one finger is reported.** The runtime passes on the primary finger alone, so there are no simultaneous pointers and no pinch gestures.
- **macOS has no touchscreens** to begin with; a trackpad pinch does not reach Compose as a gesture either.
- **Other runtimes** leave the touchscreen to the operating system's own mouse emulation instead, which has not been verified with this plugin.

These come from Compose and the runtime, so the plugin cannot work around them. An app can intercept the runtime's events itself and send them to its window's Compose scene as touch pointers (the Showcase app does this for its menus in `app/desktop/.../TouchScreen.kt`, through reflection into Compose internals). That makes ordinary Compose lists and gestures work, but not this plugin: on Desktop it only handles the mouse's pointer (id `0`) and drops every other pointer, touches included. A game that has to be playable on a touchscreen PC should therefore keep a mouse or keyboard path for anything beyond tapping.

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:plugin-pointer-input`
