<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Review the commits of every finished plan item before the next one starts, and fix what the review finds

**Kind:** process  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** process request P3 (user, 2026-11-02; the Definition of done, P2, already landed in `creator/CLAUDE.md`)  ·  **Depends on:** none
**Files:**
- `creator/plan/README.md`
- `creator/plan/HANDOVER.md` (its opening paragraph only)
- `creator/CLAUDE.md` (the **Definition of done** list, one bullet)

## Problem

This sweep found ~150 problems in code that had passed its tests: half-applied commands, fakes that differ from the real stores,
loops that die on one exception, blocking reads on the main thread. Most were introduced by one plan item and would have been
caught by a review of that item's commits while its context was fresh; instead they waited for a whole-codebase sweep. Nothing in
Creator's process asks for a review between items. `creator/plan/README.md` opens with:

```markdown
Only future work lives here; once something is implemented, it is deleted from these files, and `../CLAUDE.md` gains only the rules it
introduces that the code can't show. Each topic
has a file of its own, so a session only needs the one it works on.

[HANDOVER.md](HANDOVER.md) is where the last session stopped: start there.
```

and `creator/plan/HANDOVER.md` opens with:

```markdown
Where the next session starts: what to do next and what still has to be checked by hand. Each session rewrites it before it ends.
The rules are in `../CLAUDE.md`, what is planned in the other files here.
```

`creator/CLAUDE.md` → Definition of done lists what a change must meet, but not that a finished item is reviewed.

## Fix

1. `creator/plan/README.md`: after the sentence ending "…the code can't show.", add a short paragraph (and re-wrap the stray
   "Each topic\nhas a file" line break while there):

   > Finishing an item takes one more step before the next starts: run `/code-review` on the item's commits
   > (`git log` from where it started), fix what it finds in commits of their own, and only then delete the item here. The review
   > checks the change against `../CLAUDE.md` → Definition of done, so what a whole-codebase sweep would find later is found
   > while the item is fresh.

2. `creator/plan/HANDOVER.md`: append to the opening paragraph: "A session that finished an item notes here whether its review
   (`README.md`) ran and what it fixed; one that stopped before the review says so under Next, so the next session starts with it."
   This is the part of HANDOVER that every rewrite keeps (the rest is rewritten each session).

3. `creator/CLAUDE.md` → Definition of done: add a last bullet, "Is reviewed with `/code-review` before the next plan item starts
   (`plan/README.md`)." Keep the file within its ~4,000-word budget (it is at about 3,900); if it would cross it, shorten this
   bullet rather than another.

Use the Oxford comma in any list you write. Run `python3 .github/scripts/check_doc_names.py --staged` and
`python3 .github/scripts/check_license_headers.py` before committing.

## Tests

None (documentation only). `check_doc_names.py --staged` passes (the only names quoted are `/code-review`, `README.md`, and
`../CLAUDE.md`, which aren't code names).
