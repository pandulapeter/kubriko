<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Give `AssistantController` its storage, its agent sources, and the open project as three named types, with their production wiring moved out of `AppGraph`

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** all (desktop app)
**Written against:** `9bb8df2a6`, after F02–F04 (none of which touch these files). Re-locate code by the quoted snippets.
**Files:**
- `:creator:ai` (package `ai.assistant`, under `creator/ai/src/main/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/`):
  `AssistantController.kt`, `AssistantStorage.kt` (new), `AgentSources.kt` (new), `OpenProjectAccess.kt` (new); tests under `creator/ai/src/test/kotlin/com/pandulapeter/kubrikoCreator/ai/assistant/`:
  `AssistantControllerTest.kt`, `OpenProjectAccessTest.kt` (new)
- `:creator` (the app): `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/AppGraph.kt`
- `creator/plan/README.md`

## Problem

`AssistantController`'s primary constructor takes 18 parameters (limit 12, `check_sizes.py`):

```kotlin
public class AssistantController(
    private val scope: CoroutineScope,
    private val runner: ProcessRunner,
    private val store: AssistantSettingsStore,
    private val secrets: SecretStore,
    private val signInLauncher: AgentLauncher,
    private val ioDispatcher: CoroutineDispatcher,
    private val createAssistant: (AgentCommand, npx: File?, AgentProfile) -> Assistant,
    private val projectDigest: suspend () -> String? = { null },
    private val changeLog: () -> ChangeLog? = { null },
    private val undoTurn: suspend (toolsId: String, sinceRevision: Int) -> Int = { _, _ -> 0 },
    private val home: String = System.getProperty("user.home"),
    private val registry: AgentRegistryStore? = null,
    private val installer: BinaryAgentInstaller? = null,
    override val errorLogFile: File? = null,
    private val conversations: SavedConversationStore? = null,
    private val clock: () -> Long = System::currentTimeMillis,
    node: NodeRuntime? = null,
    private val isAiEnabled: StateFlow<Boolean> = MutableStateFlow(true),
) : AssistantPanelModel, AutoCloseable {
```

Its production wiring lives in `AppGraph.assistantController(…)` (`:creator`), which also knows how the defaults are built:

```kotlin
val errorLog = AgentErrorLog.default()
val installer = BinaryAgentInstaller.default()
return AssistantController(
    scope = scope,
    runner = ShellProcessRunner(),
    store = AssistantSettingsStore.default(),
    secrets = SystemSecretStore(),
    signInLauncher = ProcessAgentLauncher(File(System.getProperty("user.home"))),
    ioDispatcher = Dispatchers.IO,
    createAssistant = { command, npx, profile -> Assistant(…) },
    projectDigest = { executor()?.context?.let { ConversationStart.of(it) } },
    changeLog = { executor()?.changeLog },
    undoTurn = { toolsId, sinceRevision -> executor()?.undoTurn(toolsId, sinceRevision) ?: 0 },
    registry = AgentRegistryStore.default(),
    installer = installer,
    node = NodeRuntime(installer, File(BinaryAgentInstaller.defaultDirectory(), NodeRuntime.NODE_ID)),
    errorLogFile = errorLog.file,
    conversations = SavedConversationStore.default(),
    isAiEnabled = isAiEnabled,
)
```

## Fix

Three groupings in `ai.assistant`. Result: 11 parameters (`scope`, `runner`, `storage`, `signInLauncher`, `ioDispatcher`,
`createAssistant`, `openProject`, `sources`, `home`, `clock`, `isAiEnabled`).

- **`AssistantStorage`** (`store` → `settings`, `secrets`, `conversations`, `errorLogFile`): what the assistant keeps on this
  computer. They belong together because each is a place on the user's machine the assistant persists to (its settings file,
  the operating system's secret store, the saved conversations, and agents' error log), each has a `default()` location, and a
  test points all of them at one temporary folder or fake.
- **`AgentSources`** (`registry`, `installer`, `node`): where agents come from. They belong together because together they
  turn an agent choice into something runnable (the registry lists the agents, the installer downloads the ones distributed as
  archives, and `NodeRuntime` supplies `npx` for the rest, itself built on the installer), they are null together for "Claude
  Agent alone", and their production instances are built from one another.
- **`OpenProjectAccess`** (`projectDigest`, `changeLog`, `undoTurn`): what a conversation needs from the open project. They
  belong together because all three read or change the one project open at the time, through its `CommandExecutor`, and are
  absent together when no project is open. The production implementation moves from `AppGraph` into `:creator:ai`, where it is
  testable.

`ioDispatcher`, `home`, and `clock` stay separate: they are environment seams shared with nothing above, and grouping three
unrelated seams would only move the count.

