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

`ComposeViewport(viewportContainerId = "composeViewport", configure = { isA11YEnabled = false }) { ... }` mounts the Compose tree into the full-page `#composeViewport` element of `index.html`. It gets its own container because `ComposeViewport` clears whatever element it mounts into, and the loading screen has to outlive the mount. Accessibility is disabled because it causes significant performance degradation on the Wasm target.

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

## Load time

The production distribution is post-processed by the `injectWebPreloads` task in `build.gradle.kts`, which adds `<link rel="preload">` tags to the distributed `index.html` for the two wasm binaries and every resource the first frame waits for (fonts, string tables, icons, the welcome-screen and isometric-demo images). The app requests those resources strictly one after the other, so without preloading each one costs a network round trip. The preload set is defined by `webPreloadPatterns`; every pattern must match at least one file, so renaming or removing a preloaded resource fails the build until the pattern is updated. The task also injects the uncompressed size of each preloaded file as `window.kubrikoResourceSizes`, and wraps everything it injects in marker comments so that re-running it replaces the block instead of duplicating it. The dev server does not get the preloads. Production webpack source maps are disabled since they are never deployed.

The loading screen in `index.html` shows a progress bar driven by a `window.fetch` wrapper that streams every fetched resource listed in the size table through a byte counter, so the wasm binaries stay on the streaming compilation path. Sizes have to come from the build because the host compresses the responses, which makes `Content-Length` useless; without the table (on the dev server) the bar stays hidden. The screen sits above the canvas and is faded out by `hideLoadingScreen()`, which the Kotlin entry point calls through `KubrikoShowcase`'s `onFirstFrameDrawn` callback once the first real frame has been drawn, so there is no flash of an empty canvas. The logo uses `metadata/logo.webp`; keep that image small since it competes with the wasm download for bandwidth.

The wasm binaries dominate the download; the host serves them gzipped, and the file names are content hashes so revalidation after the host's short cache lifetime is a cheap 304.

Known limitations: iOS browsers have significant issues (performance, audio, frequent freezes). Chrome/Firefox desktop is near-JVM quality.
