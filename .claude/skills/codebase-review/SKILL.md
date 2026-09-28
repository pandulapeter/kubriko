---
name: codebase-review
description: The Kubriko review-sweep process — read-only area reviewers, one plan file per verified finding, a README.md that indexes them into parallel lanes, an EXECUTION.md orchestrator brief, and later the execution itself (one worktree per lane, one one-line commit per fix, merged in a fixed lane order). Invoke this skill WHENEVER the user asks to review, audit or sweep the codebase (or an area, a platform, performance, stability, "before release/launch") and write plans, OR asks to execute, carry out or land the plans in issues/ (or "follow EXECUTION.md"). Not for reviewing a single diff or PR — that is /code-review.
---

# Codebase review sweeps

A sweep has two halves that happen in **separate turns**: the **review** writes plans and changes no code; the
**execution** lands them, and only starts when the user says so. Never roll from one into the other.

## 0. Before anything

- Read the memory notes of earlier reviews (`MEMORY.md` lists them). Do not re-review an area the last sweep found
  solid, re-raise a finding that was rejected or disproven, or re-ask a decision the user already took — the notes
  record all three.
- Look at the plans folder (see the last section). If plans from an earlier sweep are still there, ask whether to
  keep them, fold them in or clear them before writing new ones. Never delete untracked plans on your own.
- Note `git rev-parse --short HEAD` and the branch; every document names that commit. If the working tree has
  uncommitted work, say so in the README and keep reviewers off those files' pending changes.
- Agree on the **angle** and the **budget** if the request leaves them open. The yield comes from a new angle (a
  platform barely run, bad network, low-end devices, scale, stress/monkey tapping, a store's rules), not from
  looking harder at the same code. If the user caps tokens or time ("an hour is enough"), size the fan-out to it:
  fewer reviewers, a cheaper model for the reviewers, one live run at most.

## 1. Review (read-only)

1. **Area reviewers in parallel** — one `Agent` per area, all spawned in one message, each told: the commit, the
   angle, its area's files, what earlier sweeps settled (so it skips them), and that it must not edit anything. Each
   returns findings with file:line, the quoted code, a concrete failure scenario, severity, platforms, and a proposed
   fix. Optionally one agent does a **live run** (a stress run of the app at scale, a throttled link)
   and reports measurements.
2. **Verify every finding against HEAD before it becomes a plan** — a second pass (writer agents per lane, or you)
   re-reads the code, and where a pure function is involved, proves it with a throwaway test in the scratchpad or an
   untracked probe test that is deleted afterwards. A finding that does not hold goes to the README's "Dropped after
   verification" with one sentence why; it is never silently discarded.
3. **Group into lanes**: sets of plans whose files do not overlap, so each lane can run in its own worktree. Name
   files shared between lanes (module `CLAUDE.md` files, `strings.xml`) and how each lane may touch them. Pick a
   **merge order**: the lane others build on first, the lane that touches the most shared UI files last.

## 2. The documents

All in the plans folder, untracked until the user commits them.

**One plan per finding**, `NN-what-the-fix-does.md` (two-digit number, kebab-case title phrased as the fix):

```
# <The fix, as one sentence>

**Kind:** bug | performance | docs | …  ·  **Severity:** high | medium | low  ·  **Platforms:** all | …
**Files:** every file the fix may touch, tests and CLAUDE.md files included

## Problem        what goes wrong, for whom, with the quoted code and file:line at the reviewed commit
## Fix            exactly what to change; where there is a choice, the options and the recommended one
## Tests          the unit test to add (pure logic only), or why none can be written
## Manual check   what a person must do on a device, account or network to see it fixed
```

A plan is self-contained: an agent that reads only it and the files it names can carry it out. Quote code rather
than relying on line numbers alone, since earlier lanes move lines. A plan may say "drop this plan if …" when its
premise needs checking at execution time.

**`README.md`** — the index and the record of decisions: the reviewed commit and the angle; **Headlines** (the few
that matter, data loss first); the **Index** table (number, title, severity, lane); the **Lanes** table (lane,
area, plans in execution order when that is not numeric, files owned) and the **merge order** with the reason; the
shared-file rules; **Decisions** — each open question with the recommended default, marked as awaiting the user;
**Checked and found solid**; **Dropped after verification**; any **measurements**; **Manual checks owed**.

**`EXECUTION.md`** — the brief for the orchestrating agent, which the user starts with *"Follow
`<plans folder>/EXECUTION.md`."* It restates section 4 below concretely for this sweep: preconditions, the lane
list with each lane's plan order, the subagent prompt with its placeholders filled in except the worktree path, the
verification commands, the merge order, and the finish.

Then report to the user in a few lines: the headline findings, the number of plans per lane, and the open decisions
— asked with `AskUserQuestion`, recommendation first. Record the answers in the README and in memory.

## 3. Memory

Write (or update) one project memory per sweep, `<ordinal>-review.md`: date, commit, angle, number of plans, lanes and
merge order, decisions taken, what is still open. Update it again when the plans land: the commit range, what was
skipped and why, and the manual checks still owed. Point the next sweep at it.

## 4. Execution (only when the user says so)

**Preconditions** — stop and tell the user if any fails: the tree is clean apart from the plans folder; every open
decision has an answer; the unit tests pass. Commit the plans first if they are untracked (`Add the <nth> review
plans.`), then note `START=$(git rev-parse HEAD)`.

**Lanes** — one **detached** worktree per lane next to the checkout, so no branch is created:
`git worktree add --detach ../<Repo>-lane-<x> START`. Spawn one `general-purpose` subagent per lane in a single
message, each confined to its worktree. Cap concurrent Gradle builds (see the last section); start the rest of the
lanes as earlier ones finish.

**Per plan, in the lane's order** (the subagent's procedure):

