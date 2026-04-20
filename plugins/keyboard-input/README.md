# Keyboard Input Plugin

The Keyboard Input plugin provides a way to handle keyboard events. It allows actors to react to key presses, releases, and continuous key states.

## Features

- **Global Key Tracking**: Check the state of any key at any time using the `KeyboardInputManager`.
- **Actor Integration**: Add the `KeyboardInputAware` trait to any actor to receive keyboard event callbacks.
- **Convenience Extensions**: Helper properties for common input patterns like WASD/Arrow key movement and zoom controls.

## Usage

### 1. Register the Manager

Add the `KeyboardInputManager` to your `Kubriko` instance:

```kotlin
val kubriko = Kubriko.newInstance(
    KeyboardInputManager.newInstance(),
    // ... other managers
)
```

### 2. Use the KeyboardInputAware Trait

Implement the `KeyboardInputAware` interface in your actor:

```kotlin
class PlayerActor : Actor, KeyboardInputAware {
    
    override fun onKeyPressed(key: Key) {
        if (key == Key.Spacebar) {
            // Jump!
        }
    }

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) {
        if (activeKeys.hasLeft) {
            // Move left
        }
    }
}
```

### 3. Accessing Key State Manually

You can also query the manager directly from other managers or actors:

```kotlin
val keyboardManager = kubriko.get<KeyboardInputManager>()
if (keyboardManager.isKeyPressed(Key.W)) {
    // ...
}
```

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:plugin-keyboard-input`
