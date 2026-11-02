<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Move the labels the UI shows for model enums and commands into the UI, and tell the AI about changes by their tool names instead of UI strings

**Challenged:** amended — names the transcript's `creatorToolTitleOf` as a title user, and says the `CommandExecutor` line numbers predate lane K.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** ARCH-7 (labels and command titles)  ·  **Depends on:** M01
**Files:**
- `actors/BlockKind.kt`, `actors/ConditionKind.kt`, `actors/PropertyKind.kt`, `project/ResourceGroup.kt`, `template/ProjectTemplate.kt`
- `commands/EditorCommand.kt`, `commands/Invocation.kt`, `commands/ChangeRecord.kt`, `commands/ChangeLog.kt`, `commands/AiStep.kt`,
  `commands/CommandHistory.kt`, `commands/CommandExecutor.kt`, and every command object under `commands/**` (each loses its
  `override val title = Res.string.command_…` line and that import; 79 files, found with `grep -rl "override val title" commands`)
- `context/WriteSummary.kt`, `context/ChangesSummary.kt`
- new `ui/CommandTitles.kt`, new `ui/actorTypes/KindTitles.kt`, new `ui/ResourceGroupTitles.kt`, new `ui/ProjectTemplateTexts.kt`
- UI users: `ui/WorkspaceToolbar.kt`, `ui/mcp/AiActivityBar.kt`, `ui/assistant/TurnChangesCard.kt`, `ui/ProjectSidebar.kt`,
  `ui/FolderTreeSection.kt`, `ui/NewProjectScreen.kt`, `ui/actorTypes/{BlockPalette,BlockTypeEditor,ConditionParameters,PropertyChoice,PropertyList}.kt`,
  `ui/assistant/AssistantTranscriptList.kt` (`creatorToolTitleOf` reads `CommandRegistry.editor[it]?.title`)
  (and any other `grep -rn "\.title\b" ui` hit on these types)
- `ai/assistant/` users of `ChangeRecord.title` (`AssistantTranscript.kt`, `TranscriptItem.kt`) if they read it
- tests: new `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/CommandTitlesTest.kt`; update
  `context/ChangesSummaryTest.kt`, `commands/CommandExecutorTest.kt`, `commands/git/GitCommandsTest.kt`,
  `ai/assistant/AssistantChatTest.kt`, `ui/AiFollowingTest.kt`, `ui/AiFeaturesOffTest.kt`, `ui/assistant/ToolCallStepsTest.kt`,
  `context/EvalSetTest.kt`/`TokenBudgetTest.kt` if they pin a summary's text
- `creator/CLAUDE.md` (AI section, one sentence)