### 1. `AssistantStorage.kt` (new)

```kotlin
/**
 * What the assistant keeps on this computer.
 *
 * @param settings The assistant's settings: the agent chosen, its configuration, and the like.
 * @param secrets Where the API key and agents' variables are kept.
 * @param conversations Where the conversations are kept per project, to be continued later, or null to keep none.
 * @param errorLogFile Where agents' standard error is kept ([AgentErrorLog]), for the user to open.
 */
public class AssistantStorage(
    internal val settings: AssistantSettingsStore,
    internal val secrets: SecretStore,
    internal val conversations: SavedConversationStore? = null,
    internal val errorLogFile: File? = null,
) {
    public companion object {
        /**
         * The files in Creator's settings folder and the operating system's secret store, with agents' error log at
         * [errorLogFile].
         */
        public fun default(errorLogFile: File?): AssistantStorage = AssistantStorage(
            settings = AssistantSettingsStore.default(),
            secrets = SystemSecretStore(),
            conversations = SavedConversationStore.default(),
            errorLogFile = errorLogFile,
        )
    }
}
```

(`errorLogFile` is a parameter of `default` because `AppGraph` shares the `AgentErrorLog` it belongs to with every `AcpAgent`.)

### 2. `AgentSources.kt` (new)

```kotlin
/**
 * Where agents come from; all null for Claude Agent alone.
 *
 * @param registry Where the registry's agents come from.
 * @param installer Downloads the agents the registry distributes as archives.
 * @param node Downloads a Node.js of Creator's own for a user without one.
 */
public class AgentSources(
    internal val registry: AgentRegistryStore? = null,
    internal val installer: BinaryAgentInstaller? = null,
    internal val node: NodeRuntime? = null,
) {
    public companion object {
        /**
         * The ACP Registry, with archives and Node.js downloaded into Creator's agents folder.
         */
        public fun default(): AgentSources {
            val installer = BinaryAgentInstaller.default()
            return AgentSources(
                registry = AgentRegistryStore.default(),
                installer = installer,
                node = NodeRuntime(installer, File(BinaryAgentInstaller.defaultDirectory(), NodeRuntime.NODE_ID)),
            )
        }
    }
}
```

### 3. `OpenProjectAccess.kt` (new)

```kotlin
/**
 * What a conversation needs from the project open at the time.
 */
public interface OpenProjectAccess {

    /** The digest of the open project, which a new conversation starts with, or null when none is open. */
    public suspend fun digest(): String?

    /** The open project's changes, which a prompt sums up for the agent when the user made some since its last turn. */
    public fun changeLog(): ChangeLog?

    /** Reverts what a conversation changed after a revision ([AssistantChat]'s Undo); how many steps it reverted. */
    public suspend fun undoTurn(toolsId: String, sinceRevision: Int): Int

    public companion object {
        /** No project, ever. */
        public val None: OpenProjectAccess = object : OpenProjectAccess {
            override suspend fun digest(): String? = null
            override fun changeLog(): ChangeLog? = null
            override suspend fun undoTurn(toolsId: String, sinceRevision: Int): Int = 0
        }

        /** The project whose commands [executor] gives, if one is open. */
        public fun of(executor: () -> CommandExecutor?): OpenProjectAccess = object : OpenProjectAccess {
            override suspend fun digest(): String? = executor()?.context?.let { ConversationStart.of(it) }
            override fun changeLog(): ChangeLog? = executor()?.changeLog
            override suspend fun undoTurn(toolsId: String, sinceRevision: Int): Int = executor()?.undoTurn(toolsId, sinceRevision) ?: 0
        }
    }
}
```

