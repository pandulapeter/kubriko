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

First check that the nightly end-to-end job's web step passes: it draws Breakout's texts only since `WebLocaleUpgrade`
(below), and runs under the POSIX locale that broke them. Then continue `04-screenplay.md` with the Documents group, then mentions and images (its Order), then the AI's side of pages, keeping them up to date, and the
writers' room. Pages, their commands and tools, the Screenplay group, and the page editor exist (`0954d5ef2..f82ba8008`, not
reviewed in `6c082fbff` and `52083d116`). What is left of its "Deleting what is referred to" comes with each kind's own delete (sprites, sounds, music,
Timers, folders with contents, pages) and with renames reading `ProjectReferences`. After `04`, `05-settings.md`'s Release, Icons,
and Signing, each extending the Project settings dialog and its tools, after which `03`'s iOS devices can follow Signing. `03`'s
new iOS simulator dialog (Setting up emulators and simulators) is small and waits for nothing, so it can go in between; its
Android emulator dialog waits for `15-onboarding.md`'s download of the SDK's command-line tools. `06-batched-rendering.md`'s
engine and sprite atlas parts touch only `engine/` and `plugins/sprites`, so they can run beside `04` and `05` in a session of
their own; its Creator part follows them. Then `06` to `10` in order. What is left in `01-large-projects.md` waits for other
plans (Traits, UI elements, git) or for the benchmarks.

What the last session did (the web font and its review fixes, `181108f26` and `566b80b6e`, are taken back by the commit
that replaces them; the rest reviewed only by its own tests and snapshots):

- **Texts on the web** (template version 12, `WebLocaleUpgrade`): the end-to-end images showed no texts in the browser
  because a Chrome started under the POSIX locale (CI's) reports its language as `en-US@posix`, which isn't a language tag,
  and Compose for Web then throws in every text's composition. Found by loading the build in a Linux container (colima on
  this Mac, `creator/build/linux-probe` held the Playwright probe); Compose for Web 1.12 has a font of its own, so the
  bundled font first tried fixed nothing and was taken out with its binary-copy machinery. The web `Main.kt` now hands
  Compose the languages without such a modifier before anything is drawn. `WebGameIntegrationTest` makes the page report
  `en-US@posix` (`HeadlessChrome.reportLanguages`), so it fails without the guard on any machine, and `HeadlessChrome`
  reaches no host but this computer.
- **Play waits for builds** (`8b0176507`): Play is disabled with a spinner while a build the console reports runs
  (`CreatorTaskRunner.runningBuilds`, not the warm-up or a session's own build).
- **Configure platforms** (`c4e3611dc`): the Play menu's link to the Targets page, which still fits once every platform is on.
- **Menus and choice lists** (`d57a966da`): as tall as the window, fading out at an edge with more beyond it
  (`scrollEdgeFades`).
- **A flaky Android player test** (`a2bf3d512`): `builtAlongside` starts the build before the device check.
- **Started, not committed:** `04-screenplay.md`'s Documents group, as `ResourceGroup.DOCUMENTS` (scanned and watched like
  the other resource groups, `ProjectChange.DOCUMENTS`), a `DocumentStore` in `CommandContext` (through `ScannedFiles`, which
  keeps its constructor at twelve parameters), and its title and icon, uncommitted in the working tree. Next:
  its commands (`read_documents`, `edit_document`, `add_document_to_screenplay`, whose undo needs an internal command moving
  the page back), opening one in the page editor (`WorkspaceSelection.Document`), its sidebar rows (clickable, with Add to
  the screenplay behind a confirmation), and `SidebarModuleTest`'s expected groups.
- A physical iPhone isn't offered by Play yet: that is `03`'s iOS devices, after `05`'s Signing.

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
