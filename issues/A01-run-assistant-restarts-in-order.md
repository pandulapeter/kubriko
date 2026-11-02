<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Run the assistant's restarts, project changes, and turning off as one ordered sequence that replaces only the agent it means to, and let a tool server stopped while starting stay stopped

**Challenged:** amended — `AcpAgent.close(conversation)` now waits for `session/close` at most a bounded time, since the ordered sequence would otherwise hang forever on an agent that never answers (its own first test did); test adjusted, one AcpAgent test added.

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all
**Findings:** AI-1, AI-11, AI-15 (the ordering part)  ·  **Depends on:** none
**Files:**
- `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/AssistantController.kt`
- `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/AcpAgent.kt`
- `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/Assistant.kt`
- `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/AgentChoice.kt` (only to keep `AssistantController.kt` under 500 lines, see Fix 5)
- `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/mcp/McpToolServer.kt`
- tests: `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/FakeAgent.kt`, `AssistantControllerTest.kt`, `AcpAgentTest.kt`, `AssistantTest.kt`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/mcp/McpToolServerTest.kt`
- `creator/CLAUDE.md` and `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/CLAUDE.md` (Assistant section, one sentence)

All paths below are relative to `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

Note: another fixer is restructuring `creator/CLAUDE.md` and has added a guide at `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/CLAUDE.md`; at execution time, put each sentence below wherever the topic now lives (the guide for assistant internals), and fix any name either file quotes that this plan changes (`check_doc_names.py --staged`).

## Problem

`ai/assistant/AssistantController.kt` starts three independent Main coroutines on a restart (agent switch, configuration change, API key or variables given/removed):

```kotlin
private fun restartAgent() {
    endChats()
    scope.launch { replaceAssistant() }
    newChat()
}

private fun endChats() {
    val ended = chatList.takeAll()
    scope.launch {
        ended.forEach { it.close() }
        // The agent's client keeps the IDs of conversations it couldn't close, and continuing one needs it to let go.
        if (ended.isNotEmpty() && assistant.value?.canCloseConversations == false) replaceAssistant()
        chatList.refreshSaved(project, _agent.value.id)
    }
}

private suspend fun replaceAssistant() {
    val replaced = assistantLock.withLock { assistant.value.also { assistant.value = null } } ?: return
    withContext(ioDispatcher) { replaced.close() }
}
```

`newChat()` creates an `AssistantChat` whose `connect()` calls `assistant()` in a third coroutine. `AssistantChat.close()` → `Assistant.closeConversation` → `AcpAgent.close(conversation)` suspends on `session/close` when the agent advertises `sessionCapabilities.close` (Claude Agent does). While it is suspended:

1. `replaceAssistant()` closes the old agent; the pending `session/close` then fails with the SDK's own `CancellationException` ("Protocol closed"), which `AcpAgent.close(conversation)` rethrows although its caller is still active:
   ```kotlin
   try {
       session?.close()
   } catch (exception: Exception) {
       if (exception is CancellationException) throw exception
   }
   ```
   The `endChats` coroutine dies: the remaining ended chats never close, and `chatList.refreshSaved(project, newAgentId)` never runs, so the saved-conversations menu keeps the previous agent's conversations (resuming one fails with CONVERSATION_LOST).
2. If `session/close` is answered first, `endChats` continues after the new chat may have created the new `Assistant`, reads `assistant.value?.canCloseConversations` on that new, not yet started assistant (`capabilities.value` is null, so `false`), and replaces it: the brand-new agent is closed under the new chat.
3. The same shape on `setProject` (`endChats()` alone) with an agent that can't close conversations: a chat opened right after (the panel's `LaunchedEffect` calls `newChat()` when there is none) gets the old assistant from `assistant()`, which `endChats` then replaces, losing the new conversation.
4. `turnOff()` runs the same three-way race (`endChats()`, `closeSignIn()`, `scope.launch { replaceAssistant() }`).

The agent's tool server has a related race (`ai/assistant/Assistant.kt`):

```kotlin
private suspend fun toolServerPort() = serverStart.withLock {
    port ?: withContext(ioDispatcher) { toolServer.start(port = 0, token = null) }.also { port = it }
}

override fun close() {
    agent.close()
    toolServer.stop()
}
```

and `mcp/McpToolServer.kt`:

```kotlin
private var server: EmbeddedServer<*, *>? = null
...
suspend fun start(port: Int, token: String?): Int {
    stop()
    ...
    val started = embeddedServer(CIO, host = HOST, port = port) { ... }
    server = started
    try {
        started.startSuspend(wait = false)
    } ...
}

fun stop() {
    server?.stop(gracePeriodMillis = 0, timeoutMillis = 1_000)
    server = null
}
```

`replaceAssistant` calls `close()` on IO while the first conversation may be inside `toolServer.start`. A `stop()` that runs before `server = started` (a plain, non-volatile `var` written from another thread) sees nothing, and the engine then starts and listens until Creator quits: one leaked CIO server with its thread pool per such restart. `McpController.close()` has the same shape at quit (harmless while the process exits, fixed by the same change).

