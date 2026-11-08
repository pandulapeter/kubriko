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

`04-screenplay.md` is done except what waits for other work: each remaining kind's delete (sprites, sounds, music, Timers,
folders with contents) comes with that kind's own delete, a deleted sprite's page images with sprite deletes, and the live
writers' room scenario is written but not run (below). Next is `05-settings.md`'s Release, Icons, and Signing, each extending
the Project settings dialog and its tools, after which `03`'s iOS devices can follow Signing. `03`'s Android emulator dialog (Setting up
emulators and simulators) waits for `15-onboarding.md`'s download of the SDK's command-line tools. `06-batched-rendering.md`'s engine and sprite atlas
parts touch only `engine/` and `plugins/sprites`, so they can run beside `05` in a session of their own; its Creator part
follows them. Then `06` to `10` in order. What is left in `01-large-projects.md` waits for other plans (Traits, UI elements,
git) or for the benchmarks.

What the last session did (`e60f4dfd3..HEAD`, unattended, every item reviewed with `/code-review` and every finding
fixed in its own commit, `:creator:allTests` green at every commit):

- **Mentions and images:** links between pages and into the project show as chips that open what they name (only a web or
  mail address opens outside, after it is shown), the `@` suggestions, pictures pasted, dropped, or chosen, scaled down and
  moved with their page, renames and moves following every mention (`withMentionsFollowed`), and "Mentioned in" on what pages
  mention.
- **The AI's side of pages:** the page index at a conversation's start, `ScreenplayStyle` holding what the AI writes (shown to
  the user only as quiet hints), `mentionedIn` on every write's result, the screenplay's health checks (as dots in the
  sidebar), the reminders and the offer to write the screenplay, and Project settings → Screenplay.
- **The writers' room:** the Blank template, "Start from an idea" on the new project form with its targets (only those this
  computer can build), `ask_user`, the room's own rules (kept when a conversation is continued, and its unanswered idea kept
  when the agent is replaced), and its ways in from the AI panel's menu and a Planned item's right-click menu.
- **The AI's changes to a page as a diff** in the page editor until its turn is kept or undone (`PageReview`), always what the
  turn's Undo would bring back.
- **Deleting what is mentioned:** a deleted type's, Scene's, or page's mentions in pages become plain text in the delete's undo
  step (documents are the user's, so theirs are left, as renames leave them); deleting a page goes through `deleteWithUsages`
  like every other delete and confirms when it would change other pages, listing them; `RenamingFollowsReferencesTest` holds
  every rename to what a delete would clean up.

Not verified in the real app yet (all covered by unit and offscreen tests, none by a person or a live agent):

- The live writers' room scenario: `KUBRIKO_CREATOR_CLAUDE_AGENT=true ./gradlew :creator:ai:test --tests "*AssistantLiveScenariosTest.aPitchInTheWritersRoom*"`
  (it spends a real Claude subscription's requests, up to an hour), then tune `AssistantSystemPrompt.WRITERS_ROOM` by what it does.
- A project created from an idea, through to the room's first questions, in the real app (an end-to-end scenario could cover
  it under Xvfb, but it needs a signed-in agent).
- Pasting an image from the system clipboard, and dropping one from Finder, onto a page.

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
  artifact. Unit tests and offscreen snapshots (`uiTest`) open no window and run anywhere.
- Never recompile `:creator` while the user has `:creator:run` up.
- The integration test, which builds but opens no window: `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=0.8.1-creator
  ./gradlew :creator:template:test --tests "*ProjectTemplateIntegrationTest*" --rerun` (after `./gradlew :publishCreatorLibraries
  -Pcreator.localKubriko=true`, since 0.8.1 isn't released; `everyTargetBuilds` also needs the Android SDK and Xcode).
