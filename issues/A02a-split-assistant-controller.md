<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Split AssistantController into AgentSelection, AssistantHost, and AssistantSignIn behind the same façade, with one AI-off gate and settings saved off the main thread

**Challenged:** amended — dropped the controller-level AI-off test (it couldn't gate a non-suspending `createAssistant` and passes at HEAD); `AssistantHost.close()` now refuses later `get()`s, closes a creation finishing after it, and is safe off Main, which H03a's quit relies on; host tests added.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** AI-9, AI-15 (the gate), AI-5 (the `store.save` calls still on Main)  ·  **Depends on:** A01
**Files:**
- `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/AssistantController.kt`
- new: `ai/assistant/AgentSelection.kt`, `ai/assistant/AssistantHost.kt`, `ai/assistant/AssistantSignIn.kt`, `ai/assistant/AssistantSettingsState.kt`
- `ai/assistant/AgentChoice.kt` (if A01 moved `choiceOf` there, `AgentSelection` uses it)
- tests: `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/AssistantControllerTest.kt` (unchanged, the safety net; only new cases for the gate), new `AgentSelectionTest.kt`, `AssistantHostTest.kt`, `AssistantSignInTest.kt`, `AssistantSettingsStateTest.kt`
- `creator/CLAUDE.md` and `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/CLAUDE.md` (the panel paragraph and the `ai/assistant` row of the package table)

Paths are relative to `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

Note: another fixer is restructuring `creator/CLAUDE.md` and has added a guide at `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/CLAUDE.md`; at execution time, put each sentence below wherever the topic now lives (the guide for assistant internals), and fix any name either file quotes that this plan changes (`check_doc_names.py --staged`).

## Problem

`ai/assistant/AssistantController.kt` (about 500 lines after A01, the size check's limit; a 19-parameter constructor) does six jobs at once:

- agent choice and the registry: `agents`, `agent`, `customAgentCommand`, `selectAgent`, `loadAgents`, `updateAgents`, `agentsLoaded`, `choiceOf`;
- the one `Assistant`'s lifecycle: `assistant()`, `assistantLock`, `replaceAssistant`, `restartAgent`, `endChats`, `turnOff`, `close`, and A01's `lifecycle`/`afterPending`;
- Claude configurations and the API key (delegated to `ClaudeAccount`, but re-exposed: `configurations`, `configuration`, `selectConfiguration`, `hasApiKey`, `canKeepApiKey`, `apiKeyFailed`, `setApiKey`, `removeApiKey`);
- signing in: `signIn`, `_signIn`, `closeSignIn`, `statusBeforeSignIn`, `needsSignIn`, `authStatus`, `signInMethods`, `variablesForm`, `variablesFailed`, `setVariables`, `closeVariablesForm`, `agentVariables`;
- Node.js: `canInstallNode`, `nodeInstallFailure`, `installNode`;
- chats: `newChat`, `resume`, `openChat`, `select`, `close(chat)`, delegating to `AssistantChats`.

Every lifecycle bug of this sweep (AI-1, AI-5, AI-15, AI-18) lived here. The AI-off guards are per entry point and incomplete:

```kotlin
fun signIn(method: SignInMethod) {
    if (!isAiEnabled.value) return
...
private fun openChat(...) {
    if (!isAiEnabled.value) return
...
fun installNode() {
    if (!isAiEnabled.value) return
...
private suspend fun assistant(): Assistant = assistantLock.withLock {
    check(isAiEnabled.value) { "AI features are off." }
```

while `selectAgent`, `selectConfiguration`, `setApiKey`, `removeApiKey`, and `setVariables` write settings or secrets and restart with AI off, and an `assistant()` started by `signIn`'s agent method (`scope.launch { assistant().authenticate(method) ... }`, not a chat, so `turnOff` cancels nothing) can finish creating an agent after AI was turned off. Settings are still saved on the main thread:

```kotlin
settings.update { it.copy(agentId = ..., customAgentCommand = ...) }
store.save(settings.value)
```

in `selectAgent`, `selectConfiguration`, and `setVariables` (`AssistantSettingsStore.save` writes a file).

## Fix

A behaviour-preserving split. `AssistantController` keeps its whole public surface (every `val`/`fun` the panel, `AiPanel`, `WorkspaceScreen`, `AppContent`, `KubrikoCreatorApp`, and `AssistantControllerTest` use), delegating to four new internal classes. Its constructor stays as it is, so `KubrikoCreatorApp.assistantController(...)` doesn't change.

1. **`AssistantSettingsState(scope, store: AssistantSettingsStore, ioDispatcher)`**: holds `value: StateFlow<AssistantSettings>` (loaded once, as now with `store.load()` in the constructor; leave that one read where it is, it runs before the window opens) and `update(transform)`, which updates the flow on the caller's thread and saves on `ioDispatcher` under a `Mutex`, writing the value as it is by then (the `McpController.updateStored` pattern: saves can finish in any order, each writes the latest). Shared by the three parts below.

2. **`AgentSelection(scope, settings: AssistantSettingsState, registry: AgentRegistryStore?, ioDispatcher, isAiEnabled, isAgentRunning: () -> Boolean)`**: `agents`, `agent`, `customAgentCommand`, `select(choice): Boolean` (false when unchanged; persists `agentId`/`customAgentCommand` through `settings.update`), `awaitLoaded()`, and the registry load (`loadAgents`/`updateAgents` moved as they are; `updateAgents` switches the choice only while `!isAgentRunning()`).

3. **`AssistantHost<T : AutoCloseable>(scope, ioDispatcher, isAiEnabled, create: suspend () -> T)`**: the one running agent, generic so it is tested without a real agent (the controller uses `AssistantHost<Assistant>`). Moves A01's machinery here unchanged in behaviour: `current: StateFlow<T?>`, `get(): T` (waits for the pending sequence, then under the lock: `check(isAiEnabled.value)`, return the current one or `create()`), `replace(expected: T? = null)`, `afterPending(block)`, `awaitPending()` (joins the sequence as it is now, for H03a's quit), `close()` (quit, synchronous as today). **The single AI-off gate lives here:** `val isOn get() = isAiEnabled.value`, and after `create()` returns, if `!isAiEnabled.value || isClosed`, close the created agent on `ioDispatcher` and throw `AgentClosedException()`, so an agent whose start began just before AI went off, or before Creator quit, never stays running (a process an agent leaves behind outlives Creator's JVM). `close()`: sets `@Volatile private var isClosed = true` first, so a later `get()` throws `AgentClosedException` instead of creating one, then takes `current` atomically (`MutableStateFlow.getAndUpdate { null }`) and closes it. It touches nothing else (not `lifecycle`, which stays Main-confined), so it may run off the main thread: H03a calls it on IO at quit.

4. **`AssistantSignIn(scope, ioDispatcher, signInLauncher: AgentLauncher, secrets: SecretStore, settings: AssistantSettingsState, host: AssistantHost<Assistant>, resendRefused: () -> Unit, restart: () -> Unit)`**: `signIn` (the terminal session with A18's displacement logic, the agent method, the variables form), `session` (today's `signIn` flow), `closeSignIn`, `statusBeforeSignIn`, `authStatus`, `signInMethods`, `needsSignIn`, `variablesForm`, `variablesFailed`, `setVariables`, `closeVariablesForm`, `stop()` (for `turnOff`/`close`), and `environmentOf(choice: AgentChoice)` for the agent variables read on IO (`agentVariables.read(...)`). `resendRefused` is `{ chats.value.forEach { it.resendAfterSignIn() } }` and `restart` the controller's `restartAgent`.

5. **What stays in `AssistantController`**: the façade properties, `ClaudeAccount` (it is already its own class), Node install, the chats (`AssistantChats`, `newChat`, `resume`, `openChat`, `select`, `close(chat)`, `setProject`), `restartAgent`/`endChats`/`turnOff` written on `host.afterPending`, and `createAssistant` wiring (`host`'s `create` lambda: `selection.awaitLoaded()`, `commands.npx()`, Claude's `claude.environment()` or `signIn.environmentOf(choice)`, then `createAssistant(...)`). Every public mutating entry (`newChat`, `resume`, `selectAgent`, `selectConfiguration`, `signIn`, `setVariables`, `setApiKey`, `removeApiKey`, `installNode`) begins with `if (!host.isOn) return`; remove the scattered `isAiEnabled` checks this replaces. `findConfigurations`, `select(chat)`, `close(chat)`, `closeSignIn`, `closeVariablesForm`, and `setProject` stay ungated (they only end or show things).

6. Keep each file ≤ 500 lines and each function ≤ 120 (`.github/scripts/check_sizes.py`); KDoc on every new class as the `code-style` skill says (the classes are internal, but this package documents its collaborators). No public API of `tools/creator-api` or `tools/creator-session` changes.

7. **`creator/CLAUDE.md`**: in the panel paragraph, replace "`AssistantController` (app-level, created in `main`) holds the one `Assistant` from first use until Creator quits" with "`AssistantController` (app-level, created in `main`) is the panel's façade over `AgentSelection` (the agent chosen and the registry), `AssistantHost` (the one `Assistant` from first use until Creator quits, replaced in one ordered sequence, and the one check of AI features), and `AssistantSignIn` (sign-in, API key aside, and agent variables)"; add the four new names to the `ai/assistant` row of the package table; in the AI features bullet, name `AssistantHost` as where the assistant's AI-off check is. Another fixer may be editing `creator/CLAUDE.md`; edit at execution time on top of what is there.

`AI-12` (closing the agent on the UI thread at quit, the orphaned sign-in stop) is H03's; leave `close()` as synchronous as it is and only route it through `signIn.stop()`/`host.close()`.

## Tests

`AssistantControllerTest` stays as it is and must pass unchanged (the split is behaviour-preserving). New cases there for the gate:
- `withAiFeaturesOffChoosingAnAgentOrGivingAKeyChangesNothing`: AI off → `selectAgent(B)`, `selectConfiguration(alias)`, `setApiKey("sk-…")`, `setVariables(method, values)` → `agent.value` unchanged, the settings file content unchanged, the `FakeSecretStore` empty, and no agent process launched. Fails today (`selectAgent` saves and `setApiKey` writes the secret).
- No controller-level case for an agent created just before AI goes off: `createAssistant` isn't a suspending function and runs on the controller's main thread, so a test can't hold it open while AI is turned off, and at HEAD `turnOff`'s `replaceAssistant` already waits for `assistantLock` and closes what the creation made, so such a case wouldn't fail without the fix. The gate's failing test is `AssistantHostTest.aCreationFinishingAfterTurnOffIsClosedAndRefused` below, where the host alone (no `turnOff` sequence) must close what it made.

`AssistantHostTest` (pure, `AssistantHost<CountingCloseable>` with a test dispatcher):
- `getCreatesOnceAndSharesIt`: two concurrent `get()` → one `create` call, same instance.
- `replaceWithAnExpectedOneLeavesANewerOneRunning`: `get()` → a; `replace(expected = b)` where `b !== a` → a not closed, still current.
- `getWaitsForThePendingSequence`: `afterPending { gate.await(); replace() }`, then `get()` in another coroutine → `create` isn't called until the gate opens, and the instance returned is the new one.
- `aFailingStepDoesntStopTheNext`: `afterPending { error("x") }`, `afterPending { ran = true }` → `ran`.
- `aCreationFinishingAfterTurnOffIsClosedAndRefused`: gated `create`; `get()` launched; AI off; release → `get()` throws `AgentClosedException`, the instance is closed once, `current.value` null.
- `aCreationFinishingAfterCloseIsClosedAndRefused`: gated `create`; `get()` launched; `close()` from another thread (`Dispatchers.IO`); release → `get()` throws `AgentClosedException`, the instance is closed once; a later `get()` throws without calling `create`.
- `closeOffTheMainThreadClosesTheCurrentOne`: `get()` → a; `withContext(Dispatchers.IO) { host.close() }` → a closed once, `current.value` null.

`AgentSelectionTest` (real `AgentRegistryStore` over a temp folder, as `AssistantControllerTest.registry(...)` builds it):
- `aRememberedRegistryAgentIsChosenOnceTheCacheLoads`, `aFetchedRegistryDoesntSwitchTheAgentWhileOneRuns`, `choosingAnAgentRemembersIt` (reads the settings file back through a new `AssistantSettingsStore` on the same file).

`AssistantSettingsStateTest`: `savesHappenOffTheCallingThreadAndTheLastValueWins` (record the saving thread through a store subclass or a real store on a temp file plus a recording dispatcher; two quick updates → file holds the second).

`AssistantSignInTest` (`FakeTerminalLauncher`, `FakeSecretStore`): `aSecondSignInStopsTheFirst` and `variablesAreKeptThenTheAgentRestarts` moved down from the controller's level (keep the controller's versions too), plus `aFailedVariablesSaveKeepsTheFormOpen`.
