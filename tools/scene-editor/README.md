<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
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

To make an actor appear in the Scene Editor, it must implement the `Editable<T>` interface (which also requires `Serializable<T>` and `Positionable`) and annotate the property setters you want to tune with `@Exposed`:

```kotlin
class PlayerActor private constructor(state: State) : Visible, Editable<PlayerActor> {

    @set:Exposed
    var speed: Float = state.speed

    @set:Exposed(name = "Movement direction")
    var direction: AngleRadians = state.direction

    // ...
}
```

`@Exposed` displays the property's own name by default; pass `name = "..."` only when you want a different label.

### 4. Registering Editable Actors

The editor needs an `EditableMetadata` entry per actor type, bundled into a `SerializationManager`. Use `EditableMetadata.create<Actor, Actor.State>` to derive the deserialization logic from the reified `State` type — you only need to supply how to instantiate a fresh actor at a placement position:

```kotlin
val serializationManager = EditableMetadata.newSerializationManagerInstance(
    EditableMetadata.create<PlayerActor, PlayerActor.State> { position ->
        PlayerActor.State(body = BoxBody(initialPosition = position))
    },
    // ...further actor types
)
```

`typeId` defaults to the actor's simple class name. Because the `typeId` is written into saved scene files, pass an explicit `typeId = "..."` if you want it to stay stable across class renames.

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:tool-scene-editor`
