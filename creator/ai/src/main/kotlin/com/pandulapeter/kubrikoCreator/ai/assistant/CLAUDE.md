<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# ai/assistant (the in-app assistant)

Hosts Agent Client Protocol agents through the official Kotlin SDK. Tests run `FakeAgent` over in-process pipes.

- **Panel state:** `AssistantController` (app-level, created once in `AppGraph`) is the panel's façade over
  `AgentSelection` (the agent chosen and the registry), `AssistantHost` (the one `Assistant` from first use until Creator quits,
  replaced in one ordered sequence, and the one check of AI features), and `AssistantSignIn` (sign-in, API key aside, and agent
  variables); `AssistantSettingsState` saves their settings off the main thread. The panel reads an `AssistantPanelModel` (which
  `AssistantController` implements), and what a given agent offers, an API key (`AgentProfile.usesAnthropicApiKey`) and its
  tool's configurations (`AgentConfiguration.clientOf`), comes from the agent, not from the UI.
- **Accounts:** an agent whose tool Kubriko Creator knows (Claude Code, Codex) can run as any shell alias that runs that tool
  with variables of its own (`CLAUDE_CONFIG_DIR`, `CODEX_HOME`), found as the MCP clients' setups are, and remembered per
  agent by the alias's name, never its variables, which may hold a secret. The variables given for signing in are kept per
  account (`AgentConfigurations.accountOf`), so switching accounts never carries one account's key into another.
- **Starting with a project:** an agent that has worked before (it holds a saved conversation about any project) opens a
  conversation as soon as a project opens, whether or not the panel is shown, so what needs one taking prompts (writing
  descriptions) works from the start; any other agent waits for the panel, since starting it may download it or ask to sign in.
  The same holds for an agent or configuration chosen in Creator settings (`isPanelShown = false`): the panel opens a
  conversation whenever it is shown without one.
- **Agents:** Claude Agent is the one fully supported agent (`RegistryAgent.isTested`); every other ACP Registry agent runs on
  generic fallbacks chosen from its `initialize` answer (`AgentProfile.fallback`), with as little per-agent code as possible, and
  is shown as untested. Such an agent hears the system prompt ahead of its first prompt, unless it advertises appending it
  to its own through a conversation's `_meta` (`AntigravityMeta`, Google Antigravity's extension), which also reaches
  a continued conversation. An agent a vendor publishes in the registry counts as meant for hosting, so vendors' terms don't gate it;
  names and icons follow each vendor's trademark rules.
- **Nothing of any agent ships with Creator:** the registry's distribution fetches it on first use. Bundling would mean
  redistributing a proprietary binary and preinstalling Claude Code, which its terms tie to the Commercial Terms.
- **Creator's tools make every change:** the agent gets neither file system nor terminal access, Claude Agent has `Bash`,
  `Write`, `Edit`, and `NotebookEdit` disallowed, Google Antigravity (which has no read-only mode) has its `edit_file`,
  `create_file`, and `run_command` turned off through the same `_meta` (`AntigravityMeta`; names checked in a real session, the
  ones its tool calls' titles show, not its model's), and every other agent's conversations are switched to its read-only mode or
  sandbox where it offers one (`ReadOnlyMode`), again whenever they are loaded: only a value named read-only, never a plan mode,
  since Claude Code's refuses MCP tools that change something, Creator's among them. What an agent still asks to edit,
  delete, move, or execute itself goes to the user, as every change outside Creator is confirmed, with the agent's diffs and
  a warning that Undo can't revert it (`PermissionRequest.isOutsideCreator`). An agent seen changing files without asking
  (`UnaskedChanges`, which needs no list of agents, since only a session shows it) is remembered by its `AgentChoice.identity`,
  marked in the agent menu, and its conversations suggest version control. Each agent works in a folder of its own per project (`agents/workspaces`),
  since an agent may write files where it starts without asking. Creator's own tools are allowed without asking when their kind
  edits, deletes, moves, and runs nothing (`PermissionHandler.allowingCreatorTools`), recognized however the agent names them
  (`CreatorToolName`). An agent without HTTP MCP reaches them through `mcp-remote`, pinned to one exact version
  (`McpToolServer.MCP_REMOTE`), since it runs with the conversation's token.
- **Tools per conversation:** the assistant's `McpToolServer` has no outside token; each conversation gets an address and token of
  its own, and closing it turns those tools off. Conversations end when the project closes or changes, since their tools work on
  whatever project is open. Ending them, replacing the agent, and listing the saved conversations run as one sequence (a restart, a
  project change, AI turned off), which a conversation opened meanwhile waits for, so it always starts on the agent chosen last.
