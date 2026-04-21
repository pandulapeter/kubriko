# Debug Menu Tool

The `debug-menu` tool provides an in-game overlay for inspecting and manipulating the state of a Kubriko project at runtime. It features a log viewer, actor inspector, performance metrics, and plugin-specific controls.

## Warning
The current implementation of the debug menu does not yet live up to the quality standards of the engine.
There are plans to completely rewrite this tool from scratch.

## Features

- **Real-time Logs**: View all messages recorded via the `logger` tool.
- **Actor Inspector**: Browse all active actors in the scene and modify their exposed properties.
- **Performance Monitoring**: Track frame rates and engine update times.
- **Adaptive Layout**: Automatically switches between vertical and horizontal layouts based on the screen's aspect ratio.
- **Customizable Theme**: Supports dark/light modes and custom Material3 themes.
- **Persistence**: Remembers the menu's state (visibility, scroll positions) across app restarts.

## Usage

### 1. Register the Menu

Wrap your `KubrikoViewport` with the `DebugMenu` composable:

```kotlin
DebugMenu(
    kubriko = myKubrikoInstance,
    isEnabled = true, // Use a build flag here (e.g., BuildConfig.DEBUG)
) {
    KubrikoViewport(kubriko = myKubrikoInstance)
}
```

### 2. Manual Control

You can also toggle the menu's visibility programmatically:

```kotlin
DebugMenu.toggleVisibility()
```

### 3. Standalone Components

If you need more control over the layout, you can use the `Vertical` or `Horizontal` components directly:

```kotlin
DebugMenu(
    kubriko = myKubrikoInstance,
    isEnabled = true, // Use a build flag here (e.g., BuildConfig.DEBUG)
) {
    Row {
        KubrikoViewport(kubriko = myKubrikoInstance)
        DebugMenu.Vertical(
            kubriko = myKubrikoInstance,
            isEnabled = true, // Same flag as above
            windowInsets = WindowInsets.safeDrawing,
        )
    }
}
```

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:tool-debug-menu`
