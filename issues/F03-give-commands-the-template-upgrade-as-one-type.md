<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Give `CommandContext` the template upgrade as one `ProjectUpgrades`, instead of a reading and a reporting lambda

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** all (desktop app)
**Written against:** `9bb8df2a6`, after F02. Re-locate code by the quoted snippets, not by line numbers.
**Files:**
- `:creator:commands`: `creator/commands/src/main/kotlin/com/pandulapeter/kubrikoCreator/commands/CommandContext.kt`,
  `…/commands/ProjectUpgrades.kt` (new), `…/commands/project/UpgradeProject.kt`,
  `creator/commands/src/testFixtures/kotlin/com/pandulapeter/kubrikoCreator/commands/CommandTesting.kt`
- `:creator:workspace`: `creator/workspace/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/WorkspaceController.kt`,
  `creator/workspace/src/test/kotlin/com/pandulapeter/kubrikoCreator/workspace/WorkspaceControllerTest.kt`
- `creator/plan/README.md`

## Problem

`CommandContext`'s primary constructor takes 13 parameters, one past the limit of 12 `check_sizes.py` enforces:

```kotlin
public class CommandContext(
    public val project: CreatorProject,
    public val console: StateFlow<List<ConsoleEntry>>,
    public val blockTypes: BlockTypes?,
    public val git: GitController,
    public val scenes: SceneStore,
    public val runner: ProjectRunner,
    public val ioDispatcher: CoroutineDispatcher,
    public val audioFiles: () -> AudioFiles? = { null },
    public val spriteFiles: () -> SpriteFiles? = { null },
    public val report: (ConsoleEntry) -> Unit = {},
    public val onProjectUpgraded: () -> Unit = {},
    public val projectUpgrade: () -> TemplateUpgrade = { TemplateUpgrade.UpToDate },
    public val rejectChanges: () -> Rejection? = { null },
)
```

Two of them are two halves of one thing: `projectUpgrade` reads the template upgrade the project needs, as last checked in
the background, and `onProjectUpgraded` tells the owner of that state that it changed. Their only reader is
`commands/project/UpgradeProject.kt`:

```kotlin
override fun validate(context: CommandContext, input: NoInput): Rejection? = rejectionOf(context.projectUpgrade())
…
if (rejection != null) return Applied.failed(rejection)
context.onProjectUpgraded()
```

and `WorkspaceController` supplies both from the same state, `_projectUpgrade` and `refreshProjectUpgrade()`:

```kotlin
onProjectUpgraded = ::refreshProjectUpgrade,
projectUpgrade = { _projectUpgrade.value },
```

## Fix

**Grouping:** one interface for the project's template-upgrade state as commands see it. Its two members belong together
because they read and invalidate the same state, owned by one party (`WorkspaceController`), and a command that uses one uses
the other. Result: 12 parameters. (`audioFiles` and `spriteFiles` could form a second group, but one is enough to reach the
limit, and they are read by more commands; leave them.)

### 1. `commands/ProjectUpgrades.kt` (new, `:creator:commands`)

```kotlin
/**
 * The template upgrade the project needs, as commands see it: as last checked in the background, so that validating reads no
 * files, and told when a command brought the template files up to date.
 */
public interface ProjectUpgrades {

    /**
     * What the project's template files need, as last checked.
     */
    public fun current(): TemplateUpgrade

    /**
     * Tells the rest of the editor that the project's template files were brought up to date.
     */
    public fun onUpgraded()

    public companion object {
        /**
         * A project whose template files need nothing, with no one to tell.
         */
        public val UpToDate: ProjectUpgrades = object : ProjectUpgrades {
            override fun current(): TemplateUpgrade = TemplateUpgrade.UpToDate
            override fun onUpgraded() = Unit
        }
    }
}
```

`current()` is a function, not a property, so it is plainly read again on every call, as the lambda was (tests rely on that:
`commandContext(projectUpgrade = { ProjectUpgrader(root).check() })` must check on each validation).

### 2. `commands/CommandContext.kt`

