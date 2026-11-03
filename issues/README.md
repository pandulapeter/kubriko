<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Fifth review follow-ups: Kubriko Creator's structure

Written at `9bb8df2a6` on `main`. The fifth review (PR #139) left two items in `creator/plan/README.md` → Structure: six `ui`
parts that still take a controller, and five primary constructors past the 12 parameters `check_sizes.py` allows (it lets them
shrink, never grow). Each plan below is one commit, carried out alone, in this order; behaviour is unchanged throughout.

## Order

| Plan | What it does | Constructor | Modules touched |
|---|---|---|---|
| [F01](F01-hand-ui-parts-only-what-they-use.md) | Hands six `ui` parts only their state and callbacks, wires them in `ui/WorkspaceScreen.kt`, adds `PackageLayeringTest.onlyEntryFilesTakeTheControllers` | — | `:creator` |
| [F02](F02-report-block-type-events-through-a-listener.md) | `BlockTypesListener` for `BlockTypesController`'s nine callbacks; the workspace's console reporting moves into `BlockTypesReport` | `BlockTypesController` 15 → 7 | `:creator:core`, `:creator:commands` (test fixtures), `:creator:workspace` |
| [F03](F03-give-commands-the-template-upgrade-as-one-type.md) | `ProjectUpgrades` for `CommandContext`'s upgrade reading and reporting | `CommandContext` 13 → 12 | `:creator:commands`, `:creator:workspace` |
| [F04](F04-group-the-scene-editors-build-sources-and-runtime.md) | `SceneClassesBuild`, `SceneSources`, and `SceneRuntimeSetup` for `SceneEditingController` | `SceneEditingController` 17 → 8 | `:creator:scene-runtime`, `:creator:workspace` |
| [F05](F05-group-the-assistants-storage-agent-sources-and-project.md) | `AssistantStorage`, `AgentSources`, and `OpenProjectAccess` for `AssistantController`, their production wiring out of `AppGraph` | `AssistantController` 18 → 11 | `:creator:ai`, `:creator` |
| [F06](F06-group-the-workspace-controllers-parameters-by-the-part-each-feeds.md) | `ProjectProcesses`, `WorkspaceFiles`, `BlockTypesSetup`, and `GitRepositories` for `WorkspaceController`, production defaults into `GradleWorkspaceOpener`, a `workspaceController` test fixture | `WorkspaceController` 23 → 12 | `:creator:workspace`, `:creator` (tests) |

Every plan also edits `creator/plan/README.md` → Structure (F01 removes the first item, F02–F05 shorten the second, F06 removes
it), and F01 and F06 each add a clause to `creator/CLAUDE.md` → Modules.

## Why this order

- **F01 first:** it touches only the app's UI and its layering test, nothing the constructor plans change, and the test it adds
  then guards every later commit.
- **The leaf constructors before `WorkspaceController` (F02–F05, then F06):** `WorkspaceController` builds
  `BlockTypesController`, `CommandContext`, and `SceneEditingController`, so changing them first means F06 wires collaborators
  whose signatures are final, instead of every leaf plan editing F06's new code. It also keeps `WorkspaceController.kt` (495 of
  its 500 lines today) within the limit: F02 moves about 30 lines of console reporting out of it before F03 and F04 add to the
  calls they change, and F06 depends on F04's `SceneRuntimeSetup`.
- **F02 before F03:** both edit `CommandTesting.kt`'s fixtures in `:creator:commands`; F02 the `blockTypesController` one, F03
  `commandContext`.
- **F05 anywhere after F01:** it touches only `:creator:ai` and `AppGraph`; it sits before F06 so the plans that change a
  module's API land before the one that changes the most callers.
- The user's suggested split (one plan for `WorkspaceController`, one for `AssistantController` and `SceneEditingController`,
  one for `BlockTypesController` and `CommandContext`) became one plan per class, so each commit is one refactor with one
  message, and a headless agent that stops mid-plan leaves a smaller gap.

## Decisions

- **Groupings, not a factory.** `creator/plan/README.md` suggested a factory for `WorkspaceController`'s collaborators. A factory
  function would take the same 23 values (functions aren't counted) and the collaborators need the workspace's own scope and
  console, so F06 groups the parameters by the collaborator each feeds instead, and moves the production defaults (the game
  process, the entry point loader) into `GradleWorkspaceOpener`. Each plan says why the members of each new type belong
  together; no type wraps unrelated parameters, and every one is a plain class or interface, not a data class.
- **Only the three controllers M01 named** (`AppController`, `WorkspaceController`, `AssistantController`) are held back from
  parts. `McpController` and `AiClientsController` are passed through unchanged, and `ui/WorkspaceScreen.kt` holds the wiring
  as private functions rather than a new entry file, so nothing else can call it.
- **Test convenience** stays through the fixtures: `commandContext` and `blockTypesController` keep their flat parameters, and
  F06 adds `workspaceController` to `:creator:workspace`'s test fixtures for the five tests that build a workspace.
