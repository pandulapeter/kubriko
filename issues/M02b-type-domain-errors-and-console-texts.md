<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Give every error and console text of the model, commands, and workspace a typed reason with fixed English for the AI, and word it for the user only in the UI

**Challenged:** amended — `NewNameField` (ui/components, below commands once M04 runs) stays UI-typed; S01's Scene refusal and warning strings get owners; texts later lanes added are named; the app may keep `StringResource` in `AppScreen`.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** ARCH-7 (errors, rejections, console statuses, MCP texts)  ·  **Depends on:** M02a
**Files:**
- new `foundation/DomainText.kt`; `console/ConsoleEntry.kt`
- model: `actors/{PropertyNaming,BlockTypeNaming,FolderPaths}.kt`, `project/{GameIdentity,ProjectInspectionException}.kt`,
  `template/{ProjectNaming,ProjectCreationException}.kt`, `scenes/{EntryPointLoadingException,SceneEditingState,SceneEditingController}.kt`
  (and new enum files next to each, listed in Fix)
- commands: `commands/{Rejection,Applied,CommandExecutor}.kt`, `commands/project/ConsoleText.kt`, every file constructing
  `Rejection(Res.string.…)` or `Applied.failed(Res.string.…)` (about 53 files: `grep -rln "Rejection(\|Applied.failed(" commands`),
  new `commands/CommandProblem.kt`
- `mcp/McpToolServer.kt`
- workspace/git/scenes statuses: `workspace/*.kt` (32 `ConsoleEntry.Status(` constructions), `git/GitController.kt`,
  `scenes/SceneEditingController.kt`, `commands/scenes/SceneRenaming.kt`, new `workspace/WorkspaceStatus.kt`, `git/GitStatus.kt`,
  `scenes/SceneStatus.kt`; `workspace/WorkspaceController.kt` (`createActorType`, `createManagerType`, `dispatchIfValid` return types),
  `workspace/ReadOnlyProjectRejection.kt`
- UI: new `ui/DomainTexts.kt`; `ui/Console.kt`, `ui/WorkspaceScreen.kt`, `ui/ReadOnlyProjectNotice.kt`, `ui/settings/ProjectSettingsDialog.kt`,
  `ui/settings/IdentityField.kt`, `ui/components/NewNameField.kt`, `ui/FolderTreeSection.kt`, `ui/actorTypes/{BlockTypeEditor,BlockTypeHeader,TimerChoice}.kt`,
  `ui/NewProjectScreen.kt`, `ui/WelcomeScreen.kt`, `AppScreen.kt`, `AppController.kt` (every `stringResource(x.reason, …)` or
  `StringResource?` validation callback; find them with `grep -rn "reason\b\|StringResource?" ui AppScreen.kt AppController.kt`)
- `creator/src/main/composeResources/values/strings.xml` (only `error_mcp_internal`, `error_mcp_no_project`, `mcp_tool_done` if
  nothing in the UI shows them any more)
- tests: new `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/DomainTextsTest.kt`; every test asserting
  `Rejection(Res.string.…)`/`ConsoleEntry.Status(Res.string.…)` (`grep -rln "Res.string" creator/src/test --include=*.kt`)
- `creator/CLAUDE.md` (AI and UI sections, one sentence each)

Paths are relative to `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

## Problem

Errors and statuses are `StringResource`s created deep in the model and commands, and the AI reads them through `getString`, so
translating the UI would change what the agent is told, and every future module would need the app's generated `Res`
(decisions.md: "typed domain errors (ARCH-7)"). At the current HEAD:

```kotlin
// commands/Rejection.kt
internal data class Rejection(
    val reason: StringResource,
    val arguments: List<String> = emptyList(),
)
// console/ConsoleEntry.kt
    data class Status(
        val text: StringResource,
        val isError: Boolean = false,
        val arguments: List<String> = emptyList(),
    ) : ConsoleEntry