Replace the two parameters with `public val projectUpgrades: ProjectUpgrades = ProjectUpgrades.UpToDate,` in the place of
`onProjectUpgraded` (before `rejectChanges`), and the two KDoc lines `@param onProjectUpgraded …` and `@param projectUpgrade …`
with "`@param projectUpgrades` The template upgrade the project needs, as last checked in the background, so that validating
reads no files, and who to tell once a command applied it." Drop the `TemplateUpgrade` import if nothing else uses it.

### 3. `commands/project/UpgradeProject.kt`

`rejectionOf(context.projectUpgrade())` → `rejectionOf(context.projectUpgrades.current())`, and
`context.onProjectUpgraded()` → `context.projectUpgrades.onUpgraded()`.

### 4. `workspace/WorkspaceController.kt` (`:creator:workspace`)

In the `CommandContext(…)` call, replace

```kotlin
onProjectUpgraded = ::refreshProjectUpgrade,
projectUpgrade = { _projectUpgrade.value },
```

with

```kotlin
projectUpgrades = object : ProjectUpgrades {
    override fun current() = _projectUpgrade.value
    override fun onUpgraded() = refreshProjectUpgrade()
},
```

and import `com.pandulapeter.kubrikoCreator.commands.ProjectUpgrades`. The file must stay at most 500 lines (F02 freed about
30).

### 5. `commands/CommandTesting.kt` (`:creator:commands` test fixtures)

`commandContext(…)` keeps its `onProjectUpgraded: () -> Unit = {}` and `projectUpgrade: () -> TemplateUpgrade = { TemplateUpgrade.UpToDate }`
parameters, so `ProjectUpgradeCommandsTest` stays as it is, and passes them on as

```kotlin
projectUpgrades = object : ProjectUpgrades {
    override fun current() = projectUpgrade()
    override fun onUpgraded() = onProjectUpgraded()
},
```

in place of the two named arguments.

### 6. `creator/plan/README.md`

In the Structure section's constructors bullet, replace "**Four constructors take more than 12 parameters**
(`WorkspaceController` 23, `AssistantController` 18, `SceneEditingController` 17, and `CommandContext` 13)" with "**Three
constructors take more than 12 parameters** (`WorkspaceController` 23, `AssistantController` 18, and `SceneEditingController`
17)".

## Tests

- The commands side is pinned by `ProjectUpgradeCommandsTest` (`upgradingChangesTheFilesAndRecordsTheVersion` counts
  `onProjectUpgraded` calls; `validatingTrustsTheLastCheckInsteadOfReadingTheFiles` and `applyingChecksAgainWhenTheLastCheckIsStale`
  the reads), which runs through the fixture's `ProjectUpgrades` unchanged.
- The workspace's wiring of the two members has no test today (no test dispatches `UpgradeProject` through a
  `WorkspaceController`). Add `upgradingTheProjectChecksTheTemplateFilesAgain` to `WorkspaceControllerTest` (`:creator:workspace`),
  next to `theTemplateUpgradeIsCheckedWhenOpenedAndWhenSourcesChange`, whose temporary-project setup it copies up to
  `val upgrade = assertIs<TemplateUpgrade.Needed>(controller.projectUpgrade.value)`. Then assert
  `controller.commands.validate(Invocation(UpgradeProject, NoInput))` is null (validation read `current()`), dispatch it with
  `controller.commands.dispatch(Invocation(UpgradeProject, NoInput))`, `runCurrent()`, and assert that
  `controller.projectUpgrade.value` no longer lists `src/commonMain/kotlin/com/example/game/Game.kt` among its `changes` (it is
  `UpToDate`, or `Needed` with only the by-hand file), which only holds when `onUpgraded()` ran `refreshProjectUpgrade()`. Show it
  bites: with `override fun onUpgraded() = Unit` in `WorkspaceController` it fails; put the real body back. Delete the temporary
  folder in `finally`, as the neighbouring test does.
- Run `./gradlew :creator:allTests` and `./gradlew spotlessCheck`; after staging, `python3 .github/scripts/check_sizes.py` must
  print nothing (`CommandContext` now has 12 parameters).
