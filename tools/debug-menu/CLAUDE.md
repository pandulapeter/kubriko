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
- `InternalDebugMenu.kt` — singleton; owns an internal `Kubriko` instance (for `PersistenceManager`), a per-game overlay Kubriko, keyed by the game `Kubriko` instance and reference-counted across `OverlayOnly`, `Horizontal` and `Vertical` (the last one to leave composition disposes it), and all persisted settings
- `DebugMenuManager.kt` — `Manager + Overlay + Unique`; added to a separate per-game Kubriko instance with its own `ViewportManager` (mirroring the game's `targetFrameRate`); it reads camera, scale and size from the game's and assumes the game canvas is centered in the overlay area; draws cyan body bounds and magenta collision mask outlines over `visibleActorsWithinViewport`
- `DebugMenuContainer.kt` — the panel root: registers the game Kubriko with `InternalDebugMenu`, draws the panel `Surface`, and collects the `InternalDebugMenu`/`Logger` state it passes down to `ui/DebugMenuContents` (including the `logsHeader` slot)
- `ui/` — the panel's Composables: `DebugMenuContents` (metadata, overlay switches, log list), `LogsHeader` (importance toggles, text filter, clear button; plain state and callbacks only), `LogEntry` (one log line)
- `RefCountedRegistry.kt` — main-thread, reference-counted key → value holder backing the per-game overlays

## Architecture

`registerGameKubriko(kubriko)` / `unregisterGameKubriko(kubriko)` maintain a per-game overlay Kubriko, keyed by the game `Kubriko` instance and reference-counted (`RefCountedRegistry`) across `OverlayOnly`, `Horizontal` and `Vertical`; the last one to leave composition disposes it. Each game gets its own `DebugMenuManager` overlay — multi-instance support works by creating isolated debug overlays.

`InternalDebugMenu` uses persistence file name `"kubrikoDebugMenu"` for settings.

## Layout

- `invoke` auto-selects Horizontal (portrait, `maxWidth < maxHeight`, in `DebugMenu.invoke`'s `BoxWithConstraints`) or Vertical (landscape)
- Default sizes: `invoke` uses a 192 dp wide vertical panel and a 160 dp tall horizontal one; the simple `Horizontal` overload is 180 dp tall, `Vertical` 192 dp wide
- Four overloads in the API: `invoke` (auto), `Horizontal`, `Vertical`, `OverlayOnly`
- `OverlayOnly` applies `modifier` to its root; the debug overlay viewport fills that root.

## Log Viewer

Shows `InternalDebugMenu.logs`: a `combine` of `Logger.logs` with the LOW/MEDIUM/HIGH importance toggles and the text filter (both persisted). `LogEntry` formats each line with `logEntryText` and color-codes entries with a source by an HSV hue hashed from the source string's suffix (`sourceHue`).

## Gotchas

- The debug menu uses its own internal `Kubriko` instance — do not pass it to `registerGameKubriko`
- Actor body outlines are drawn by `DebugMenuManager` as an `Overlay` — they are in screen-space overlaid on top of the game world, not in scene coordinates
- Visibility toggle and overlay enable/disable states are persisted and survive app restarts
- Depends on `debug-menu-api`; consumers must also depend on `debug-menu-api`, never on this module directly in production
