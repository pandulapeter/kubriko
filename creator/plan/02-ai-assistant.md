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
  (`09-debugger.md`) exists.
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

1. **ACP client** (`AcpAgent`, see `../CLAUDE.md` → Assistant): the agent's standard error in a log the user can open. Move to protocol
   version 2 once it is final (the adapter serves its draft).
2. **The agent list:** the registry fetched at start and cached in the settings folder (so it works offline), filtered to distributions
   Creator can run on this platform. Claude Agent first, then Codex and Gemini CLI, built in as a fallback list before the first fetch.
   Versions are pinned to the registry's and offered as an update when it moves. A custom agent is a command line the user enters.
3. **Runtimes:** npx agents need Node.js. Creator finds the user's through the login shell (`ProcessRunner.locate`, as the MCP clients
   do) and otherwise offers to download a Node.js LTS into its tools folder with the downloader `12-onboarding.md` builds for the JDK.
   Binary agents are unpacked per version under Creator's tools folder.
4. **Sign-in:** the agent's `authMethods` as buttons in the panel. Terminal methods run in a small terminal pane in Creator (a pty; the
   OS terminal as a fallback), since `claude auth login` opens the browser and may ask for a pasted code; then `authenticate`. The panel
   header shows the identity from `_auth/status_update` ("Claude subscription", "API key"). **API key** for Claude: kept in the OS
   keychain and passed as `ANTHROPIC_API_KEY` in the agent's environment only; other agents read their own variables. Creator never
   sees or stores Claude.ai credentials, and never passes `--hide-claude-auth` or otherwise hides a sign-in method (the terms forbid
   restricting Claude Code's).
5. **Tools** (per conversation, see `../CLAUDE.md` → Assistant): each `AiStep` knowing its conversation, so the panel's tool call
   cards tie into it; reading staying allowed for Claude until the Kubriko API read tool replaces its use for Kotlin Blocks. Other agents
   have no portable switch, so their `session/request_permission` for edits and commands is rejected by default (`PermissionHandler.Reject`),
   with the user able to allow it.
6. **System prompt** (`AssistantSystemPrompt`, appended for Claude through `_meta.systemPrompt`): agents without a way to take one get it
   as the first prompt's leading text; move Claude to ACP's own `systemPrompt` field once the Kotlin SDK has it.
7. **The panel** (see `../CLAUDE.md` → Assistant): tool call cards naming their `CommandTarget` and tying into the activity view's
   following and flashing (clicking one goes to what it changed); context chips for the open Scene and the selected Actor type, sent as
   `resource_link`s; a Scene or game screenshot attached as an image where `promptCapabilities.image` allows; conversations kept per
   project after it closes and resumed after a restart with `session/load` where the agent offers it; Markdown tables, which the
   panel's reader shows as plain lines.
8. **Keep and Undo:** before each prompt Creator commits a checkpoint ("Before Claude's changes") when the project is under version
   control; after each turn it lists what changed (grouped like the git panel) with **Keep** and **Undo**.
9. **Usage:** shown where the agent reports it (ACP usage updates); an API-key conversation says that it is billed to the key.

**Order:** first, right after `01-large-projects.md`, the sign-in methods and the API key (4), then the tool call cards of the panel
(7). Then alongside `04` to `07`: agent profiles and the fallbacks (Other agents and services), Codex first, then Gemini CLI and
Copilot; the registry, the Node.js download, and binary agents (2, 3); resuming and attachments; Keep and Undo (8); usage (9); and
the MCP server items above.

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

### Other agents and services

`AcpAgent` speaks to any ACP agent; what differs between agents goes into an **agent profile**, of which Claude's is the first
(`RegistryAgent.claude` and `ClaudeAgentOptions`): how the agent starts (the registry's `npx`, per-platform `binary`, or `uvx`
distribution), how it reaches Creator's tools, how it gets the system prompt, how it is kept off the project's files, how it signs
in, and which account it uses. Where a profile has nothing better, the panel falls back on what the agent's `initialize` answer
advertises:

- **Tools:** an HTTP MCP server where `mcpCapabilities.http` is set; otherwise a stdio one running `mcp-remote` through the user's
  `npx`, as Claude Desktop's entry does, with the conversation's token in its environment.
- **System prompt:** ACP's own `systemPrompt` where `sessionCapabilities.systemPrompt` is advertised (once the Kotlin SDK has the
  field), the profile's own way otherwise, and as a last resort the first prompt's leading text (not repeated after `session/load`).
