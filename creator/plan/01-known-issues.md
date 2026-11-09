<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Known issues

Problems found in review that need no other plan, fixed before any new feature. Each one gets its fix, a test that would have
caught it, and the review (`README.md`).

## Costs that grow with the project

Creator has to hold up for games the size GameMaker made (hundreds of Actor types, Scenes with thousands of Actors), and nothing a
user builds is capped in number. Each of these was found in review rather than measured, but grows with the project and runs often
enough to matter:

- **Sidebar rows** are rebuilt inside the lazy list's builder on every AI step's flash (`folderTreeSection` calls `folderTreeRows`
  each time). Fix: remember the rows per group.

## Mistakes and failures in the first hour

What someone who knows neither Kotlin nor Gradle meets when something goes wrong:

- **An unreadable project file reads as any other opening failure.** `AppController.openProject` turns any exception other than a
  `ProjectInspectionException` into `error_project_not_opened` with the cause's own message, so an invalid `.kubriko/project.json`
  says the same as a daemon that can't start. Fix: a message of its own for an unreadable `.kubriko/project.json`, naming the file
  and what is wrong in it.
- **Creating a project that fails loses the form.** `AppScreen.NewProject(error = …)` comes back with an empty form, since its state
  lives in `NewProjectScreen`'s `remember`, and `error_project_not_created` never says why. Fix: keep the request in
  `AppScreen.NewProject` and show the cause (network, disk, or permission).
- **Creating a project can't be cancelled.** Opening one can, but `ProgressScreen` offers no Cancel while a project is created, and a
  first project waiting minutes for Gradle's download leaves only quitting. `ProjectGenerator.create` blocks, so cancelling the job
  alone would let it finish writing in the background. Fix: make the wrapper download cancellable, and remove the folder when
  creation is cancelled, as it already is when creation fails.
- **Messages in code words.** Rejections name Events by their IDs (`error_command_event_not_allowed`, `error_command_key_not_allowed`:
  "an Actor type can't have a gameStarted one", "Only keyPressed Events have a key"), though the editor calls them "When the game
  starts"; a Timer named `class` is told to start with a lowercase letter; and project names with accents (`Kalandjáték`) are refused
  for not using "only letters". Fix: the editor's Event titles in user-facing rejections (the AI gets both), and one message per
  actual reason.
- **The git panel** puts the commit form below every change (`ui/git/GitPanel.kt`), out of sight in a big change set. Fix: put the
  form first.
- **Undoing an Actor's removal** clears the Scene editor's selection. Fix: select the Actor again, as placing one does.
- **The audio Blocks' file menus** are empty when the project has no sounds or music. Fix: say how to add some instead.

## Missing checks

- **Plain and batched drawing compared in Creator:** a generated game's Scene drawn with batched looks and drawn plain, compared as
  images in the end-to-end harness under Xvfb, with a tolerance for edge smoothing on ellipses (the engine's own comparison is
  `BatchedRenderingComparisonTest`).

## Order

As listed: the costs first, since every later plan adds to the project sizes they grow with.
