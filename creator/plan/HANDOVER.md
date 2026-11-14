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
  its list once, the `SpriteManager.newInstance(imageLoader, …)` overload, and session protocol 4 (`PressKeySteps`, `CaptureActors`),
  which `press_keys` steps and `take_screenshot`'s `actorType` use. It also has `KeyboardInputManager.isKeyPressed` no
  longer boxing the `Key` (75aa099c6), which Move with keys calls every frame.
- `README.md` → Roadmap is in dependency order as of 2026-11-09: every file depends only on the ones before it, so work goes
  file by file from `01`. Leftovers that waited on later work were moved into the file they wait for, and what waits on something
  outside this repository is under its file's Waiting on others (ACP protocol 2, the Kotlin SDK's `systemPrompt`, Codex's quota
  until Nov 21, 2026, and the registry's icons, all in `ai-assistant.md`).

## Next

Go on with `01-blocks.md`, at More for Manager types (Reacting to Actors). Simple behavior's last buildable item, Steering
with keys, is done (2026-11-14): Move with keys (`BlockDefinition.MoveWithKeys`, `SteeringKeys`) sets the velocity every
frame from the arrow keys, WASD, or both, in four or eight directions, through `velocityFromKeys` (reserved by format
version 11), with the call checking only the Block's own keys. It works only in Every frame Events, and its speed is never
random. What is left of that section, the shooter without Kotlin, needs a live eval run, which spends a real agent's
requests, so it waits until one can be spent (it is the section's done check, not something to build).

`/code-review` ran on it and every finding was fixed in commits of their own: the keyboard plugin's boxing (above);
and a random speed refused, the keys checked at the call, plugins decided by one rule for what code is generated
(`GeneratedCode`, which the generators, `PluginUse`, and `PluginManagers` share), and Property names only Actor classes
use (`look`, `opacity`, the Animate and movement members, `com`) free in Manager types, so migrations no longer rename
them there (098e4ebb4). The system prompt stays under 42,000 characters by shorter Block descriptions; the tool budgets
went to 24,250 and 4,175 tokens for the new Block's name. The integration test built every template after the fixes.

Not verified in the real app yet (all covered by unit and offscreen tests, none by a person or a live agent):

- The sprite editor in the real app: drawing with each tool and both mouse buttons, the selection with copy, cut, and
  paste, the Image menu, Save and the question on leaving (switching sprites, closing the project, and quitting), the frame
  strip's dragging, the onion skin, the preview, pinching on a trackpad, and a drawn sprite reaching the playing game; and
  the Assistant drawing a sprite with `create_sprite`, `draw_pixels`, and `edit_sprite_frames` (the alienshooter
  conversation that asked for a ship).
- A sprite sheet playing in a real game, and the Frames dialog and the playing thumbnail in the real app: an end-to-end
  scenario under Xvfb could cut the Breakout ball's sprite into frames through `set_sprite_sheet` and check by pixels, in
  two `take_screenshot`s with an `actorType` crop, that its frame changes.
- Adding sprites, sounds, and music by dropping them from Finder onto the sidebar, through the file chooser, and by pasting
  a copied image or copied files; and the game playing one added that way (a Compose resource accessor appears only once the
  game builds, which Play does).
- The live writers' room scenario: `KUBRIKO_CREATOR_CLAUDE_AGENT=true ./gradlew :creator:ai:test --tests "*AssistantLiveScenariosTest.aPitchInTheWritersRoom*"`
  (it spends a real Claude subscription's requests, up to an hour), then tune `AssistantSystemPrompt.WRITERS_ROOM` by what it does.
- A project created from an idea, through to the room's first questions, in the real app (an end-to-end scenario could cover
  it under Xvfb, but it needs a signed-in agent).
- Pasting an image from the system clipboard, and dropping one from Finder, onto a page.
- `take_screenshot`'s `actorType` crop and `press_keys` steps on a real game (`ActorImageRegion` places the viewport in
  the window by its size and alignment, which assumes nothing but a letterbox surrounds it, as in the templates); an
  end-to-end scenario under Xvfb could check the crop by pixels.
- A real agent other than Claude Agent switched to its read-only mode, and one that edits without asking being marked
  (Codex's `read-only` sandbox once it has quota again, after Nov 21, 2026).
- Google Antigravity's tools turned off through Creator itself rather than a hand-written ACP client (the same `_meta`;
  the binary Creator downloaded is in the settings folder's `tools/agents`).
- Codex started as a `codex-work` alias's account signing in to that `CODEX_HOME` (running `codex-acp` fetched from npm wasn't
  allowed in this session; it is listed under Codex's profile in `ai-assistant.md`).
- No live run has tried the agents' standing job of keeping the project organized in folders (`McpToolServer.INSTRUCTIONS`).
- Find usages, quick open, and the Search panel are covered by offscreen UI tests of the whole workspace
  (`WorkspaceScreenTest`), but no person has used them in the real app yet.
- Searching conversations against a real agent: continuing a found conversation through `session/load` and scrolling to
  the message once its history arrives (`jumpIndexOf`) is tested with transcripts written by hand, not a replayed one.
- The number value field in the real app (switching kinds, typing a range, choosing a Property or an edge), and a played
  game drawing random numbers and placing an Actor at an edge (the integration test compiles every kind, but nothing runs
  one); an end-to-end scenario under Xvfb could add a Brick at the right edge and check by pixels where it lands.
- The eval set's `bat.json` recording (`EvalRecordings`) was made before format 7: its Bat stops at an edge with an edge
  Event but doesn't stay inside, so in the replayed game it can slide off screen once stopped there. Record it again
  (`EvalSetLiveTest` with `KUBRIKO_CREATOR_EVAL_RECORD=true`) when a live run can be spent, and consider checking in
  its request that the Bat stays inside.
- A played game steered with Move with keys (the integration test compiles it, but nothing presses a key in one); an
  end-to-end scenario under Xvfb could give the Breakout paddle a Move with keys and check by pixels, with `press_keys`
  steps, that holding the right arrow moves it right and releasing it stops it.
- A played game running Animate Blocks (the integration test compiles every value and easing, but nothing runs one); an
  end-to-end scenario under Xvfb could add a Brick that animates its opacity from 100 to 0 over a second, removing it at the
  end, and check by pixels that it fades and is gone.
- A played game changing an Actor's color and opacity, and a sprite that isn't tinted fading (the integration test compiles
  every Block, but nothing runs one); an end-to-end scenario under Xvfb could add a Brick with Set the opacity to 50 and
  check by pixels that it is half as strong. The Scene editor draws every Actor opaque, since the opacity is only the
  playing game's.
- A played game moving Actors in a direction (the integration test compiles every kind of value, but nothing runs one);
  an end-to-end scenario under Xvfb could add a Brick moving at 90 degrees and check by pixels that it moved down.
- A played game with Actors leaving the Scene and staying inside it (the integration test compiles every combination, but
  nothing runs one); an end-to-end scenario under Xvfb could add a Brick moving left from past the right edge with a
  When it leaves the Scene → Remove this Actor, and check by pixels that it crosses and is gone.
- The eval set has no request using `find_usages` or `search_project`, nor the new resource tools, `move_folder`, `set_sprite_sheet`, the animation Blocks, Move in a direction, the color and opacity Blocks, Animate, Move with keys, or the drawing
  tools yet; record one (`EvalSetLiveTest` with
  `KUBRIKO_CREATOR_EVAL_RECORD=true`) once a live run can be spent on it.

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
