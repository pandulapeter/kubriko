<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Installing Creator

One installer per system, and everything Creator needs fetched for the user with visible progress on first launch, so a beginner never
installs a JDK, Gradle, or the Android SDK by hand. Today Creator runs from this repository with a JDK already installed; git needs no
installation (JGit), and Gradle distributions are downloaded by the wrapper inside a build, with nothing to show for it but the
console.

## Installers

- One installer per system with `jpackage`, with its own release workflow and test job. Its bundled runtime can't start Gradle
  (`jpackage` strips `bin/`), so the first-run setup (below) downloads a full JDK 21 for that.
- A Creator release follows each Kubriko release, since opted-in projects use Creator's own Kubriko version (`../CLAUDE.md` → Library
  side).
- **Updates:** Creator checks for a new release and offers it, never installing silently; whether it checks is a Creator setting.

## First-run setup

- **One setup screen** on first launch, before the project list, listing what Creator needs and what each costs: a JDK 21 (a full
  one, with `jmods`, since desktop release builds need it), the Gradle distribution the templates use, and optionally the Android SDK
  (only for Android targets and exports). Each has its size, a progress bar with speed and time left, and a cancel button; required
  parts start at once, optional ones on a click.
- **AI features** are asked about on the same screen, once, defaulting to on; the setup and any later AI hints read
  `LocalIsAiEnabled` like the rest.
- **Everything into Creator's own data folder**, never the system: the JDK and Gradle unpacked there, the Tooling API pointed at them,
  nothing on `PATH` changed. A JDK 21 or Gradle that is already installed can be chosen instead.
- **Downloads that survive real networks:** resumed after a dropped connection or a restart, checksums verified before unpacking,
  mirrors or a manual file as the fallback, proxy settings read from the system (and editable), and a clear error that says what to
  do.
- **The Android SDK** through its command-line tools (`cmdline-tools`), with the licenses shown and accepted in Creator, and only the
  packages the templates need. The command-line tools stay installed, for the emulator dialog (below) and Android exports.
- **Warming up:** after the downloads, the starter template is built once in the background, so the user's first project doesn't wait
  for Gradle to fill its caches.
- **Settings → Components** shows the same list later: versions, disk use, update, repair, and remove.
- **Tested** against a fake download server (slow, interrupted, wrong checksum, and offline), like the rest of Creator's controllers.

## A new Android emulator

The last entry of Play's Android submenu (`PlayTargetMenu`, through `WorkspaceController.openAndroidEmulatorGuide`) opens Android's
guide to creating emulators in the browser. It becomes a dialog of Creator's own, as calm as the iOS one (`NewIosSimulatorDialog`),
which creates the emulator and chooses it in Play's menu:

- A name, a device profile (`avdmanager list device`), and a system image: the installed ones first, then those that can be downloaded
  (`sdkmanager --list`), downloaded with the setup's progress, cancel, and licenses. Created with `avdmanager create avd`, and started
  like any emulator when it is played.
- An SDK without the command-line tools (one installed without Android Studio's setup) offers to download them first.
- It runs its tools through `ToolProcesses` with `FakeToolProcesses` replaying recorded output (the lists, a creation refused, and a
  download that fails or is cancelled), as `IosSimulatorSetup` does, and the dialog gets offscreen snapshots.

## Order

The first-run setup and Components first (they also work from this repository), then the emulator dialog, then the installers and
updates.