// actors/PropertyNaming.kt
    fun validate(name: String, existingNames: Collection<String>): StringResource? = when {
        !namePattern.matches(name) -> Res.string.error_property_name_invalid
// project/GameIdentity.kt
    fun validate(): Pair<StringResource, List<String>>? = when {
// project/ProjectInspectionException.kt (and template/ProjectCreationException.kt, scenes/EntryPointLoadingException.kt)
internal class ProjectInspectionException(
    val reason: StringResource,
) : Exception()
```

and the AI reads them as UI text:

```kotlin
// mcp/McpToolServer.kt:212-221
            return result(toolName, arguments, getString(Res.string.error_mcp_internal, throwable.toString()), isError = true)
            null -> getString(Res.string.error_mcp_no_project) to true
                JsonNull -> getString(Res.string.mcp_tool_done)
            is CommandOutcome.Rejected -> getString(outcome.rejection.reason, *outcome.rejection.arguments.toTypedArray()) to true
// commands/project/ConsoleText.kt:27
            put("text", getString(text, *arguments.toTypedArray()))
```

By package, the non-UI code references 159 distinct strings in `commands`, 47 in `actors` (30 of them labels, moved by M02a), 33
in `workspace`, 11 in `project`, 7 in `template`, 6 in `scenes`, 3 in `git`, and 3 in `mcp`.

## Fix

**1. The type** (`foundation/DomainText.kt`):

```kotlin
/**
 * A text the model, the commands, or the workspace hands to both the user and the AI, as a typed value: the user interface
 * words it from its strings (`ui/DomainTexts.kt`), and the AI reads [english], which translating the user interface never
 * changes. [english] uses the placeholders of the matching string (`%1$s`, …).
 */
internal interface DomainText {
    val english: String
}

/**
 * [DomainText.english] with [arguments] put into its placeholders.
 */
internal fun DomainText.englishWith(arguments: List<String>): String = String.format(Locale.ROOT, english, *arguments.toTypedArray())
```

Each owner declares an `enum class … (override val english: String) : DomainText`, one entry per string it uses today, with the
English copied verbatim from the string's current value in `strings.xml` (generate the entries with a throwaway script from
`strings.xml` and the `grep` of each package's `Res.string.error_…`/`status_…` references, so nothing is retyped by hand). Names
are the string keys without their prefix, upper-cased (`error_property_name_taken` → `PropertyNameProblem.TAKEN`).

**2. The owners** (each enum in its own file next to its users):

| Enum | Package | Replaces |
|---|---|---|
| `NameProblem` | `actors` | `error_type_name_*`, `error_property_name_*`, `error_folder_name_invalid` (`BlockTypeNaming.validate`, `PropertyNaming.validate`, `FolderPaths.validateName` return `NameProblem?`) |
| `IdentityProblem` | `project` | `error_identity_*`; `GameIdentity.validate()` returns `Pair<IdentityProblem, List<String>>?` |
| `ProjectProblem` | `project` | `error_no_desktop_module`, `error_not_a_gradle_project`, `error_project_format_*`, `error_project_newer_format*`; `ProjectInspectionException.reason: ProjectProblem` |
| `TemplateProblem` | `template` | `error_package_name_invalid`, `error_project_*` (`ProjectNaming.validate*`, `ProjectCreationException.reason`) |
| `SceneProblem` | `scenes` | `error_entry_point_*`, `error_scene_view_failed`, `scene_build_failed`, S01's `scene_unsupported` and `scene_unsupported_library` (`EntryPointLoadingException.reason`, `SceneEditingController.LoadResult.Failed.reason`/`Unsupported.reason`, `SceneEditingState.Failed.reason`/`Unsupported.reason`) |
| `CommandProblem` | `commands` | every `error_command_*` string and any other `Rejection`/`Applied.failed` string used only by commands |
| `WorkspaceStatus` | `workspace` | `status_*` used by `workspace/` |
| `GitStatus`, `SceneStatus` | `git`, `scenes` | `status_git_*`, `status_scene_opened`, S01's `status_scene_newer_libraries` |

The table is from `cbf3b7140`; lanes K, H, and S added texts since (K01a's unsaved-type status, K01b's refusals of Commit and
Discard, K02a's rejection of a throwing command, K03's dropped undo step, S01's above): build the entries from the grep at the
HEAD you start from, not from this table, and give each new string the owner of the package that constructs it.

A string used by two owners belongs to the lower one in M01's layers (for example a `NameProblem` returned as a command's
rejection stays `NameProblem`); don't duplicate an entry. `CommandExecutor.kt:183`'s `Rejection(Res.string.error_mcp_internal, …)`
becomes `CommandProblem.INTERNAL`.

**3. The carriers** take a `DomainText`:
- `Rejection(val reason: DomainText, val arguments: List<String> = emptyList())`, its vararg constructor, and
  `Applied.failed(reason: DomainText)`.
- `ConsoleEntry.Status(val text: DomainText, val isError: Boolean = false, val arguments: List<String> = emptyList())`.
- The exceptions and `SceneEditingState.Failed` as in the table; `WorkspaceController.createActorType/createManagerType/dispatchIfValid`
  return `Rejection?` (they return a reason with arguments today through `StringResource?`; carry the arguments too).
- The app's UI callbacks typed `(String) -> StringResource?` (`FolderTreeSection`'s `onCreateItem` etc., `BlockTypeHeader.onRename`,
  `TimerChoice`'s) become `(String) -> Rejection?` or `-> DomainText?` (whichever the producer returns), and render through the UI
  helper below. `ui/components/NewNameField.kt` is the exception: it becomes part of `:creator:ui-components` (M04), which sits
  below `commands` and the app, so it must not take a `Rejection` or call `ui/DomainTexts.kt`. Keep its callback UI-typed
  (`(String) -> StringResource?`, or a small `ui.components` value such as `data class FieldError(val text: StringResource, val
  arguments: List<String> = emptyList())` if the arguments must show), and map at its app callers with `DomainText.resource`.
- `AppScreen.Welcome`/`NewProject.error` are app state, so they may stay `StringResource` (mapped from `ProjectProblem` /
  `TemplateProblem` with `.resource` in `AppController`, next to the app's own `error_project_not_opened` and H04's
  `status_unexpected_problem`), or take a `DomainText`; pick one and keep `AppControllerTest`'s assertions as strict.

**4. The AI reads English:** `McpToolServer` uses `outcome.rejection.reason.englishWith(outcome.rejection.arguments)`; its own three
texts become private constants in `McpToolServer` (`"Kubriko Creator ran into a problem: %1$s"` etc., copied from the strings) since
only the AI reads them, and the strings are deleted from `strings.xml` when `grep -rn "error_mcp_internal\|error_mcp_no_project\|mcp_tool_done"
creator/src` finds no UI user (`StringResourcesTest` fails otherwise). `ConsoleText.toJson` uses `text.englishWith(arguments)`.
`ConsoleText` stops being `suspend` if nothing else suspends.

**5. The UI words it** (`ui/DomainTexts.kt`):

```kotlin
internal val DomainText.resource: StringResource
    get() = when (this) {
        is NameProblem -> when (this) { NameProblem.TYPE_INVALID -> Res.string.error_type_name_invalid; … }
        is CommandProblem -> when (this) { … }
        …
        else -> error("No string for ${this::class.simpleName}.$this")
    }

@Composable
internal fun DomainText.text(arguments: List<String> = emptyList()) = stringResource(resource, *arguments.toTypedArray())
```

Each inner `when` is exhaustive over its enum (no `else`), so a new entry without a string doesn't compile; the outer `else`
covers only an enum missing from the list, which the test catches. If the file passes 500 lines, give each enum's mapping its own
file (`ui/texts/CommandProblemTexts.kt`, …) and keep `DomainTexts.kt` as the dispatcher. `Console.kt`'s `ConsoleEntry.text()` and
every `stringResource(x.reason, …)` call site use these.

**6. Docs.** `creator/CLAUDE.md`: in the AI section, "The AI reads `DomainText.english`, never a UI string; a new error or status
is an entry of its owner's enum with its English, and a string in `strings.xml` mapped in `ui/DomainTexts.kt`." In the UI section,
replace any sentence saying commands return string resources.

After this, `grep -rln "kubriko.creator.generated.resources" creator/src/main/kotlin | grep -v "/ui/\|/window/\|AppScreen\|AppController\|KubrikoCreatorApp"`
prints nothing: only the app's UI uses `Res`. Say so in the commit's report (it is what lets M03–M14 give no module but
ui-components its own resources).

## Tests

- `DomainTextsTest.everyDomainTextHasAString`: for every entry of every `DomainText` enum, `entry.resource` doesn't throw. The list
  of enums is a literal in the test (`NameProblem.entries + IdentityProblem.entries + …`).
- `DomainTextsTest.everyDomainTextEnumIsListed`: scan the main sources (`File("src/main/kotlin")`, asserting it is a directory) for
  `enum class (\w+)\(.*\) : DomainText` and assert each name is in the literal list above, so a new enum can't be forgotten.
- `DomainTextsTest.englishHasTheStringsPlaceholders`: for every entry, the set of `%N$s` placeholders in `english` equals the set in
  the string's value in `src/main/composeResources/values/strings.xml` (read the file; find the key from `resource.key`), so the AI
  and the user are given the same arguments.
- `McpToolServerTest`: a rejected tool call returns the literal English (for example `"There is no Actor type called Rock."` for a
  `CommandProblem.ACTOR_TYPE_MISSING` with `Rock`; take the real sentence from `strings.xml` at execution), not a `getString`.
- `ConsoleTextTest` (or the existing `ActionCommandsTest` case for `get_console`): a `Status(WorkspaceStatus.BUILT)` reads as the
  literal English.
- Update existing tests mechanically: `Rejection(Res.string.error_command_x, …)` → `Rejection(CommandProblem.X, …)`, and the same
  for console statuses and validation results. Keep each assertion as strict as it was.
