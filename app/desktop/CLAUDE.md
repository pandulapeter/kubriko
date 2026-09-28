<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# app/desktop — Desktop JVM entry point

Compose Desktop (JVM) app launched via `fun main()` in `KubrikoShowcaseApp.kt`.

## Entry point and window setup

`application { ... }` block creates the Compose Desktop application. Initial window size is 860×660dp; minimum size is enforced via AWT `window.minimumSize = Dimension(400, 400)`.

`windowState` is stored in an internal engine extension property (`com.pandulapeter.kubriko.implementation.windowState`) so other desktop-only modules (like the Scene Editor) can access it.

## Fullscreen handling

Fullscreen is platform-specific on Desktop:

- **Windows**: The `Window` Composable is recreated via `key(isInFullscreenMode.value)` when toggling, because Windows requires `undecorated = true` + `resizable = false` for true fullscreen. `WindowPlacement.Fullscreen` alone is not sufficient on Windows.
- **macOS / Linux**: The window is never recreated; `windowState.placement = WindowPlacement.Fullscreen` is used directly, and `undecorated`/`resizable` remain unchanged.

When entering fullscreen, the previous `WindowPlacement`, `windowState.size`, window location, and AWT `window.bounds` are saved to restore them accurately on exit. A 100ms `delay` is needed before restoring `window.bounds` on exit (race condition with the Compose window re-render).

A `WindowStateListener` detects if the user exits fullscreen via OS gestures (e.g. pressing Escape on macOS) and syncs the `isInFullscreenMode` state.

## Title bar

On macOS and Windows the window's content is laid out under the system title bar, keeping only the window buttons, so the Showcase's own surface is the title bar (`TitleBar.kt`). The window is a `SwingWindow` so this can be set in `init`, before it is shown, since the JDK does not lay out the content again afterwards. Both use the JetBrains Runtime's custom title bar (`JBR.getWindowDecorations()` from `jbr-api`), which behaves like a title bar — dragging, and a double click that does what the system is set to do with one (zoom, fill, minimize or nothing on macOS) — only where nothing listens to the mouse. Compose's canvas listens everywhere, so a toolkit-wide AWT listener marks every mouse event over the strip as the title bar's (`forceHitTest(false)`), except in full screen, where there is no strip. macOS's transparent title bar client properties would lay the content out the same way, but let the double click through to the content, where it does nothing. The listener is removed when the window leaves the composition, since the Windows fullscreen toggle recreates the window. Linux keeps the window manager's title bar, as does a runtime other than the JetBrains Runtime, and the undecorated Windows fullscreen window has none.

The strip (28dp on macOS, 32dp on Windows) reaches the shared UI as a top system bar inset (`TitleBarInsets`, which overrides Compose Desktop's internal `LocalPlatformWindowInsets`), so `WindowInsets.safeDrawing` keeps content clear of it, as it does for a phone's status bar. A window in `WindowPlacement.Fullscreen` gets no inset. `TitleBarAppearance` draws the buttons for the current system theme (`apple.awt.windowAppearance`, `controls.dark`), polled the same way `KubrikoTheme` polls it. Both properties exist only in the JetBrains Runtime.

## Scene Editors

The desktop app registers scene editors for examples that support them. These launch as separate windows (handled by the scene-editor tool) and write scenes directly into source directories:
- `AnnoyedPenguinsGameSceneEditor` → `examples/game-annoyed-penguins/.../files/scenes`
- `BlockysJourneyGameSceneEditor` → `examples/game-blockys-journey/.../files/scenes`
- `IsometricGraphicsDemoSceneEditor` → `examples/demo-isometric-graphics/.../files/scenes`
- `PerformanceDemoSceneEditor` → `examples/demo-performance/.../files/scenes`
- `PhysicsDemoSceneEditor` → `examples/demo-physics/.../files/scenes`

Scene editors are only compiled when `showcase.isSceneEditorEnabled = true` in `gradle.properties`; otherwise the `-noop` implementation is linked.

## Build configuration

- macOS: `.dmg` distribution; requires code signing identity `"PETER PANDULA"` and notarization env vars (`NOTARIZATION_APPLE_ID`, `NOTARIZATION_PASSWORD`, `NOTARIZATION_TEAM_ID`).
- Windows: `.exe` distribution; icon from `icon.ico`.
- Linux: `.deb` distribution; icon from `icon.png`.
- ProGuard (minification + obfuscation) is enabled for release builds. The Compose plugin's bundled default rules cover Skiko/coroutines/serialization; `proguard-rules.pro` adds what they miss (JLayer reflection, enums, attributes) and has to stay wired in through `configurationFiles` — without it JLayer's `ServiceLoader`-resolved audio device factory gets shrunk away and music playback dies while sounds keep working. `javaHome` points the toolchain at the JetBrains Runtime 21, which the title bar needs (see above) and which `run` and every package use. ProGuard needs the `jmods` to resolve `java.**` references. The `jbrsdk` build that Gradle provisions has them, but Android Studio's bundled JBR doesn't, so never point `javaHome` at that. `proguard-rules.pro` keeps `com.jetbrains.**` by name, since the runtime binds the `jbr-api` interfaces by name. No obfuscation mapping file is written, so release crash reports cannot be de-obfuscated.
- Packaging tasks (`createReleaseDistributable` etc.) need `JAVA_HOME` to point at a full JDK with `jpackage`; the Android Studio JBR lacks it.
- Steam releases come from `.github/workflows/showcase-publish-desktop.yml`, which uploads the bare `createReleaseDistributable` output of all three platforms rather than the installers. The launch options in Steamworks point into that layout (`Kubriko Showcase.exe`, `Kubriko Showcase.app`, `bin/Kubriko Showcase`), so changing `packageName` breaks them. The macOS build is arm64 only, as that is what the runner and `compose.desktop.currentOs` produce.
- `System.setProperty("apple.awt.application.name", "Kubriko Showcase")` sets the macOS menu bar app name.
