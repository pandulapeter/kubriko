---
name: codebase-review
description: The Kubriko review-sweep process — read-only area reviewers plus a CLAUDE.md reviewer in every sweep; verified findings that are simple are fixed on the spot during the review, one commit each; complex ones become one plan file each, indexed by a README.md into parallel lanes with an EXECUTION.md orchestrator brief; and later the execution of those plans (one worktree per lane, one one-line commit per fix, merged in a fixed lane order). Invoke this skill WHENEVER the user asks to review, audit or sweep the codebase (or an area, a platform, performance, stability, "before release/launch"), OR asks to execute, carry out or land the plans in issues/ (or "follow EXECUTION.md"). Not for reviewing a single diff or PR — that is /code-review.
---

# Codebase review sweeps

A sweep has two halves. The **review** finds and verifies issues, **fixes the simple ones on the spot**, one commit
each, and writes a plan for each complex one. The **execution** lands those plans, in a later turn, and only starts
when the user says so — never roll from the review into it.

## 0. Before anything

- Read the memory notes of earlier reviews (`MEMORY.md` lists them). Do not re-review an area the last sweep found
  solid, re-raise a finding that was rejected or disproven, or re-ask a decision the user already took — the notes
  record all three.
- Look at the plans folder (see the last section). If plans from an earlier sweep are still there, ask whether to
  keep them, fold them in or clear them before writing new ones. Never delete untracked plans on your own.
- Note `git rev-parse --short HEAD` and the branch; every document names that commit, and it is the start of the range
  the review's own fixes land in. If the working tree has uncommitted work, say so: fixes stage only their own paths,
  and a finding in a file with pending changes is planned rather than fixed.
