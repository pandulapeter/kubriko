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

Claude edits Kubriko games through the same validated operations the user has, so its work stays inside the editor's constraints by
construction, shows up as readable Events and Blocks, and can be inspected, tweaked, or undone like any other change. What exists (the
commands, the MCP server, one-click setup, the game tools, and the activity view) is in `commands/`, `mcp/`, and `ai/assistant`.

## More for the MCP server

- **Actors' positions and states** for the AI, read from the running game rather than from screenshots and logs, once the debug agent
  (`09-debugger.md`) exists.
- **The activity view:** lighting Scenes up beyond the sidebar (opening one compiles the project), and numbering steps per project
  rather than per run of Creator.

## The assistant panel

1. **ACP client** (`AcpAgent`): move to protocol version 2 once it is final (the adapter serves its
   draft).
2. **The agent list** (`AgentRegistry`): the registry's icons in the menu (checked against each vendor's trademark rules
   first), and Claude Agent's version, pinned to the one checked, offered as an update once the registry's moves and it is checked.
3. **Sign-in** (`SignInSession`): what is left is the operating system's terminal as a fallback for a sign-in command
   that turns out to need a terminal device.
4. **Tools** (per conversation, `ConversationToolServer`): see whether Claude still reaches for its own Read for Kubriko's sources now that
   `get_kubriko_api` exists (it keeps Read for the game's own Kotlin). Other agents
   have no portable switch, so their `session/request_permission` for edits and commands is rejected (`PermissionHandler.rejectingChanges`),
   whatever the user would choose; letting the user allow it is still to come.
5. **System prompt** (`AssistantSystemPrompt`, appended for Claude through `_meta.systemPrompt`): agents without a way to take one get it
   as the first prompt's leading text; move Claude to ACP's own `systemPrompt` field once the Kotlin SDK has it.
6. **The panel** (`ui/assistant`): a screenshot of the open Scene as a chip too (the game's exists).

**Order:** alongside `04` to `07`, what is left of the fallbacks (Other agents and services); Codex's profile once it can be checked; and the MCP server items above.

### What the terms allow

Claude Code's [Legal and compliance](https://code.claude.com/docs/en/legal-and-compliance) page:

- Third-party apps may not offer Claude.ai login of their own, route requests through a user's plan themselves, or collect,
  store, or intermediate Claude.ai credentials; sign-in must complete through Anthropic's own flow. Creator never does: Claude Code
  signs in by itself.
- It "does not prevent an end user from signing in to the unmodified Claude Code binary with their own Claude subscription, including
  where a platform hosts Claude Code". Running Claude Code in a product requires agreeing to the Commercial Terms, keeping the binary
  unmodified, removing no sign-in method, and letting each user authenticate and be billed as themselves, never paying for or reselling
  their usage. Creator satisfies all of these by construction: it runs the registry's adapter, which runs Anthropic's own binary.
- Creator may say in plain text that it runs Claude Code, but not use "Claude Code" or Anthropic's names or logos in its own feature
  names: the panel says "Assistant", the agent is "Claude Agent" as the registry names it, and showing the registry's icon is checked
  against Anthropic's trademark guidelines first.

Android Studio, Zed, and JetBrains IDEs host the same adapter the same way. Before release, confirm with Anthropic (the page points
to sales) whether a free, MPL-2.0 desktop app that launches the adapter needs to accept the Commercial Terms itself, naming Android
Studio as the precedent.

### Other agents and services

Agents come and go faster than Creator can follow them, so Creator writes as little per-agent code as it can. Claude Agent is the one
agent it fully supports. Every other agent runs on generic fallbacks, chosen from what its `initialize` answer advertises, and is
marked untested in the list; when a vendor renames, retires, or replaces an agent, the registry changes and Creator follows without
code of its own.

`AcpAgent` speaks to any ACP agent, and `AgentProfile` holds what an agent needs beyond the fallbacks (`AgentProfile.fallback`
chooses the fallbacks that exist): Claude's is the only one. What is left:

- **System prompt:** ACP's own `systemPrompt` where `sessionCapabilities.systemPrompt` is advertised, once the Kotlin SDK has the
  field. Google Antigravity advertises `_meta.antigravity.systemPrompt.append` in its `initialize` answer, a way to append it as
  Claude Agent does, worth trying in place of leading text.
- **Staying off the files:** a read-only mode or sandbox where the agent offers one (a session mode or config option), before
  falling back on rejecting permission requests. An agent that changes files without asking is marked as such in the list, and its
  conversations suggest version control, since the turn's Undo only reverts changes made through Creator's tools.
- **Accounts:** the variable that picks an agent's configuration folder (`CLAUDE_CONFIG_DIR` for Claude Code, `CODEX_HOME` for Codex),
  offered from the user's shell aliases (`ShellAliases`) as the MCP clients' setups are, so `claude-personal` and `claude-flashfood`
  each become an account to choose.

A profile is written only for an agent that is popular, stable, and checked in a real session, and only for what the fallbacks
can't do. The one candidate so far:

| Agent | Known | To check |
|---|---|---|
| Codex (`codex-acp` 2.4.0, npx, Apache-2.0) | Takes client MCP servers over HTTP; ChatGPT login, `CODEX_API_KEY` / `OPENAI_API_KEY`, or a custom gateway; `INITIAL_AGENT_MODE=read-only` and a sandbox mode; `CODEX_CONFIG` merged into its session config; ships its own `@openai/codex`. | The system prompt through `CODEX_CONFIG` (Codex's developer instructions) or leading text; whether read-only still lets MCP tools run; `CODEX_HOME` for accounts. |

The registry's other agents are listed with the fallbacks alone and marked untested, and a custom agent gets them too.

**Model services** need no agents of their own: Claude through Amazon Bedrock, Google Vertex AI, or a company gateway is Claude
Agent's gateway sign-in (offered when the client advertises `auth._meta.gateway`) or Claude Code's own environment
(`CLAUDE_CODE_USE_BEDROCK`, `CLAUDE_CODE_USE_VERTEX`); Codex has a gateway of its own; local OpenAI-compatible servers go through
OpenCode or goose. Creator keeps only the keys and addresses the user gives it, in the keychain, never a login.

## Context for large games

Creator holds a structured model of the game (Actor types, Events, Blocks, Manager types, and Scenes) from which the generated Kotlin
follows, so it can hand an agent exact, compact, and always fresh context that it computes itself, where a generic coding agent greps
its way to understanding and the model pays for it. These serve the panel and outside MCP clients alike. Whatever helps the AI
understand the game is shown to the user too.

- **Following the user's attention:** the selected Event as a chip too (its index lives in `BlockTypeEditor`'s own state today).
- **The game's notes in the editor:** a way for the user to open and edit `AGENTS.md` from Creator (the AI's changes already
  go through Keep and Undo), and a diff of what a change to them would be before it is kept.
- **"Last time" summaries:** the agent writing a real summary at the end of a conversation, in place of the start of its last
  answer that a new conversation hears now.
- **A smaller tool list:** (the size `TokenBudgetTest` measures) since the Block schema went to
  `get_block_catalog`: terser descriptions and Claude Code's tool search, which loads tool schemas only when they are needed. Lower `TOOL_LIST_BUDGET` with it.
- **More health checks:** a Scene nothing goes to other than the first (needs the first Scene known from `Game.kt`), and the
  findings shown where their subject is edited, beside the map's list.
- **Fewer retries:** validation rejections that say precisely what to change (inputs that don't decode already do, and
  `check_change` tries a change without making it).
- **Cheaper screenshots:** cropped to an Actor (the region crop exists), and replaced by Actors' states from the debug agent
  (`09-debugger.md`) where those say enough.
- **Planning first:** approving a plan from its checklist (an Approve button that answers the agent), rather than in a reply,
  and model hints (a smaller model for small edits) that the user decides on, since they pay.

## Testing

- `FakeAgent` variants for the fallbacks still to come: an advertised `systemPrompt` and a read-only mode.
- More live scenarios (`AssistantLiveScenariosTest`): the chips' links followed, game notes proposed after a settled decision,
  the `game-explorer` subagent used, and a scenario per profile once there is another (with that agent's account variable).
- The eval set (`EvalSetTest`) with calls recorded from real sessions rather than written by hand, more requests ("stop the paddle on
  key release"), and an opt-in live run that has a real agent answer each request against a scratch project, like the template
  integration test.
