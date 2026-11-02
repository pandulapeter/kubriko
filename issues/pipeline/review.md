# Review of one phase of the fifth review's plans, in CI

You run headless in GitHub Actions on the branch `{{BRANCH}}`, in a fresh checkout at its latest commit. Nobody is watching,
so decide yourself and finish.

**What to review:** the commits `{{BASE}}..HEAD`, which landed the review plans {{PLANS}} together with any extra fixes.
Read each plan's text with `git show {{BASE}}:issues/<plan file>`. `issues/EXECUTION-LOG.md` holds the implementing agents'
reports.

**Step 1, find bugs.** Look for correctness bugs only:
- a fix that breaks a caller, a documented usage (`creator/CLAUDE.md`, the package guides), or another plan's fix;
- threading, ordering, or lifecycle problems the changes introduce;
- state left half-changed on a failure path;
- a moved file that lost something on the way: a test, an annotation, a reference in docs or skills (grep for the old names);
- a test that can't fail.

Also check each commit against `creator/CLAUDE.md` → Definition of done. Skip style nits and anything a plan explicitly
decided. To review in parallel, spawn up to three read-only subagents, one per group of commits, and verify every finding
they return against the code yourself.

**Step 2, fix what you verified.** Fix each verified finding in its own commit, with a test that fails without the fix where
the logic is testable:
- load `code-style` before editing and `commit-messages` before committing;
- the pre-commit hook must pass, never `--no-verify`;
- verify with `./gradlew :creator:test` and `:creator:spotlessApply`, plus the tasks a fix touches;
- `git push origin HEAD:{{BRANCH}}` after every commit;
- never rewrite landed commits.

Never leave a verified bug unfixed for a decision: the user wants no known bugs left. Where a fix has several reasonable
forms, take the safest one that keeps what the plans decided, and say in the log which you chose and why.

**Step 3, log it.** Append a `## Review of {{PLANS}}` section to `issues/EXECUTION-LOG.md`: each finding with its fix commit
or its reason, and one line for what you dropped as not real. Commit it as `Log the review of the <phase> review plans.` and
push.

**Rules:** never branch, merge, rebase, force-push, bump a version, publish, or dispatch a workflow. Never edit
`.github/workflows/fifth-review-*.yml` or `issues/pipeline/`. Scratch files go to `build/scratch/` (git-ignored). Only plain shell commands are allowed: no variable-assignment prefixes, no inline Python or heredocs; use the Read, Edit, and Write tools for files.

End with a short summary: findings, fixes, and the final test result.
