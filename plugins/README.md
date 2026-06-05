<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Plugins
This folder contains all the plugin submodules that can be added to games that need their specific functionality.

## Implementation
Each plugin exposes a [Manager](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/Manager.kt) subclass that needs to be added to the [Kubriko]((https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/Kubriko.kt)) `newInstance()` function. 
Some plugins also introduce new [Actor](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/actor/Actor.kt) Traits.

## Modules
- [audio-playback](https://github.com/pandulapeter/kubriko/tree/main/plugins/auido-playback) - For playing music and sound effects.
- [collision](https://github.com/pandulapeter/kubriko/tree/main/plugins/collision) - For detecting overlapping Actors.
- [keyboard-input](https://github.com/pandulapeter/kubriko/tree/main/plugins/keyboard-input) - For detecting key presses.
- [particles](https://github.com/pandulapeter/kubriko/tree/main/plugins/particles) - For drawing particle effects.
- [persistence](https://github.com/pandulapeter/kubriko/tree/main/plugins/persistence) - For loading and saving data from / to local storage.
- [physics](https://github.com/pandulapeter/kubriko/tree/main/plugins/physics) - For simulating physics interactions.
- [pointer-input](https://github.com/pandulapeter/kubriko/tree/main/plugins/pointer-input) - For detecting mouse and touch screen events.
- [serialization](https://github.com/pandulapeter/kubriko/tree/main/plugins/serialization) - For serializing / deserializing Actors.
- [shaders](https://github.com/pandulapeter/kubriko/tree/main/plugins/shaders) - For drawing shader effects.
- [sprites](https://github.com/pandulapeter/kubriko/tree/main/plugins/sprites) - For managing and drawing images.