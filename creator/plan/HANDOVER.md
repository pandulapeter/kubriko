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
  until Nov 21, 2026, and the registry's icons, all in `02-ai-assistant.md`).

## Next

Continue `01-known-issues.md` at its first item (Plain and batched drawing compared in Creator, under Missing checks), then `02`.

This session works through Mistakes and failures in the first hour, each item reviewed with `/code-review` and its findings
fixed in commits of their own:

- An unreadable `.kubriko/project.json` is named with what is wrong in it (`ProjectSettingsReader`: empty, not JSON with the
  line of the mistake, not an object, or a value missing, of the wrong kind, or too large), in plain words rather than the
  parser's, through `ProjectProblem`'s `PROJECT_FILE_*` entries and `ProjectInspectionException`'s arguments. `ProjectFormat`
  parses it through the same reader, so the two never disagree about a file; while the project is open, a broken one is
  reported in the console. A newer format's project whose settings this Creator can't read opens without its types, since
  browsing needs none. `UnreadableProjectFileException` is an `IOException`, so what already gave up on an unreadable file
  still does. Opening reads the small file twice (the format, then the store), more only while migrating.
- A creation that fails comes back to the whole form (`AppScreen.NewProject.form`; the request would lose the template
  chosen before an idea), saying why: `CreationFailure` tells no network, a full disk, and a refused folder from the
  causes (by class name, since the Gradle daemon's arrive as placeholders), blames a folder only when a path names it,
  and otherwise names the root cause. `KubrikoAvailability.lookUp` tells a missing version, no network, and an error
  answer (a proxy) apart; `invoke` still says only whether it was found. Writability is checked first with
  `Files.isWritable`, skipped when no folder above the location exists (an unplugged drive).
- Creating a project can be cancelled (Cancel on its progress screen, back to the form). `ProjectCreator` stays blocking
  and is cancelled by interrupting its thread (`runInterruptible`); `ProjectGenerator` checks between files and steps and
  removes what it wrote. The wrapper download waits through `ToolingOutcome` (`:creator:gradle-session`, which
  `GradleProjectConnection` uses too), at most 30 seconds after a cancel; past that the connection is left open, since
  closing it would wait too. Each operation waits for the one before it (a cancelled creation may still be removing the
  folder the next writes), and quitting waits for both through `AppController.allOf`, which belongs to no scope.
- Rejections name one reason each, in the editor's words: Events and Blocks by their titles (`english` adds their IDs in
  brackets for the AI), each `BlockKind` and `EventType` carrying the `PlacementProblem` that says why a Manager type
  or another Event can't have it; names (types, Properties, Timers, Scenes, folders, pages, documents, Actor IDs,
  packages, and application IDs) through `NameFlaw`, which tells a letter beyond a to z or an accent, a Kotlin word, an
  empty part, a bad first character, and any other character apart, by code point. A project's name may hold any
  letters (`Kalandjáték`); its folder, Gradle name, and package are folded to a to z (`kalandjatek`), which the
  integration test now builds. Declined from the review: packaging a game whose name has letters some installers'
  code pages lack, since a game name with any letters could already be set in Project settings; it stays on Before a
  release.
- The git panel's commit form comes first and stays in sight while the changes and the history scroll under it, taking
  at most half of the panel's height (`atMostHalfTheHeight`, scrolling within it in a short window; the message field
  scrolls past five lines or so), and its button counts the changes. The changes and the history each have a sticky
  heading, and their rows are keyed (`changeKeyOf` by title, since a type's paths change as its code is generated;
  `commitKeyOf` by the abbreviated ID and the time). Declined from the review: duplicate change keys, since
  `JGitRepository.status` lists each path once (`distinctBy { it.path }`).
