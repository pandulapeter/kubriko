<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Export

Release builds of a game for every target, the Project settings they read (Release, Icons, and Signing), and Play on a real iOS device,
which needs the signing. Every value lives where the build reads it, so the project still builds the same in IntelliJ and without
`.kubriko/` (`../CLAUDE.md` → Principles 1).

## Build variants

Two variants, named the same on every platform:

- **Debug** is what Play already runs: the play session, logging, Play from this Scene, hot reload, and the debugger (`04-debugger.md`).
  Exporting it is offered only where someone else runs it: an Android debug APK for testers, and an unminified web build with source maps.
  Desktop has no debug export, since Play is that.
- **Release** is what Export makes, and the only variant a player gets: shrunk, optimized, obfuscated where the platform can be, and with
  nothing of Creator left in it.

### What a release build has stripped

- **Creator's glue moves out of the game's sources.** `GameEntryPoint`, its `META-INF/services` file, the `tool-creator-api` dependency, and
  the desktop `Main.kt`'s reads of `kubriko.creator.logging` and `kubriko.creator.scene` move into a source folder (`src/creatorMain`) that the
  build adds only when the `kubriko.creator` Gradle property is set, which only Creator's init script sets. A template upgrade; IntelliJ and
  plain Gradle builds then compile what a release has. `KubrikoBuildFiles` keeps `tool-creator-api` there instead of in `commonMain`.
- **Log Blocks are removed, not just skipped:** `KubrikoWiring.isLoggingEnabled` becomes a constant `false` in release builds (an
  `-assumevalues` rule for ProGuard and R8; on the web, Binaryen sees the constant), so the shrinker drops each Log Block with its strings.
- **What stays:** `scene-editor-api`, since Scenes load at runtime through `EditableMetadata` (it is small, and the shrinker removes what the
  game doesn't use), and the serialization of Actor states. Neither is Creator's: a hand-written Kubriko game has them too.
- **Checked, not assumed:** after every release export, Creator looks inside the result for `com.pandulapeter.kubriko.creator` classes,
  `kubriko.creator.` strings, and a Log Block's text, and fails the export if it finds one. The integration test does the same for every
  template.

### Optimization per platform

| Platform | Release build |
|---|---|
| Desktop | Compose's release packaging with ProGuard: shrinking, optimization, and obfuscation, with the rules from `app/desktop/proguard-rules.pro` written into the project (desktop ProGuard reads no consumer rules) and the user's own from Project settings. Needs a JDK with `jmods`, which the JDK Creator downloads is. |
| Web | The Kotlin/Wasm production distribution: dead code elimination, Binaryen's optimizer, and webpack's minified JavaScript. Names are stripped rather than obfuscated, which is what Wasm offers. Exported as static files, zipped. |
| Android | R8 in full mode with `isMinifyEnabled` and `isShrinkResources`; the engine's consumer rules ship with its artifact. |
| iOS | The Kotlin/Native release framework (optimized, no debug info) in Xcode's Release configuration; Kotlin/Native has no obfuscation step. |

Shrinking breaks things only at runtime (a class kept by name, a reflective lookup in hand-written code), so **Play release build** runs the
release variant from Creator before it is exported (without the session: the console shows its output and exit code), and the export
smoke-tests it by starting it and waiting a few seconds for a crash.

## Exporting games

| Target | How | Signing |
|---|---|---|
| Web | The Wasm distribution task, zipped as static files | None |
| Android | APK and AAB, with the SDK the first-run setup downloads (`05-installing-creator.md`). | Keystore created with `keytool` into a signing profile (Signing, below), stored outside the project with a prominent backup reminder, passwords in the OS keychain |
| Windows | `packageExe` / `packageMsi`, on Windows only | Authenticode through `signtool`, the user's certificate |
| macOS | `packageDmg`, on macOS only | Signing and notarization with the user's Apple Developer account |
| Linux | `packageDeb` / `packageRpm`, on Linux only | None standard |
| iOS | `xcodebuild`, on macOS with Xcode only | The user's Apple Developer account |

Desktop installers build only for the host OS; an optional generated GitHub Actions workflow builds all three (as this repository does for the
Showcase). Every export is the release variant (above), named and versioned from Project settings → Identity, and signed with the
signing profile the project names (Signing, below); keystores and certificates stay outside the project. A project with editions
(`08-project-upkeep.md` → Editions, which adds this once editions exist) exports the ones chosen in one go, each from its own entry points, with its own name, version, and
application ID, and the check of what a release has stripped also looks for what only another edition uses.

## Project settings

New sections of the Project settings dialog (`ProjectSettingsDialog`), beside Identity, Game, and Targets:

- **Release:** whether release builds are obfuscated (on by default), and extra shrinker rules for hand-written code, both read by the
  build (above).
- **Icons:** one square source image, from which every platform's icon is generated (Android adaptive, `.icns`, `.ico`, PNG sizes, and
  the web favicon), written where each platform's build reads it.
- **Signing:** which signing profile each target uses, from the profiles on this machine (below).
- **Identity:** raising the version number with each release export, offered in the export dialog.
- **AI tools:** Release and Icons join `get_project_settings` and `set_project_settings`, with the same diffs and confirmations as the
  panel (raising the tool list's budget in `TokenBudgetTest`). Signing gets no tools, since it is secret.

### Signing

Signing secrets never go into the project: it is committed, and often public. Creator keeps per-machine signing profiles in its
settings folder (keystore path, alias, certificate name, and Apple team ID), with every password in the system's keychain, and the
project only names a profile in `.kubriko/local/`. Project settings → Signing picks or creates a profile; a project whose profile isn't
on this machine (a fresh clone, a teammate) asks for one at its first release export instead of failing. An Android keystore is
created with `keytool`, with a prominent reminder to back it up.

## Play on an iOS device

Built for the device with the signing profile's team, installed and started with `xcrun devicectl`, and its output streamed to the
console. iOS devices are `PlayTarget.Kind.IOS` too (by an `ios-device:` ID), with a source of places and a start in `TargetPlayers`,
which `WorkspaceController.play` runs through `SessionController.playElsewhere`, as `IosSimulatorPlayer` does. A device without a
profile that allows it says what is missing rather than failing in Xcode's words. `devicectl` runs through `ToolProcesses`, as `adb`,
`emulator`, `simctl`, and `xcodebuild` do, with `FakeToolProcesses` replaying recorded outputs, so the device list, the state of each
launch, and every failure (no profile, install refused, and a crash) are unit tested; playing on a real device is in `HANDOVER.md` →
Before a release.

## Order

Release and its build variant first, then what a release build has stripped, Signing, Icons, the exports target by target (web,
desktop, Android, then iOS), Play on an iOS device, and the generated GitHub Actions workflow last.
