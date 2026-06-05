<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# tool-debug-menu

Real implementation of the debug menu overlay: log viewer, actor body visualizer, and performance metrics panel.

## Key Files

- `DebugMenu.kt` — public `object` implementing `DebugMenuContract`; delegates to `InternalDebugMenu`
- `InternalDebugMenu.kt` — singleton; owns an internal `Kubriko` instance (for `PersistenceManager`), a per-game Kubriko map keyed by `instanceName`, and all persisted settings
- `DebugMenuManager.kt` — `Manager + Overlay + Unique`; added to a separate per-game Kubriko instance sharing the game's `ViewportManager`; draws cyan body bounds and magenta collision mask outlines over `visibleActorsWithinViewport`
- `DebugMenuContainer.kt` — Composable layout switching between Horizontal/Vertical panels

## Architecture

`setGameKubriko(kubriko)` creates a per-instance debug Kubriko keyed by `kubriko.instanceName`. Each game gets its own `DebugMenuManager` overlay — multi-instance support works by creating isolated debug overlays. `clearGameKubriko` disposes the overlay Kubriko for that instance.

`InternalDebugMenu` uses persistence file name `"kubrikoDebugMenu"` for settings.

## Layout

- `invoke` auto-selects Horizontal (portrait, `maxWidth < maxHeight`) or Vertical (landscape)
- Vertical panel width: 192 dp; Horizontal panel height: 160 dp
- Four overloads in the API: `invoke` (auto), `Horizontal`, `Vertical`, `OverlayOnly`

## Log Viewer

Reads directly from `Logger.logs`. Filters by LOW/MEDIUM/HIGH importance and text (both persisted). Source entries are color-coded using HSV hashing of the source string suffix.

## Gotchas

- The debug menu uses its own internal `Kubriko` instance — do not pass it to `setGameKubriko`
- Actor body outlines are drawn by `DebugMenuManager` as an `Overlay` — they are in screen-space overlaid on top of the game world, not in scene coordinates
- Visibility toggle and overlay enable/disable states are persisted and survive app restarts
- Depends on `debug-menu-api`; consumers must also depend on `debug-menu-api`, never on this module directly in production
