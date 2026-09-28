# Executing the first review sweep

You are the orchestrating agent. The user started you with *"Follow `issues/EXECUTION.md`."* Read `issues/README.md`
first (lanes, merge order, decisions) and the `codebase-review` skill's section 4. Repository:
`/Users/pandulapeter/Projects/Kubriko`, branch `main`. Never create a branch, push, bump `library.version`, publish,
or dispatch a workflow.

## 1. Preconditions — stop and tell the user if any fails

1. `git status --short` shows nothing but `?? issues/` (or, if the plans are already committed, nothing at all).
2. Every plan file carries a `**Challenged:**` line (the challenge pass ran), none says `DROP` or `needs user
   decision`, and every row of the README's **Decisions** table is marked answered. A plan whose decision went against its
   recommendation is carried out with the option the user chose — the plan's Fix section describes both.
3. `./gradlew build` passes at the current commit (there are no unit tests before plan `00`).

## 2. Before the lanes

1. If `issues/` is untracked, load `commit-messages` and commit it: `git add issues && git commit -m "Add the first
   review plans."`
2. Apply plan `00` in the main checkout exactly as written (load `code-style` first), run `./gradlew desktopTest`,
   `git rm issues/00-*.md` in the same commit, and commit it with one sentence (e.g. `Add a unit test setup to the
   library modules.`).
3. `START=$(git rev-parse HEAD)` — record it; every lane is cut from it.

## 3. Lanes

Create one detached worktree per lane next to the checkout, only when that lane starts:

```
git worktree add --detach ../Kubriko-lane-<x> $START
```

| Lane | Plans, in this order | Module test tasks | Compile checks |
|---|---|---|---|
| A | 01 … 25 | `:engine:desktopTest` | `:engine:build`, `:examples:game-annoyed-penguins:build` (for 22) |
| B | 30 … 44 | `:plugins:audio-playback:desktopTest`, `:plugins:keyboard-input:desktopTest`, `:plugins:pointer-input:desktopTest`, `:plugins:gamepad-input:desktopTest` | `:plugins:<name>:build` for each touched plugin |
| C | 45 … 61 | `:plugins:<name>:desktopTest` for persistence, particles, shaders, physics, collision, sprites, serialization | `:plugins:<name>:build` for each touched plugin |
| D | 65 … 79 | `:tools:<name>:desktopTest` for debug-menu, scene-editor, ui-components | `:tools:<name>:build` for each touched tool; `:app:desktop:compileKotlin` |
| E | 80 … 88 | `:app:shared:desktopTest` | `:app:desktop:compileKotlin`, `:app:web:compileKotlinWasmJs`, `:app:ios:compileKotlinIosSimulatorArm64`, `:app:android:assembleDebug` (whichever the plan's platform needs) |
| F | 90 … 99 | none (no example tests) | `:examples:<module>:build` for each touched module |

(Task names are the expected ones; if one does not exist, use `./gradlew :<module>:tasks` to find its equivalent.
iOS compile tasks need a Mac with Xcode; skip and report them otherwise.)

**At most two Gradle builds at once.** Start lanes **A** and **B** together; when one finishes start **C**, then
**D**, then **F**, then **E**. Spawn each lane's subagent (`general-purpose`) with the prompt below, filling in
`<WORKTREE>`, `<LANE>`, `<PLANS>`, `<TEST TASKS>` and `<COMPILE CHECKS>` from the table.

### Lane subagent prompt

```
You are executing lane <LANE> of Kubriko's first review sweep. Work ONLY inside the worktree <WORKTREE> (a detached
checkout of commit START; never touch /Users/pandulapeter/Projects/Kubriko itself). Your plans, in this exact order:
<PLANS> — each is issues/NN-*.md inside your worktree. Read issues/README.md's Decisions table: carry out each plan with
the option recorded there (the recommended one unless the table says otherwise).

For each plan, in order:
1. Read the plan and every file it names. Before your first edit load the `code-style` skill (MPL-2.0 header on new
   files, KDoc on public API, sparse comments, strings in strings.xml).
2. Implement exactly the plan. Earlier plans may have moved code — re-locate it by the quoted snippet. If the plan is
   wrong, impossible, or its own "drop this plan if" condition holds, do NOT improvise another fix: `git checkout -- .`
   and `git clean -fd` for files you created, leave the plan file in place, note why, move on.
3. Add the tests the plan asks for; update the CLAUDE.md/README/documentation files and strings it names. Tests must
   not create a real `ImageBitmap` or run a Skia draw: library test classpaths have no native Skia runtime, so use a
   fake. `commonTest` must compile on every target; JVM-only APIs go in `desktopTest`.
4. Verify: <TEST TASKS>, then <COMPILE CHECKS> for the modules this plan touched. Only one Gradle build at a time.
   Manual checks needing a device, account, store or network condition are skipped and reported.
5. `git rm issues/NN-*.md` in the same commit as the fix.
6. Load the `commit-messages` skill. Commit with a single `-m` sentence ending in a period, no trailer, no attribution —
   one plan, one commit. Then run:
     test "$(git cat-file commit HEAD | sed '1,/^$/d' | wc -l | tr -d ' ')" = "1" || echo "MORE THAN ONE LINE"
     git log -1 --format=%B | grep -qiE 'co-authored|claude|session|generated' && echo "ATTRIBUTION"
   and amend on the spot if either prints.
Never create a branch, merge, rebase, push, bump the version or dispatch a workflow.

Finish with a report, one line per plan:
  NN: <hash> "<message>" — verified: <commands>; skipped manual: <what, why>
then `Not done: NN — <why>` for each skipped plan, then the final result of <TEST TASKS>.
```

## 4. Merging — in the order A → B → C → D → F → E

A lane that finishes early waits for the lanes before it in this order. For each lane, in the main checkout:

```
git cherry-pick $START..<lane worktree HEAD hash>
```

Lane F's cherry-pick touches Annoyed Penguins' `GameplayManager.kt`, which lane A (`20`) already edited in a
separate hunk. Resolve conflicts keeping both sides' intent; `CLAUDE.md` and `strings.xml` as a word-level three-way merge that keeps
every sentence and key from both sides (none are expected — see the README's shared-file rules). After each lane run
`./gradlew desktopTest`. After the last lane run `./gradlew build`. A break is fixed with one more commit in the repo's
voice (`Fix the iOS build after the audio changes.`), never by rewriting landed commits. Then
`git worktree remove ../Kubriko-lane-<x>`.

## 5. Finish

1. Plan files still in `issues/` are the skipped ones. Trim `README.md` to them (their index rows, lanes, decisions)
   and ask the user whether to keep or delete the folder; if everything landed, `git rm -r issues` with the commit
   `Remove the review plans.`
2. Final checks:
   - `git log --format=%B $START..HEAD | grep -ciE 'co-authored|claude|session|generated'` prints `0`;
   - `git log --oneline $START..HEAD` is one line per landed plan (plus any build-fix and the plans-removal commit);
   - `git status` is clean and `git worktree list` shows only the checkout.
3. Report to the user: the commit count, the skipped plans with reasons, and the manual checks owed (the README's
   list minus nothing — none can be done by an agent). Update the `first-review` memory with the commit range, the
   skipped plans and the manual checks still owed. **Do not push.**
