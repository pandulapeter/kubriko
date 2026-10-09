<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
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

## Platform Notes

Keys typed into a text field reach the game on Desktop, Web and iOS, so a game that mixes text input with keyboard
control should ignore keys while its text field is focused. On Android, Compose focus traversal and focused clickables
can consume arrows, Tab, Enter and Space before the game sees them; keep Compose focus off in-game controls (for
example with `focusProperties { canFocus = false }`) if the game listens to those keys.

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:plugin-keyboard-input`
