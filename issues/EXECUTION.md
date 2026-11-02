<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Executing the fifth review's plans

The brief for the orchestrating session, started with *"Follow `issues/EXECUTION.md`."* Read `README.md` here first (lanes,
decisions, shared-file rules), then load the `codebase-review` skill: its section 4 is the procedure, and this file fills
it in for this sweep. Every decision is answered (README → Decisions); none is re-asked.

## Preconditions (stop and tell the user if one fails)

1. `git status` is clean apart from `issues/`, and `git log origin/main..HEAD` is what the README's "Fixed during the
   review" lists (nothing was pushed; ask before any push, never push yourself).
2. The challenge ran on every plan (README → Challenge).
3. `./gradlew :creator:test` and `./gradlew :tools:creator-session:desktopTest` pass.
4. Commit the plans first if they are untracked: `git add issues && git commit -m "Add the fifth review plans."`, then
   `START=$(git rev-parse HEAD)`.

## Lanes and their order

Run the phases in order. Inside a phase, lanes run in parallel, each in its own **detached** worktree next to the checkout
(`git worktree add --detach ../Kubriko-lane-<x> <base>`), each by one `general-purpose` subagent confined to it, **at most
two Gradle builds at once** across all lanes (a shared semaphore script in each lane's own scratch folder is fine; a
"FqNames can't be derived from DirtyData" error is a stale cache, rerun). A lane that runs after another phase is cut from
the main checkout's `HEAD` once that phase is merged, and its cherry-pick range starts at that base.

| Phase | Lane | Plans, in this order | Base |
|---|---|---|---|
| 1 | T | T01, T04, T06, P01, P02 | `START` |
| 1 | S | S03, S02, S01, S04, S05 | `START` |
| 1 | G | G01, G02, G03, G04, G05, G07, G09, G06, G08 | `START` |
| 1 | A | A01, A02a, A02b, A03, A04, T02a, T02b | `START` |
| 1→2 | (serial, main) | T03 after lane S merged; T05 after lane G merged | main `HEAD` |
| 2 | K | K12, K01a, K01b, K01c, K02a, K02b, K03, K04, K05, K06, K07, K08, K11a, K11b, K09a, K09b, K10 | main `HEAD` after phase 1→2 |
| 3 | H | H01, H02a, H02b, H03a, H03b, H04, H05, H06a, H06b, H07a, H07b | main `HEAD` after K |
| 4 | M | M01, M02a, M02b, M03, M04, M05, M06, M07, M08, M09, M10, M11, M12, M13, M14 | main `HEAD` after H |
| 4 | D | D01 (after M, in the same worktree or main) | main `HEAD` after M |

**Phase 1 merge order:** T, S, G, A. T first because the other lanes' tests use its `testing` package; A last because it
touches the most shared test files.

**P01 edits `.claude/settings.json`** (one phrase in the size-check hook's `additionalContext`). That file configures Claude
Code itself: ask the user before that commit, and land P01 without the `settings.json` line if they decline.

## The lane subagent's prompt

Fill in `<x>`, `<worktree>`, `<base>`, and `<plans>`; everything else is literal.

> You carry out the Kubriko review plans `<plans>` (in that order) from `/Users/pandulapeter/Projects/Kubriko/issues/`, in
> the detached worktree `<worktree>` (cut from `<base>`), one commit per plan. Load the `code-style` skill before your first
> edit and the `commit-messages` skill before your first commit, and read `creator/CLAUDE.md` (Running and testing,
> Definition of done) once.
>
> Isolation: every command starts with `cd <worktree> &&` or uses `git -C <worktree>`; scratch files go only to
> `<scratchpad>/lane-<x>/`, and you never run a script you didn't write; never `git stash`; never branch, merge, rebase,
> push, bump a version, or dispatch a workflow.
>
> Per plan: read the plan and every file it names; implement exactly the plan, taking its decided or recommended option,
> re-locating moved code by the quoted snippets. If the plan is wrong, impossible, or its "Drop this plan if" condition
> holds, revert the working tree, leave the plan file, note why, and move on — never improvise another fix. Add the tests
> it asks for (they must fail without the fix: show it by copying your main changes aside, checking those paths out,
> running the test, and copying them back) and update the `creator/CLAUDE.md` lines, package guides, and `strings.xml`
> entries it names. Verify with `./gradlew :creator:test` (the whole suite), plus `:tools:creator-session:desktopTest` /
> `:tools:creator-api:build` when touched, `./gradlew :creator:spotlessApply`, and from M03 on the aggregate task the plan
> introduces. `git rm` the plan file in the same commit as its fix, stage explicit paths only, and commit with one sentence
> (`git commit -m "..."`, the pre-commit hook runs the repository checks; never `--no-verify`). Then:
> `test "$(git cat-file commit HEAD | sed '1,/^$/d' | wc -l | tr -d ' ')" = "1" || echo "MORE THAN ONE LINE"`,
> `git log -1 --format=%B | grep -qiE 'co-authored|generated with|claude code|anthropic\.com' && echo "ATTRIBUTION"`, and
> `git show --stat HEAD` showing only that plan's files; amend on the spot if any fails.
>
> Report: `NN: <hash> "<message>" — verified: <commands>; skipped manual: <what, why>` per plan, then `Not done: NN — <why>`,
> then the final test result.

## Merging

As lanes report, in the phase's merge order: list each lane commit's paths (`git -C <worktree> log --stat --format=%s
<base>..HEAD`) and stop on any outside that lane's plans' **Files**; then in the main checkout `git cherry-pick
<base>..<lane HEAD>`. Resolve conflicts keeping both sides' intent: `creator/CLAUDE.md`, the package guides, and
`strings.xml` as a word-level three-way merge with every sentence and key from both sides. Run `./gradlew :creator:test`
after each lane and `./gradlew build` after each phase; a break is fixed by one more commit, never by rewriting landed ones.
Then `git worktree remove ../Kubriko-lane-<x>`.

## Finish

Plan files still present are the skipped ones: leave them with the README trimmed to them, and tell the user why each was
skipped. Final checks: `git log --format=%B START..HEAD | grep -ciE 'co-authored|generated with|claude code|anthropic\.com'`
prints 0, `git log --oneline START..HEAD` is one line per plan (plus merge fixes), `git status` is clean, and
`git worktree list` shows only the checkout. Update the `fifth-review` memory with the range and the skipped plans. Offer a
regression review of the landed range (did each lane preserve behaviour, area by area). When no plan file is left, delete
`issues/` and commit `Remove the review plans.`. **Do not push.**

Hand checks worth doing in the real app afterwards (only where a mistake would hurt): Play and open a Scene on macOS (session
files now live under "Application Support"); quitting while a game runs (H03b); a project whose type files can't be written
(K01a/K01b); the window exception handler (H04); the Edit mode removal against a project on an older Kubriko (S05).
