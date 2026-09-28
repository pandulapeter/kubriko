# Executing the second review sweep

You are the orchestrating agent. The user started you with *"Follow `issues/EXECUTION.md`."* Read `issues/README.md`
first and the `codebase-review` skill's section 4. Repository: `/Users/pandulapeter/Projects/Kubriko`, branch `main`.
Never create a branch, push, bump `library.version`, publish, or dispatch a workflow.

## 1. Preconditions — stop and tell the user if any fails

1. `git status --short` shows nothing outside `issues/`.
2. Every plan carries a `**Challenged:**` line, none says `DROP`, and every row of the README's **Decisions** table
   has an answer. Carry out each plan with the answered option (its Fix section describes both).
3. `./gradlew desktopTest` passes.

## 2. Before the plans

If `issues/` is uncommitted, load `commit-messages` and commit it: `Add the second review plans.` Then
`START=$(git rev-parse HEAD)`.

## 3. The single lane — in the main checkout, plans 01 → 07 in order

Seven plans, one lane, no worktrees. You may do them yourself or through one `general-purpose` subagent; if you use a
subagent, every Bash command it runs must start with `cd /Users/pandulapeter/Projects/Kubriko && …`, and its scratch
files go only under its own subfolder of the session scratchpad.

For each plan:
1. Read the plan and the files it names; load `code-style` before the first edit.
2. Implement exactly the plan with the answered option. If it is wrong or impossible, do not improvise: revert, leave
   the plan file, note why, move on. If a small variation clearly works where the plan fails, stop and report it
   instead of substituting it.
3. Add its tests; update the docs it names.
4. Verify:

   | Plan | Tests | Compile checks |
   |---|---|---|
   | 01 | `:plugins:physics:desktopTest` | `:plugins:physics:build` |
   | 02 | `:plugins:physics:desktopTest` (the un-ignored allocation budget) | `:plugins:physics:build`; `:plugins:collision:build` if Step 3 is approved |
   | 03 | `:engine:desktopTest` | `:engine:build` |
   | 04 | `:engine:desktopTest` | `:engine:build` |
   | 05 | — | `:engine:build` |
   | 06 | `:engine:desktopTest`, `:plugins:physics:desktopTest` | `:tools:test-fixtures:build` |
   | 07 | the plan's dry-run counts and one real `:engine:build` | `./gradlew build` (the final full build doubles as this) |

5. `git rm issues/NN-*.md` in the same commit as the fix.
6. Load `commit-messages`; one plan, one single-sentence commit; then check it:
   ```
   test "$(git cat-file commit HEAD | sed '1,/^$/d' | wc -l | tr -d ' ')" = "1" || echo "MORE THAN ONE LINE"
   git log -1 --format=%B | grep -qiE 'co-authored|claude|session|generated' && echo "ATTRIBUTION"
   git show --stat HEAD   # only this plan's files
   ```

## 4. Finish

1. `./gradlew desktopTest`, then `./gradlew build`. A break is fixed with one more commit, never by rewriting.
2. If every plan landed, `git rm -r issues` and commit `Remove the review plans.`; otherwise trim the README to the
   skipped plans and ask the user.
3. `git log --format=%B $START..HEAD | grep -ciE 'co-authored|claude|session|generated'` prints `0`; `git status` is
   clean.
4. Report the commits, skipped plans with reasons and the manual checks owed; update the `second-review` memory.
   **Do not push.**
