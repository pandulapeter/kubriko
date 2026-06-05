<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# UI Components Tool

The `ui-components` module provides a library of standardized Material3-based Composables shared across all Kubriko tools and the Showcase app. It ensures a consistent look and feel for the engine's internal tooling.

## Design Philosophy

- **Consistency**: All tools (Debug Menu, Scene Editor) use these components to provide a unified user experience.
- **Adaptive**: Components are designed to work well on both mobile (Debug Menu) and desktop (Scene Editor).
- **Material3**: Built on top of `androidx.compose.material3` for modern styling and theme support.

## Available Components

- **Buttons**: `LargeButton`, `SmallButton`, `FloatingButton`.
- **Inputs**: `SmallSlider`, `SmallSwitch`, `TextInput`.
- **Panels**: `Panel`, `InfoPanel` for structured data display.
- **Feedback**: `LoadingIndicator`, `LoadingOverlay`.

## Internal Use

While this module is publicly available, it is primarily designed for internal Kubriko tooling. APIs may change more frequently than other modules as the engine's tools evolve.

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:tool-ui-components`
