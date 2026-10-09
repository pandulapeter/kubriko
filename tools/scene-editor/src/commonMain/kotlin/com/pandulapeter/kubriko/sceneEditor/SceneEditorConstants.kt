/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor

/**
 * This constant is only here because completely empty Kotlin Multiplatform modules cannot be built for iOS.
 *
 * Always false, in both `tool-scene-editor` and `tool-scene-editor-noop`: it exists so the module has common code on
 * every target, and it is not a reliable availability check.
 */
const val IS_SCENE_EDITOR_AVAILABLE = false