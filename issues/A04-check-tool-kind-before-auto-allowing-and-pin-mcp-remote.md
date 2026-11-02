<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Auto-allow a Creator tool call only when its kind doesn't change files or run commands, and pin mcp-remote to one exact version

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** AI-17  ·  **Depends on:** none
**Files:**
- `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/PermissionHandler.kt`
- `ai/assistant/ConversationToolServer.kt`, `mcp/clients/AiConfigFileClient.kt`, `mcp/McpToolServer.kt` (the shared constant)
- tests: new `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/PermissionHandlerTest.kt`, `ai/assistant/CreatorToolNameTest.kt` (its handler test moves to the new file), `mcp/clients/AiConfigFileClientTest.kt`, new `ai/assistant/ConversationToolServerTest.kt`
- `creator/CLAUDE.md` and `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/CLAUDE.md` (the Assistant section's permission sentence and the `mcp-remote` mentions)

Paths are relative to `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

Note: another fixer is restructuring `creator/CLAUDE.md` and has added a guide at `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/CLAUDE.md`; at execution time, put each sentence below wherever the topic now lives (the guide for assistant internals), and fix any name either file quotes that this plan changes (`check_doc_names.py --staged`).

## Problem

`ai/assistant/PermissionHandler.kt` trusts the tool call's title alone:

```kotlin
fun allowingCreatorTools(other: PermissionHandler) = PermissionHandler { toolCall, options ->
    val allow = options.firstOrNull { it.kind == PermissionOptionKind.ALLOW_ONCE } ?: options.firstOrNull { it.kind == PermissionOptionKind.ALLOW_ALWAYS }
    if (allow != null && CreatorToolName.of(toolCall.title) != null) RequestPermissionOutcome.Selected(allow.optionId) else other.answer(toolCall, options)
}

private val changingKinds = setOf(ToolKind.EDIT, ToolKind.DELETE, ToolKind.MOVE, ToolKind.EXECUTE)
```

and `ai/assistant/Assistant.kt` wraps it outermost:

```kotlin
PermissionHandler.allowingCreatorTools(if (profile.keepsOffFiles) permissions else PermissionHandler.rejectingChanges(permissions)),
```

The title is whatever the agent writes, so a tool call of kind `EDIT`/`EXECUTE` titled like `kubriko-creator_read_scene` is allowed before `rejectingChanges` sees its kind. The agent process is trusted to run at all, so this guards against a confused or buggy agent, not a hostile one.

`mcp-remote` floats within a range in two places:

```kotlin
// ai/assistant/ConversationToolServer.kt
private const val MCP_REMOTE = "mcp-remote@0.15"
// mcp/clients/AiConfigFileClient.kt (companion)
const val MCP_REMOTE = "mcp-remote@0.15"
```

`npx -y mcp-remote@0.15` resolves to any `0.15.x` at run time, for every assistant conversation of an agent without HTTP MCP and for Claude Desktop, so a new patch release runs unreviewed with the conversation's (or the MCP server's) token in its environment.

Decided (`decisions.md`, "AI permissions"): auto-allow Creator tools only for non-changing kinds; pin `mcp-remote` exactly; check how registry agents label MCP calls first.

## Fix

**Step 1 — check how registry agents label their MCP tool calls (no code; do this first).** Only agents that send `session/request_permission` for MCP tools matter (Claude Agent doesn't ask: its `allowedTools` in `ClaudeAgentOptions` allow Creator's tools). For each of these registry agents (IDs from `agent-registry.json` in Creator's settings folder, e.g. `~/Library/Application Support/Kubriko Creator/agent-registry.json`, whose entries link each agent's repository): `antigravity-acp`, `gemini`, `codex-acp`, `goose`, `github-copilot-cli`, `opencode`, `cursor`, read the ACP adapter's source (WebFetch on the GitHub repository: where it builds the `toolCall` of a `session/request_permission` for an MCP tool, e.g. Gemini CLI's `zedIntegration` mapping of a tool's `Kind` to the ACP `kind`, codex-acp's MCP tool call conversion) and note the `kind` it sends on the permission request's `toolCall` (not only on the earlier `tool_call` update). Write the findings into the commit's plan notes (not into the repository).

**Drop the kind check (land only the `mcp-remote` pin below, and add one KDoc sentence to `allowingCreatorTools` saying the agent's title is trusted on purpose, naming the agents found) if any of those agents labels a Creator MCP tool call, read or write, with `EDIT`, `DELETE`, `MOVE`, or `EXECUTE`** on its permission request (for example by mapping MCP tools without `readOnlyHint` to `EDIT`): under `rejectingChanges` that agent would then have every such Creator tool rejected, which breaks the assistant for it. If an agent's source is closed (Antigravity) and nothing else decides it, keep the kind check and list that agent under the optional manual check below.

**Step 2 — the kind check** (when Step 1 doesn't drop it): in `allowingCreatorTools`, auto-allow only when `toolCall.kind !in changingKinds` (null, `READ`, `SEARCH`, `THINK`, `FETCH`, `SWITCH_MODE`, and `OTHER`, every other value of the SDK's `ToolKind` in acp-model 0.34.0, pass). A Creator-titled call of a changing kind falls through to `other`: for Claude Agent (`keepsOffFiles`) that is the user's permission card; for every other agent `rejectingChanges` rejects it. Update the KDoc ("Allows Kubriko Creator's own tools once when their kind changes no files and runs nothing ..."). `changingKinds` stays private and shared by both functions.

**Step 3 — pin mcp-remote**: one constant, `McpToolServer.MCP_REMOTE = "mcp-remote@0.15.2"` (in its companion beside `SERVER_NAME`; `0.15.2` is the newest `0.15.x` on npm on 2026-11-02, `npm view mcp-remote versions`; `0.16.0` exists but a minor bump is its own reviewed change), with a KDoc line: "An exact version, bumped deliberately after reading its changes, since it runs with the token in its environment." `ConversationToolServer` and `AiConfigFileClient` use it and drop their own constants. Claude Desktop configuration files written before keep `mcp-remote@0.15` until the user connects Claude Desktop again (`AiClientsController.status` only checks that the entry exists); that is acceptable and needs no migration.

**`creator/CLAUDE.md`**: in the Assistant section, change "Every agent's requests for Creator's own tools are allowed without asking (`PermissionHandler.allowingCreatorTools`)" to "... allowed without asking when their kind edits, deletes, moves, and runs nothing (`PermissionHandler.allowingCreatorTools`)" (or, if Step 1 dropped the check, add "trusting the title the agent gives them"); where it mentions `mcp-remote`, add "pinned to one exact version (`McpToolServer.MCP_REMOTE`)". Another fixer may be editing `creator/CLAUDE.md`; edit at execution time on top of what is there.

## Tests

`PermissionHandlerTest` (new, pure, `runTest`; move `creatorsToolsAreAllowedWithoutAskingAndTheRestAreLeftToTheUser` here from `CreatorToolNameTest`, which keeps its name-recognition tests):
- `aCreatorToolThatChangesNothingIsAllowedOnce`: titles `kubriko-creator_read_scene` and `mcp__kubriko-creator__add_block` with kinds `null`, `READ`, `OTHER`, `FETCH`, `SEARCH`, `THINK` → `Selected(PermissionOptionId("allow"))`, and `other` was never asked.
- `aCreatorTitledCallThatEditsOrRunsSomethingIsLeftToTheNextHandler` (Step 2 only): kinds `EDIT`, `DELETE`, `MOVE`, `EXECUTE` with a Creator title → `other` asked with that call; and through `allowingCreatorTools(rejectingChanges(recording))` → `Selected(PermissionOptionId("reject"))`. Fails today.
- `otherToolsAreLeftToTheNextHandler` (the existing `run_command` case).

`ConversationToolServerTest` (new, pure): `anAgentWithoutHttpMcpRunsThePinnedMcpRemote`: `ConversationToolServer.of(AgentCapabilities(mcpCapabilities = McpCapabilities(http = false)), AgentProfile.fallback, url, "t", File("/usr/bin/npx"))` is an `McpServer.Stdio` whose `args` equal the literal `listOf("-y", "mcp-remote@0.15.2", url, "--header", "Authorization:\${KUBRIKO_CREATOR_AUTHORIZATION}")` and whose env holds `Bearer t`, not the args. `anAgentWithHttpMcpGetsTheAddressDirectly`: `http = true` → `McpServer.Http` with the `Authorization: Bearer t` header.

`AiConfigFileClientTest.claudeDesktopReachesTheServerThroughMcpRemoteRunByTheNpxTheShellFinds`: change the literal `"mcp-remote@0.15"` to `"mcp-remote@0.15.2"` (a literal, not the constant, so a careless bump fails a test).

## Manual check

Optional, and only if the user wants to spend an agent session on it: for any agent Step 1 couldn't decide from source (Antigravity), open a conversation in Creator, ask it to list the Scenes and add a Block, and confirm both Creator tools run without a permission card or a rejection.
