# Executing the third review's plans

Brief for the orchestrating agent. Start: *"Follow `issues/EXECUTION.md`."* The process is the `codebase-review`
skill's section 4 and section 5; this file fills it in for this sweep.

**State:** the 179 Now plans landed as `92b16c51..70de96c6` with the procedure below (six lanes from `91c5941b`, merged
E → P → T → G → D → A, no conflicts). What remains are the 48 **Planned** plans (`<lane>50`+), rebased on `70de96c6`.
Run them only after the user has answered their decisions in `README.md` → Decisions (record the answers there first),
and only the plans the user starts. For those, the same procedure applies with these changes:

- Ranges: each lane's Planned plans in the dependency order README → Lanes gives (E50 before E53, E51 before E54, P52
  before P53, P55 before P56, T50 and T51 before T53, A51 before A52/G55/G60, D58 before D59).
- Cross-lane plans (E51 → pointer-input and `app/desktop/CLAUDE.md`; E55 → shaders/sprites actuals; G55 and G60 →
  demos and `app/`) run in the lane that owns the plan, and that lane's worktree is cut after every lane whose files it
  touches has merged.
- Re-run the challenge on any Planned plan whose decision the user answered differently from the recommendation.
- Before starting, re-check the plans against `HEAD` if commits landed after `70de96c6`.

## Preconditions

Stop and tell the user if any fails:

- The tree is clean apart from `issues/` (the user's own uncommitted edits to the iOS xcschemes are allowed; no plan
  touches them, and no lane may).
- The challenge has run on every plan (README → Challenge).
- `./gradlew desktopTest` passes on the main checkout.
- Commit the plans if they are untracked (`Add the third review plans.`), then `START=$(git rev-parse HEAD)`.

## Lanes

| Lane | Area | Now plans, in order | Worktree |
|---|---|---|---|
| E | `engine/`, `gradle/build-logic/`, root `CLAUDE.md`, `documentation/` | E01–E11 | `../Kubriko-lane-e` |
| P | `plugins/*` | P01–P22 | `../Kubriko-lane-p` |
| T | `tools/*` | T01–T35 | `../Kubriko-lane-t` |
| G | `examples/shared`, `examples/game-*` | G01–G45 | `../Kubriko-lane-g` |
| D | `examples/demo-*`, `examples/test-*` (incl. `-noop`) | D01–D47 | `../Kubriko-lane-d` |
| A | `app/*` | A01–A19 | `../Kubriko-lane-a` |

No two lanes' Now plans touch the same file, so every lane is cut from `START`:

```
git worktree add --detach ../Kubriko-lane-<x> $START
grep '^sdk.dir=' local.properties > ../Kubriko-lane-<x>/local.properties   # Android SDK only, never the signing keys
```

Spawn all six lane subagents (`general-purpose`) in one message. At most two Gradle builds run at once: every Gradle call
goes through the slot script, which waits for a free slot:

```
/private/tmp/claude-501/-Users-pandulapeter-Projects-Kubriko/1bad350b-37e9-4473-b5cd-931734e90434/scratchpad/gradle-slot.sh <absolute worktree> <gradle args>
```

If a lane dies holding a slot, remove `scratchpad/gradle-slots/slot<n>` after checking that its `owner` pid is gone.

## Lane subagent prompt

Fill in `<x>` (lowercase lane letter), `<X>` (uppercase), `<WT>` (absolute worktree path) and `<RANGE>`:

