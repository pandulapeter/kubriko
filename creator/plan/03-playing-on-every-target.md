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

Play runs the game on the desktop only. Targets (`04-settings.md`) builds it for the web, Android, and iOS too, but nothing shows it
running there. This plan lets Play run it in a browser, on an Android device or emulator, and on an iOS simulator or device, from the
same button, so that what a target turns on can be seen working. It comes before the rest of Settings, since Release, Icons, and
Signing only matter once exporting does (`11-export-and-distribution.md`), while this is what makes Targets useful now.

Worked on in this order, each item shipped and reviewed on its own: Android, iOS simulator; then iOS devices, which wait for
Signing. Each adds its kind to `PlayTarget.Kind`, a source of places to `PlayTargetChoice`, and its start to
`WorkspaceController.play` through `SessionController.playElsewhere`, as the web's `WebPlayer` does.

## Android

- Devices from `adb devices -l` and emulators from `emulator -list-avds`, through the SDK `04-settings.md` → Targets finds. An emulator
  that isn't running is started and waited for (`sys.boot_completed`); none is created here (`13-onboarding.md` downloads the SDK, and
  creating an emulator joins it).
- Builds `:androidApp:assembleDebug`, installs it with `adb install -r`, starts it with `am start`, and streams `adb logcat` for its
  process into the console; Stop is `am force-stop`. A device that leaves mid-game ends the game, with the reason.
- More than one device is chosen from the menu, never guessed; an unauthorized device says how to allow it.

## iOS simulator

- Simulators from `xcrun simctl list devices available --json`; one that is shut down is booted. Builds with `xcodebuild` for the
  simulator (`-destination id=…`), installs with `simctl install`, and starts with `simctl launch --console-pty`, whose output is the
  console; Stop is `simctl terminate`.
- Only on a Mac with Xcode, as building is (`04-settings.md` → Targets).

## iOS devices

- After Signing (`04-settings.md`): builds for the device with the signing profile's team, installs and starts it with `xcrun devicectl`,
  and streams its output. A device without a profile that allows it says what is missing rather than failing in Xcode's words.

## Testing

- `adb`, `emulator`, `simctl`, `xcodebuild`, and `devicectl` run behind one process interface with a fake that replays recorded
  outputs, so the device lists, the state of each launch, and every failure (no device, unauthorized, boot timeout, install refused,
  a crash) are unit tested; the web server is tested for real on localhost.
- The integration test already builds every target; launching on a real emulator or simulator needs hardware acceleration and windows
  that CI's runners can't give reliably, so it goes to `HANDOVER.md` → Before a release, alongside playing on real devices.
