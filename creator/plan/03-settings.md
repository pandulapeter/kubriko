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

What `../CLAUDE.md` → Creator settings describes, and:

- **AI features:** asked once on first run (`12-onboarding.md`), defaulting to on; onboarding and any later AI hints or
  empty-state suggestions read `LocalIsAiEnabled` like the rest.
- **Editor:** Scene grid and snapping defaults (`06-blocks.md`), whether Play saves first (writes are already automatic, so
  only if that changes), and confirmation prompts turned off with "Don't ask again", listed so they can be turned back on (none
  exist yet).
- **Components:** the JDK, Gradle, and Android SDK (`12-onboarding.md` → Settings → Components).
- **Updates:** whether Creator checks for updates (`10-export-and-distribution.md`).
- **AI:** the defaults behind the AI panel that aren't about one conversation (the MCP server's port, token, and commands from
  Advanced; the agent and Claude Code configuration) may move here as a section, hidden with the rest when AI is off.

## Project settings

What `../CLAUDE.md` → Project settings describes (the dialog and Identity), and the sections below. Every value lives where the
build reads it, so the project still builds the same in IntelliJ and without `.kubriko/` (Principle 2): `.kubriko/project.json` only
holds what Creator needs for itself.

- **Identity, still to come:** raising the version number with each release export (`10-export-and-distribution.md`), and the
  Android `defaultConfig` and iOS `Info.plist` reading the keys once those targets exist (Targets below).
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

`get_project_settings` and `set_project_settings` exist for Identity; Targets, Game, and Release join them, with the same diffs and
confirmations as the panel (the tool list's budget in `TokenBudgetTest` will need raising).
Signing and Creator settings get no tools: one is secret, the other is the user's own.
