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
- **Multitouch**: Currently, multitouch support is fully available on Android and iOS. On Desktop (JVM) and Web platforms, only a single pointer (the mouse) is typically supported, though some touch-enabled hardware may vary.
- **Pointer Movement**: The `tryToMoveHoveringPointer` function is only supported on Desktop and may require specific platform permissions.

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:plugin-pointer-input`
