<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# tool-debug-menu-noop

No-op implementation of `DebugMenuContract`. Swapped in when `isDebugMenuEnabled=false` in `gradle.properties` to exclude the real debug menu from production builds.

## Key Files

- `DebugMenu.kt` — single file; `object DebugMenu : DebugMenuContract`

## Implementation

- `isVisible` is a permanently-false `MutableStateFlow`
- `toggleVisibility()` is a no-op
- All four detailed composable overloads (`invoke`, `Horizontal`, `Vertical`, `OverlayOnly`) return `Unit` and render only `content()`
- The simple overloads with `= Unit` bodies in `DebugMenuContract` are inherited without override

## When to Modify

Only change this file if `DebugMenuContract` in `debug-menu-api` gains new API — the noop must satisfy the full interface. The noop must never introduce dependencies on the real debug-menu implementation.
