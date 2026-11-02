<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Give AssistantPanel a model interface instead of the controller, and take Claude-only features from the agent's profile instead of `isClaude` in the UI

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** AI-10  ·  **Depends on:** A02a
**Files:**
- `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ui/assistant/AssistantPanel.kt`
- `ui/assistant/SignInCard.kt`, `ui/assistant/AssistantSignInArea.kt` (only if the note's flag is passed down rather than read from `LocalAgent`)
- new: `ai/assistant/AssistantPanelModel.kt`
- `ai/assistant/AssistantController.kt` (implements the interface), `ai/assistant/AgentProfile.kt`, `ai/assistant/AgentChoice.kt`
- `ui/AiPanel.kt` (the parameter's name and type)
- tests: new `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/assistant/AssistantPanelTest.kt` (with a `FakeAssistantPanelModel` beside it), `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/AgentProfileTest.kt` (new)
- `creator/CLAUDE.md` and `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/CLAUDE.md` (one sentence in the panel paragraph)

Paths are relative to `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

Note: another fixer is restructuring `creator/CLAUDE.md` and has added a guide at `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/CLAUDE.md`; at execution time, put each sentence below wherever the topic now lives (the guide for assistant internals), and fix any name either file quotes that this plan changes (`check_doc_names.py --staged`).

## Problem

`ui/assistant/AssistantPanel.kt` takes the concrete controller and reads about twenty of its flows and functions directly:

```kotlin
internal fun AssistantPanel(
    controller: AssistantController,
    aiSteps: List<AiStep>,
    ...
    val chats by controller.chats.collectAsState()
    val chat by controller.currentChat.collectAsState()
    val authStatus by controller.authStatus.collectAsState()
    ...
```

so the panel can't be rendered in a test or preview without a real controller (an `AcpAgent`, a tool server, a registry). And which features exist for an agent is decided in the UI by its identity:

```kotlin
val isClaude = LocalAgent.current.isClaude
val hasApiKey = controller.hasApiKey.collectAsState().value && isClaude
...
configurations = controller.configurations.collectAsState().value.takeIf { isClaude }.orEmpty(),
...
canKeepApiKey = controller.canKeepApiKey && isClaude,   // twice: the account menu and the sign-in area
...
text = if (LocalAgent.current.isClaude) stringResource(Res.string.assistant_note) else stringResource(Res.string.assistant_note_untested, LocalAgent.current.name),
```

and `ui/assistant/SignInCard.kt`:

```kotlin
text = if (LocalAgent.current.isClaude) stringResource(Res.string.assistant_sign_in_note) else stringResource(Res.string.assistant_sign_in_note_other, LocalAgent.current.name),
```

The API key and configurations are what Claude Agent's `ClaudeAccount` provides; the "note" texts are about whether the agent was tested (`RegistryAgent.isTested`), which `AgentMenu.kt` already reads (`if (this is AgentChoice.Registry && agent.isTested) name else ...`). Adding a second profiled agent would mean finding every `isClaude` in the UI.

## Fix

1. **Capabilities from the model, not the UI:**
   - `AgentProfile` gains `val usesClaudeAccount: Boolean` (KDoc: "Whether the agent signs in through a Claude Code configuration or the user's Anthropic API key (`ClaudeAccount`)"): `true` in `AgentProfile.claude`, `false` in `fallback`.
   - `AgentChoice` gains `val isTested get() = this is Registry && agent.isTested`; `AgentMenu.kt`'s `label()` uses it.
   - In `AssistantController` (the façade after A02a), `hasApiKey`, `canKeepApiKey`, and `configurations` already account for the agent: `hasApiKey` = `claude.hasApiKey` combined with `agent.profile.usesClaudeAccount`, `canKeepApiKey` likewise, `configurations` empty for an agent without it. The panel then reads them as they are.
   - The two notes read `LocalAgent.current.isTested` (the same texts; Claude Agent is the only tested agent, so what the user sees doesn't change).
   - `isClaude` stays in `ai/assistant` where it picks the profile; no `isClaude` is left under `ui/`.

2. **`AssistantPanelModel`** (new internal interface in `ai/assistant`, KDoc'd): exactly the members the panel uses: `agent`, `agents`, `customAgentCommand`, `chats`, `currentChat`, `savedConversations`, `authStatus`, `signInMethods`, `needsSignIn`, `signIn` (the session), `hasApiKey`, `canKeepApiKey`, `apiKeyFailed`, `configuration`, `configurations`, `download`, `canInstallNode`, `nodeInstallFailure`, `variablesForm`, `variablesFailed`, `errorLogFile`, and the functions `findConfigurations`, `newChat`, `resume`, `select`, `close(chat)`, `selectAgent`, `selectConfiguration`, `signIn(method)`, `closeSignIn`, `setApiKey`, `removeApiKey`, `installNode`, `setVariables`, `closeVariablesForm`. `AssistantController` implements it (`override` on the existing members; no behaviour change). `AssistantChat` stays a concrete class in the interface's types (the panel already renders it directly in `ChatContent`).
   - `AssistantPanel(model: AssistantPanelModel, ...)`; `AssistantPanelContent` takes the same; replace `controller.` with `model.`. `AiPanel` keeps taking `AssistantController` (it also hands it nowhere else) and passes it as `model`. `WorkspaceScreen`/`AppContent` don't change.
   - Alternative considered: an immutable `AssistantPanelState` data class plus a callbacks object, collected by the caller. Rejected for now: it collects every flow at the top, recomposing the whole panel on each change, where the interface keeps today's per-flow `collectAsState()`.

3. Strings: none change (`strings.xml` untouched). `StringResourcesTest` keeps passing.

4. **`creator/CLAUDE.md`**, panel paragraph: add "The panel reads an `AssistantPanelModel` (which `AssistantController` implements), and what a given agent offers, an API key and Claude Code configurations, comes from its `AgentProfile.usesClaudeAccount`, not from the UI." Another fixer may be editing `creator/CLAUDE.md`; edit at execution time on top of what is there.

## Tests

`AgentProfileTest`:
- `onlyClaudeAgentUsesAClaudeAccount`: `AgentChoice.Registry(RegistryAgent.claude).profile.usesClaudeAccount` is true; a registry agent with another ID and `AgentChoice.Custom("goose acp")` are false.
- `onlyATestedRegistryAgentIsTested`: `isTested` for Claude Agent true, an untested registry agent false, a custom command false.

`AssistantPanelTest` (Compose `uiTest` from `ui/UiSnapshots.kt`/`ui/uiTest`, as `AssistantPanelPartsTest` uses), with `FakeAssistantPanelModel` (plain `MutableStateFlow`s, recorded calls; no agent, no server):
- `anUntestedAgentShowsTheUntestedNoteAndNoClaudeOptions`: agent = an untested registry agent, the fake reporting what the controller reports for it (`configurations` empty, `canKeepApiKey` false, `hasApiKey` false) → no API key item, no configuration choice, and the untested note with the agent's name (`assistant_note_untested`). (Which values the controller reports per agent is pinned by the `AssistantControllerTest` case below; this test pins that the panel no longer adds an `isClaude` rule of its own.)
- `claudeAgentOffersItsApiKeyAndConfigurations`: agent = Claude Agent, configurations listed, `canKeepApiKey` true → the configuration menu and the API key item show, with `assistant_note`.
- `theFirstShowingStartsAConversation`: an empty `currentChat` → the fake records one `newChat()` and one `findConfigurations()`.
- `choosingACustomAgentPassesItsCommand`: enter a command in `CustomAgentForm` → the fake records `selectAgent(AgentChoice.Custom(command))`.

`AssistantControllerTest` gains `anAgentWithoutAClaudeAccountReportsNoApiKeyOrConfigurations`: with a remembered API key in the `FakeSecretStore` and a non-Claude registry agent chosen → `hasApiKey.value` false, `canKeepApiKey` false, `configurations.value` empty; with Claude Agent chosen → `hasApiKey.value` true. Fails today (the controller reports the key for any agent; only the UI hid it).

Look up the exact text of the API key item and the configuration menu in `composeResources/values/strings.xml` (`assistant_*` keys) and assert against `stringResource` lookups in the test, not hand-typed copies.