- An Actor the user's undo or redo brings back is selected in the Scene editor, as a placed one is: `RestoreActor` and
  `RestoreActors` (and a composite holding them) report it in `Applied.broughtBack`, `CommandExecutor.broughtBack`
  announces it for `undo` and `redo` only (an AI turn's undo leaves the selection alone), inside the change lock after
  the Scenes are delivered, and `SceneSelection` selects it once the Scene shows it, dropping it if the next load
  doesn't or it is the Scene settings. Declined from the review: redoing a placement not selecting, since its redo
  applies `RestoreActor` (`redoingAPlacementTellsWhichActorItBroughtBack`).
- The sound and music menus (the Blocks' and the Scene settings') say how to add files when the project has none, and
  that Kubriko Creator hasn't read the project's files while `AudioFiles.ofScanned` is null (before the first scan, or
  after one failed), through `SearchableChoiceButton`'s new `hint`, shown below the options and kept in sight below a
  long list.

The last session finished every item of Costs that grow with the project, each reviewed with `/code-review` and its findings fixed in commits of
their own:

- The type editor no longer rebuilds the project's family, scope, and inheritance per keystroke (`TypeEditorContextCache`,
  keyed on each type's `TypeOutline`; the sidebar's groups on `SidebarType`). `CreatorBenchmarkTest` times it: selecting a
  member of the wide family about 1.4 ms, ten keystrokes about 1 ms together. The cache lives only while a type editor is shown.
- A Property is renamed once its name is done (Enter, leaving the field, or the field going away), validated first
  through `dispatchIfValid`, so a refused rename says why on Enter and reverts when left; each rename is its own undo
  step (`EditProperty.mergeKey` merges only typed initial values, and `WriteCommand.mergesWith`/`mergeReverts` are gone).
  A Scene folder's Scenes are renamed as one batch (`renameScenes`), going through the types, the first Scene setting,
  and the Kotlin once.
- Reordering or restoring Actors keeps the ones in front of the first change of order and creates only the rest again
  (`ScenePatch`). `SceneView` tracks the order Kubriko draws its Actors in (`drawnRanks`), so a reorder also takes in a
  changed Actor drawn out of order before it, and the picking grid is built again whenever an index changes
  (`ScenePatch.hasNewIndices`). Re-adding the same Actors instead isn't safe: `ActorManager` may split the removal and
  the addition into two batches, disposing Actors that stay. A cancelled load disposes what it created before it
  returns (`createEachDisposingIfCancelled`), so the next load waits for it; that `ActorManager` disposes replaced Actors
  on its own thread while the next load creates others is accepted (the user, 2026-11-09), since games live with the
  same and Principle 4 rules out an engine change for it.
- A game logging every frame no longer floods the UI thread: `ConsoleLog` adds a line without copying (a window over an
  array later lines only go after) and publishes `ConsoleLines` (a revision, the first line's number, the game's first
  logged line since its session started, and the latest problem, all kept as lines arrive). The console follows new
  lines while it shows the last one (one collector; the user's own scrolling stops it), keys lines by number, and opens
  by itself for what the game logs only at its first line per session (`ConsoleOpening`).
- The Scene settings' music and a type's further children are `SearchableChoiceButton`s (music through `AudioFileChoice`,
  which can offer none). A Scene's music that is gone is a missing reference (`sceneMusic`), and no audio file counts as
  missing before the project is scanned (`AudioFiles.ofScanned`).
- The sidebar's trees (folder groups, documents, and Kotlin files) are worked out in a `derivedStateOf` (`SidebarRows`)
  for the modules shown open, each tree keeping its rows while what it is made of is equal, and an AI step's flash
  reaches only the rows (`SidebarFlash`), so a selection or a flash works out nothing again and opening a folder only
  its own tree.

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
  artifact. Unit tests and offscreen snapshots (`uiTest`) open no window and run anywhere.
- Never recompile `:creator` while the user has `:creator:run` up.
- The integration test, which builds but opens no window: `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=0.8.1-creator
  ./gradlew :creator:template:test --tests "*ProjectTemplateIntegrationTest*" --rerun` (after `./gradlew :publishCreatorLibraries
  -Pcreator.localKubriko=true`, since 0.8.1 isn't released; `everyTargetBuilds` also needs the Android SDK and Xcode).
