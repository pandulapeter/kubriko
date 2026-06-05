<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# plugin-serialization internals

## Wire format
The outer format is a **kotlinx.serialization JSON array** of `ActorStateWrapper` objects:
```json
[{"typeId":"myType","state":"<inner string>"},...]
```
The `state` field is an opaque string produced by `Serializable.State.serialize()` — the plugin
imposes no structure on it. In practice implementations use `kotlinx.serialization` JSON objects
encoded to string, but any round-trippable string works. The JSON decoder uses
`ignoreUnknownKeys = true`, so adding new fields to `ActorStateWrapper` is safe.

## Type registration
Every serializable actor type must have a `SerializableMetadata<T>` registered at manager
construction time. The metadata holds:
- `typeId: String` — unique key written to the outer JSON
- `type: KClass<T>` — used for the `actor::class → typeId` reverse lookup during save
- `deserializeState: (String) -> Serializable.State<T>` — called during load with the inner string

The convenience factory `SerializableMetadata<reified T>(typeId, deserializeState)` captures
`T::class` automatically. Unknown `typeId` values encountered during deserialization are silently
skipped. A `SerializationException` from the outer JSON parse returns an empty list.

## Save/load flow
- **Save**: `serializationManager.serializeActors(actors)` — iterates the list, calls
  `actor.save().serialize()` for each, skips actors whose `KClass` is not registered. Returns a
  JSON string.
- **Load**: `serializationManager.deserializeActors(json)` — decodes the array, calls
  `deserializeState(inner).restore()` for each registered type, returns the resulting actor list.
  The caller is responsible for adding restored actors to `ActorManager`.
- Neither call touches `ActorManager` directly; triggering save/load is entirely the caller's
  responsibility (e.g., from a custom Manager or a scene-editor integration).

## Extending with custom metadata types
`SerializationManager` is generic (`MD : SerializableMetadata<out T>, T : Serializable<out T>`).
This allows subclassing `SerializableMetadata` to carry extra data (e.g., editor display names)
and then calling `getMetadata(typeId)` to retrieve the typed metadata at runtime (used by
`scene-editor`). Use `SerializationManager.newInstance(vararg MD)` for typed instances;
use `SerializableMetadata.newSerializationManagerInstance(vararg SerializableMetadata<*>)` for the
default untyped convenience path.

## Built-in kotlinx serializers for engine types
The plugin ships `KSerializer` implementations for all engine geometry types, usable as
`@Serializable(with = ...)` or via the provided typealiases:
`SerializableBoxBody`, `SerializableSceneOffset`, `SerializableSceneSize`, `SerializableScale`,
`SerializableAngleRadians`, `SerializableAngleDegrees`, `SerializablePointBody`,
`SerializableOffset`, `SerializableSize`, `SerializableColor`.
These cover all fields of `BoxBody` and `PointBody` and are the standard way to implement
`Serializable.State.serialize()` using `Json.encodeToString(...)`.
