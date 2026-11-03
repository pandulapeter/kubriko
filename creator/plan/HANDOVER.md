<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Handover

Where the next session starts: what to do next and what hasn't been verified in the real app yet. Each session rewrites it before it ends.
The rules are in `../CLAUDE.md`, what is planned in the other files here. A session that finished an item notes here whether its
review (`README.md`) ran and what it fixed; one that stopped before the review says so under Next, so the next session starts with it.

## Status

- Ask before every push (`git log origin/main..HEAD` shows what would go).
- `library.version` is 0.8.1, unreleased (0.8.0 is on Maven Central). It holds library changes Creator uses: `Logger.log` building
  its list once, and the `SpriteManager.newInstance(imageLoader, …)` overload.
- Creator is AI-centric. `02-ai-assistant.md` is built except what needs a real session or someone else's work:
  Codex's profile (Codex is out of quota until Nov 21, 2026), ACP protocol 2 and its `systemPrompt` field (not final, not in the
  Kotlin SDK), Actors' states for the AI (needs `09-debugger.md`), the registry's icons (each vendor's trademark rules), and the
  smaller follow-ups the file lists.

## Next

The end-to-end checks' pass is done: Not yet verified is settled, and both scenarios (`OpeningAndPlayingTest` and
`UpgradingAndPlayingTest`) pass headless in `Tests`' `creator-end-to-end` job. `UpgradingAndPlayingTest` found that stopping a game
while a hot reload ran could start it again (fixed in `GameReloader`). Never run a scenario on the user's Mac, where the game tools
bring windows to the front.

Project settings in `03-settings.md`: Identity and Game are built and reviewed (template versions 9 and 10, the
dialog behind the toolbar's settings menu, and `get_project_settings` / `set_project_settings`). Next is Targets, then Release,
Icons, and Signing, each extending the same dialog and tools. Then `04` to `07` in order. What is left in
`01-large-projects.md` waits for other plans (Traits, UI elements, git) or for the benchmarks.

## Before a release

What only the user can check, needing something no agent has (a high refresh rate panel, a real Windows machine, a browser
sign-in, an OS keychain prompt, a vendor's quota). One line each; the user goes through it once a larger feature set is ready,
never item by item.

- Every frame the display shows (Project settings → Game) on a 120 Hz screen.
- The macOS title bar's controls following Creator's light and dark theme.
- The packaged game and its installers on macOS and Windows: the version, vendor, and a name with an accent reaching them.
- The web build in a browser (`wasmJsBrowserDevelopmentRun`): the page's title from `game.name`, a fixed aspect ratio, and web
  audio.
- Hearing a played game: music and sounds from Blocks, music pausing and resuming with the window's focus, a non-looping music
  played again, and going between Scenes with the same and different music.
- The Scene editor's middle-button pan and scroll pan with a real mouse wheel.
- A real Claude sign-in through the browser (with a scratch `CLAUDE_CONFIG_DIR`) and a real API key in the macOS keychain.
- The Linux (`secret-tool`) and Windows (Credential Locker) secret stores.
- On a real Windows machine: Claude Agent through `npx.cmd`, Stop ending `node` and `claude` too, renaming a Scene the running
  game holds open, and crash-safe writes over a file another program holds; and on any system, writes to a network drive.
- Connecting Claude Desktop, the Claude Code app, Cursor, Codex (out of quota until Nov 21, 2026), and fish aliases with a real
  fish; and a real non-Claude agent (Google Antigravity) starting.

## Working with the live app

- **Only headless.** The end-to-end checks and anything else that opens Creator's or a game's window never run on the user's Mac,
  where they would take the screen and the focus while the user works. Run them under Xvfb on Linux: publish to Maven Local as
  `:creator:run` does (`./gradlew publishDesktopPublicationToMavenLocal publishWasmJsPublicationToMavenLocal
  publishKotlinMultiplatformPublicationToMavenLocal -Pcreator.localKubriko=true`), then `KUBRIKO_CREATOR_E2E=true
  KUBRIKO_CREATOR_VERSION=0.8.1-creator xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew :creator:test --tests "*endToEnd*" --rerun`,
  and look at `creator/build/end-to-end/<scenario>`; or dispatch `Tests`, whose `creator-end-to-end` job keeps the images as an
  artifact. Unit tests and offscreen snapshots (`uiTest`) open no window and run anywhere.
- Never recompile `:creator` while the user has `:creator:run` up.
- The integration test, which builds but opens no window: `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=0.8.1-creator
  ./gradlew :creator:template:test --tests "*ProjectTemplateIntegrationTest*" --rerun` (after a `publishToMavenLocal` with
  `-Pcreator.localKubriko=true`, since 0.8.1 isn't released).
