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
- `README.md` → Roadmap is in dependency order as of 2026-11-09: every file depends only on the ones before it, so work goes
  file by file from `01`. Leftovers that waited on later work were moved into the file they wait for, and what waits on something
  outside this repository is under its file's Waiting on others (ACP protocol 2, the Kotlin SDK's `systemPrompt`, Codex's quota
  until Nov 21, 2026, and the registry's icons, all in `01-ai-assistant.md`).

## Next

Start `01-ai-assistant.md` at its first item (Chips, under The panel).

This session finished Sign-in in a terminal, reviewed with `/code-review`, its findings fixed in two commits (the shared `PATH`
lookup, `findProgram`, and a terminal that never opens or closes early ending the wait):

- A terminal sign-in method can continue in the system's own terminal (`SystemTerminalLauncher`): Terminal (or whatever
  opens `.command` files) on macOS, the first known terminal emulator on Linux, and PowerShell on Windows. Creator writes a
  script that deletes itself first, since it holds the agent's variables, and waits for the exit code it writes to a file,
  so a success resends the refused prompts as the panel's own terminal does. The panel offers it on every terminal sign-in,
  first when the output says a terminal device was needed (`TerminalDeviceErrors`).
- The POSIX and PowerShell quoting moved into `ShellQuoting`, and a racy Codex accounts test was fixed.

Not verified in the real app yet (all covered by unit and offscreen tests, none by a person or a live agent):

- The live writers' room scenario: `KUBRIKO_CREATOR_CLAUDE_AGENT=true ./gradlew :creator:ai:test --tests "*AssistantLiveScenariosTest.aPitchInTheWritersRoom*"`
  (it spends a real Claude subscription's requests, up to an hour), then tune `AssistantSystemPrompt.WRITERS_ROOM` by what it does.
- A project created from an idea, through to the room's first questions, in the real app (an end-to-end scenario could cover
  it under Xvfb, but it needs a signed-in agent).
- Pasting an image from the system clipboard, and dropping one from Finder, onto a page.
- A real agent other than Claude Agent switched to its read-only mode, asking before its own edits, and one that edits
  without asking being marked (Codex's `read-only` sandbox once it has quota again, after Nov 21, 2026).
- Google Antigravity following the appended system prompt in a real conversation (it spends the user's Antigravity
  requests; the binary Creator downloaded is in the settings folder's `tools/agents`).
- Codex started as a `codex-work` alias's account signing in to that `CODEX_HOME` (running `codex-acp` fetched from npm wasn't
  allowed in this session; it is listed under Codex's profile in `01-ai-assistant.md`).

## Before a release

What only the user can check, needing something no agent has (a high refresh rate panel, a real Windows machine, a browser
sign-in, an OS keychain prompt, a vendor's quota). One line each; the user goes through it once a larger feature set is ready,
never item by item.

- Every frame the display shows (Project settings → Game) on a 120 Hz screen.
- The macOS title bar's controls following Creator's light and dark theme.
- Continuing a sign-in in the system's terminal: Terminal on macOS, PowerShell on a real Windows machine, and a Linux
  desktop's terminal emulator opening the script, and the panel carrying on once it signs in.
- The packaged game and its installers on macOS, Windows, and Linux: the version, vendor, and a name with an accent, and one in another script (Игра), reaching them.
- The web build in a browser (`wasmJsBrowserDevelopmentRun`): the page's title from `game.name`, a fixed aspect ratio, and web
  audio; and Play in the browser opening the system's browser on macOS, Windows, and Linux (`xdg-open`).
- Hearing a played game: music and sounds from Blocks, music pausing and resuming with the window's focus, a non-looping music
  played again, and going between Scenes with the same and different music.
- A game with Android and iOS turned on running on a phone or tablet of each (the integration test builds both apps but never
  launches them), and Play on a real Android phone and emulator: an emulator started from the menu, the console's logs, Stop,
  unplugging the phone mid-game, and a phone that hasn't allowed debugging yet; and Play on an iOS simulator with a real game:
  DeviceHub (or the Simulator app) showing it, Log Blocks in the console, starting from a Scene, and Stop, and one made in
  the New simulator dialog against a real Xcode (its tests replay `simctl`'s recorded lists, but never ran `create`); and a real phone
  connected through ADB appearing in the Android submenu, including one that hasn't allowed debugging yet.
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
  `:creator:run` does (`./gradlew :publishCreatorLibraries -Pcreator.localKubriko=true`), then `KUBRIKO_CREATOR_E2E=true
  KUBRIKO_CREATOR_VERSION=0.8.1-creator xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew :creator:test --tests "*endToEnd*" --rerun`,
  and look at `creator/build/end-to-end/<scenario>`; or dispatch `Tests`, whose `creator-end-to-end` job keeps the images as an
  artifact. On this Mac, a Linux container under Colima works: an `eclipse-temurin:21-jdk` image with `xvfb`, `libgl1`,
  `libfontconfig1`, and the X libraries, the repository copied into a volume (`docker cp`, never the host's build folders), a
  JetBrains Runtime 21 for linux-aarch64 unpacked and named in `org.gradle.java.installations.paths` (foojay has none for it),
  `-Pkotlin.internal.mpp.allowMultiplatformPublicationsOnUnsupportedHost=true` on the publish (Kotlin/Native has no
  linux-aarch64 host) with CI's three `publish…ToMavenLocal` tasks, and Playwright's Chromium in `KUBRIKO_CREATOR_CHROME` for
  the web step. Unit tests and offscreen snapshots (`uiTest`) open no window and run anywhere.
- Never recompile `:creator` while the user has `:creator:run` up.
- The integration test, which builds but opens no window: `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=0.8.1-creator
  ./gradlew :creator:template:test --tests "*ProjectTemplateIntegrationTest*" --rerun` (after `./gradlew :publishCreatorLibraries
  -Pcreator.localKubriko=true`, since 0.8.1 isn't released; `everyTargetBuilds` also needs the Android SDK and Xcode).
