# Serialization Plugin

The `serialization` plugin provides a framework for saving and loading the state of actors in Kubriko. This is essential for features like scene saving/loading, level editors, and state persistence.

## Features

- **Actor Serialization**: Convert any actor that implements the `Serializable` interface into a string representation.
- **Metadata-driven Deserialization**: Use `SerializableMetadata` to define how each actor type should be restored.
- **Generic Support**: Flexible enough to handle custom base types and metadata for advanced use cases.
- **Scene Editor Integration**: Built-in compatibility with Kubriko's scene editing tool.

## Usage

### 1. Implement the Serializable Interface

Your actors need to implement `Serializable` and provide a `State` object that holds their data:

```kotlin
class MyActor(val initialPosition: Offset) : Actor(), Serializable<MyActor> {

    var currentPosition = initialPosition

    override fun save() = MyActorState(currentPosition)

    data class MyActorState(val position: Offset) : Serializable.State<MyActor> {
        override fun restore() = MyActor(position)
        override fun serialize() = "${position.x},${position.y}" // Simple CSV serialization
    }
}
```

### 2. Define Metadata

Create a `SerializableMetadata` instance for your actor type. This tells Kubriko how to parse the serialized string:

```kotlin
val myActorMetadata = SerializableMetadata<MyActor>(
    typeId = "my_actor",
    deserializeState = { data ->
        val parts = data.split(",")
        MyActor.MyActorState(Offset(parts[0].toFloat(), parts[1].toFloat()))
    }
)
```

### 3. Register the Manager

Add the `SerializationManager` to your `Kubriko` instance:

```kotlin
val kubriko = Kubriko.newInstance(
    SerializableMetadata.newSerializationManagerInstance(
        myActorMetadata,
        // ... other actor metadata
    ),
    // ... other managers
)
```

### 4. Serialize and Deserialize

Use the manager to save or load lists of actors:

```kotlin
val serializationManager = kubriko.get<SerializationManager<*, *>>()

// Save a list of actors to a string
val serializedData = serializationManager.serializeActors(myActors)

// Load actors from a string
val loadedActors = serializationManager.deserializeActors(serializedData)
```

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:plugin-serialization`
