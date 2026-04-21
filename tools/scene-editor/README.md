# Scene Editor Tool

The `scene-editor` tool is a powerful Desktop application for arranging and customizing `Editable` actors in a Kubriko scene. It provides a visual interface for property tuning, actor placement, and scene persistence.

## Features

- **Visual Scene Tree**: Browse and manage the hierarchy of actors in your scene.
- **Property Inspector**: Modify properties of `Editable` actors at runtime using the `@Exposed` annotation.
- **Transform Tools**: Precisely position, rotate, and scale actors in the 3D or 2D space.
- **Serialization**: Save and load scene states to/from JSON files using the `SerializationManager`.
- **Custom Managers Support**: Integrate with custom game logic by providing a list of `Manager` instances.
- **Adaptive Layout**: Optimized for desktop workflows with resizable panels and sidebars.

## Usage

### 1. Launch the Editor

The Scene Editor is designed to run as a standalone window on Desktop:

```kotlin
SceneEditor.show(
    defaultSceneFilename = "main_scene.json",
    defaultSceneFolderPath = "assets/scenes",
    serializationManager = mySerializationManager,
    title = "My Kubriko Project - Scene Editor"
)
```

### 2. Embed as a Composable

Alternatively, you can embed the Scene Editor into an existing Compose for Desktop window:

```kotlin
SceneEditor(
    defaultSceneFilename = "level_1.json",
    defaultSceneFolderPath = "data/levels",
    serializationManager = mySerializationManager,
    onCloseRequest = { /* Handle exit */ }
)
```

### 3. Making Actors Editable

To make an actor appear in the Scene Editor, it must implement the `Editable` interface and use the `@Exposed` annotation on its property setters:

```kotlin
class PlayerActor : Editable {
    var speed: Float = 5f
        @Exposed(name = "Movement Speed") set

    // ...
}
```

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:tool-scene-editor`
