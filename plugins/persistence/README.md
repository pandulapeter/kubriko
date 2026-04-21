# Persistence Plugin

The `persistence` plugin provides a simple way to persist game data across sessions. It uses a key-value store and exposes data through `MutableStateFlow`s that automatically save their values whenever they are updated.

## Features

- **Automatic Synchronization**: Changes to the provided `MutableStateFlow`s are automatically persisted to local storage.
- **Type Safety**: Supports `Boolean`, `Int`, `Float`, `String`, and custom generic types.
- **Platform Agnostic**: Works across all supported Kubriko platforms.

## Usage

### 1. Register the Manager

Add the `PersistenceManager` to your `Kubriko` instance:

```kotlin
val kubriko = Kubriko.newInstance(
    PersistenceManager.newInstance(fileName = "my_game_prefs"),
    // ... other managers
)
```

### 2. Access Persisted Values

You can retrieve a `MutableStateFlow` for a specific key. If the key doesn't exist, the `defaultValue` will be used and eventually persisted when the value changes.

```kotlin
val persistenceManager = kubriko.get<PersistenceManager>()

// Get or create a persisted integer (e.g., high score)
val highScoreFlow = persistenceManager.int(key = "high_score", defaultValue = 0)

// Update the value (this will automatically save it to disk)
highScoreFlow.value = 1000

// Collect the value like any other Flow
scope.launch {
    highScoreFlow.collect { score ->
        println("Current high score: $score")
    }
}
```

### 3. Custom Types

For types not supported out of the box, use the `generic` function by providing a serializer and deserializer:

```kotlin
val userProfileFlow = persistenceManager.generic(
    key = "user_profile",
    defaultValue = UserProfile(name = "Player 1"),
    serializer = { Json.encodeToString(it) },
    deserializer = { Json.decodeFromString<UserProfile>(it) }
)
```

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:plugin-persistence`