> You execute lane `<X>` of the third Kubriko review sweep, in the git worktree `<WT>` (detached HEAD — never create a
> branch, never merge, rebase, push, stash, bump a version or dispatch a workflow). Your plans are
> `<WT>/issues/<X>01`… through `<RANGE>`, in numeric order; ignore `<X>50`+ and every other lane's files.
>
> Isolation (other lanes run at the same time and share the scratchpad):
> - every shell command starts with `cd <WT> &&` or uses `git -C <WT>`; never touch `/Users/pandulapeter/Projects/Kubriko` itself;
> - scratch files only in `/private/tmp/claude-501/-Users-pandulapeter-Projects-Kubriko/1bad350b-37e9-4473-b5cd-931734e90434/scratchpad/lane-<x>/`; run no script you did not write, except the Gradle slot script;
> - run Gradle only as `/private/tmp/claude-501/-Users-pandulapeter-Projects-Kubriko/1bad350b-37e9-4473-b5cd-931734e90434/scratchpad/gradle-slot.sh <WT> <args>`.
>
> Load the `code-style` skill before your first edit and the `commit-messages` skill before your first commit. Then, per plan:
> 1. Read the plan and every file it names. Re-locate code by the quoted snippet (earlier plans move lines). A
>    `**Challenged:** amended` line means the plan text is already the corrected version.
> 2. Implement exactly the plan, taking the recommended option. Verbatim moves stay verbatim (comments travel with
>    their declarations; `private` widens to `internal` only where another file needs it). If the plan is wrong,
>    impossible, or its "Drop this plan if" condition holds: do not improvise — `git -C <WT> checkout -- . && git -C <WT> clean -fd -- ':!issues'`,
>    leave the plan file, note why, and go on.
> 3. Add the tests it asks for; update the CLAUDE.md files, strings and docs it names. After a move or rename, grep
>    the whole worktree (code, docs, CLAUDE.md files, skills, build files, workflows) for the old name and path.
> 4. Verify with the plan's Verify commands (through the slot script). A compile of every source set the plan
>    touches; `:<module>:desktopTest` when it adds or moves tests. Manual checks are skipped and reported.
> 5. `git -C <WT> rm` the plan file, and stage exactly the plan's changes.
> 6. Commit with one `-m` sentence in the repo's voice (one plan, one commit). Then check:
>    ```
>    test "$(git -C <WT> cat-file commit HEAD | sed '1,/^$/d' | wc -l | tr -d ' ')" = "1" || echo "MORE THAN ONE LINE"
>    git -C <WT> log -1 --format=%B | grep -qiE 'co-authored|claude|session|generated' && echo "ATTRIBUTION"
>    git -C <WT> show --stat HEAD
>    ```
>    and amend on the spot if the message fails or the commit touched a file outside the plan.
> 7. After the last plan: `gradle-slot.sh <WT> <every touched module>:build` (or the compile tasks of each touched
>    module on all its targets, plus `desktopTest`) once for the whole lane.
>
> Report: `NN: <hash> "<message>" — verified: <commands>; skipped manual: <what>` per plan, then `Not done: NN — <why>`,
> then the final build/test result.

Ranges: E → `E11`, P → `P22`, T → `T35`, G → `G45`, D → `D47`, A → `A19`.

## Merging

In this order, as lanes report: **E → P → T → G → D → A**. The engine and plugins go first because everything compiles
against them; `app/` goes last because it compiles against every example.

For each lane:

1. `git -C ../Kubriko-lane-<x> log --stat --format=%s $START..HEAD` — stop on any path outside the lane's area, and on
   any commit whose message has more than one line or an attribution.
2. In the main checkout: `git cherry-pick $START..$(git -C ../Kubriko-lane-<x> rev-parse HEAD)`.
3. `./gradlew desktopTest`. A break is fixed by one more commit (`Fix the <x> build after the <y> changes.`), never by
   rewriting landed commits.
4. `git worktree remove ../Kubriko-lane-<x>`.

After the last lane: `./gradlew build` and `node engine/src/webMain/checkTriangleBridge.mjs`.

## Finish

- `git log --format=%B $START..HEAD | grep -ciE 'co-authored|claude|session|generated'` prints 0.
- `git log --oneline $START..HEAD` is one line per landed plan (plus any build fix).
- `git status` is clean apart from the user's xcscheme edits; `git worktree list` shows only the checkout.
- Every Now plan file is gone. Skipped ones stay, listed in the README with the reason.
- Re-check the remaining plans against the new `HEAD`, update `README.md` and this file, and commit that as
  `Update the remaining review plans to the landed changes.`
- Update the `third-review` memory note: the landed range, skips, deviations, manual checks owed.
- Report the commit count, the skipped plans, the manual checks owed and the open decisions. **Do not push.** When no
  plan file is left, delete `issues/` and commit `Remove the review plans.`
