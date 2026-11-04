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

Beyond the theme, the interface scale, the AI features switch, and the default project folder (`CreatorSettings`):

- **AI features:** asked once on first run (`15-onboarding.md`), defaulting to on; onboarding and any later AI hints or
  empty-state suggestions read `LocalIsAiEnabled` like the rest.
- **Editor:** Scene grid and snapping defaults (`09-blocks.md`), whether Play saves first (writes are already automatic, so
  only if that changes), and confirmation prompts turned off with "Don't ask again", listed so they can be turned back on (none
  exist yet).
- **Components:** the JDK, Gradle, and Android SDK (`15-onboarding.md` → Settings → Components).
- **Updates:** whether Creator checks for updates (`13-export-and-distribution.md`).
- **AI:** the defaults behind the AI panel that aren't about one conversation (the MCP server's port, token, and commands from
  Advanced; the agent and Claude Code configuration) may move here as a section, hidden with the rest when AI is off.

## Project settings

Beyond the dialog (`ProjectSettingsDialog`), Identity (`GameIdentity`), Game (`GameSettings`, with the libraries in use), and Targets
(`GameTargets`), the sections below. Every value lives where the
build reads it, so the project still builds the same in IntelliJ and without `.kubriko/` (`../CLAUDE.md` → Principles 1): `.kubriko/project.json` only
holds what Creator needs for itself.

- **Identity, still to come:** raising the version number with each release export (`13-export-and-distribution.md`).
- **Icons:** one square source image, from which every platform's icon is generated (Android adaptive, `.icns`, `.ico`, PNG sizes,
  the web favicon).
- **Release:** whether release builds are obfuscated (on by default), and extra shrinker rules for hand-written code
  (`13-export-and-distribution.md` → Build variants).
- **Signing:** which key each target uses, from the keys on this machine (below).
- **Entry points** (`14-project-upkeep.md` → Entry points and Editions): the entry points found in the build, IntelliJ's, and
  Creator's own, as a master-detail page grouped into the game's and the tools, each by platform, with a filter by edition once
  there are several. A beginner's project shows one row per target and a New entry point button, nothing else. A row's detail shows
  where it comes from (the build, IntelliJ, or Creator; shared or this computer's only), its name, whether it is the game's or a
  tool, its edition, and whether it is its kind's default; then, folded under Advanced, what its kind takes: on the desktop the
  program arguments, JVM options, environment variables, working directory, and Gradle properties; on the web the Gradle properties
  and the browser it opens in (this computer's); on Android the flavor and the launch activity; on iOS the scheme and the
  configuration. A value that is this computer's only says so in the row, and one that can't play says why rather than being left
  out. Targets keeps turning the template's targets on and off; what each one turned on adds shows here.
- **Editions:** added, renamed, reordered (the first is the default), and removed, each with its name, application ID suffix,
  version, and the Scenes it leaves out. Identity shows each value with the edition overriding it, so a demo's name is set where
  the game's is.

### Signing

Signing secrets never go into the project: it is committed, and often public. Creator keeps per-machine signing profiles in its settings
folder (keystore path, alias, certificate name, Apple team ID), with every password in the OS keychain, and the project only names a
profile in `.kubriko/local/`. Project settings → Signing picks or creates a profile; a project whose profile isn't on this machine (a
fresh clone, a teammate) asks for one at its first release export instead of failing. Creating an Android keystore, backing it up, and the
platform details are in `13-export-and-distribution.md`.

## AI tools

`get_project_settings` and `set_project_settings` exist for Identity and Game, and `get_target_change` and `set_target` for Targets;
Release joins them (and Entry points and Editions through the commands `14-project-upkeep.md` lists), with the same diffs and confirmations as the panel (the tool list's budget in `TokenBudgetTest` will need raising).
Signing and Creator settings get no tools: one is secret, the other is the user's own.
