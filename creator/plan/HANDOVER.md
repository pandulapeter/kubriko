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
  Kotlin SDK), Actors' states for the AI (needs `10-debugger.md`), the registry's icons (each vendor's trademark rules), and the
  smaller follow-ups the file lists.

## Next

Start with `03-playing-on-every-target.md`, in its order: choosing where to play (Play's menu, a `target` for `play` and
`start_game`, `list_play_targets`), then the web, Android, and the iOS simulator, each shipped and reviewed on its own. Then
`04-settings.md`'s Release, Icons, and Signing, each extending the Project settings dialog and its tools, after which `03`'s iOS
devices can follow Signing. Then `05` to `08` in order. What is left in `01-large-projects.md` waits for other plans (Traits, UI
elements, git) or for the benchmarks.

What the last session did, all reviewed:

- **Project settings → Targets** (`a03a3f435..74182f7f9`): web, Android, and iOS turn on and off in the Targets section and through
  `get_target_change` / `set_target`, each as a diff confirmed by its `changeId`. Android adds a library target and an `androidApp`
  module, iOS a framework and an Xcode project in `iosApp/`, both taking the game's identity. `ProjectTemplateIntegrationTest.everyTargetBuilds`
  builds the Android APK and, through `xcodebuild`, the iOS app for the simulator, and passes locally. Its review fixed an Apply that
  could write a diff other than the one shown, catalog entries other entries still used being removed, an undo that dropped what the
  user added inside a target's blocks (undo now exists only where turning back is exact), a stale `sdk.dir`, a half-written change
  when a write fails, brackets in strings or comments ending a build block, Xcode selected outside `/Applications`, the bundle ID
  when iOS is turned on again, and two duplicated helpers. No end-to-end scenario: the integration test covers the real builds.
- **A project moved onto an unreleased Kubriko** (`b83325272`, `77f450369`, `40d79a9a9`): Creator started as `../gradlew run` from
  `creator/` skipped the development mode and rewrote the projects it opened onto Kubriko 0.8.1 without Maven Local, so
  `~/Projects/breakout` no longer built (it is repaired and builds again). Every run task, by path or by name from the root or
  `creator/`, now publishes through `:publishCreatorLibraries` (desktop, web, Android, and on a Mac iOS), and Creator never moves a
  project, or creates one, on a version Maven Local (development) or Maven Central (otherwise) doesn't have (`KubrikoAvailability`).

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
- A game with Android and iOS turned on running on a phone or tablet of each (the integration test builds both apps but never
  launches them).
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