- **Staying off the files:** the agent's own switch for its tools where it has one; otherwise a read-only mode or sandbox where the
  agent offers one (a session mode or config option); otherwise every permission request for an `edit`, `delete`, `move`, or `execute`
  tool call is rejected (`PermissionHandler.Reject`). An agent that changes files without asking is marked as such in the list, and its
  conversations suggest version control, so that Keep and Undo (8) can take its changes back.
- **Sign-in:** every kind of `authMethods` entry: agent (`authenticate`), terminal (the terminal pane of step 4), and environment
  variable (`EnvVarAuth`, whose `vars` name the fields; the values kept in the OS keychain per agent).
- **Accounts:** the variable that picks an agent's configuration folder (`CLAUDE_CONFIG_DIR` for Claude Code, `CODEX_HOME` for Codex),
  offered from the user's shell aliases (`ShellAliases`) as the MCP clients' setups are, so `claude-personal` and `claude-flashfood`
  each become an account to choose.

The first profiles after Claude, each checked in a real session before it is listed as built in:

| Agent | Known (read October 29, 2026) | To check |
|---|---|---|
| Codex (`codex-acp` 2.4.0, npx, Apache-2.0) | Takes client MCP servers over HTTP; ChatGPT login, `CODEX_API_KEY` / `OPENAI_API_KEY`, or a custom gateway; `INITIAL_AGENT_MODE=read-only` and a sandbox mode; `CODEX_CONFIG` merged into its session config; ships its own `@openai/codex`. | The system prompt through `CODEX_CONFIG` (Codex's developer instructions) or leading text; whether read-only still lets MCP tools run; `CODEX_HOME` for accounts. Out of quota on this Mac until November 21, 2026. |
| Gemini CLI (`gemini` 0.66.0, npx, `--acp`) | Apache-2.0. | Client MCP servers over HTTP; the system prompt (`GEMINI_SYSTEM_MD` replaces Gemini's own rather than appending, so leading text may be better); excluding its edit and shell tools; Google login and `GEMINI_API_KEY`. Not installed on this Mac. |
| GitHub Copilot CLI (`github-copilot-cli`, npx, `--acp`) | Proprietary. | The same four, and signing in with a Copilot subscription. |
| Cursor, OpenCode, goose, Junie (`binary`) | Per-platform archives in the registry. | Unpacking per version under Creator's tools folder (3); OpenCode and goose with a local model (Ollama, LM Studio). |

The registry's other agents (43 in all) are listed with the fallbacks alone and marked untested, and a custom agent (2) gets them
too.

**Model services** need no agents of their own: Claude through Amazon Bedrock, Google Vertex AI, or a company gateway is Claude
Agent's gateway sign-in (offered when the client advertises `auth._meta.gateway`) or Claude Code's own environment
(`CLAUDE_CODE_USE_BEDROCK`, `CLAUDE_CODE_USE_VERTEX`); Codex has a gateway of its own; local OpenAI-compatible servers go through
OpenCode or goose. Creator keeps only the keys and addresses the user gives it, in the keychain, never a login.

**Terms per vendor:** before a profile offers an agent's subscription sign-in, its vendor's terms for hosting it are read and summed
up here, as Claude Code's are above (OpenAI's for Codex with a ChatGPT login, Google's for Gemini CLI, GitHub's for Copilot), and its
name and icon follow that vendor's trademark rules.

## Context for large games

Creator holds a structured model of the game (Actor types, Events, Blocks, Manager types, and Scenes) from which the generated Kotlin
follows, so it can hand an agent exact, compact, and always fresh context that it computes itself, where a generic coding agent greps
its way to understanding and the model pays for it. These serve the panel and outside MCP clients alike. Whatever helps the AI
understand the game is shown to the user too.

**First,** with the panel's first steps:

1. **The project digest:** about 1,000 to 2,000 tokens that Creator builds, not the model, and refreshes on every change: types by
   folder with a one-line role each, Manager types, Scenes, and who spawns, collides with, reads, or goes to whom. It opens every
   conversation through the system prompt (step 6 above), in an order that stays the same between requests, so prompt caching keeps
   hitting. Optional one-line **descriptions** on Actor types, Manager types, and Scenes, filled in by the user or the AI, give each
   its role. The same digest is shown to the user as a map of the game.
2. **Compact Block notation and zoom levels:** tool results write Blocks as the editor shows them (`on Collision with Brick: destroy
   other; add 10 to score of Game`) rather than JSON, leaving out defaults and shortening IDs. Reads zoom in: the digest, a type's outline
   (its Events and their Block counts, a `detail` level on `read_actor_type`), then one Event in full.
3. **Reads that know what was seen:** the per-conversation MCP address lets Creator remember what each conversation has read, so a
   repeated read answers "unchanged since you read it" and a changed object returns only what changed. Write results return the
   revision and the change, not the whole object. The user's own edits between two prompts reach the next one as a short summary ("you
   added a Collision with Brick Event to Ball"), so the agent stays in sync without reading everything again.
4. **`get_relations`:** what references an Actor type, Manager type, Property, or Scene, and what it references, from the model; it
   replaces chains of reads, and `search_project` and `find_usages` (`04-resource-organization.md`) build on it.
5. **Token budgets as tests**, like the size checks and the build benchmark: the whole tool list, and each read tool on the Breakout
   copy (`read_actor_type` of the Ball, `read_scene` of a level, the digest), stay under a budget, so their cost can't creep up.

**Then:**

- **Following the user's attention:** context chips filled in on their own (the open Scene, the selected Actor type and Event, the
  last build failure or console exception), sent as links the agent follows when it needs them rather than as pasted text.
