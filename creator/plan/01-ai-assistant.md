<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# AI assistant (next)

Claude edits Kubriko games through the same validated operations the user has, so its work stays inside the editor's constraints by
construction, shows up as readable Events and Blocks, and can be inspected, tweaked, or undone like any other change.

The command layer it builds on exists (`../CLAUDE.md` → Commands): every editor change is a validated command with an
inverse, undo and redo use them, and `CommandRegistry.editor.tools` is the tool list, with read tools (overview, Block catalog,
Actor types, Scenes, console, version control) and action tools (build, play).

## The MCP server: what's left

The server, one-click setup for Claude Code, Codex, and Gemini CLI (one row per shell alias that runs them), the game tools, and the
step-by-step view of the AI's work (activity bar, follow mode, highlights) exist (`../CLAUDE.md` → AI connection). Still to do:

- **Real sessions so far** (October 2026, headless Claude Code with Opus 5.5 on a copy of the Breakout project; Codex not yet, its
  account was out of quota): "make the Ball faster and log when the game starts, then play it" took 18 turns, 45 s, and $0.46, entirely
  through the tools. The model read before writing, placed an invisible Actor to get a "game started" Event, and checked its work with
  `start_game`, `press_keys`, `take_screenshot`, and `read_console`. "Also move with A and D, stop on key release, and show a score" took
  42 turns, 3 minutes, and $1.06. A and D went through the tools; for the rest there are no Blocks yet (key released, score, on-screen
  text: `02-blocks.md`), so it hand-wrote a Manager and edited `Game.kt` with its own file tools, and said so. What it revealed was fixed:
  the edge Event ran on every frame an Actor rested at the edge, the first key press after another app had taken the focus was lost, and
  internal errors reached the model as a bare class name. The server's instructions now ask for every change to go through the tools,
  using a Kotlin Block when no Block fits and otherwise saying what's missing; the same request then took 34 turns and $0.91 with no file
  written by hand. It still read Kubriko's sources jars with its own shell to find the API for those Kotlin Blocks, so a read tool with
  Kubriko's API (the root `CLAUDE.md`'s overview, or KDoc for a named class) would save turns. Still to do: a Codex run, and watching the
  activity view pace those bursts of calls in the window. The Gemini CLI commands come from its documentation and haven't been run yet.
- Claude Desktop and Cursor are connected through their configuration files (`../CLAUDE.md` → AI connection). `mcp-remote` was tried
  from a terminal against the server; still owed: clicking Connect for Claude Desktop and using the tools from a real chat (its log
  is `~/Library/Logs/Claude/mcp-server-kubriko-creator.log`), Cursor at all (not installed here), and Windows, where the entry
  runs plain `npx`.
- A screenshot right after `start_game` can be empty: the window is open, but the Scene's Actors aren't added yet. Waiting
  for the first frame that draws Actors (the session can ask the Kubriko instance through the entry point) would fix it.
- The game tools see the game through screenshots and logs. Reading Actors' positions and states directly needs a way into the
  running Kubriko instance, which is the debug agent's job (`04-debugger.md`); expose it as a tool once that exists.
- Windows: `ShellProcessRunner` passes commands to `cmd /c` unverified; try the one-click setup on Windows. fish's `alias name 'value'`
  listing is read as its `alias.fish` prints it, but has never been tried with a real fish.
- The activity view lights Scenes up only in the sidebar (opening one compiles the project), and numbers steps per run of
  Kubriko Creator. Naming the client (from MCP's `clientInfo`) in the bar could help when two are connected.
- Checked only through tests and screenshots, never by clicking: the Log Block's message field, the Event move buttons, the New
  Scene field, undo and redo, and the Connect and Disconnect buttons against a real (non-scratch) Claude Code configuration.

## Next: in-app assistant panel

A chat panel next to the editor, behind a small backend interface, so it can talk to Claude in either of two ways:

- **The user's API key** (the default): Anthropic's Java SDK (`com.anthropic:anthropic-java`, 3.3.0 in October 2026), called from Kotlin
  directly, with `CommandRegistry.editor.tools` as its tools. The key lives in the OS keychain (`java-keyring` covers the macOS Keychain,
  Windows Credential Manager, and the Linux Secret Service, but its last release is from 2023, so check it still works on current JDKs, or
  use JNA directly; macOS only protects the item from other apps when Creator ships as a signed app bundle with its own runtime).
- **The user's own Claude Code**, for subscription users, if Anthropic confirms it is allowed (see below): Creator runs the user's installed,
  unmodified `claude` in headless mode with streamed JSON in and out, pointed at Creator's MCP server, and shows its messages in
  the panel. The user signs in through Claude Code's own flow; Creator never sees their credentials.

**Local models** (checked October 2026): Apple's on-device model (Foundation Models, macOS 26) has a 4,096-token context (8,192
for the newer model, by one report), less than Creator's tool list alone (about 6,000 tokens), and early tool-calling reports are
mixed. Windows' Phi Silica (being replaced by Aion Instruct) needs a Copilot+ PC and a packaged app. The practical local option on
every platform is an OpenAI-compatible local server (Ollama, LM Studio, Microsoft's Foundry Local) running an open model with tool
calling: one more panel backend, offered as free and private but less capable, after the Claude ones work.

**What the terms allow** (Claude Code's [Legal and compliance](https://code.claude.com/docs/en/legal-and-compliance) page, read
October 2026): third-party apps may not offer Claude.ai login, route requests through a user's Free, Pro, or Max plan, or collect Claude.ai
credentials, and products built on the Agent SDK must use API keys. So Creator can't call the API with a subscription itself, and the Agent
SDK (which has no JVM version anyway) doesn't change that. The same page does allow an end user to sign in to the unmodified Claude Code
binary with their own subscription where a product runs it, but running Claude Code in a product requires agreeing to the Commercial Terms
of Service. Whether that applies to a free, open-source desktop app running a copy the user installed themselves is unclear: ask Anthropic
(the page points to sales) before building the second backend. MCP needs no such answer, since the user's own Claude client
makes the requests.

- Model: Claude Opus 5.5 by default, with a selector (the user pays). Streaming, adaptive thinking, prompt caching on the tool list and the
  system prompt (Kubriko's concepts plus the generated Block catalog), refusal handling.
- Before each request Creator commits a checkpoint ("Before Claude's changes") when the project is under version control; afterwards it lists
  what changed (grouped like the git panel) with **Keep** and **Undo**.
- Shows token usage and cost per conversation. Files sent to the API are the ones the tools read; nothing else.

## Testing

- A small eval set of realistic requests ("make the Ball faster", "add a Paddle moved with A and D", "remove Bricks the Ball hits", and the
  two real sessions above: "log when the game starts", "stop the paddle on key release and show a score") replayed
  against the tools with recorded model responses, so tool changes are checked without spending tokens; a live run is opt-in, like the
  template integration test.