- Agree on the **angle** and the **budget** if the request leaves them open. The yield comes from a new angle (a
  platform barely run, bad network, low-end devices, scale, stress/monkey tapping, a store's rules), not from
  looking harder at the same code. If the user caps tokens or time ("an hour is enough"), size the fan-out to it:
  fewer reviewers, a cheaper model for the reviewers, one live run at most.

## 1. Review

1. **Area reviewers in parallel** — one `Agent` per area, all spawned in one message, each told: the commit, the
   angle, its area's files, what earlier sweeps settled (so it skips them), and that it must not edit anything. Each
   returns findings with file:line, the quoted code, a concrete failure scenario, severity, platforms, and a proposed
   fix. Every sweep also has the docs reviewer of section 6. Optionally one agent does a **live run** (a stress run
   of the app at scale, a throttled link) and reports measurements.
2. **Verify every finding against HEAD** — a second pass (you, or writer agents per area) re-reads the code, and where
   a pure function is involved, proves it with a throwaway test in the scratchpad or an untracked probe test that is
   deleted afterwards. A finding that does not hold goes to "Dropped after verification" with one sentence why; it is
   never silently discarded. This proves the finding, not the fix.
3. **Triage each verified finding as simple or complex.**
   - **Simple**: local, its fix is obvious and has one reasonable form, it needs no decision from the user, and it
     changes no threading, timing, lifecycle, persistence or sync format, public API or cross-module contract. A stale
     or wrong doc line, a missing translation, a wrong constant, a missing bounds or null check, dead code, an
     off-by-one pinned by a test, a mechanical refactor (split a file by its declarations, extract a private function
     or Composable, rename a private or internal name). Every docs-only finding and most low-severity ones are simple.
   - **Complex**: anything else — it crosses modules, touches what the list above excludes, has several reasonable
     fixes, needs a decision, or its diff would run past about a screen. When in doubt it is complex.
4. **Fix the simple ones now**, one at a time in the main checkout (never in parallel: they share one tree), in the
   order that keeps fixes to one file together:
   1. Load `code-style` before the first edit.
   2. Make the fix, with the test it needs where the logic is testable, and the `CLAUDE.md` lines and strings it makes
      stale.
   3. Verify with the commands in the last section — the tests of the touched modules and a compile of every platform
      the change touches.
   4. Stage only the fix's paths, load `commit-messages`, and commit with a single `-m` sentence — one finding, one
      commit, never two folded or one split — then run the commit checks of section 4, step 6.
   5. A fix that turns out bigger, riskier or different than the finding said is reverted from the tree and becomes
      a plan instead.

   When the last one is in, one fresh read-only agent reads `git diff <review start>..HEAD` against the findings and
   looks for a fix that breaks a caller, a documented usage or another fix; what it finds is fixed by one more commit,
   never by rewriting the landed ones. **Do not push.**
5. **Group the complex findings into lanes**: sets of plans whose files do not overlap, so each lane can run in its own
   worktree. Name files shared between lanes (module `CLAUDE.md` files, `strings.xml`) and how each lane may touch
   them. A plan that moves, splits or renames a file owns that file: every other plan that edits the same code goes in
   its lane, after it, since a cherry-pick does not carry an edit into code another lane moved. Pick a **merge
   order**: the lane others build on first, the lane that touches the most shared UI files last.
6. **Write the plans** (section 2). With no complex finding there is no plans folder: report and write the memory.
7. **Challenge every plan before reporting** — verification proves a finding is real, not that its fix is right, and a
   plan's own tests share its blind spots, so they pass with the flaw in place. Fresh read-only agents that did not
   write the plans (one per group of lanes, spawned in one message) try to break each fix:
   - trace it through every usage pattern the project documents (guides, `CLAUDE.md`, KDoc examples) and every
     in-repo caller — a fix that is right for the reported scenario can break a documented one;
   - check it against every other plan, in any lane, that changes the same behaviour or relies on it;
   - check the threading, ordering and lifecycle the fix itself introduces, not only the ones it removes;
   - for a published API, check how the known consumers call it.

   Plans that change timing, threading, lifecycle or a public contract get the closest look. Each plan comes back
   **sound**, **amended** (the challenger rewrites the plan and adds `**Challenged:** amended — <what changed>` under
   its header) or **dropped** (moved to "Dropped after verification" with why). The README records that the
   challenge ran and what it changed. Only then report to the user.

## 2. The documents

All in the plans folder, untracked until the user commits them, and only for complex findings.

**One plan per complex finding**, `NN-what-the-fix-does.md` (two-digit number, kebab-case title phrased as the fix):

```
# <The fix, as one sentence>

**Kind:** bug | performance | docs | …  ·  **Severity:** high | medium | low  ·  **Platforms:** all | …
**Files:** every file the fix may touch, tests and CLAUDE.md files included

## Problem        what goes wrong, for whom, with the quoted code and file:line at the reviewed commit
## Fix            exactly what to change; where there is a choice, the options and the recommended one
## Tests          the unit test to add (pure logic only), or why none can be written
## Manual check   only when the fix can't be verified without a device, account or network and a mistake would
                 hurt (data loss, a crash, a store rejection); otherwise leave the section out
```

A plan that only adds tests never touches production code: a test that fails on a real defect is `@Ignore`d with a
reason and reported as a finding for a later plan.

A plan is self-contained: an agent that reads only it and the files it names can carry it out. Quote code rather
than relying on line numbers alone, since the review's own fixes and earlier lanes move lines. A plan may say "drop
this plan if …" when its premise needs checking at execution time.

**`README.md`** — the index and the record of decisions: the reviewed commit and the angle; **Headlines** (the few
that matter, data loss first); **Fixed during the review** (hash and message of each commit); the **Index** table
(number, title, severity, lane); the **Lanes** table (lane, area, plans in execution order when that is not numeric,
files owned) and the **merge order** with the reason; the shared-file rules; **Decisions** — each open question with
the recommended default, marked as awaiting the user; **Checked and found solid**; **Dropped after verification**;
any **measurements**. No list of manual checks owed: the user does not track them.

**`EXECUTION.md`** — the brief for the orchestrating agent, which the user starts with *"Follow
`<plans folder>/EXECUTION.md`."* It restates section 4 below concretely for this sweep: preconditions, the lane
list with each lane's plan order, the subagent prompt with its placeholders filled in except the worktree path, the
verification commands, the merge order, and the finish.

Then report to the user in a few lines: what was fixed during the review (the count, and the headline fixes), the
number of plans per lane, what the challenge amended or dropped, and the open decisions — asked with
`AskUserQuestion`, recommendation first. Record the answers in the README and in memory.

## 3. Memory

Write (or update) one project memory per sweep, `<ordinal>-review.md`: date, commit, angle, the commit range of the
fixes made during the review, number of plans, lanes and merge order, decisions taken, what is still open. Update it
again when the plans land: the commit range, and what was skipped and why. Point the next sweep at it.

## 4. Execution (only when the user says so)

**Preconditions** — stop and tell the user if any fails: the tree is clean apart from the plans folder; the
challenge (section 1, step 7) has run on every plan; every open decision has an answer; the unit tests pass. Commit
the plans first if they are untracked (`Add the <nth> review plans.`), then note `START=$(git rev-parse HEAD)`.

**Lanes** — one **detached** worktree per lane next to the checkout, so no branch is created:
`git worktree add --detach ../<Repo>-lane-<x> START`. Spawn one `general-purpose` subagent per lane in a single
message, each confined to its worktree. Cap concurrent Gradle builds (see the last section); start the rest of the
lanes as earlier ones finish. A lane that builds on another lane's results is cut from the main checkout's `HEAD`
after that lane merged, not from `START`; its cherry-pick range then starts at that base.

Every lane prompt keeps the lanes apart — subagents share the session scratchpad, and one lane once ran another's
same-named helper scripts and committed into the wrong worktree:
- scratch files go to `<scratchpad>/lane-<x>/` only, and no lane runs a script it did not write;
- every command starts with `cd <absolute worktree> &&` or uses `git -C <absolute worktree>`;
- after every commit, `git show --stat HEAD` confirms the commit touched only that plan's files;
- never `git stash` — the worktree is the isolation.

**Per plan, in the lane's order** (the subagent's procedure):

