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

Where the next session starts: what to do next and what still has to be checked by hand. Each session rewrites it before it ends.
What exists is in `../CLAUDE.md`, what is planned in the other files here.

## Status

- Ask before every push (`git log origin/main..HEAD` shows what would go). Everything up to 2026-11-01 evening is pushed.
- `library.version` is 0.8.1, unreleased (0.8.0 is on Maven Central). It holds library changes Creator uses: `Logger.log` building
  its list once, and the `SpriteManager.newInstance(imageLoader, …)` overload.
- The user wants every item of `01-large-projects.md` to `05-reuse-and-traits.md` done, with nothing capped in number.
- Creator is AI-centric. `02-ai-assistant.md` is built (2026-11-01) except what needs a real session or someone else's work:
  Codex's profile (Codex is out of quota until Nov 21, 2026), ACP protocol 2 and its `systemPrompt` field (not final, not in the
  Kotlin SDK), Actors' states for the AI (needs `09-debugger.md`), the registry's icons (each vendor's trademark rules), and the
  smaller follow-ups the file lists.
- User decisions to keep: Claude Agent is the one fully supported agent; every other ACP Registry agent runs on generic fallbacks
  with as little per-agent code as possible, and vendors' terms don't gate an agent (no "ask the vendor" steps). The AI connection
  and the assistant are one AI panel; the map ("What the AI knows about your game") stays behind a button in it, not on the toolbar.

## Next

`03-settings.md` (Creator settings with the switch that turns every AI feature off, and Project settings), then `04` to `07` in
order. What is left in `01-large-projects.md` waits for other plans (Traits, UI elements, git) or for the benchmarks.
The AI switch's test can walk the AI panel's parts offscreen (`uiTest`) with it off and find no AI string; the AI
surfaces today are `AiPanel` (and its toolbar button), `AiActivityBar` with the AI following and flashing (`ui/AiFollowing.kt`),
`McpController`, and `AssistantController`.

## How to check work

- `./gradlew :creator:test`: unit tests, also on Windows and Linux in the `Tests` workflow (the Windows job since 2026-11-01).
  Run the whole suite before every commit, not only the tests you touched: tests elsewhere catch leaks and races (a server
  left running, an exception escaping a scope), and Windows paths differ (write expected paths with `File`, not `/`).
- **UI offscreen**: `uiTest { showing { … }; saveSnapshot("name") }` (`ui/UiSnapshots.kt`) renders any composable in the app's theme
  and scale without a window; assert texts and clicks, then look at `creator/build/ui-snapshots/name.png`. Every new UI gets one.
  Use the real app (below) only for what needs windows, focus, or a live agent.
- **Live agent**: `KUBRIKO_CREATOR_CLAUDE_AGENT=true KUBRIKO_CREATOR_CLAUDE_CONFIG_DIR=$HOME/.claude-personal ./gradlew :creator:test
  --tests "*AssistantLiveScenariosTest*" --rerun` holds real Claude Agent conversations on the Breakout fixture (a change and its
  Undo, a read-only question, resuming after a restart, health checks and `check_change`); a few requests on the user's
  subscription, so run it when the assistant's behavior changed. `EvalSetTest` replays recorded tool calls for free.
- **Templates for real**: the integration test in `../CLAUDE.md` → Running and testing builds every template.

## To check by hand

**Stability (built and unit-tested on 2026-11-01, not yet seen in the app)**
- Format version 1 on the scratch Breakout copy: opening it records `"formatVersion": 1` and `minimumCreatorVersion`, and Kotlin
  Blocks or Kotlin UI that named another Manager type's field (`score.value`) now say `scoreManager.value` and build. A Manager
  type called `Viewport`, `KeyboardInput`, or `Sprite` is reported as a reserved name.
- A scratch copy whose `.kubriko/project.json` says `"formatVersion": 99, "minimumCreatorVersion": "9.0.0"`, at opening and set
  while open: the red notice names 9.0.0, every change (an inspector edit, a palette drop, the AI's tools, undo) is rejected in the
  console with the same text, no code is regenerated and no upgrade is offered, and building and playing still work. Setting it back
  to 1 lifts it without reopening.
- Undo: delete `Bat`, then a type whose collision Event names it, and undo both; rename a Property by typing, then undo (one step
  back to the old name), and edit two Properties one after another (two steps). Discard a change in the version control panel,
  then make one at once (the old definition must not come back), and see the undo history emptied.
- A hand-written Manager whose constructor throws, opened in a Scene: the Scene shows the failure with the exception (no crash), and
  for a swap after a change, the old classes stay as outdated. An exception nothing catches shows "Kubriko Creator ran into a problem"
  in the console.
- Switching projects many times: the temporary `kubriko-creator-classes` folders go a few seconds after each close.
- A Scene file broken by hand (not JSON, and an Actor whose state is `[1]`): the red notice with Open in the system editor; an Actor
  of a type that no longer exists: marked in the Actor list, explained in the inspector, removable. Dragging a hand-written Actor
  without a saved position pans instead.
- The recent projects list with a project on an unplugged drive (greyed, Forget works, back when plugged in).
- On Windows: playing with a long user folder (the classpath `@argfile`), and renaming a Scene (and its folder) while the game has
  it open: refused in the console, nothing renamed.
- A broken `.kubriko/actors/*.json` in the scratch copy, at opening and while open: the console names the file and why, the
  other types load, building and playing still work (on the code generated before), closing the window works, and fixing the
  file says the code is up to date again and regenerates it.
- Creating or renaming a type to the name of an unreadable file (rejected as taken), and a type file renamed in the file manager,
  one copied into another folder, and one whose name was edited to `my enemy` (each reported, nothing deleted).
- A type deleted by a git checkout (or in the file manager) while the project is open and while it is closed: its
  `generated/<Name>.kt` goes, the console says so, and the build works; the same with that file edited by hand first (kept,
  named in the console as an error once).
- Crash-safe writes on Windows (a Scene the running game holds open, which a move may be refused for) and on a network drive.

**Context for large games (built and unit-tested on 2026-11-01, not yet seen in the app)**
- The map (the AI panel's map button) on the scratch Breakout copy: types by section with descriptions and references, Scenes with what they place, a
  click opening each, and it following a change (an inspector edit, a Scene opened, an AI step).
- The description field under a type's header and, with nothing selected, in the Scene inspector: typing (one undo step), a
  description set by the AI showing up, and `.kubriko/scenes.json` after renaming and deleting a Scene (and undoing that).
- A real conversation: whether it uses the digest it starts with rather than reading everything, reads `detail` outline then one
  Event, `get_relations`, and whether a repeated read's "unchanged" or delta (and `full: true`) confuses it. Change something by
  hand between two prompts and see whether the next answer knows (the summary goes before the message, not in the transcript).
- Write tools now return `{"revision", "changed", "event"}` instead of "Done.": whether the AI stops reading the type again after
  each Block it adds.
- The Block tools without the Block schema: whether the AI calls `get_block_catalog` before its first Block rather than guessing
  properties, and whether a rejection such as "There is no property called 'speed'." gets it right on the next try.

**The rest of the assistant plan (built and unit-tested on 2026-11-01, not yet seen in the app)**
- The scratch Breakout copy now offers the template version 8 upgrade (Game.kt adds its instance to `KubrikoWiring.runningInstances`,
  GameEntryPoint reports them): review it, apply it, and see `start_game` wait for drawn Actors.
- Whether the agent reads what a chip links to (the chips themselves are checked offscreen, `AssistantPanelPartsTest`).
- Undo of a real turn after a change of your own (a plain Undo is covered by `AssistantLiveScenariosTest`).
- Closing a conversation and reopening it from the conversation menu (resuming after a restart is covered by `AssistantLiveScenariosTest`).
- Whether Claude Agent sends usage reports at all (the usage line is checked offscreen).
- On a machine without Node.js (a scratch `PATH`): Download Node.js for Kubriko Creator, its progress, and Claude Agent starting
  with it.
- An agent offering a sign-in by environment variables (Google Antigravity's "Gemini API key" if its method is that kind): the form,
  and the agent starting with them.
- `get_kubriko_api` (with and without a class), `read_game_notes` and `set_game_notes` from a real conversation, and whether Claude
  uses the `game-explorer` subagent and plans big requests first (`get_health_checks` and `check_change` are covered live).
- Two MCP clients at once (Claude Code and the assistant): the activity bar naming each step's client.

**Choosing an agent (built and unit-tested on 2026-11-01, not yet seen in the app)**
- The agent menu: Claude Agent first and the registry's others marked untested, the list there offline after one start (from
  `agent-registry.json`), and a custom agent entered (`goose acp` or similar) and remembered across restarts.
- Every text naming the agent (the empty conversation, Starting, failures, the sign-in card, the account menu), and the Claude
  Code configuration menu and API key hidden for other agents.
- Google Antigravity: the download into `tools/agents/antigravity-acp/1.4.5` (checked against its SHA-256), its program starting,
  and a second start not downloading again; an npx agent other than Claude (Codex) and a uvx one without uv (Install uv).

**Holding up and fallbacks (built and unit-tested on 2026-11-01, not yet seen in the app)**
- A long streaming reply (ask for a 300-line explanation): smooth while it streams, scrolling up stays put while it goes on, and
  scrolling back to the end follows it again; Markdown (lists, code fences) right at every moment.
- Killing the agent's `node` process mid-reply: "Claude Agent stopped while answering." in the transcript, and the next message
  works in the same conversation.
- The AI connection panel's switch and port toggled quickly: one server left, on the last port; the call log scrolling smoothly
  after many `read_scene` calls.
- A failure's details under its sentence: the client row's output after a failed Connect, and an agent that can't start.
- On a real Windows machine: the assistant finding `npx.cmd` and starting Claude Agent through `cmd /c`, and Stop ending `node`
  and `claude` too (the unit tests run on Windows in CI, but no agent does).
- Claude Agent unchanged by the profiles: its `_meta`, HTTP tools, and permission requests as before (`ClaudeAgentLiveTest`).

**Tool call cards (built and unit-tested on 2026-11-01, not yet seen in the app)**
- In a real conversation, each Creator tool call card names what it worked on (`Ball, Event 2`), clicking one about a type or a
  Scene goes there and lights it up again (also when clicked twice), and a new AI step takes the light back. This relies on Claude
  Code sending `_meta["claudecode/toolUseId"]` (found in the bundled `claude` binary) matching the adapter's tool call ID; if
  cards show no target, the ordered fallback is in use or the IDs differ.

**The assistant panel (seen working on 2026-10-29: a change followed in the editor and undone, Stop, Tab completion, Effort,
several conversations, the default and alias configurations, and closing the project mid-turn)**
- A permission request inline (Claude Agent rarely asks, since its editing tools are off; a mode such as Manual may).
- A machine without Node.js (the Install Node.js link), and the first run on a machine where `npx` still has to download the adapter (how long "Starting" lasts).
- Whether answers now name Events and Blocks as the editor does ("Set velocity", not `setVelocity`), after the system prompt asked.

**Signing in (seen in part on 2026-10-29, see Next)**
- A real sign-in to completion: the browser flow, a pasted code, the prompt refused before it sent again by itself, and the
  account menu changing after that prompt. Use a scratch `CLAUDE_CONFIG_DIR` (Creator inherits it for the default configuration),
  never the user's own.
- A real API key: saved to the keychain, the agent starting again with it and reporting "Anthropic API key", billing, and Stop
  using the API key going back to the login. The macOS keychain may ask for access the first time.
- Linux (`secret-tool`) and Windows (the Credential Locker through PowerShell) secret stores, untested on those systems.
- The text field's outline is hard to see on the cards' background.

**Large projects (built and unit-tested on 2026-10-29, not yet seen in the app)**
- The paged `read_scene` (summary, type, rectangle), `list_actor_types` by folder, and the batch tools (`place_actors`,
  `remove_actors`, `restore_actors`, `set_actors_values`, `set_actors_state`) from a real AI session, say "fill the top of the
  Scene with a grid of bricks, then make every second one golden": one undo step each, and the open Scene showing the result.
- The Timer menu in a timerFinished Event's header and in the Timer Blocks: choosing, New Timer… typed in place (a bad name shows
  why), Rename this Timer… following through a family, and an old project's numbered Timers showing as `timer1`.
- The key picker (`KeyChoiceButton`): its popup's search, the list scrolling through all 296 keys, choosing by pressing a key
  (Shift, F-keys, the numpad), and an old project's `left`/`space` keys loading.
- The Scene editor's Actors button and list (filtering, selecting, centering, following a canvas selection), and the
  inspector's send-to-back and bring-to-front buttons.
- Getting there: the live check stopped on a Gradle 9.7.1 daemon started from a sandboxed shell, which can't run `uname`, so
  Creator's project model fails with "couldn't open this folder as a Gradle project". Start Creator after `./gradlew --stop` in
  the scratch copy (outside the sandbox).

Each of these is built and unit-tested, but wasn't seen in the running app.

**Playing**
- Hot reload: a hand-written Kotlin change (a Composable in `Game.kt`); a Scene, sprite, or sound changed while playing (Hot
  Reload's copy holds the resources too, so whether a reload brings them is open); a Manager type's Property added (should
  restart); a JetBrains Runtime that Gradle has to download first (the first Play waits with only "Building…"); the AI's
  `start_game` after a restart, and with a `scene`.
- A game that crashes (its exit code and last lines in the console), audio and sprites in a played game, a project whose `run`
  task has no Java toolchain (should fall back to Gradle), and Windows paths.
- A game played with the configuration cache on, the version 5 and 6 upgrades' diffs in the dialog, and a Log Block's line
  reaching the console after the version 5 upgrade.
- Hearing the audio: music and sounds from Blocks, music pausing and resuming with the window's focus, a non-looping music played
  again, going between Scenes with the same and different music, and web audio with `Res.getUri` paths. The audio Blocks' file
  menus with no sounds or music in the project (empty, with no hint how to add files).
- A ball bouncing exactly at the bounds' edge, going to another Scene with different (or no) settings, and Playing the scratch
  Breakout with GoldBrick placed.

**Builds**
- Opening a Scene, or pressing Play, while the build at opening still runs (should wait for it, not build twice), and a project
  whose build fails on open (the problems in the console right away, the first Scene showing the failure without building again).
- A change made while the compiler warm-up runs (should wait for it), and a project whose warm-up fails (nothing should show).
- Incremental common code: Reload taking a full build, and a broken Kotlin Block's failed build followed by a clean one.
- Quitting with a change made less than 100 ms before (it should be written).

**The Scene editor**
- Swapping classes: a change made during that build (should build once more after it), closing the Scene, opening another one,
  or Reload during it, and a Scene of many Actors (whether the old one stays smooth while the new one loads).
- A sprite type's preview; a sprite and a Scene's music still found from the class folders; a Scene kept open (outdated) while a
  type change builds.
- A Scene swapped in while Creator is in the background (should no longer stay empty), and what drawing every frame there costs.
- After an inspector edit (a replaced Actor), placing and removing Actors (`ScenePatch`), and a Scene changed by git while open.
- Clicking Actors, selecting right after placing, removing, and undoing (the same Actor back, selected), a click after a drag, and
  an old Scene file gaining IDs on its first edit.
- The inspector's Property fields and color, the Scene settings (adding them from the hint, music, camera start, which shouldn't
  move the editor's camera), scale and size (whether the pivot moving to the middle surprises anyone), and that typing a number
  is one undo step.
- A hand-written Actor type in a Scene (drawn, selectable, movable), an Actor without a `BoxBody`, a Scene whose Actors use a
  Manager, a paused game, zoom to fit with and without Scene settings, the middle-button pan, scroll pan on a mouse wheel,
  dropping onto a zoomed-out view, the version notice, and the outdated notice after a type change followed by Reload.

**The type editor and the sidebar**
- Dragging a Block by its handle up past the top edge (auto-scroll upwards), a Block or Event flashing after undoing its removal,
  typing into a just-added Block (the field should keep the focus), and a palette drag adding its Block twice.
- Deselecting by clicking the selected type or Scene again, or empty space (a Kotlin file row counts as empty space).
- Parents: the inherited Properties and Events, replacing an inherited Event (it starts with runParentEvent), the parent menu
  leaving out the type's own descendants, deleting a parent with children (rejected in the console), and a child of another
  shape colliding.
- Folders: renaming a Scene folder that holds the open Scene, and dragging a Manager type into a folder.
- Sprites: a non-square or translucent image, a `.webp` or `.jpg` (its size isn't read when ImageIO can't), deleting the image
  while a type uses it (the game should build and draw nothing), undo and redo of choosing a sprite, the sprite menu's hint with
  no images, and a game window opened before the template upgrade (an older `Game.kt` has no `KubrikoWiring`, so
  `kubriko.get<SpriteManager>()` would throw).
- A rename following Kotlin code from the UI. Adding a Property named like another Manager type's field (`lives` beside a `Lives`
  type) moves that field to `livesManager`, and Kotlin code isn't updated for that.
- The go to a Scene card's Scene menu, the "score of Game" Property menu in an Actor's Block, the New Scene field, and undo and redo.

**Project files**
- The dependency sync and the version 4 upgrade on the real Breakout copy (on `0.7.5-creator` and template version 3): opening it
  should raise it to Creator's version, offer the upgrade, and after it drop nothing it uses. Then remove the last collision Event,
  watch `plugin-collision` leave the build, and undo it.
- A project whose sources import a plugin by hand (say physics) keeping it; a dependency added by hand before any code uses it is
  removed on the next regeneration, which the user may not expect.

**AI connections**
- `set_scene_settings`, and the Manager type, text, and upgrade tools from a real AI session ("stop the paddle on key release and
  show a score"), watching the activity view pace a burst of calls.
- Connect for the plain "Claude Code" row in the Claude app, Claude Desktop from a real chat (writing its config needs the user),
  and Connect and Disconnect against a real config.
- Codex (out of quota until Nov 21, 2026), Cursor, Windows, and fish aliases with a real fish.

## Working with the live app

- **Guard every synthetic click and keystroke.** The user works alongside, and Android Studio covers the same screen area. Click only
  when the topmost window under the point belongs to Creator's PID (`CGWindowListCopyWindowInfo` in a Swift script), and press keys
  only while the target app is frontmost. Prefer editing `.kubriko/*.json` on disk, which Creator reloads.
- Test on a fresh scratch copy of `~/Projects/breakout` (it has the user's uncommitted work), opened with `:creator:run --args=<dir>`;
  the copy gets `0.8.1-creator` and the Maven Local block on its own. Leave the real one, which shows the upgrade notice, to the user.
- Never recompile `:creator` while `:creator:run` is up, and restart it with the publish tasks after `tools/creator-session` changes.
- The integration test: `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=0.8.1-creator ./gradlew :creator:test --tests
  "*ProjectTemplateIntegrationTest*" --rerun` (after a `:creator:run` or `publishToMavenLocal` with `-Pcreator.localKubriko=true`,
  since 0.8.1 isn't released).
