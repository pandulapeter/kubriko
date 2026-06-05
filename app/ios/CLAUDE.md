<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# app/ios — iOS entry point

Kotlin/Native iOS module. The Xcode project calls `KubrikoShowcaseViewController()` to get a `UIViewController` that hosts the shared `KubrikoShowcase` Composable.

## Entry point

`KubrikoShowcaseViewController()` returns a custom `UIViewController` subclass. The Compose UI is embedded by creating a `ComposeUIViewController` as a child view controller, then adding its view with `UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight` so it fills the parent.

## Fullscreen handling

Fullscreen state is a file-level `mutableStateOf<Boolean>`. Toggling it:
1. Flips the `isInFullscreenMode` state (consumed by `KubrikoShowcase` to hide/show the top bar).
2. Calls `UIApplication.sharedApplication.keyWindow?.rootViewController?.setNeedsStatusBarAppearanceUpdate()` to trigger a re-query of `prefersStatusBarHidden()`, which returns `isInFullscreenMode.value`. This hides the iOS status bar in fullscreen mode.

There is no native system fullscreen on iOS — it is purely a UI-level affordance (hiding the in-app top bar and the status bar).

## Build / run

There is no `gradlew` run task for iOS. Build and run via Xcode or the IDE run configuration. The Xcode project is located at `app/ios/` (look for `*.xcodeproj` / `*.xcworkspace`).