1. Read the plan and the files it names. Load `code-style` before the first edit.
2. Implement exactly the plan, taking the recommended option. Re-locate moved code by the quoted snippet. If the plan
   is wrong, impossible, or its own "drop this plan if" condition holds, **do not improvise another fix**: revert the
   working tree, leave the plan file, note why, move on.
3. Add the tests it asks for; update the `CLAUDE.md` files and strings it names.
4. Verify with the commands in the last section — the tests, and a compile of every platform the change touches.
   A check that needs a device, an account, a store or a network condition is skipped, and named in the lane
   report only if a mistake there would hurt.
5. `git rm` the plan file in the same commit as its fix.
6. Load `commit-messages`; commit with a single `-m` sentence — one fix, one commit, never two fixes folded or one
   split. Then check the raw commit object and amend on the spot if it fails:

   ```
   test "$(git cat-file commit HEAD | sed '1,/^$/d' | wc -l | tr -d ' ')" = "1" || echo "MORE THAN ONE LINE"
   git log -1 --format=%B | grep -qiE 'co-authored|generated with|claude code|anthropic\.com' && echo "ATTRIBUTION"
   ```

   Never branch, merge, rebase, push, bump the version or dispatch a workflow from a lane.
7. Lane report: `NN: <hash> "<message>" — verified: <commands>; skipped manual: <what, why>`, then `Not done: NN —
   <why>`, then the final test result.

**Merging**, in the README's order, as lanes report: first list the paths each lane commit touched
(`git -C <worktree> log --stat --format=%s <base>..HEAD`) and stop on any outside the lane's files; then, in the main
checkout, `git cherry-pick <base>..<lane HEAD>`. Resolve conflicts keeping both sides' intent — `CLAUDE.md` paragraphs
and `strings.xml` as a word-level three-way merge, every sentence and key from both sides kept, never one side's
version whole. Run the tests after each lane and the full build after the last; a break is fixed by one more commit
(`Fix the iOS build after the storage changes.`), never by rewriting landed commits. Then
`git worktree remove ../<Repo>-lane-<x>`.

**Finish**: plan files still present are the skipped ones — leave them, with the README trimmed to them, or ask the
user whether to delete the folder. Final checks: `git log --format=%B START..HEAD | grep -ciE
'co-authored|generated with|claude code|anthropic\.com'` prints 0, `git log --oneline START..HEAD` is one line per
plan, `git status` is clean and `git worktree list` shows only the checkout. Report the commit count and the skipped plans
with reasons; name a hand check only where a mistake would hurt (data loss, a crash, a store rejection). Update the
memory.

**Last step**: when no plan file is left (only `README.md` and `EXECUTION.md`), delete the plans folder and commit it
as `Remove the review plans.` without asking. If skipped plans remain, the folder stays until the user decides.
**Do not push.**

## 5. Structure and refactoring sweeps

An angle of readability, SOLID, testability or file structure produces mostly refactors, most of them simple and
fixed during the review. Everything above still holds, with these additions.

- **Behaviour is preserved.** A refactor changes no observable behaviour; a bug found on the way is its own finding
  and its own commit, never folded into a move. A refactor that would change a public contract is a decision, as
  always.
- **A move carries everything with it.** Tests of the moved code, file-level annotations, flags and options of
  moved scripts, and every reference in docs, `CLAUDE.md` files and skills. After a move or rename, grep the whole
  repository for the old name and path. A refactor sweep once lost a lock test when code moved modules, a shell
  script's `-e`, and nine references in the notes, and none of it failed a build.
