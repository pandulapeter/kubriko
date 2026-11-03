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

Play runs the game on the desktop, in the browser, and on an Android device or emulator. Targets (`05-settings.md`) builds it for
iOS too, but nothing shows it running there. This plan lets Play run it on an iOS simulator or device, from the same button, so that
what a target turns on can be seen working. It comes before the rest of Settings, since Release, Icons, and
Signing only matter once exporting does (`12-export-and-distribution.md`), while this is what makes Targets useful now.

Worked on in this order, each item shipped and reviewed on its own: the iOS simulator; then iOS devices, which wait for Signing.
Each adds its kind to `PlayTarget.Kind`, a source of places and its start to `TargetPlayers`, which `WorkspaceController.play`
runs through `SessionController.playElsewhere`, as Android's `AndroidPlayer` does.

## iOS simulator

- Simulators from `xcrun simctl list devices available --json`; one that is shut down is booted. Builds with `xcodebuild` for the
  simulator (`-destination id=…`), installs with `simctl install`, and starts with `simctl launch --console-pty`, whose output is the
  console; Stop is `simctl terminate`.
- Only on a Mac with Xcode, as building is (`05-settings.md` → Targets).

## iOS devices

- After Signing (`05-settings.md`): builds for the device with the signing profile's team, installs and starts it with `xcrun devicectl`,
  and streams its output. A device without a profile that allows it says what is missing rather than failing in Xcode's words.

## Testing

- `simctl`, `xcodebuild`, and `devicectl` run through `ToolProcesses`, as `adb` and `emulator` do, with `FakeToolProcesses` replaying
  recorded outputs, so the device lists, the state of each launch, and every failure (no simulator, boot timeout, install refused, a
  crash) are unit tested.
- The integration test already builds every target; launching on a real emulator or simulator needs hardware acceleration and windows
  that CI's runners can't give reliably, so it goes to `HANDOVER.md` → Before a release, alongside playing on real devices.
