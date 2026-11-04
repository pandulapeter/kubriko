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
  `AssistantController` implements), and what a given agent offers, an API key and Claude Code configurations, comes from its
  `AgentProfile.usesClaudeAccount`, not from the UI.
- **Agents:** Claude Agent is the one fully supported agent (`RegistryAgent.isTested`); every other ACP Registry agent runs on
  generic fallbacks chosen from its `initialize` answer (`AgentProfile.fallback`), with as little per-agent code as possible, and
  is shown as untested. An agent a vendor publishes in the registry counts as meant for hosting, so vendors' terms don't gate it;
  names and icons follow each vendor's trademark rules.
- **Nothing of any agent ships with Creator:** the registry's distribution fetches it on first use. Bundling would mean
  redistributing a proprietary binary and preinstalling Claude Code, which its terms tie to the Commercial Terms.
- **Creator's tools make every change:** the agent gets neither file system nor terminal access, its requests to edit, delete,
  move, or execute are rejected whatever the user would choose (`PermissionHandler.rejectingChanges`), Claude Agent has `Bash`,
  `Write`, `Edit`, and `NotebookEdit` disallowed, and each agent works in a folder of its own per project (`agents/workspaces`),
  since an agent may write files where it starts without asking. Creator's own tools are allowed without asking when their kind
  edits, deletes, moves, and runs nothing (`PermissionHandler.allowingCreatorTools`), recognized however the agent names them
  (`CreatorToolName`). An agent without HTTP MCP reaches them through `mcp-remote`, pinned to one exact version
  (`McpToolServer.MCP_REMOTE`), since it runs with the conversation's token.
- **Tools per conversation:** the assistant's `McpToolServer` has no outside token; each conversation gets an address and token of
  its own, and closing it turns those tools off. Conversations end when the project closes or changes, since their tools work on
  whatever project is open. Ending them, replacing the agent, and listing the saved conversations run as one sequence (a restart, a
  project change, AI turned off), which a conversation opened meanwhile waits for, so it always starts on the agent chosen last.
- **Signing in never passes through Creator:** the agent signs in by itself, a terminal method runs in the agent's environment so it
  signs in to the same configuration, and the API key lives in the system's secret store, passed to the secret tool on standard
  input (never its arguments) and to the agent only as `ANTHROPIC_API_KEY`. Claude Agent reports a new identity only at the next
  prompt, so after a terminal sign-in `needsSignIn` stays false until it reports something new.
- **Processes:** one that ends unasked is started again and its conversations loaded again, but one that exits soon after starting
  several times in a row is `Failed` until it is used again. Ending them all restarts an agent that can't close conversations, so
  their IDs can be loaded later.
- **Deleting** a conversation ends it and forgets it for good; it is removed from the saved ones only once it ended, since a turn
  ending meanwhile would save it again. The panel lists open and saved conversations together, the one talked in last first
  (`AssistantChat.lastMessageAt`).
- **Updates:** an agent announces its slash commands before anything collects a conversation's `updates`, so
  `AgentConversation.availableCommands` keeps the first announcement.
- **Downloads** (`BinaryAgentInstaller`, `NodeRuntime`) are checked against the published SHA-256 where there is one and refuse
  archive entries outside their folder.
- **Turns:** a turn that changed the project ends with Keep and Undo, which reverts the conversation's own changes through the undo
  history while they are the newest (`CommandExecutor.undoTurn`).
