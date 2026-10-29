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

## Build variants

Two variants, named the same on every platform:

- **Debug** is what Play already runs: the play session, logging, Play from this Scene, hot reload, and the debugger (`09-debugger.md`).
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
| Desktop | Compose's release packaging with ProGuard: shrinking, optimization, and obfuscation, with the rules from `app/desktop/proguard-rules.pro` written into the project (desktop ProGuard reads no consumer rules) and the user's own from Project settings. Needs a JDK with `jmods`, so the JDK Creator downloads (`12-onboarding.md`) must be a full one. |
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
| Android | Android SDK command-line tools downloaded by Creator after the user accepts the licenses; APK and AAB. Adding the SDK also lets templates gain an Android target (a project with one can't even be configured without the SDK). | Keystore created with `keytool` into a signing profile (`03-settings.md` → Signing), stored outside the project with a prominent backup reminder, passwords in the OS keychain |
| Windows | `packageExe` / `packageMsi`, on Windows only | Authenticode through `signtool`, the user's certificate |
| macOS | `packageDmg`, on macOS only | Signing and notarization with the user's Apple Developer account |
| Linux | `packageDeb` / `packageRpm`, on Linux only | None standard |
| iOS | `xcodebuild`, on macOS with Xcode only | The user's Apple Developer account |

Desktop installers build only for the host OS; an optional generated GitHub Actions workflow builds all three (as this repository does for the
Showcase). Every export is the release variant (above), named and versioned from Project settings (`03-settings.md`), and signed with the
signing profile the project names there; keystores and certificates stay outside the project.

## Distributing Creator

- One installer per OS with `jpackage`. Its bundled runtime can't start Gradle (jpackage strips `bin/`), so Creator downloads a full JDK 21 into
  its data folder on first launch, alongside Gradle, and points the Tooling API at it (the setup screen in `12-onboarding.md`).
- Update checks that offer, never install silently; its own release workflow and test job.
- A Creator release follows each Kubriko release, since opted-in projects use Creator's own Kubriko version (`../CLAUDE.md` → Conventions).
