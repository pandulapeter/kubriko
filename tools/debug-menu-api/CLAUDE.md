<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# tool-debug-menu-api

Public API surface for the debug menu. Consumers depend on this module; the real implementation (`tool-debug-menu`) or the noop (`tool-debug-menu-noop`) is swapped in at build time via `isDebugMenuEnabled` in `gradle.properties`.

## Key Files

- `DebugMenuContract.kt` — the sole file; defines the entire public interface

<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
## Public API

```kotlin
interface DebugMenuContract {
    val isVisible: StateFlow<Boolean>
    fun toggleVisibility()

    // Four composable entry points — all have a simple overload and a detailed overload:
    @Composable fun invoke(kubriko: Kubriko, isEnabled: Boolean, content: @Composable () -> Unit)
    @Composable fun Horizontal(kubriko: Kubriko, isEnabled: Boolean, content: @Composable () -> Unit)
    @Composable fun Vertical(kubriko: Kubriko, isEnabled: Boolean, content: @Composable () -> Unit)
    @Composable fun OverlayOnly(kubriko: Kubriko, isEnabled: Boolean, content: @Composable () -> Unit)
}
```

All four composable overloads have default `= Unit` implementations in the interface — the noop module inherits them without override. The real implementation overrides all four.

## Usage Pattern

Wrap `KubrikoViewport` with the chosen composable:

```kotlin
DebugMenu(kubriko = kubriko, isEnabled = BuildConfig.DEBUG) {
    KubrikoViewport(kubriko = kubriko)
}
```

`DebugMenu` is the `object` that implements this interface in both the real and noop modules.

## Module Dependency Rule

Always depend on `tool-debug-menu-api`. The build-logic convention plugin handles swapping in the real vs noop implementation via the `isDebugMenuEnabled` flag.
