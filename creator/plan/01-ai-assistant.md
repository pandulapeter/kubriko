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

## Step 1: the command layer

Every change the UI makes today goes straight into controllers (`ActorTypesController.update` with an edited definition, `SceneEditingController`,
`GitController`). Replace that with **commands**: one class per operation, each with a stable name, a description written for both humans and
models, a JSON input schema, validation that returns a user-facing reason, and an `apply` that produces the change. The UI, undo and redo, and
the AI all go through the same registry.

- **Parity is structural, not a convention:** the AI tool list is generated from the command registry, and a test fails if a command has no
  description or schema, or if any UI mutation bypasses the registry. A new feature without its command can't reach the UI either.
- **Read tools** (no command needed, but in the same registry): project overview, the Block catalog (generated from `EventType` and `BlockKind`,
  with what each one does and where it's allowed), list and read Actor types, list Scenes and read their Actors (Scene JSON is `typeId` + state,
  readable without loading classes for block-built types), the console (compiler errors, logs).
- **First write commands:** create, rename, and delete an Actor type; set appearance; add, remove, and move Events; set an Event's key or other
  Actor type; add, remove, move, and edit Blocks; create a Scene; place, move, and remove Actors in a Scene; start version control; commit.
- **Action tools:** play for a few seconds and return logs and errors; build and return problems.
- Undo and redo for the user come for free from the same commands (`apply` returns its inverse).

## Step 2: MCP server

Creator serves its command registry over MCP on localhost (Streamable HTTP, behind a per-session token shown in the UI), so Claude Desktop and
Claude Code drive the open project with the user's own subscription while Creator shows every change live. Verify the Kotlin MCP SDK's current
API before implementing. Settings: on/off, the port, the token, and a log of every tool call.

## Step 3: in-app assistant panel

A chat panel next to the editor, using the user's API key (OS keychain) through Anthropic's Java SDK (Kotlin uses it directly).

- Model: Claude Opus 5.5 by default, with a selector (the user pays). Streaming, adaptive thinking, prompt caching on the tool list and the
  system prompt (Kubriko's concepts plus the generated Block catalog), refusal handling.
- Before each request Creator commits a checkpoint ("Before Claude's changes") when the project is under version control; afterwards it lists
  what changed (grouped like the git panel) with **Keep** and **Undo**.
- Shows token usage and cost per conversation. Files sent to the API are the ones the tools read; nothing else.

## Testing

- Every command: validation, apply, inverse, and its tool schema, against fakes and temporary projects.
- The MCP transport over real localhost connections.
- A small eval set of realistic requests ("make the Ball faster", "add a Paddle moved with A and D", "remove Bricks the Ball hits") replayed
  against the tools with recorded model responses, so tool changes are checked without spending tokens; a live run is opt-in, like the
  template integration test.
