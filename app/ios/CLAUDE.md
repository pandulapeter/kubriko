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

Version and build number come from `gradle.properties` (`showcase.versionName`, `showcase.iosBuildNumber`). A "Set version from gradle.properties" build phase writes them into the built `Info.plist`, so `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` are deliberately absent from `project.pbxproj` — editing the Version/Build fields in Xcode's General tab has no effect. Bump the properties instead.

## Publishing

The `[Showcase] Publish iOS` workflow archives, signs and uploads the app to App Store Connect. Two details of this project shape it:

- The only scheme (`[iOS] Showcase`) lives under `xcuserdata`, which a CI runner never sees, so `xcodebuild` there falls back to the scheme it auto-creates from the target. The workflow builds `-scheme iosApp -configuration Release`, which also makes it independent of any scheme's archive configuration.
- Xcode drives Gradle through the `embedAndSignAppleFrameworkForXcode` build phase, so there is no command line for the workflow to add `-P` overrides to. It rewrites the four `showcase.*` feature flags in `gradle.properties` before archiving instead, which is also what gets the matching values into `BuildConfig`.

Bump `showcase.iosBuildNumber` before every run — App Store Connect rejects a build number it has already seen. Upload is as far as automation goes: the build still has to finish processing, and reaching the App Store needs a manual submission for review.
