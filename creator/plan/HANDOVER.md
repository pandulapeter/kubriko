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
  Kotlin SDK), Actors' states for the AI (needs `12-debugger.md`), the registry's icons (each vendor's trademark rules), and the
  smaller follow-ups the file lists.

## Next

Start `04-screenplay.md`, an important feature that shouldn't wait longer, then `05-settings.md`'s Release, Icons, and Signing,
each extending the Project settings dialog and its tools, after which `03`'s iOS devices can follow Signing. `03`'s new iOS
simulator dialog (Setting up emulators and simulators) is small and waits for nothing, so it can go in between; its Android
emulator dialog waits for `15-onboarding.md`'s download of the SDK's command-line tools. `06-batched-rendering.md`'s engine and sprite atlas parts touch only `engine/` and
`plugins/sprites`, so they can run beside `04` and `05` in a session of their own; its Creator part follows them. Then `06` to `10` in order. What is left
in `01-large-projects.md` waits for other plans (Traits, UI elements, git) or for the benchmarks.

Owed: the end-to-end scenario's last step (`UpgradingAndPlayingTest`, Breakout played on the web in headless Chrome through
`start_game`) has never run, since it needs Linux; dispatch `Tests` once the commits are pushed (ask first) and look at
`creator-end-to-end`'s images.

What the last session did, all reviewed:

- **Log Blocks printing** (`49e4f73ba`, `cec5dce4f`): Kubriko's `Logger` only keeps entries, so nothing a Log Block logged
  reached the browser's console or a device's output. Each Log Block now also prints on one line while logging is on, and the
  Scene loader prints its failures; the print is generated into each Block, since a hand-changed `KubrikoWiring` would lack
  a function of its own.
- **Android** (`a9a817dd2`, `de4a44029..dddcea6ce`): devices and emulators in Play's menu; an emulator starts while
  `:androidApp:assembleDebug` builds; the app is installed, started with the switches, logged from `logcat`, and
  force-stopped by Stop. This Mac's real `adb` and `emulator` output matched the parsers. The review fixed a failed `pidof`
  ending the game, Stop while starting, the build outliving an unusable device, a tool's child holding a run past its
  timeout, `System.err` lines, and two misleading failure messages.
- **iOS simulator** (`f20f4cf56`, `a1ce20e40`, `0a2367c2c`): simulators in Play's menu once the target is on and Xcode is
  there; one booted while Xcode builds the app, then installed and started with `simctl launch --console-pty`, whose output
  is the console; Stop terminates it. A real headless launch on this Mac showed `simctl` names the app's process sometimes
  after its first lines, which the code now allows for. The review fixed Xcode's build skipping the pending writes and the
  build lock, a Scene name `NSUserDefaults` would read as an array, a stopped build killing Gradle's daemons, a stale app
  installed, and an exit code, an unreadable simulator list, and a boot that couldn't start being reported as something else.
- **After the user tried it** (`eecd279a9..5578416d1`): `simctl` holds back what it prints into a pipe until the app ends, so
  Creator never saw the game start and kept saying Building; the launch now runs inside `script`. Xcode 27 has no Simulator
  app, so DeviceHub shows the game instead (and the console says when neither opens). Play's menu groups Android's and iOS's
  places in submenus (devices first, then emulators or simulators, each group ending in the way to set up more, icons only
  at the top), shows every platform the project is built for and this computer can play even with nothing found yet, and
  opens Project settings on Targets from Build for more platforms. `RightClickMenu` gained submenus, which don't take the
  focus, since a focused popup takes every pointer event and the menu under it has to see the pointer move. The failures
  seen earlier were a flaky Android test (the log reader started after the first check, now fixed) and two sessions running
  `:creator:test` at once in one folder, deleting each other's in-progress results.

## Before a release

What only the user can check, needing something no agent has (a high refresh rate panel, a real Windows machine, a browser
sign-in, an OS keychain prompt, a vendor's quota). One line each; the user goes through it once a larger feature set is ready,
never item by item.

- Every frame the display shows (Project settings → Game) on a 120 Hz screen.
- The macOS title bar's controls following Creator's light and dark theme.
- The packaged game and its installers on macOS and Windows: the version, vendor, and a name with an accent reaching them.
- The web build in a browser (`wasmJsBrowserDevelopmentRun`): the page's title from `game.name`, a fixed aspect ratio, and web
  audio; and Play in the browser opening the system's browser on macOS, Windows, and Linux (`xdg-open`).
- Hearing a played game: music and sounds from Blocks, music pausing and resuming with the window's focus, a non-looping music
  played again, and going between Scenes with the same and different music.
- A game with Android and iOS turned on running on a phone or tablet of each (the integration test builds both apps but never
  launches them), and Play on a real Android phone and emulator: an emulator started from the menu, the console's logs, Stop,
  unplugging the phone mid-game, and a phone that hasn't allowed debugging yet; and Play on an iOS simulator with a real game:
  DeviceHub (or the Simulator app) showing it, Log Blocks in the console, starting from a Scene, and Stop; and a real phone
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
  artifact. Unit tests and offscreen snapshots (`uiTest`) open no window and run anywhere.
- Never recompile `:creator` while the user has `:creator:run` up.
- The integration test, which builds but opens no window: `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=0.8.1-creator
  ./gradlew :creator:template:test --tests "*ProjectTemplateIntegrationTest*" --rerun` (after `./gradlew :publishCreatorLibraries
  -Pcreator.localKubriko=true`, since 0.8.1 isn't released; `everyTargetBuilds` also needs the Android SDK and Xcode).
