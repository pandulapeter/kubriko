<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Debug Menu Tool - No-op

The `debug-menu-noop` module provides a no-op implementation of the `debug-menu-api` contract. It is designed to be used in production builds to ensure that the debug menu overlay is completely excluded from the final application while maintaining source code compatibility.

## Why use this?

The Debug Menu is an essential tool during development, but it should not be accessible to end-users. By using this no-op version, you can:
- **Improve Security**: Ensure no debug information or state manipulation tools are present in the release binary.
- **Reduce binary size**: Exclude all UI components and logic associated with the debug menu.
- **Simplify integration**: Use the same `DebugMenu` calls in your UI code without wrapping them in `if (DEBUG)` blocks everywhere.

## Usage

Replace the `debug-menu` dependency with `debug-menu-noop` in your production build flavor:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.pandulapeter.kubriko:tool-debug-menu-noop")
}
```

The API remains identical to the real implementation, but calling its methods will have no effect, and the `invoke` composable will simply render its content without any overlay.

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:tool-debug-menu-noop`
