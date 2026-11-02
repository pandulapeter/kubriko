<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Tell the agent to pass `full: true` whenever it lacks an earlier read's result, and forget a conversation's reads when a turn is cancelled

**Challenged:** amended — the cancel test now waits for the prompt to reach the agent, since 91040c959's `stop()` cancels a turn still gathering context before it is sent (no reads, nothing to forget); a case pins that path.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** AI-3  ·  **Depends on:** none (lands after A01/A02a in this lane; the one wiring line goes wherever `openChat` is by then)
**Files:**
- `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/context/ResultDelta.kt`
- `mcp/McpToolServer.kt` (the `full` parameter's description, a `forgetReads` function)
- `ai/assistant/Assistant.kt`, `ai/assistant/AssistantChat.kt`, `ai/assistant/AssistantController.kt` (wiring)
- tests: `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/context/ResultDeltaTest.kt`, `mcp/McpToolServerTest.kt`, `ai/assistant/AssistantChatTest.kt`, new `mcp/ConversationReadsTest.kt`
- `creator/CLAUDE.md` and `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/CLAUDE.md` (the "Reads that know what was seen" bullet)

Paths are relative to `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

Note: another fixer is restructuring `creator/CLAUDE.md` and has added a guide at `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/CLAUDE.md`; at execution time, put each sentence below wherever the topic now lives (the guide for assistant internals), and fix any name either file quotes that this plan changes (`check_doc_names.py --staged`).

## Problem

`mcp/ConversationReads.kt` remembers each read's last result per conversation address and answers a repeat with a delta:

```kotlin
fun answer(conversationId: String, toolName: String, arguments: JsonObject, result: JsonElement, isFull: Boolean): JsonElement {
    val seen = reads.getOrPut(conversationId) { ConcurrentHashMap() }
    val key = toolName + JsonObject(arguments.toSortedMap())
    val previous = seen.put(key, result)
    return if (previous == null || isFull) result else ResultDelta.of(previous, result)
}
```

The conversation address (`McpToolServer.createServer(conversationId = ...)`) isn't one-to-one with what the model has in its context:

- Claude Agent's **game-explorer subagent** (`ai/assistant/ClaudeAgentOptions.kt`, recommended by `AssistantSystemPrompt.kt`) reads through the same address, and its whole point is that what it reads stays out of the conversation's context. A later read by the main agent of the same thing gets "Unchanged" for content it never saw.
- **Compaction** summarizes earlier full results away; later reads still answer with deltas.
- A **cancelled turn** may have run reads whose results the turn never delivered; they stay recorded.

The texts tell the agent it has already read the thing, so it rarely asks again (`context/ResultDelta.kt`):

```kotlin
const val UNCHANGED = "Unchanged since you last read it in this conversation. Pass full: true for the whole result."
private const val CHANGED = "Only what changed since you last read it in this conversation. Pass full: true for the whole result."
private const val LINES_CHANGED = "The lines that changed since you last read it in this conversation, - as they were and + as they are. Pass full: true for the whole result."
```

and `McpToolServer`'s `fullSchema` description says: `"Whether to return the whole result even if this conversation has read it before, rather than what changed since."`

Decided (`decisions.md`, "AI read memory"): reword Unchanged (and the delta texts) to tell the agent to pass `full` when it lacks the content, and forget reads on cancel. Deltas stay on; no separate address for the subagent.

## Fix

1. **Texts** in `ResultDelta`, each keeping its first sentence and ending with the same recovery sentence (keep them one line, they are sent on every delta):
   - `UNCHANGED = "Unchanged since you last read it in this conversation. If that result isn't in front of you (a subagent read it, or it was summarized), read again with full: true."`
   - `CHANGED = "Only what changed since you last read it in this conversation. If that result isn't in front of you (a subagent read it, or it was summarized), read again with full: true."`
   - `LINES_CHANGED = "The lines that changed since you last read it in this conversation, - as they were and + as they are. If that result isn't in front of you (a subagent read it, or it was summarized), read again with full: true."`
   Extract the shared sentence as a `private const val RECOVERY` and build the three with string templates of constants (`const val` concatenation is allowed). Mind `ResultDelta.of`'s rule that a delta is used only when shorter than the whole result: the longer text makes small results come back whole a little more often, which is fine.
   - `McpToolServer.fullSchema` description: `"Whether to return the whole result even if this conversation has read it before, rather than what changed since. Pass it whenever you don't have the earlier result, for example after a subagent read it."`

2. **Forget on cancel**:
   - `McpToolServer.forgetReads(conversationId: String)`: `conversationReads.forget(conversationId)`, leaving the token (the conversation keeps its tools). KDoc: what the conversation read is answered in full again.
   - `Assistant.forgetReads(conversation: AgentConversation)`: `toolConversations[conversation]?.let(toolServer::forgetReads)`.
   - `AssistantChat`: a constructor parameter `forgetReads: (AgentConversation) -> Unit = {}` (KDoc'd with the others). In `prompt()`, after `conversation.prompt(...)` returns, `if (response.stopReason == StopReason.CANCELLED) forgetReads(conversation)`; and in the `catch (exception: CancellationException)` branch that keeps going (`AGENT_STOPPED`), call it too. Forgetting at the turn's end rather than in `stop()` catches the reads still in flight between the cancel request and the turn ending.
   - `AssistantController.openChat`: pass `forgetReads = { conversation -> owner?.forgetReads(conversation) }` beside `end`/`toolsIdOf`.
   - Compaction: Creator gets no signal for it from Claude Agent today; the reworded text covers it. Don't add detection.

3. **`creator/CLAUDE.md`**, the "Reads that know what was seen" bullet: after "(`ConversationReads`, forgotten with the conversation" add "and when a turn is cancelled", and add a sentence: "Every delta tells the agent to read again with `full: true` when it no longer has the earlier result, since a subagent (Claude Agent's game explorer) reads through the same address and compaction drops old results." Another fixer may be editing `creator/CLAUDE.md`; edit at execution time on top of what is there.

## Tests

- `ResultDeltaTest`: update the literal in the existing object-delta assertion (`{"sinceYourLastRead":"Only what changed since you last read it in this conversation. Pass full: true for the whole result.",...}`) to the new text written out as a literal, and add `everyDeltaSaysHowToGetTheWholeResultBack`: the unchanged answer, an object delta's `sinceYourLastRead`, and a text delta's first line each end with the literal `"read again with full: true."`.
- `ConversationReadsTest` (new, pure): `aRepeatedReadIsADeltaUntilForgotten`: answer twice → second is `JsonPrimitive(ResultDelta.UNCHANGED)`; `forget(id)`; third → the whole result. `forgettingOneConversationKeepsTheOthers`.
- `McpToolServerTest`: update the literal in `aConversationRereadingSomethingGetsOnlyWhatChangedUnlessItAsksForAll`; add `forgettingAConversationsReadsAnswersInFullAgainAndKeepsItsTools`: over a real conversation address on a real port, read `read_actor_type` twice (second is `UNCHANGED`), `server.forgetReads(conversation.id)`, read again → contains `"appearance"` (the whole type), and the call is authorized (the token still works).
- `AssistantChatTest`: `aCancelledTurnForgetsWhatTheConversationRead`: a chat built with `forgetReads = { forgotten += it }`; send `wait` (FakeAgent works until cancelled), wait until the prompt has reached the agent (the transcript shows the FakeAgent's first update for it, as the file's other stop tests wait), then `stop()`, wait until not busy → `forgotten` holds the chat's conversation once. Stopping earlier would take 91040c959's path instead: a turn still gathering its context is cancelled before anything is sent, so it has read nothing and forgets nothing (keep that path as it is; add `aTurnStoppedBeforeItIsSentForgetsNothing`: `send` and `stop()` in one main-thread block → `forgotten` empty, the transcript ends the turn as cancelled). `aFinishedTurnForgetsNothing`: send `hello` → `forgotten` empty.

Run with `./gradlew :creator:test`; no live agent test is needed.