- **A game notes file:** the game's design notes (mechanics, naming, and decisions) as a plain committed file, the project's
  `AGENTS.md` (with a `CLAUDE.md` pointing to it), which Claude Code and other agents read by themselves, outside clients included.
  After a conversation the AI proposes updates as a diff, which the user confirms (Principle 4).
- **"Last time" summaries:** a new conversation may start from a short summary of the project's previous one, rather than the user
  explaining again.
- **A small tool list:** about 6,000 tokens today and growing with every feature: terse descriptions, related tools grouped, and
  Claude Code's tool search, which loads tool schemas only when they are needed.
- **Exploring in subagents:** questions such as "which types change the score?" go to a subagent, defined through the session options
  rather than files in the project, so the conversation's context stays clean.
- **Health checks** computed from the model: unused types, Scenes nothing goes to, Properties never read, and Events that can never
  fire. Shown in the editor and readable by the AI.
- **Fewer retries:** rejections that say precisely what to change, and a dry run for write tools.
- **Cheaper screenshots:** downscaled or cropped to a region or an Actor, and replaced by Actors' states from the debug agent
  (`09-debugger.md`) where those say enough.
- **Planning first** for big requests, as a checklist the user approves (ACP `plan` updates), and model hints (a smaller model for
  small edits) that the user decides on, since they pay.

## Testing

- `FakeAgent` variants for each fallback: no HTTP MCP, no system prompt capability, no `session/load`, restricted only by permission
  requests, and each kind of sign-in method.
- An opt-in live test per profile, as `ClaudeAgentLiveTest` is for Claude, with that agent's account variable: one tool call through
  the conversation's address, and nothing written to the project folder.
- A small eval set of realistic requests ("make the Ball faster", "add a Paddle moved with A and D", "remove Bricks the Ball hits", and the
  requests real sessions were given: "log when the game starts", "stop the paddle on key release and show a score") replayed
  against the tools with recorded model responses, so tool changes are checked without spending tokens; a live run is opt-in, like the
  template integration test.
