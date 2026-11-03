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
Signing only matter once exporting does (`12-export-and-distribution.md`), while this is what makes Targets useful now.

iOS devices wait for Signing. They are `PlayTarget.Kind.IOS` too (by an `ios-device:` ID), with a source of places and a start in
`TargetPlayers`, which `WorkspaceController.play` runs through `SessionController.playElsewhere`, as `IosSimulatorPlayer` does.

## iOS devices

- After Signing (`05-settings.md`): builds for the device with the signing profile's team, installs and starts it with `xcrun devicectl`,
  and streams its output. A device without a profile that allows it says what is missing rather than failing in Xcode's words.

## Testing

- `devicectl` runs through `ToolProcesses`, as `adb`, `emulator`, `simctl`, and `xcodebuild` do, with `FakeToolProcesses` replaying
  recorded outputs, so the device list, the state of each launch, and every failure (no profile, install refused, a crash) are unit
  tested.
- The integration test already builds every target; launching on a real emulator or simulator needs hardware acceleration and windows
  that CI's runners can't give reliably, so it goes to `HANDOVER.md` → Before a release, alongside playing on real devices.
