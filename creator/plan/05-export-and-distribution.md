<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Export and distribution

## Exporting games

| Target | How | Signing |
|---|---|---|
| Web | The Wasm distribution task, zipped as static files | None |
| Android | Android SDK command-line tools downloaded by Creator after the user accepts the licenses; APK and AAB. Adding the SDK also lets templates gain an Android target (a project with one can't even be configured without the SDK). | Keystore created with `keytool`, stored outside the project with a prominent backup reminder, passwords in the OS keychain |
| Windows | `packageExe` / `packageMsi`, on Windows only | Authenticode through `signtool`, the user's certificate |
| macOS | `packageDmg`, on macOS only | Signing and notarization with the user's Apple Developer account |
| Linux | `packageDeb` / `packageRpm`, on Linux only | None standard |
| iOS | `xcodebuild`, on macOS with Xcode only | The user's Apple Developer account |

Desktop installers build only for the host OS; an optional generated GitHub Actions workflow builds all three (as this repository does for the
Showcase). Desktop exports run ProGuard with the rules from `app/desktop/proguard-rules.pro`.

## Distributing Creator

- One installer per OS with `jpackage`. Its bundled runtime can't start Gradle (jpackage strips `bin/`), so Creator downloads a full JDK 21 into
  its data folder on first launch, alongside Gradle, and points the Tooling API at it; sizes are shown up front.
- Update checks that offer, never install silently; its own release workflow and test job.
- Until a Kubriko release ships `tool-creator-api` and `tool-creator-session`, Creator only works through the development route (`../CLAUDE.md`).