`creator/src/test/.../ai/assistant/FakeAgent.kt` advertises no `sessionCapabilities.close` (`AgentCapabilities(loadSession = supportsLoading, mcpCapabilities = McpCapabilities(http = supportsHttpMcp))`), so `AssistantControllerTest` never takes the suspending path; that is why the tests pass.

## Fix

1. **`AcpAgent.close(conversation)`**: treat the SDK's cancellation as `connect()` already does, rethrowing only when the caller itself is cancelled, and wait for `session/close` at most `conversationCloseWait`:
   ```kotlin
   withTimeoutOrNull(conversationCloseWait) {
       try {
           session?.close()
       } catch (exception: CancellationException) {
           if (!currentCoroutineContext().isActive) throw exception
       } catch (_: Exception) {
       }
   }
   ```
   (The conversation is already `markLost()`; an agent that went away has nothing left to close.) `conversationCloseWait` is a new `internal` constructor parameter, `Duration = CLOSE_WAIT` with `private val CLOSE_WAIT = 2.seconds` beside `EXIT_WAIT` in the companion; tests pass a short one. **Why the bound is needed:** at HEAD `replaceAssistant` runs beside `endChats`, so closing the old agent is what ends a `session/close` the agent never answers. In the ordered sequence below the agent is replaced only after its chats closed, so without the bound one agent that never answers `session/close` would stop the sequence for good: the agent is never replaced, and every later `assistant()` (every new chat, the sign-in) waits forever. The bound's own `TimeoutCancellationException` is rethrown by the catch (the block's context is no longer active) and turned into null by `withTimeoutOrNull`.

