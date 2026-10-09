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
- `IS_SCENE_EDITOR_AVAILABLE = false` (commonMain) — exists so the module has a common source and builds for iOS (completely empty KMP modules cannot be built for iOS). The real `scene-editor` module declares the same constant, also `false`, so it cannot tell the two apart, and nothing in the repo reads it. Whether it should become a real availability guard is an open decision; until then do not use it as one.

## Usage pattern

`SceneEditor.show(...)` and `SceneEditor(...)` are always safe to call; with the noop linked nothing happens.
