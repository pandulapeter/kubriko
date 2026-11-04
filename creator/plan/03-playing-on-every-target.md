<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Playing on every target

Play runs the game on the desktop, in the browser, on an Android device or emulator, and on an iOS simulator. This plan lets Play
run it on an iOS device too, from the same button, so that what a target turns on can be seen working. It comes before the rest of Settings, since Release, Icons, and
Signing only matter once exporting does (`13-export-and-distribution.md`), while this is what makes Targets useful now.

Setting up a new iOS simulator can be done at any time; a new Android emulator waits for the Android SDK's command-line tools,
which `15-onboarding.md` downloads; iOS devices wait for Signing. iOS devices are `PlayTarget.Kind.IOS` too (by an `ios-device:` ID), with a source of places and a start in
`TargetPlayers`, which `WorkspaceController.play` runs through `SessionController.playElsewhere`, as `IosSimulatorPlayer` does.

## Setting up emulators and simulators

The last entry of Play's Android and iOS submenus (`PlayTargetMenu`, through `WorkspaceController.openDeviceSetup`) opens Android's
guide to creating emulators in the browser, and DeviceHub or the Simulator app for iOS. Both become a dialog of Creator's own, as
calm as Project settings, which creates the device and chooses it in Play's menu, so a beginner never leaves Creator for it.

- **iOS simulator:** a name, a device type (`xcrun simctl list devicetypes --json`, iPhones and iPads only, the newest first), and an
  iOS version (`xcrun simctl list runtimes --json`, the installed and available ones), created with `simctl create`. A Mac with no iOS
  runtime says how to get one (Xcode → Settings → Components) rather than offering an empty list; a name already taken is refused
  in the field, as `NewNameField` does.
- **Android emulator:** a name, a device profile (`avdmanager list device`), and a system image: the installed ones first, then those
  that can be downloaded (`sdkmanager --list`), downloaded with the progress, cancel, and licenses `15-onboarding.md` → First-run setup
  gives the SDK's packages. Created with `avdmanager create avd`, and started like any emulator when it is played. Waits for that
  download of the command-line tools (`cmdline-tools`), which an SDK installed without Android Studio's setup lacks; until then the
  entry keeps opening the guide.
- Both run their tools through `ToolProcesses` with `FakeToolProcesses` replaying recorded output (the lists, a creation refused, a
  download that fails or is cancelled), and the dialogs get offscreen snapshots.

## iOS devices

- After Signing (`05-settings.md`): builds for the device with the signing profile's team, installs and starts it with `xcrun devicectl`,
  and streams its output. A device without a profile that allows it says what is missing rather than failing in Xcode's words.

## Testing

- `devicectl` runs through `ToolProcesses`, as `adb`, `emulator`, `simctl`, and `xcodebuild` do, with `FakeToolProcesses` replaying
  recorded outputs, so the device list, the state of each launch, and every failure (no profile, install refused, a crash) are unit
  tested.
- The integration test already builds every target; launching on a real emulator or simulator needs hardware acceleration and windows
  that CI's runners can't give reliably, so it goes to `HANDOVER.md` → Before a release, alongside playing on real devices.
