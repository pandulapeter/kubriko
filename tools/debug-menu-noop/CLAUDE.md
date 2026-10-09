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
- `invoke` and `OverlayOnly` render `kubrikoViewport()` inside a `Box(modifier)`; `Horizontal` and `Vertical` render nothing
- The simple overloads are inherited from `DebugMenuContract`, where they delegate to the detailed ones; the noop overrides the four detailed overloads.

## When to Modify

Only change this file if `DebugMenuContract` in `debug-menu-api` gains new API — the noop must satisfy the full interface. The noop must never introduce dependencies on the real debug-menu implementation.
