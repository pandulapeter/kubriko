<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# AI assistant

What is left of the assistant panel (`:creator:ai`'s `ai/assistant`, `ui/assistant`) and the MCP server (`mcp`) that needs no other
plan. Searching conversations comes with the rest of search (`02-resources-and-search.md`), and the AI's view of the running game
with the debugger (`07-debugger.md`). Every new tool keeps `TokenBudgetTest` within its budget and joins `EvalSetTest` where models
call it.

## Context for large games

Creator computes compact, always fresh context from its own model of the game, where a generic coding agent greps its way to
understanding and the model pays for it. These serve the panel and outside MCP clients alike, and whatever helps the AI understand
the game is shown to the user too.

- **More health checks:** a Scene nothing goes to other than the first (needs the first Scene known from `Game.kt`), and the
  findings shown where their subject is edited, beside the map's list.
- **Fewer retries:** validation rejections that say precisely what to change, for every rejection that doesn't yet.
- **Planning first:** approving a plan from its checklist (an Approve button that answers the agent) rather than in a reply, and
  model hints (a smaller model for small edits) that the user decides on, since they pay.

## The activity view

- Lighting Scenes up beyond the sidebar (opening one compiles the project).
- Numbering steps per project rather than per run of Creator.

## Guards

- More live scenarios (`AssistantLiveScenariosTest`): the chips' links followed, game notes proposed after a settled decision, and
  the `game-explorer` subagent used.
- The eval set (`EvalSetTest`) with calls recorded from real sessions rather than written by hand, more requests ("stop the paddle on
  key release"), and an opt-in live run that has a real agent answer each request against a scratch project, like the template
  integration test.

## Waiting on others

Each starts when what it waits for happens; nothing is built for it before.

- **ACP protocol 2** (`AcpAgent`), once it is final (the adapter serves its draft).
- **ACP's `systemPrompt` field**, once the Kotlin SDK has it: Claude moves to it from `_meta.systemPrompt`, and any agent that
  advertises `sessionCapabilities.systemPrompt` gets it there instead of as leading text, tested with a `FakeAgent` variant that
  advertises it.
- **Codex's profile** (`codex-acp`), once Codex has quota again (after Nov 21, 2026). Known: it takes client MCP servers over HTTP;
  ChatGPT login, `CODEX_API_KEY` or `OPENAI_API_KEY`, or a custom gateway; `INITIAL_AGENT_MODE=read-only` and a sandbox mode;
  `CODEX_CONFIG` merged into its session config. To check in a real session: the system prompt through `CODEX_CONFIG` (its developer
  instructions) or leading text, whether read-only still lets MCP tools run, and that a `codex-work` alias's account
  (`CODEX_HOME`, which the setup already offers) is the one it signs in to. A profile is written only
  for what the fallbacks can't do, and gets a live scenario of its own.
- **The registry's icons** in the agent menu, once each vendor's trademark rules are checked (Anthropic's first).
- **Claude Agent's version**, pinned to the one checked, offered as an update once the registry's moves and the new one is checked.
- **The Commercial Terms** (`README.md` → Open questions 4), asked before release.
