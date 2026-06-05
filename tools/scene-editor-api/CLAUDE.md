<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# scene-editor-api

Public API surface for the Scene Editor tool. Consumers depend on this module (and the `-noop` or real `scene-editor` swap) — never on `scene-editor` directly.

## Key types

### `Editable<T>`
```kotlin
interface Editable<T : Editable<T>> : Serializable<T>, Positionable
```
An Actor that can be manipulated in the editor. Requirements:
- Must implement `Serializable<T>` (save/restore state as a string) — see `plugin-serialization`.
- Must implement `Positionable` (has a `PointBody`) so the editor can move it.
- `Serializable.State<T>` must be provided via `EditableMetadata`.

### `@Exposed`
```kotlin
@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class Exposed(val name: String)
```
Apply to **property setters** of an `Editable` actor to surface them in the property inspector panel. The editor discovers them at runtime via Kotlin reflection (`KMutableProperty.setter.findAnnotation<Exposed>()`).

Supported setter types (anything else is silently ignored):
- `Boolean` — rendered as a checkbox
- `Float` — rendered as a number input
- `Int` — rendered as a number input
- `String?` — rendered as a text input
- `SceneUnit` — rendered as a number input
- `SceneOffset` — rendered as two number inputs (x, y)
- `Scale` — rendered as two number inputs (horizontal, vertical)
- `AngleRadians` / `AngleDegrees` — rendered as a rotation editor (wheel or numeric, user-configurable)
- `Color` — rendered as a color picker (RGB sliders or hex, user-configurable)

### `EditableMetadata<T>`
Extends `SerializableMetadata<T>` with an `instantiate: (SceneOffset) -> Serializable.State<T>` lambda. This lets the editor create a new default instance at a given scene position when the user places an actor.

Use the companion `invoke` operator for idiomatic construction:
```kotlin
EditableMetadata(
    typeId = "MyActor",
    deserializeState = { MyActor.State.deserialize(it) },
    instantiate = { position -> MyActor.State(position = position) },
)
```

Pass multiple `EditableMetadata` instances to `EditableMetadata.newSerializationManagerInstance(...)` to build the `SerializationManager` required by the editor.

### `SceneEditorContract` (Desktop only)
Interface implemented by both the real `SceneEditor` object and the noop. Two entry points:
- `show(...)` — launches a standalone Compose Desktop `application { }` window (blocking call; use for dedicated editor entry points).
- `operator fun invoke(...)` — embeds the editor as a Composable inside an existing window.

Key parameters shared by both:
- `defaultSceneFolderPath` — default `"./src/commonMain/composeResources/files/scenes"`.
- `serializationManager` — must be built from `EditableMetadata.newSerializationManagerInstance(...)`.
- `customManagers` — additional Managers injected into the editor's internal Kubriko instance (e.g. for actors that need plugin managers).

### `SceneEditorMode` (Desktop only)
- `Normal` — editor manages its own scene state (load/save from disk).
- `Connected(sceneJson, onSceneJsonChanged)` — editor mirrors an external JSON string; changes are pushed back via the callback. Used for live in-app editing where the game and editor share scene state.

## Constant
`IS_SCENE_EDITOR_AVAILABLE` — `false` in the `-noop` module, not defined in `-api` (check the noop for this guard). The real `scene-editor` module does not define this constant.
