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

## Step 1: MCP server

Creator serves its command registry over MCP on localhost (Streamable HTTP, behind a per-session token shown in the UI), so Claude Desktop
and Claude Code drive the open project with the user's own subscription while Creator shows every change live. Each tool call goes through
`CommandExecutor.call` on the main dispatcher (the history is confined to it); rejections are resolved to text with `getString`. Verify the
Kotlin MCP SDK's current API before implementing. Settings: on/off, the port, the token, and a log of every tool call.

## Step 2: in-app assistant panel

A chat panel next to the editor, using the user's API key (OS keychain) through Anthropic's Java SDK (Kotlin uses it directly).

- Model: Claude Opus 5.5 by default, with a selector (the user pays). Streaming, adaptive thinking, prompt caching on the tool list and the
  system prompt (Kubriko's concepts plus the generated Block catalog), refusal handling.
- Before each request Creator commits a checkpoint ("Before Claude's changes") when the project is under version control; afterwards it lists
  what changed (grouped like the git panel) with **Keep** and **Undo**.
- Shows token usage and cost per conversation. Files sent to the API are the ones the tools read; nothing else.

## Testing

- The MCP transport over real localhost connections.
- A small eval set of realistic requests ("make the Ball faster", "add a Paddle moved with A and D", "remove Bricks the Ball hits") replayed
  against the tools with recorded model responses, so tool changes are checked without spending tokens; a live run is opt-in, like the
  template integration test.
