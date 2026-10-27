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

The server and its panel exist (`../CLAUDE.md` → AI connection). Still to do:

- Try it with a real Claude Code session end to end (`claude mcp add` from the panel), and write down what works and what Claude
  stumbles on; that feeds the eval set below.
- Claude Desktop only starts local MCP servers over stdio, so it needs a bridge (such as `mcp-remote`) with the header; verify one
  and show its config in the panel.
- Notify clients when the project changes underneath them (`tools/list` is static, but resources or a changed-project hint could
  save rereads), if sessions show it matters.

## Next: in-app assistant panel

A chat panel next to the editor, behind a small backend interface, so it can talk to Claude in either of two ways:

- **The user's API key** (the default): Anthropic's Java SDK (`com.anthropic:anthropic-java`, 3.3.0 in October 2026), called from Kotlin
  directly, with `CommandRegistry.editor.tools` as its tools. The key lives in the OS keychain (`java-keyring` covers the macOS Keychain,
  Windows Credential Manager, and the Linux Secret Service, but its last release is from 2023, so check it still works on current JDKs, or
  use JNA directly; macOS only protects the item from other apps when Creator ships as a signed app bundle with its own runtime).
- **The user's own Claude Code**, for subscription users, if Anthropic confirms it is allowed (see below): Creator runs the user's installed,
  unmodified `claude` in headless mode with streamed JSON in and out, pointed at Creator's MCP server, and shows its messages in
  the panel. The user signs in through Claude Code's own flow; Creator never sees their credentials.

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

- A small eval set of realistic requests ("make the Ball faster", "add a Paddle moved with A and D", "remove Bricks the Ball hits") replayed
  against the tools with recorded model responses, so tool changes are checked without spending tokens; a live run is opt-in, like the
  template integration test.