(`CommandExecutor` with `context`, `changeLog`, and `suspend fun undoTurn(conversationId: String, sinceRevision: Int): Int` is in
`:creator:commands`, which `:creator:ai` has as an `api` dependency; `ConversationStart.of(context: CommandContext): String` is a
`suspend fun` in `:creator:commands`' `context` package.)

### 4. `AssistantController.kt`

```kotlin
public class AssistantController(
    private val scope: CoroutineScope,
    private val runner: ProcessRunner,
    private val storage: AssistantStorage,
    private val signInLauncher: AgentLauncher,
    private val ioDispatcher: CoroutineDispatcher,
    private val createAssistant: (AgentCommand, npx: File?, AgentProfile) -> Assistant,
    private val openProject: OpenProjectAccess = OpenProjectAccess.None,
    private val sources: AgentSources = AgentSources(),
    private val home: String = System.getProperty("user.home"),
    private val clock: () -> Long = System::currentTimeMillis,
    private val isAiEnabled: StateFlow<Boolean> = MutableStateFlow(true),
) : AssistantPanelModel, AutoCloseable {
```

with `override val errorLogFile: File? = storage.errorLogFile` as a body property (next to the other `override val`s; it is
read only by the panel, after construction). Replace each use, keeping the initialization order of the body as it is:
- `AssistantSettingsState(scope, store, ioDispatcher)` → `storage.settings`;
- `AgentSelection(scope, settings, registry, …)` → `sources.registry`;
- `ClaudeAccount(scope, ioDispatcher, runner, secrets, home, …)` and `AssistantSignIn(… secrets = secrets, …)` → `storage.secrets`;
- `AssistantChats(scope, ioDispatcher, conversations)`, `conversations?.of(…)` in `lastConversationOf`, and
  `conversations?.save(…)` in `openChat` → `storage.conversations`;
- `AgentCommands(runner, installer, home, ioDispatcher, node)` → `AgentCommands(runner, sources.installer, home, ioDispatcher, sources.node)`;
- `LastConversation.withProject(projectDigest(), …)` in `newChat` → `openProject.digest()`;
- in `openChat`, `changeLog = changeLog` → `changeLog = { openProject.changeLog() }` and `undoTurn = undoTurn` →
  `undoTurn = { toolsId, sinceRevision -> openProject.undoTurn(toolsId, sinceRevision) }` (`AssistantChat` keeps its lambda
  parameters).

In the class KDoc, replace the `@param` lines of the moved parameters (`secrets`, `projectDigest`, `changeLog`, `undoTurn`,
`registry`, `installer`, `errorLogFile`, `conversations`, `node`) with "`@param storage` What the assistant keeps on this
computer.", "`@param openProject` What conversations read and undo in the open project.", and "`@param sources` Where agents
come from, all null for Claude Agent alone."

### 5. `AppGraph.kt` (`:creator`)

`assistantController(…)` becomes:

```kotlin
val errorLog = AgentErrorLog.default()
return AssistantController(
    scope = scope,
    runner = ShellProcessRunner(),
    storage = AssistantStorage.default(errorLogFile = errorLog.file),
    signInLauncher = ProcessAgentLauncher(File(System.getProperty("user.home"))),
    ioDispatcher = Dispatchers.IO,
    createAssistant = { command, npx, profile -> /* unchanged */ },
    openProject = OpenProjectAccess.of(executor),
    sources = AgentSources.default(),
    isAiEnabled = isAiEnabled,
)
```

Update the imports (add `AgentSources`, `AssistantStorage`, `OpenProjectAccess`; drop `AgentRegistryStore`,
`AssistantSettingsStore`, `BinaryAgentInstaller`, `NodeRuntime`, `SavedConversationStore`, `SystemSecretStore`, and
`ConversationStart` if nothing else uses them). The same objects are created as before, in the same call, so quitting and
startup are unchanged.

### 6. Docs

- The package guide `ai/assistant/CLAUDE.md` names no constructor parameter and gains nothing: the change adds no rule, and
  the guide is already past its 600-word budget.
- `creator/plan/README.md`: replace the constructors bullet "**Two constructors take more than 12 parameters**
  (`WorkspaceController` 23 and `AssistantController` 18), which the size check lets shrink but not grow. Direction: …" with
  "**`WorkspaceController`'s constructor takes 23 parameters**, which the size check lets shrink but not grow. Direction: group
  them by the part each feeds (`issues/`)."

## Tests

- `AssistantControllerTest.controller(…)` passes `storage = AssistantStorage(settings = AssistantSettingsStore(File(settingsDirectory,
  "assistant.json")), secrets = secretStore, conversations = SavedConversationStore(File(settingsDirectory, "conversations.json")))`
  and `sources = AgentSources(registry = registry, installer = installer)` in place of the separate arguments; nothing else
  changes. Grep the test class for any other `AssistantController(` or `errorLogFile` use first.
- New `OpenProjectAccessTest`: `OpenProjectAccess.of { null }` answers `null`, `null`, and `0`; and with an executor built from
  `commandContext()` (`:creator:commands`' test fixtures, already on `:creator:ai`'s test classpath), `changeLog()` is that
  executor's `changeLog` (`assertSame`) and `digest()` equals `ConversationStart.of(executor.context)`. Build the executor as
  `mcp/McpToolServerTest.kt` does: `CommandExecutor(commandContext(ActorTypeDefinition("Ball")), testScope())` inside `runTest`.
- Run `./gradlew :creator:allTests` and `./gradlew spotlessCheck`; after staging, `python3 .github/scripts/check_sizes.py` must
  print nothing.
