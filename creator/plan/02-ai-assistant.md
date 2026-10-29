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
commands, the MCP server, one-click setup, the game tools, and the activity view) is in `../CLAUDE.md`.

## More for the MCP server

- **Projects reporting their Kubriko instances:** a template upgrade that makes `GameEntryPoint` override `getRunningInstances`
  (in `tool-creator-api` since 0.8.0), so that `start_game` waits until the game has drawn Actors; the debugger needs it too.
- **Kubriko's API as a read tool** (the root `CLAUDE.md`'s overview, or the KDoc of a named class): real sessions read Kubriko's sources
  jars with their own shell to write Kotlin Blocks.
- **Actors' positions and states** for the AI, read from the running game rather than from screenshots and logs, once the debug agent
  (`08-debugger.md`) exists.
- **The activity view:** lighting Scenes up beyond the sidebar (opening one compiles the project), numbering steps per project rather than
  per run of Creator, and naming the client (from MCP's `clientInfo`) when two are connected.

## Next: in-app assistant panel

A chat panel inside Creator, built in Compose, hosting the agent the user picks: Claude by default, signed in with the user's own Claude
subscription or their API key, and Codex, Gemini CLI, or any other agent of the ACP Registry beside it. Creator becomes an Agent Client
Protocol (ACP) client, exactly as Android Studio is.

### How Android Studio does it

Read from Android Studio 2026.2.2's running processes and settings on October 29, 2026, while hosting this very conversation:

- **The agent comes from the [ACP Registry](https://agentclientprotocol.com/registry)**
  (`https://cdn.agentclientprotocol.com/registry/v1/latest/registry.json`): its settings remember `lastSelectedAgentId` `claude-acp`,
  and it starts that entry's `distribution`, `npx` → `@agentclientprotocol/claude-agent-acp@0.99.0`, as `npm exec` with the user's own
  Node.js. Each entry has an `id`, `name`, `version`, `description`, `icon`, `license`, and a `distribution` of `npx` (package and args),
  per-platform `binary` archives, or `uvx`. Codex is `codex-acp` (npx), Gemini CLI is `gemini` (npx, `--acp`); 43 agents in all.
- **The adapter** (`claude-agent-acp`, by Anthropic, Zed, and JetBrains, Apache-2.0; listed as "proprietary" because of what it runs)
  speaks ACP over stdio and drives Claude Code through the Agent SDK, which starts the unmodified `claude` binary shipped in
  `@anthropic-ai/claude-agent-sdk-<platform>` with `--input-format stream-json --output-format stream-json
  --permission-prompt-tool=stdio --include-partial-messages --setting-sources=user,project,local --session-id=<id>`.
- **Studio's tools reach Claude as an MCP server per conversation:** `--mcp-config` names
  `http://127.0.0.1:<port>/mcp/conversations/<conversation id>` with a bearer token, and Claude's own `Bash`, `Read`, `Write`, and
  `Edit` are disallowed (the client's `_meta.claudeCode.options` in `session/new`), so every change goes through Studio's tools,
  which keep its buffers and undo in step. Studio's Android-specific system prompt is appended to Claude Code's own (ACP's
  `systemPrompt` in `append` mode).
- **Sign-in never passes through Studio:** no key or token is in the agent's environment; Claude Code uses its own stored login. The
  adapter's `initialize` answer lists `authMethods`: "Claude Subscription" (`claude-ai-login`, which runs `claude auth login --claudeai`),
  "Anthropic Console" (`console-login`, API billing), and custom gateways. A client that advertises terminal auth
  (`clientCapabilities.auth.terminal`, or `_meta["terminal-auth"]`) gets the command and runs it in a terminal it shows, so sign-in
  completes in Anthropic's own browser flow. The adapter pushes `_auth/status_update` with the identity in use (subscription, API key,
  gateway, or none), probed with `claude auth status --json` at start and at every prompt.
- **Model, mode, and effort** are ACP session config options; Studio keeps the user's choice (`acpConfigOptions` → `opus`).

### What Creator needs

All of it lives in a new `ai/assistant` package; the MCP connection for outside clients stays as it is.

1. **ACP client:** the official Kotlin SDK, `com.agentclientprotocol:acp` (JVM, 0.34.0 on Maven Central in October 2026; check its
   license file, which the README gives as both MIT and Apache-2.0, either fine beside MPL-2.0). Creator starts the agent process,
   talks over `StdioTransport`, and calls `initialize` with protocol version 1 (the adapter serves the draft v2 too; move once it is
   final), advertising terminal auth but neither `fs` nor `terminal`, since Creator's own tools make every change. One process per agent,
   many sessions on it; stopped when Creator quits, restarted after a crash with its sessions reloaded, its stderr in Creator's log.
2. **The agent list:** the registry fetched at start and cached in the settings folder (so it works offline), filtered to distributions
   Creator can run on this platform. Claude Agent first, then Codex and Gemini CLI, built in as a fallback list before the first fetch.
   Versions are pinned to the registry's and offered as an update when it moves. A custom agent is a command line the user enters.
3. **Runtimes:** npx agents need Node.js. Creator finds the user's through the login shell (`ProcessRunner.locate`, as the MCP clients
   do) and otherwise offers to download a Node.js LTS into its tools folder with the downloader `11-onboarding.md` builds for the JDK.
   Binary agents are unpacked per version under Creator's tools folder.
4. **Sign-in:** the agent's `authMethods` as buttons in the panel. Terminal methods run in a small terminal pane in Creator (a pty; the
   OS terminal as a fallback), since `claude auth login` opens the browser and may ask for a pasted code; then `authenticate`. The panel
   header shows the identity from `_auth/status_update` ("Claude subscription", "API key"). **API key** for Claude: kept in the OS
   keychain and passed as `ANTHROPIC_API_KEY` in the agent's environment only; other agents read their own variables. Creator never
   sees or stores Claude.ai credentials, and never passes `--hide-claude-auth` or otherwise hides a sign-in method (the terms forbid
   restricting Claude Code's).
5. **Tools:** `session/new` hands the agent Creator's `McpToolServer` in `mcpServers`, at a path per conversation
   (`/mcp/conversations/<id>`) with a token of its own, so each `AiStep` knows its conversation and closing a conversation turns its
   tools off. For Claude, `_meta.claudeCode.options.disallowedTools` removes `Bash`, `Write`, `Edit`, and `NotebookEdit` (Principle
   6: the AI does what the user can, through the same commands); reading stays allowed until the Kubriko API read tool replaces its use
   for Kotlin Blocks. Other agents have no portable switch, so their `session/request_permission` for edits and commands is rejected
   by default, with the user able to allow it.
6. **System prompt:** Kubriko's concepts, Creator's terminology, and the generated Block catalog, appended (ACP `systemPrompt`, `append`)
   so the agent keeps its own prompt; agents without the capability get it as the first prompt's leading text.
7. **The panel:** conversations per project; `agent_message_chunk`s streamed as Markdown, thoughts collapsed; `tool_call` cards titled
   from `CommandTarget`, which tie into the activity view's following and flashing; `plan` updates as a checklist; permission requests
   inline (Allow once, Always, Reject); `available_commands_update` as slash command completion; session config options (model, mode,
   effort) as menus; Stop (`session/cancel`); context chips for the open Scene and the selected Actor type, sent as `resource_link`s; a
   Scene or game screenshot attached as an image where `promptCapabilities.image` allows; conversations resumed after a restart with
   `session/load` where the agent offers it.
8. **Keep and Undo:** before each prompt Creator commits a checkpoint ("Before Claude's changes") when the project is under version
   control; after each turn it lists what changed (grouped like the git panel) with **Keep** and **Undo**.
9. **Usage:** shown where the agent reports it (ACP usage updates); an API-key conversation says that it is billed to the key.

**Order:** first, right after `01-large-projects.md`:

1. The ACP client, with a fake agent in tests (1).
2. Claude Agent on an existing Claude Code login, with Creator's tools per conversation and the system prompt (5, 6), from a
   registry entry built in rather than fetched.
3. The panel (7), without resuming or attachments.
4. The sign-in methods and the API key (4).

Then alongside `03` to `06`: the registry, the Node.js download, and other agents (2, 3); resuming and attachments; Keep and
Undo (8); usage (9); and the MCP server items above.

**The MCP server stays:** ACP has no way of its own to hand an agent custom tools, so it is how the panel's agents reach Creator's
commands, as Android Studio's reach its own. Outside clients (Claude Code in a terminal, Claude Desktop, Cursor) keep using it too.
The panel starts it on demand, whatever the switch in the AI connection panel says.

**Nothing of any agent ships with Creator:** the registry's distribution fetches the adapter, and with it Anthropic's own `claude`
binary, from npm the first time an agent is chosen. Bundling it would mean redistributing a proprietary binary and preinstalling
Claude Code, which the terms tie to the Commercial Terms. Claude Code keeps its login in its own credential store, so a user already
signed in to `claude` is signed in in Creator too.

**Dropped:** the direct Anthropic Java SDK backend this plan used to lead with. Claude Agent over ACP takes an API key just as well,
so one path serves both kinds of user and brings Claude Code's agent loop, compaction, and subagents with it. Revisit only if the
Node.js requirement proves a problem. Local models need no backend of their own either: registry agents such as goose and OpenCode
talk to an OpenAI-compatible local server (Ollama, LM Studio, Foundry Local). Apple's on-device model is too small for Creator's tool list
(a 4,096-token context against about 6,000 tokens of tools, checked October 2026).

### What the terms allow

Claude Code's [Legal and compliance](https://code.claude.com/docs/en/legal-and-compliance) page, read October 29, 2026:

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

Android Studio, Zed, and JetBrains IDEs host the same adapter the same way, and the page now covers hosting explicitly, so this no longer
blocks building. Before release, confirm with Anthropic (the page points to sales) whether a free, MPL-2.0 desktop app that launches the
adapter needs to accept the Commercial Terms itself, naming Android Studio as the precedent.

## Testing

- The ACP client against a fake agent built on the Kotlin SDK's agent side: streaming, tool calls, permission requests, sign-in
  methods, cancellation, a crash and restart, and a session reloaded. An opt-in test (like the template integration test) runs the real
  `claude-acp` from the registry on an existing login.
- A small eval set of realistic requests ("make the Ball faster", "add a Paddle moved with A and D", "remove Bricks the Ball hits", and the
  requests real sessions were given: "log when the game starts", "stop the paddle on key release and show a score") replayed
  against the tools with recorded model responses, so tool changes are checked without spending tokens; a live run is opt-in, like the
  template integration test.