- **Check moves mechanically.** For a split or a move, compare the declarations before and after
  (`git diff --color-moved=dimmed-zebra`, or the sorted declaration lists) rather than re-reading by eye.
- **Plans written after the review's fixes** re-check every quoted snippet and file path against the new `HEAD`,
  since the refactors moved code.
- **Afterwards**, offer a regression review of the landed range: its angle is "did these refactors preserve
  behaviour", area by area against the commit before them.

## 6. The CLAUDE.md files, in every sweep

Whatever the angle, one reviewer in step 1 reviews every `CLAUDE.md` file (and the skills' repo-specific sections)
for three things. Its findings are almost always simple, so they are fixed during the review.

- **Relevant**: each file holds only what a reader changing code there cannot see in the code — invariants, rules a
  plausible change would break, the one-sentence why behind a surprising design, platform quirks. Narration of what the
  code does, inventories (files, classes, tests, strings, parameters), history ("used to", review numbers, dates), and
  long justifications are cut.
- **Up to date**: every class, function, file, setting, and behaviour a file names is checked against the code (grep
  it); what is false is fixed or dropped. A rule the code no longer follows is either the code's bug (a finding) or the
  doc's (fixed).
- **Not too long, and in the right place**: each level holds only what applies to everything below it — the root the
  repository-wide rules, a module's file the module's, a package's file the package's — and never repeats its parent
  or a sibling; a rule lives in one file and others point to it, or say nothing. Default budgets, unless the repository
  section sets others: root ≤ 2,500 words, a module's ≤ 1,500, a package's ≤ 600 (`wc -w`). A file over budget is cut,
  not split into more prose; a package with nothing non-obvious to say has no `CLAUDE.md`.

The docs reviewer also reads the diff of the review's own fixes at the end, so that fixes leave the files as tight as
they found them.

## This repository: Kubriko

- **Plans folder:** `issues/` at the repository root — not `documentation/`, which holds the library's published
  guides. Branch: `main`.
- **Kubriko is a published library.** A fix that changes public API (a signature, a default, a behaviour a consumer
  can observe) is a **decision** in the README, never a silent part of a plan: it breaks every game built on it,
  Tesselar first. Plans say which published artifact (`engine`, a plugin, a tool) a change ships in; public
  declarations they add need KDoc (see `code-style`). The published modules are the ones applying
  `kubriko-public-artifact`; `app/` and `examples/` are not published and may be restructured freely.
- **Moves in published modules are API changes too.** A public top-level function, property or Composable compiles
  into the JVM facade class of its file (`KubrikoViewportKt`), so moving it to another file breaks every consumer
  compiled against the old version; a public class moved to another package breaks source and binary alike. A
  refactor plan in a published module keeps public types in their packages and public top-level declarations in
  their files — or splits such a file keeping its facade (`@file:JvmName("<OldName>Kt")` with
  `@file:JvmMultifileClass` on every part, as `code-style` says) — or raises a decision. Private and internal code
  moves freely.
- **Consumers for the challenge:** Tesselar (`../Tesselar`) is the known consumer; the challenge greps it for every
  public API a plan changes. The documented usage patterns live in `documentation/*.md` — the headless `TickSource`
  flow in `documentation/TICK_SOURCE.md` is the one a first-sweep plan broke.
- **Areas that make good reviewer splits:** the engine core (`Kubriko`, `KubrikoViewport`, managers, actors, traits,
  `TickSource`, `TriangleBatch`); the plugins; the tools (debug menu, scene editor); the Showcase app and its
  platform shells; the examples. Per-frame paths are reviewed for allocations and redundant work — see the
  Performance section of `CLAUDE.md`.
- **Tests:** `./gradlew desktopTest`, or `./gradlew :<module>:desktopTest` for the modules a lane touched. A change to the
  web bridge's embedded JS also runs `node engine/src/webMain/checkTriangleBridge.mjs`.
- **Compile checks:** `./gradlew :<module>:build` for each module touched; the Showcase on the platform a change is
  specific to (`:app:desktop:run` is not needed — compiling is). Full build after the last lane: `./gradlew build`.
- **At most two concurrent Gradle builds.** A KGP "FqNames can't be derived from DirtyData" error is a stale
  incremental cache, not a code error — rerun before calling a lane red.
- A behaviour change updates `CLAUDE.md` and, where a published guide describes it, the matching
  `documentation/*.md`. Nothing is published to Maven Central from a sweep; that is the user's release step.
- Commit examples in this repo's voice: `Match the Showcase's desktop window background to its theme while
  resizing.` · `Add the <nth> review plans.` · `Remove the review plans.`