- **Waiting for the user:** `ask_user` waits for as long as the user takes, and Claude Code gives up on a tool call after a
  minute, so Claude Agent starts with `MCP_TOOL_TIMEOUT` set to a day (`AgentProfile.environment`, under the variables of the
  user's own configuration).
- **Signing in never passes through Creator:** the agent signs in by itself, a terminal method runs in the agent's environment so it
  signs in to the same configuration, and the API key lives in the system's secret store, passed to the secret tool on standard
  input (never its arguments) and to the agent only as `ANTHROPIC_API_KEY`. Claude Agent reports a new identity only at the next
  prompt, so after a terminal sign-in `needsSignIn` stays false until it reports something new. A sign-in command that needs a
  terminal device continues in the system's own terminal (`SystemTerminalLauncher`): a script Creator writes, readable only by
  the user, deletes itself before anything else, since it holds the agent's variables, and reports its process ID and the exit
  code through files, since the window isn't Creator's to read, so a window closed early (or never opened) ends the wait;
  the sign-in it replaces keeps running until the terminal opened; it is offered on every terminal sign-in, first when the output says a
  device was needed (`TerminalDeviceErrors`), since that wording differs between programs.
- **Processes:** one that ends unasked is started again and its conversations loaded again, but one that exits soon after starting
  several times in a row is `Failed` until it is used again. Ending them all restarts an agent that can't close conversations, so
  their IDs can be loaded later.
- **Deleting** a conversation ends it and forgets it for good; it is removed from the saved ones only once it ended, since a turn
  ending meanwhile would save it again. The panel lists open and saved conversations together, the one talked in last first
  (`AssistantChat.lastMessageAt`).
- **Searching conversations:** the agent holds a conversation's transcript, which Creator only replays, so each saved
  conversation's user and agent messages are kept as plain text beside it (`KeptMessage`, capped, newest kept; one file of
  its own each, readable by the user only, `ConversationTextStore`), written at every turn's end and forgotten with the
  conversation, however it goes. The search reads them all in memory off the main thread, every agent's, since a project's
  conversations belong to it. A match is told from the others by how many later messages hold the query too
  (`ConversationMatch.laterOccurrences`), since kept messages leave out a long conversation's oldest; another agent's is
  read from the kept text, as only its own agent can continue it.
- **Updates:** an agent announces its slash commands before anything collects a conversation's `updates`, so
  `AgentConversation.availableCommands` keeps the first announcement.
- **Downloads** (`BinaryAgentInstaller`, `NodeRuntime`) are checked against the published SHA-256 where there is one and refuse
  archive entries outside their folder.
- **Turns:** a turn that changed the project lists what changed before its final answer (`TurnChanges`) and ends with Keep and
  Undo (`TurnReview`, shown only when the setting is on), which reverts the conversation's own changes through the undo history
  while they are the newest (`CommandExecutor.undoTurn`).
- **Plans:** a plan the agent laid out in its last turn and started none of (`AssistantTranscript.isPlanAwaitingApproval`)
  is approved from its checklist, whose Approve button sends the approval as the user's next message. The agent lays it
  out with Creator's own `set_plan` (`ConversationPlans`, shown as an ACP plan is), since Claude Code run through the SDK
  has no todo or task tool to report one with (a live run showed it planning in prose); a plan an agent reports through
  ACP shows the same. The system prompt asks the agent to end its turn on a plan rather than ask with `ask_user`, and
  the user can still answer with what to change instead. Where the agent offers a model a tier below the
  conversation's (`SmallerModel`), a second button approves with that one, switching the conversation to it before the
  turn: the plan shows how much work is left, so the user, who pays, decides there. Tiers are known only by family words
  whose order is certain (Haiku, Sonnet, Opus; Flash, Pro), and a model of no known tier is offered nothing.
- **Last time:** a new conversation hears what the project's previous one noted with `remember_for_next_time` (which the
  system prompt asks for once a request is done), or else the start of its last answer. The note is written during the
  turn rather than asked for when a conversation is left, since the next conversation starts as soon as the project opens.
  The tool keeps it in the project's `ConversationSummaries` by the conversation's tool address, and the assistant takes it
  when a turn ends, stopped ones too, and saves it with the conversation. Turns that note nothing keep it, marked as
  earlier (`isSummaryEarlier`), so the next conversation hears their last answer after it rather than a stale account alone.
- **Leaving a conversation:** once it holds something and its agent isn't working, the panel offers a new one above the
  composer (`NewConversationOffer`), stopping a turn that waits for an answer and answering its questions for the user
  (`UserQuestions.LEFT_UNANSWERED`, since an agent may not cancel the call that asked), as a follow-up question shouldn't
  hold the user in a conversation they are done with; the system prompt tells the agent to stop once a request is done
  rather than offer more. While the agent waits, the composer answers its newest question (`waitingQuestions`), and every
  other card keeps a field of its own, so there is always one field to type each answer in.
