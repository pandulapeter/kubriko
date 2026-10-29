<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Settings

Two separate panels, because they are stored in different places and belong to different people: **Creator settings** are this user's,
on this machine, in the settings folder (next to `mcp.json`); **Project settings** are the game's, in the project, committed, and shared
with everyone who clones it. Anything that is both per-project and private (signing keys) is neither: see Signing below.

## Creator settings

Opened from the toolbar and from the welcome screen, so they work without a project.

- **AI features (on or off).** Off removes every AI surface: the AI connection panel and its toolbar button, the assistant panel
  (`02-ai-assistant.md`), `AiActivityBar` and the AI's highlighted targets, AI hints and empty-state suggestions, and AI mentions in
  onboarding and the template's README. It is a guarantee, not just hiding: the MCP server is stopped, no agent process is started, and
  nothing is sent anywhere. Commands, the undo history, and command titles stay, since they aren't AI features.
  - One check (`CreatorSettings.isAiEnabled`, observed) that every AI surface reads, so a new one can't forget it; a UI test walks the
    screens with it off and finds no AI string.
  - Asked once on first run (`12-onboarding.md`), defaulting to on, and changeable later. Turning it back on restores the MCP settings
    and connected clients as they were.
  - Per user, never per project: a project made with AI opens normally with it off (its `AGENTS.md`/`CLAUDE.md`, if `02-ai-assistant.md`
    generates one, is just a file).
- **Appearance:** theme (system, light, or dark) and UI scale (Campfire's 0.85 by default).
- **Editor:** default project folder, Scene grid and snapping defaults (`06-blocks.md`), whether Play saves first, and confirmation
  prompts that were turned off with "Don't ask again", listed so they can be turned back on.
- **Components:** the JDK, Gradle, and Android SDK (`12-onboarding.md` → Settings → Components).
- **Updates:** whether Creator checks for updates (`10-export-and-distribution.md`).
- **AI connection** moves here as a section once Settings exists (hidden with the rest when AI is off).

## Project settings

Opened from the toolbar of an open project. Every value lives where the build reads it, so the project still builds the same in IntelliJ
and without `.kubriko/` (Principle 2): `.kubriko/project.json` only holds what Creator needs for itself.

- **Identity:** game name (window title, web page title, installer and app names), version name, and version number (raised by one with
  each release export, as `showcase.buildNumber` is here), application ID / bundle ID, vendor, and description.
  - A template change: these become keys in `gradle.properties` (`game.name`, `game.versionName`, `game.versionNumber`,
    `game.applicationId`), read by `build.gradle.kts` for `nativeDistributions`, the Android `defaultConfig`, and the iOS `Info.plist`,
    and turned into a generated `GameInfo` constant for the game's own code (the desktop `Main.kt`'s `Window(title = …)`, an about
    screen) by a small task in the build file, so there is one source. `index.html`'s title is filled in by the same task.
    `{{PROJECT_NAME}}` stops being baked into `Main.kt` and `index.html`. Shipped as a template upgrade (`ProjectUpgrader`).
  - Creator edits only those keys, line by line (as `VersionCatalog` does), so they count as Creator-owned in the README's ownership table
    and need no diff each time; a value changed by hand in the file shows up in the panel.
- **Icons:** one square source image, from which every platform's icon is generated (Android adaptive, `.icns`, `.ico`, PNG sizes,
  the web favicon).
- **Targets:** desktop, web, Android, and iOS, each turned on or off, as a confirmed diff of the build file (Principle 4). Android needs the
  SDK, iOS needs a Mac with Xcode (`12-onboarding.md`).
- **Game:** the first Scene, aspect ratio mode, target frame rate, and the Managers and plugins in use (`KubrikoWiring` already decides
  the plugins; this shows why each is there).
- **Release:** whether release builds are obfuscated (on by default), and extra shrinker rules for hand-written code
  (`10-export-and-distribution.md` → Build variants).
- **Signing:** which key each target uses, from the keys on this machine (below).

### Signing

Signing secrets never go into the project: it is committed, and often public. Creator keeps per-machine signing profiles in its settings
folder (keystore path, alias, certificate name, Apple team ID), with every password in the OS keychain, and the project only names a
profile in `.kubriko/local/`. Project settings → Signing picks or creates a profile; a project whose profile isn't on this machine (a
fresh clone, a teammate) asks for one at its first release export instead of failing. Creating an Android keystore, backing it up, and the
platform details are in `10-export-and-distribution.md`.

## AI tools

`get_project_settings` and `set_project_settings` (identity, targets, game, release), with the same diffs and confirmations as the panel.
Signing and Creator settings get no tools: one is secret, the other is the user's own.