Paths are relative to `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

## Problem

The model and the commands hold UI strings, which ties every future Gradle module to the app's generated `Res` (ARCH-7: 201 main
files import it) and makes what the AI reads depend on the UI's wording. The labels:

```kotlin
// actors/BlockKind.kt
internal enum class BlockKind(
    val title: StringResource,
    val createDefault: () -> BlockDefinition,
    val needsActor: Boolean = false,
) {
    SET_VELOCITY(Res.string.block_set_velocity, { BlockDefinition.SetVelocity(x = 200f, y = 0f) }, needsActor = true),
```

(`ConditionKind.title`, `PropertyKind.title`, `ResourceGroup.title`, `ProjectTemplate.title`/`description` are the same), and the
command titles:

```kotlin
// commands/EditorCommand.kt
    /**
     * How the user interface names the command: in the titles of undo and redo, and while the AI runs it.
     */
    val title: StringResource
```

which reach the AI through `getString`:

```kotlin
// context/WriteSummary.kt
    put("changed", getString(title) + (target?.let { " at " + it.notation() } ?: ""))
// context/ChangesSummary.kt
                append(getString(record.title))
```

The UI already has the right pattern for one model enum, `ui/actorTypes/BlockTypeTexts.kt`:

```kotlin
internal val EventType.title: StringResource
    get() = when (this) {
        EventType.GAME_STARTED -> Res.string.event_game_started
```

## Fix

Labels are UI; the model and commands keep only identity. Errors and console texts are M02b.

1. **Model enums.** Remove the `title` (and `ProjectTemplate.description`) constructor parameters and their `Res` imports from
   `BlockKind`, `ConditionKind`, `PropertyKind`, `ResourceGroup`, and `ProjectTemplate`. Add UI-side extension properties with an
   exhaustive `when` (no `else`), like `EventType.title`:
   - `ui/actorTypes/KindTitles.kt`: `internal val BlockKind.title: StringResource`, `ConditionKind.title`, `PropertyKind.title`
     (keep the file under 500 lines; `BlockKind` has ~30 entries). If G02 replaced `BlockKind`'s constructor with a Block
     descriptor that carries the title, move the title out of the descriptor the same way.
   - `ui/ResourceGroupTitles.kt`: `ResourceGroup.title`; `ui/ProjectTemplateTexts.kt`: `ProjectTemplate.title`,
     `ProjectTemplate.description`.
   UI call sites keep `stringResource(kind.title)` and only gain the import.
2. **Command titles.** Remove `EditorCommand.title` and each command's `override val title`. Identity is the command's `name`
   (its AI tool name). Change the carriers to the name:
   - `ChangeRecord.title: StringResource` → `commandName: String`; `ChangeLog.record(title, …)` → `record(commandName, …)`;
     `CommandExecutor` passes `invocation.command.name` (`:85`, `:133` at `cbf3b7140`; K02a, K03, K05, and K09a rewrite the
     file, so find the `record(` calls by name).
   - `AiStep.title` is removed: `AiStep.toolName` already holds the same name (`CommandExecutor.kt:168,192` set both).
   - `CommandHistory.nextUndoTitle`/`nextRedoTitle: StringResource?` → `nextUndoCommandName`/`nextRedoCommandName: String?`.
   - `Invocation.title` is removed.
   Add `ui/CommandTitles.kt`:
   ```kotlin
   /**
    * How the user interface names each command (undo and redo, the AI activity bar, a turn's changes), by its tool name.
    */
   internal fun commandTitle(name: String): StringResource? = commandTitles[name]

   private val commandTitles = mapOf(
       PlaceActor.name to Res.string.command_place_actor,
       …
   )
   ```
   keyed by `<Command>.name` (not string literals), one entry per command, generated from the removed `override val title` lines.
   UI users (`WorkspaceToolbar` undo/redo labels, `AiActivityBar` step title, `TurnChangesCard`, `creatorToolTitleOf`) call `commandTitle(…)` and fall
   back to the raw name for an unknown one (a command added without a title is caught by the test below, never by users).
3. **The AI reads tool names.** `writeSummary(revision, title: StringResource, target)` → `writeSummary(revision, commandName: String,
   target)` writing `put("changed", commandName + (target?.let { " at " + it.notation() } ?: ""))`, and `ChangesSummary.of` appends
   `record.commandName`. Both functions stop being `suspend` if nothing else in them suspends (check callers; `getString` was
   the only suspension in `ChangesSummary.of`'s loop). The agent calls the same names as tools, so `"changed": "place_actor at
   Scene level"` is at least as clear to it as the old title, and no longer changes with the UI's wording.
4. `creator/CLAUDE.md`, AI section: one sentence, "What the AI reads is never a UI string: changes are named by their tool names
   (`ChangesSummary`, `writeSummary`), and the UI maps names and model enums to its strings (`ui/CommandTitles.kt`,
   `ui/actorTypes/KindTitles.kt`)." Put it wherever the AI's text rules now live.

No string is added or removed; `StringResourcesTest.everyStringAndIconIsUsed` keeps passing because each moved `Res.string.…`
reference still appears in the sources.

## Tests

- `CommandTitlesTest.everyRegisteredCommandHasATitle`: for every command of the app's `CommandRegistry` (construct it as
  `CommandRegistryTest` does), `commandTitle(command.name)` is not null; fails if a new command has no entry.
- `CommandTitlesTest.everyTitleBelongsToARegisteredCommand`: no map key outside the registry's names (a renamed tool can't leave a
  dead entry). Expose the keys through an `internal val commandTitleNames: Set<String>` or test through `commandTitle` with the
  registry's names plus one unknown name (`assertNull(commandTitle("no_such_tool"))`).
- `ChangesSummaryTest`: update its expected strings to the literal tool names (for example `"Changed in the editor since your last
  turn, by the user unless marked, oldest first: set_property_value at Ball#0; undid place_actor at Scene level. …"`), written
  as literals, not built from the code under test.
- A `WriteSummaryTest` (or an existing `commands` test that checks a write's result) asserting `"changed"` equals
  `"add_block at Ball#0.2"` for an added Block (pick the real tool name and notation at execution).
- Update the other listed tests mechanically (`AiStep(title = …)` constructions, `nextUndoTitle` assertions become name
  assertions against the literal tool name). `EvalSetTest` and `TokenBudgetTest` replay or count text: re-run them and update
  a recorded expectation only when it quotes a summary; say in the commit's report which changed.