2. **One ordered sequence in `AssistantController`** (Main-confined, like the rest of the class):
   - `private var lifecycle: Job? = null` and
     ```kotlin
     private fun afterPending(block: suspend () -> Unit) {
         val previous = lifecycle
         lifecycle = scope.launch {
             previous?.join()
             try { block() } catch (exception: CancellationException) { if (!currentCoroutineContext().isActive) throw exception } catch (_: Exception) { }
         }
     }
     ```
     so a failure in one step never stops the next restart from running (and never fails the app's Main scope). The per-chat `try` in `closeChats` has no failing-without-the-fix test of its own (the old agent's replacement hides which chat closed); `switchingAgentsWhileAConversationIsClosing…` below covers the coroutine that died.
   - `private suspend fun closeChats(ended: List<AssistantChat>)` closes each chat in its own `try` (same cancellation rule), so one failing chat doesn't skip the rest.
   - `replaceAssistant(expected: Assistant? = null)`: under `assistantLock`, detach `assistant.value` only if `expected == null || assistant.value === expected`, then close the detached one on `ioDispatcher`. Waiting for the lock waits out an `assistant()` creation in progress, so a restart replaces whatever exists once that creation ends.
   - `restartAgent()`:
     ```kotlin
     val ended = chatList.takeAll()
     afterPending {
         closeChats(ended)
         replaceAssistant()
         chatList.refreshSaved(project, _agent.value.id)
     }
     newChat()
     ```
     `newChat()` stays synchronous so the panel shows the new chat at once.
   - `endChats()` (used by `setProject`): capture `val owner = assistant.value` synchronously, then `afterPending { closeChats(ended); if (ended.isNotEmpty() && owner != null && !owner.canCloseConversations) replaceAssistant(expected = owner); chatList.refreshSaved(project, _agent.value.id) }`. The capabilities are read on the agent those conversations ran on, never on a newer one.
   - `turnOff()`: `_variablesForm.value = null`, `closeSignIn()`, then the same `afterPending` as `restartAgent()` without `newChat()` (this is the AI-15 ordering part; the single AI-off gate is A02a's).
   - `assistant()` first waits for the sequence: `while (true) { val pending = lifecycle; if (pending == null || pending.isCompleted) break; pending.join() }`, then takes `assistantLock` and runs as now (`check(isAiEnabled.value)` stays inside the lock). A chat opened during a restart therefore always starts on the agent chosen after it. No deadlock: the sequence never calls `assistant()` (it closes chats through their own `end`), and a chat it ends is cancelled out of its wait.
   - `close()` (quit) stays as it is; H03 (orderly shutdown, AI-12) changes it.

3. **`McpToolServer` start/stop**: guard `server` with one lock object and a generation, so a `stop()` from any thread during `start` wins:
   - `private val lifecycle = Any()`, `private var generation = 0` and `server`, both only touched inside `synchronized(lifecycle)`.
   - `stop()`: inside the lock, `generation++` and take `server` (set it null); stop the taken engine outside the lock (`stop(gracePeriodMillis = 0, timeoutMillis = 1_000)` blocks up to a second).
   - `start()`: call `stop()`, then inside the lock record `val own = ++generation` and assign `server = started`; after `startSuspend` returns, inside the lock check `generation == own`; if not, stop `started` outside the lock and throw `IOException("The server was stopped while it started.")`. Keep the existing `CancellationException` → `IOException` mapping for a port that can't be bound. Document the new `@throws` case in the KDoc.
   - A test seam: an `internal` constructor parameter `beforeEngineStart: suspend () -> Unit = {}`, called right before `started.startSuspend`, used only by tests (the race can't be hit deterministically otherwise).

4. **`Assistant`**: `@Volatile private var isClosed = false`; `close()` sets it first. `toolServerPort()` throws `AgentClosedException()` if `isClosed` before starting, and maps an `IOException` from `toolServer.start` to `AgentClosedException()` when `isClosed` is set by then (otherwise rethrows it). `McpController` needs no change: a `close()` racing its start now leaves the state `Failed`, at quit.

5. **Size:** `AssistantController.kt` is at 491 lines and `check_sizes.py` fails a main Kotlin file over 500. Keep it at or under 500: move the private `choiceOf(settings, agents)` from its companion into `AgentChoice.Companion` as `internal fun of(settings: AssistantSettings, agents: List<RegistryAgent>): AgentChoice` (a pure move, same body), and fold `endChats`/`turnOff`/`restartAgent` onto the shared `afterPending` helper. A02a splits the class properly.

6. **`creator/CLAUDE.md`**, Assistant section (where it says the conversations "all end when the project closes or changes"): add one sentence: "Ending them, replacing the agent, and listing the saved conversations run as one sequence (a restart, a project change, AI turned off), which a conversation opened meanwhile waits for, so it always starts on the agent chosen last." Another fixer may be editing `creator/CLAUDE.md`; edit at execution time on top of what is there.

## Tests

`FakeAgent`: a new parameter `sessionClose: (suspend () -> Unit)? = null`. When non-null, `initialize` advertises `sessionCapabilities = SessionCapabilities(close = SessionCloseCapabilities())` and the fake session's `close(_meta)` override runs it, then returns `CloseSessionResponse()` (the SDK's `AgentSession.close` is only called when the capability is declared). Update the class KDoc.

`AssistantControllerTest` (real `AcpAgent`, `McpToolServer`, `FakeAgentLauncher`, a temp `SavedConversationStore` and registry as the file already does):
- `switchingAgentsWhileAConversationIsClosingStartsTheNewAgentAndListsItsConversations`: the test's `createAssistant` builds its `AcpAgent` with `conversationCloseWait = 300.milliseconds`; launcher whose first agent is `FakeAgent(sessionClose = { awaitCancellation() })` (its `session/close` is never answered: today closing the agent beside it fails it with "Protocol closed", and after the fix the wait ends it); a saved conversation exists for a second registry agent `B` in the project; with a Ready chat, `selectAgent(B)` → the new current chat reaches `ChatState.Ready` on `B`'s command, the first agent's process has stopped, and `savedConversations` equals `B`'s list. Fails today: the coroutine dies on the rethrown cancellation, so the list stays the old agent's.
- `aConversationOpenedWhileTheProjectChangesStartsOnAFreshAgent`: default `FakeAgent` (can't close conversations); with a Ready chat, `setProject(other)` and `newChat()` in the same Main block → the new chat reaches Ready and stays Ready (not CONVERSATION_LOST) after the sequence finished; exactly two agent processes were launched and the first one has stopped. Fails today (the new chat starts on the old agent, which is then replaced).
- `twoSwitchesInARowEndOnTheLastAgent`: `selectAgent(B)` then `selectAgent(C)` in one Main block → exactly one open chat, Ready, on `C`'s command; no agent process but `C`'s is still running.

`AcpAgentTest`:
- `closingAConversationWhileTheAgentStopsReturnsNormally`: agent with `sessionClose = { awaitCancellation() }` and the default wait; launch `agent.close(conversation)` (it suspends), then `agent.close()` → the launched call completes without exception and its coroutine is not cancelled.
- `aConversationTheAgentNeverClosesIsLeftAfterTheWait`: `conversationCloseWait = 200.milliseconds`, `sessionClose = { awaitCancellation() }`; `agent.close(conversation)` returns normally within a second, while the agent's process keeps running (it is not closed by this). Fails today (the call never returns).

`McpToolServerTest`:
- `aServerStoppedWhileStartingStopsListening`: pick a free port (`ServerSocket(0).use { it.localPort }`), build the server with `beforeEngineStart = { gate.await() }`, launch `start(port, token)`, wait until the seam is reached, call `stop()`, complete the gate → `start` throws `IOException`, and `ServerSocket(port, 1, InetAddress.getByName(McpToolServer.HOST)).close()` succeeds (nothing listens). Fails today: the engine keeps listening.

`AssistantTest`:
- `closingWhileTheToolServerStartsFailsTheConversationAsClosed`: an `Assistant` whose `McpToolServer` has a gated `beforeEngineStart`; launch `newConversation(...)`, wait for the seam, `close()`, release → `newConversation` throws `AgentClosedException`.

Run with `./gradlew :creator:test` (the executor runs it; no live agent tests are needed).

## Manual check

Optional, and it uses the user's Claude subscription, so only if the user wants it: in Creator with Claude Agent, open a conversation, then switch to another registry agent and back; the conversation menu lists each agent's own saved conversations, and Activity Monitor shows no leftover `claude-agent-acp` process from the first agent.