1. Read the plan and the files it names. Load `code-style` before the first edit.
2. Implement exactly the plan, taking the recommended option. Re-locate moved code by the quoted snippet. If the plan
   is wrong, impossible, or its own "drop this plan if" condition holds, **do not improvise another fix**: revert the
   working tree, leave the plan file, note why, move on.
3. Add the tests it asks for; update the `CLAUDE.md` files and strings it names.
4. Verify with the commands in the last section — the tests, and a compile of every platform the change touches.
   Manual checks that need a device, an account, a store or a network condition are skipped and reported.
5. `git rm` the plan file in the same commit as its fix.
6. Load `commit-messages`; commit with a single `-m` sentence — one fix, one commit, never two fixes folded or one
   split. Then check the raw commit object and amend on the spot if it fails:

   ```
   test "$(git cat-file commit HEAD | sed '1,/^$/d' | wc -l | tr -d ' ')" = "1" || echo "MORE THAN ONE LINE"
   git log -1 --format=%B | grep -qiE 'co-authored|claude|session|generated' && echo "ATTRIBUTION"
   ```

   Never branch, merge, rebase, push, bump the version or dispatch a workflow from a lane.
7. Lane report: `NN: <hash> "<message>" — verified: <commands>; skipped manual: <what, why>`, then `Not done: NN —
   <why>`, then the final test result.

**Merging**, in the README's order, as lanes report: in the main checkout,
`git cherry-pick START..<lane HEAD>`. Resolve conflicts keeping both sides' intent — `CLAUDE.md` paragraphs and
`strings.xml` as a word-level three-way merge, every sentence and key from both sides kept, never one side's version
whole. Run the tests after each lane and the full build after the last; a break is fixed by one more commit
(`Fix the iOS build after the storage changes.`), never by rewriting landed commits. Then
`git worktree remove ../<Repo>-lane-<x>`.

**Finish**: plan files still present are the skipped ones — leave them, with the README trimmed to them, or ask the
user whether to delete the folder. Final checks: `git log --format=%B START..HEAD | grep -ciE
'co-authored|claude|session|generated'` prints 0, `git log --oneline START..HEAD` is one line per plan, `git status`
is clean and `git worktree list` shows only the checkout. Report the commit count, the skipped plans with reasons,
and the manual checks owed; update the memory. **Do not push.**

## This repository: Kubriko

- **Plans folder:** `issues/` at the repository root — not `documentation/`, which holds the library's published
  guides. Branch: `main`.
- **Kubriko is a published library.** A fix that changes public API (a signature, a default, a behaviour a consumer
  can observe) is a **decision** in the README, never a silent part of a plan: it breaks every game built on it,
  Tesselar first. Plans say which published artifact (`engine`, a plugin, a tool) a change ships in; public
  declarations they add need KDoc (see `code-style`).
- **Areas that make good reviewer splits:** the engine core (`Kubriko`, `KubrikoViewport`, managers, actors, traits,
  `TickSource`, `TriangleBatch`); the plugins; the tools (debug menu, scene editor); the Showcase app and its
  platform shells; the examples. Per-frame paths are reviewed for allocations and redundant work — see the
  Performance section of `CLAUDE.md`.
- **Tests:** `./gradlew test`, or `./gradlew :<module>:desktopTest` for the modules a lane touched. A change to the
  web bridge's embedded JS also runs `node engine/src/webMain/checkTriangleBridge.mjs`.
- **Compile checks:** `./gradlew :<module>:build` for each module touched; the Showcase on the platform a change is
  specific to (`:app:desktop:run` is not needed — compiling is). Full build after the last lane: `./gradlew build`.
- **At most two concurrent Gradle builds.**
- A behaviour change updates `CLAUDE.md` and, where a published guide describes it, the matching
  `documentation/*.md`. Nothing is published to Maven Central from a sweep; that is the user's release step.
- Commit examples in this repo's voice: `Match the Showcase's desktop window background to its theme while
  resizing.` · `Add the <nth> review plans.` · `Remove the review plans.`
