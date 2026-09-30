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

Kotlin/Native iOS module. The Xcode project calls `KubrikoShowcaseViewController(onFullscreenModeChanged)` to get a `UIViewController` that hosts the shared `KubrikoShowcase` Composable.

## Entry point

`KubrikoShowcaseViewController(onFullscreenModeChanged)` returns a custom `UIViewController` subclass. `ContentView.swift`'s `ComposeView` passes the callback, which feeds `ContentView`'s `@State`. The Compose UI is embedded by creating a `ComposeUIViewController` as a child view controller, then adding its view with `UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight` so it fills the parent.

## Fullscreen handling

Fullscreen state is a file-level `mutableStateOf<Boolean>`. Toggling it:
1. Flips the `isInFullscreenMode` state (consumed by `KubrikoShowcase` to hide/show the top bar).
2. Calls `onFullscreenModeChanged` with the new value, which drives `ContentView`'s `.statusBarHidden` and so hides the iOS status bar in fullscreen mode. The app's root is SwiftUI's hosting controller, which takes the status bar from that modifier and does not consult the embedded controller's `prefersStatusBarHidden()`.

There is no native system fullscreen on iOS — it is purely a UI-level affordance (hiding the in-app top bar and the status bar).

## Build / run

There is no `gradlew` run task for iOS. Build and run via Xcode or the IDE run configuration. The Xcode project is located at `app/ios/` (look for `*.xcodeproj` / `*.xcworkspace`).

Version and build number come from `gradle.properties` (`showcase.versionName`, `showcase.buildNumber`, shared with Android). A "Set version from gradle.properties" build phase writes them into the built `Info.plist`, so `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` are deliberately absent from `project.pbxproj` — editing the Version/Build fields in Xcode's General tab has no effect. Bump the properties instead.

## Publishing

The `[Showcase] Publish iOS` workflow archives, signs and uploads the app to App Store Connect. Two details of this project shape it:

- The shared schemes (`[iOS] Showcase` and `[iOS] Showcase Release`, under `xcshareddata`) reference the target, so Xcode no longer auto-creates an `iosApp` scheme. The workflow builds `-scheme "[iOS] Showcase Release" -configuration Release`, which also makes it independent of the scheme's archive configuration. Keep the schemes complete (every action, as Xcode writes them): Android Studio's Xcode build service crashes on a stripped-down scheme, which breaks the `.run/` iOS configurations.
- Xcode drives Gradle through the `embedAndSignAppleFrameworkForXcode` build phase, so there is no command line for the workflow to add `-P` overrides to. It rewrites the four `showcase.*` feature flags in `gradle.properties` before archiving instead, which is also what gets the matching values into `BuildConfig`.

Bump `showcase.buildNumber` before every release — it is shared with Android, and each store rejects a number it has already seen. To repeat an upload without a commit, pass the `build_number` input instead. After the upload the workflow waits for App Store Connect to process the build and submits it for review (`.github/scripts/app_store_submission.py`), with the release notes — the `release_notes` input, or else the commit log since the last successful run — as its "What's New". A draft version already prepared in App Store Connect is used as it is, screenshots and release option included. With `submit` off, the version is prepared but not submitted, for screenshots to be added by hand. Where another version is still waiting for Apple, the build stays in TestFlight and the run ends with a warning.

The workflow signs with the team's Apple Distribution certificate, stored with its private key in the `IOS_DISTRIBUTION_CERTIFICATE_BASE64` / `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD` secrets; xcodebuild makes the App Store provisioning profile for it through the App Store Connect key. It does not create a certificate per run: Apple allows the team two Apple Distribution certificates, and Campfire's pipeline, which signs for the same team, holds one while its builds wait for review. Never revoke the stored certificate while a build it signed is waiting for App Review, which would get that build refused (ITMS-90238). It expires yearly: the run warns 90 days ahead and fails once it has, both times printing the renewal steps (`RENEWAL_STEPS` in the workflow), after which **Re-run failed jobs** continues the run with the new secrets.
