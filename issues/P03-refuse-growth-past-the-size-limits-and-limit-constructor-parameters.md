<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Refuse a change that grows a file, function, or constructor already past its size limit, and limit primary constructors to 12 parameters

**Kind:** process  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** ported from Campfire's 54th review (2026-11-02), added after this sweep's challenge  ·  **Depends on:** P01, and runs
after every other plan (lane D, after D01)
**Files:**
- `.github/scripts/check_sizes.py`
- `.github/scripts/test_check_sizes.py`
- `CLAUDE.md` (the **Checks** paragraph)
- `.claude/skills/code-style/SKILL.md` (the "Files and functions stay small" bullet)
- `.claude/skills/codebase-review/SKILL.md` (section 4, step 3)
- `.claude/settings.json` (the PreToolUse `additionalContext` text only; ask the user before that commit, as for P01)
- `.github/workflows/tests.yml` (the comment above "Check the file and function sizes" only)

## Problem

`check_sizes.py` fails only a crossing. What is already past a limit may grow with a note, so the largest files, functions,
and classes keep growing: each change adds a few lines to the one place that already does too much, and the note is read
and left. Campfire's checks had the same rule until its 54th review; its execution then showed the stricter rule working
(lanes split what they had to grow instead of growing it). The docstring states the current rule:

```python
Only crossing a limit fails: a Kotlin file or function within it before the change and past it after. What was already
past it may grow or shrink, since splitting it is a refactor of its own that a review sweep plans, …
```

Nothing limits a class's constructor either, and the classes that gather the most collaborators are the ones the fifth review
had to split: at `b2ee1e48b`, counted as below, `WorkspaceController` takes 21 parameters, `AssistantController` 18,
`SceneEditingController` 16, `CommandContext` 14, `BlockTypesController` 13, `PhysicsBody` 13 (`plugins/physics`, published),
and `PreparedCuboidTracks` 15 (`examples/demo-isometric-graphics`).

**Why this runs last:** the program's plans were written and challenged under today's rule. S01 adds a parameter to
`WorkspaceController` (21) and `SceneEditingController` (16) and a branch to `WorkspaceScreen` (273 lines), and A02a, A02b,
H01, and H02a edit `WorkspaceScreen` before H02b splits it. Landed earlier, the stricter check would stop those plans at commit
time (Campfire's plan 13 stopped on exactly that). By the end, A02a, K09b, H02b, and H07a have split the largest of them.

## Fix

Port Campfire's rules from `/Users/pandulapeter/Projects/Campfire/.github/scripts/check_sizes.py` (read it; the functions
named below are its), on top of P01's member-function measurement:

1. **Constructors.** Add `CONSTRUCTOR_PARAMETERS = 12` and Campfire's `CLASS`, `PRIMARY_CONSTRUCTOR`, and `PARAMETER` patterns and
   `constructors(text)`: the `(name, parameter count)` of every class with a primary constructor in a main Kotlin file, nested
   ones included, cutting the parameter list at commas outside brackets and counting only pieces that start like a parameter
   (so commas in defaults, lambdas, type arguments, and a trailing comma don't count). A `data class` is skipped: its parameters
   are its fields, not collaborators. `measure` records each as `"<path>::class <Name>"` with the unit `parameters`, keeping
   the larger count when two nested classes share a name.
2. **Growth fails.** In `judged`, as Campfire's: a size past its limit fails when it was within it before (or is new), with
   "split it by cohesion" (lines) or "give the class fewer responsibilities" (parameters); and fails when it was already past
   and the change made it larger, with "grew from A to B …, already past the limit of L; move code out of it, never into it" or
   "take a responsibility out of the class". As long or shorter passes, so a split passes. Keep the rename and "moved to another
   file" fallbacks (a constructor keeps its past when its class moves, matched by `::class <Name>`). Nothing is stored and no
   list of exceptions is kept: the base revision is the only reference.
3. **Hooks.** `--hook` fails a growth like a crossing (no note any more); `--shell-hook` reports both. Drop the "already past …
   gets a note" wording from the module docstring and replace its rule paragraph with Campfire's ("A Kotlin file, function or
   constructor fails when the change takes it past its limit, or when it was already past it and the change makes it longer
   (a constructor: gives it more parameters) than it was; staying as long or shrinking passes …").
4. Leave out Campfire's `CLAUDE.md` word budgets: Kubriko's root and Creator notes are far past any default, and the
   codebase-review skill keeps budgets to what a change adds.

A public constructor in a published module is API: one that would need a 13th parameter is a decision anyway (root
`CLAUDE.md` → Conventions), and the check now says so early.

Docs, one phrase each:
- `CLAUDE.md` **Checks**: "only crossing a limit fails" → "a change may not push a file, function, or constructor past its
  limit or grow one already past it; primary constructors at most 12 parameters, data classes exempt".
- `.claude/skills/code-style/SKILL.md`: replace "but only on crossing a limit: what is already past one may change" with "and a
  change may neither push a file, function, or constructor past its limit nor make one already past it longer (or give it more
  parameters): it moves code out instead, so a split always passes. A primary constructor takes at most 12 parameters (a data
  class's are its fields and don't count): a class that needs more has more than one responsibility".
- `.claude/skills/codebase-review/SKILL.md`, section 4, step 3: append "A file, function or constructor already past its size
  limit may not end a commit longer than it began, nor may one be pushed past it (code-style)."
- `.claude/settings.json`: in the PreToolUse `additionalContext`, after the size phrase, "; never grow what is already past them;
  constructors take at most 12 parameters" (keep the JSON and the shell quoting valid).
- `.github/workflows/tests.yml`: the comment "what was past them already may change" → "nor grows what is already past them".

At execution, run the new `measure` over every tracked main Kotlin file (a throwaway loop over `git ls-files '*.kt'`) and list
in the commit's report what is past a limit then; nothing of it is changed in this plan.

## Tests

Port Campfire's `ConstructorsTest` (parameters counted past commas in defaults, types, lambdas, and strings; a data class and
test sources not measured) and its `JudgedTest` cases `test_what_was_already_past_a_limit_may_not_grow`,
`test_what_was_already_past_a_limit_may_stay_as_long_or_shrink`, `test_a_split_that_shrinks_the_old_file_passes`,
`test_a_function_moved_to_another_file_may_not_grow_there`, `test_a_constructor_crossing_the_limit_fails`,
`test_a_new_class_past_the_limit_fails`, `test_a_constructor_already_past_the_limit_may_not_gain_parameters_but_may_keep_or_lose_them`,
and `test_a_class_moved_to_another_file_keeps_its_past`, plus the shell-hook case
`test_the_shell_hook_fails_a_file_already_past_its_limit_that_grows_and_passes_one_that_shrinks`. Replace Kubriko's tests that
assert a growth only gets a note. Run `python3 -m unittest discover -s .github/scripts -p 'test_*.py'`; the growth and constructor
tests fail against the current script.
