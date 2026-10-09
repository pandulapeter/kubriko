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

## Public API

```kotlin
interface DebugMenuContract {
    val isVisible: StateFlow<Boolean>
    fun toggleVisibility()

    // Four composable entry points, each with a simple overload (below) and a detailed overload:
    @Composable operator fun invoke(kubriko: Kubriko?, isEnabled: Boolean, kubrikoViewport: @Composable () -> Unit)
    @Composable fun Horizontal(kubriko: Kubriko?, isEnabled: Boolean, windowInsets: WindowInsets)
    @Composable fun Vertical(kubriko: Kubriko?, isEnabled: Boolean, windowInsets: WindowInsets)
    @Composable fun OverlayOnly(kubriko: Kubriko?, kubrikoViewport: @Composable () -> Unit, buttonAlignment: Alignment?)
}
```

The detailed overloads add these parameters (the simple ones pass the values in brackets):

- `invoke` — `modifier`, `windowInsets` (`WindowInsets.safeDrawing`), `buttonAlignment` (`Alignment.TopStart`), `debugMenuTheme` (`{ it() }`), `verticalDebugMenuWidth` (192 dp), `horizontalDebugMenuHeight` (160 dp)
- `Horizontal` — `modifier`, `debugMenuTheme` (`{ it() }`), `height` (180 dp)
- `Vertical` — `modifier`, `debugMenuTheme` (`{ it() }`), `width` (192 dp)
- `OverlayOnly` — `modifier`

The detailed overloads have `= Unit` default bodies in the interface; both the real and the noop implementation override all four.

## Usage Pattern

Wrap `KubrikoViewport` with the chosen composable:

```kotlin
DebugMenu(kubriko = kubriko, isEnabled = BuildConfig.DEBUG) {
    KubrikoViewport(kubriko = kubriko)
}
```

`DebugMenu` is the `object` that implements this interface in both the real and noop modules.

## Module Dependency Rule

Always code against `tool-debug-menu-api`. There is no build-logic swap: each consuming module's own `build.gradle.kts` depends on `projects.tools.debugMenu` or `projects.tools.debugMenuNoop` depending on `showcase.isDebugMenuEnabled` (e.g. `examples/demo-physics/build.gradle.kts`, `app/shared/build.gradle.kts`), and both of those expose this module with `api(...)`.
