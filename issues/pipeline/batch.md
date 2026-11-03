# One batch of the fifth review's follow-up plans, in CI

You run headless in GitHub Actions on the branch `{{BRANCH}}`, in a fresh checkout at its latest commit. Nobody is watching:
there is no one to ask, so decide as the instructions below say, and finish the batch.

You carry out the Kubriko review plans {{PLANS}} (in that order) from `issues/`, one commit per plan, then stop. Earlier
batches already landed every plan whose file is gone from `issues/`.

## Read first

- `issues/README.md` holds the plans' order and which modules each touches. Kubriko Creator is ten Gradle modules; see
  `creator/CLAUDE.md` → Modules.
- `issues/EXECUTION-LOG.md` holds every earlier batch's report. Read it: the code your plans were written against has moved,
  so re-locate code by the plans' quoted snippets and by what it does, not by line numbers or old names.
- Load the `code-style` skill before your first edit and the `commit-messages` skill before your first commit, and read
  `creator/CLAUDE.md` (Running and testing, Definition of done) once.

## Per plan

1. Read the plan and every file it names. Implement exactly the plan, taking its decided or recommended option.
2. **If the plan is wrong, impossible, or its "Drop this plan if" condition holds:** revert only that plan's paths and never
   improvise another fix. Append one line `- <ID>: <why>` to `issues/SKIPPED.md`, commit that alone
   (`Skip review plan <ID>, <why in a few words>.`), push, and move on.
3. Add the tests it asks for. They must fail without the fix; show it by setting your main changes aside (copy them to
   `build/scratch/`, which git ignores), checking those paths out, running the test, and copying them back. Never `git stash`.
4. Update the `CLAUDE.md` lines, package guides, and `strings.xml` entries it names.
5. Verify:
   - `./gradlew :creator:allTests` (every Creator module) and `./gradlew spotlessApply`;
   - `:tools:creator-session:desktopTest` and `:tools:creator-api:build` when touched;
   - `python3 .github/scripts/check_sizes.py` over the files you changed (constructors at most 12 parameters, functions 120
     lines, files 500), and `python3 .github/scripts/check_doc_names.py` for every `CLAUDE.md` you touched.
   - Gradle runs can take several minutes: give the Bash tool a long timeout, or run Gradle in the background and wait for it.
   - A KGP "FqNames can't be derived from DirtyData" error is a stale cache; rerun it before treating it as a failure.
6. `git rm` the plan file in the same commit as its fix. Append the plan's report (format below) to
   `issues/EXECUTION-LOG.md` in that same commit, stage explicit paths only, and commit with one sentence
   (`git commit -m "..."`). The pre-commit hook runs the repository checks and tests: never `--no-verify`. If the hook fails,
   fix the cause and commit again.
7. Check the commit, and amend on the spot if any check fails:
   - `test "$(git cat-file commit HEAD | sed '1,/^$/d' | wc -l | tr -d ' ')" = "1" || echo "MORE THAN ONE LINE"`
   - `git log -1 --format=%B | grep -qiE 'co-authored|generated with|claude code|anthropic\.com' && echo "ATTRIBUTION"`
   - `git show --stat HEAD` shows only that plan's files, the plan file, and the log.
8. `git push origin HEAD:{{BRANCH}}` after every commit, so a failure later loses nothing.

## Rules

- **Bugs:** a bug you discover that no plan covers is fixed, never just reported. Fix it in its own commit, with a test that
  fails without the fix, after the plan that exposed it, push it, and add it to the log under `Extra fixes`.
- **Git:** never branch, merge, rebase, force-push, bump a version, publish, or dispatch a workflow.
- **Scope:** never edit `.github/workflows/fifth-review-*.yml` or `issues/pipeline/`.
- **Scratch files** go to `build/scratch/` in the checkout (git ignores it); edits outside the checkout are refused.
- **Shell:** only plain commands are allowed (git, `./gradlew`, `python3 .github/scripts/…`, and the usual read-only tools).
  Don't start a command with a variable assignment (`F=… && cp`), don't run inline Python or heredoc scripts, and don't
  write through `cat >`; use the Read, Edit, and Write tools for files, and full paths instead of variables.

## The log entry for each plan

```
## <ID> — <hash of the fix commit, or "see next commit" if unknown before committing>
"<commit message>" — verified: <commands>; skipped manual: <what, why>
- what changed (types, files, and names later plans need), and every deviation from the plan with its reason
- Extra fixes: <hash> "<message>", or none
```

End your run with a short summary of the batch: plans landed, plans skipped and why, and the final test result.
