<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# app/web — Web (Kotlin/Wasm) entry point

Kotlin/Wasm browser application. `fun main()` in `KubrikoShowcaseApp.kt` is the entry point.

## Entry point

`ComposeViewport(configure = { isA11YEnabled = false }) { ... }` mounts the Compose tree into the browser viewport. Accessibility is disabled because it causes significant performance degradation on the Wasm target.

## Browser history / deeplink integration

The web target is the only platform that drives browser navigation. `KubrikoShowcase`'s `deeplink` and `onDestinationChanged` parameters are wired to the browser's History API:
- Navigation to an entry: `pushState` if the user was at the root, otherwise `replaceState`.
- Navigation back to root: `history.back()` if the session started at root (so the browser back button works naturally), otherwise `replaceState`.
- A `popstate` event listener syncs `currentPath` when the user presses the browser back/forward buttons.
- `WEB_ROOT_PATH_NAME` (from `BuildConfig`) is stripped from all paths before processing, allowing deployment under a sub-path.
- Exiting fullscreen is triggered on every destination change to avoid the fullscreen state persisting across entries.

## Fullscreen handling

`isInFullscreenMode` is `null` on iPhone browsers (fullscreen is not supported / meaningful), and `false` otherwise. The `onFullscreenModeToggled` callback calls `document.documentElement?.requestFullscreen()` / `document.exitFullscreen()`. A `fullscreenchange` event listener syncs the state if the user exits fullscreen via the browser's own UI.

## Build / run

```bash
./gradlew :app:web:wasmJsBrowserDevelopmentRun   # Dev server
./gradlew :app:web:wasmJsBrowserDistribution      # Production build
```

Known limitations: iOS browsers have significant issues (performance, audio, frequent freezes). Chrome/Firefox desktop is near-JVM quality.
