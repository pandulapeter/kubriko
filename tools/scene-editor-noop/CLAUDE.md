<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# scene-editor-noop

No-op swap for `scene-editor`. Activated when `showcase.isSceneEditorEnabled=false` in `gradle.properties`.

## Contents

- `SceneEditor` (Desktop only) — `object` implementing `SceneEditorContract`. Both `show()` and `invoke()` are stubs that return `Unit`. Safe to call; nothing happens.
- `IS_SCENE_EDITOR_AVAILABLE = false` (commonMain) — constant available on all platforms. Use this guard to conditionally show editor launch buttons in the app UI. The real `scene-editor` module does not declare this constant; its absence signals the real implementation is linked.

## Usage pattern

```kotlin
if (IS_SCENE_EDITOR_AVAILABLE) {
    // show a "Launch Editor" button
}
// SceneEditor.show(...) is always safe to call regardless
```

The noop exists because completely empty KMP modules cannot be built for iOS; the constant satisfies the compiler without pulling in any editor logic.
