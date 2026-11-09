# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

import unittest

from renumber_creator_plans import renamed, rewritten

FILES = ["01-known-issues.md", "03-blocks.md", "04-git.md", "README.md", "HANDOVER.md"]


class RenamedTest(unittest.TestCase):

    def test_a_gap_left_by_a_deleted_file_closes(self):
        self.assertEqual(
            {"01-known-issues.md": "01-known-issues.md", "03-blocks.md": "02-blocks.md", "04-git.md": "03-git.md"},
            renamed(FILES),
        )

    def test_named_files_come_first_and_the_rest_keep_their_order(self):
        self.assertEqual(
            {"04-git.md": "01-git.md", "01-known-issues.md": "02-known-issues.md", "03-blocks.md": "03-blocks.md"},
            renamed(FILES, ["git"]),
        )

    def test_a_name_given_twice_is_refused(self):
        with self.assertRaises(ValueError):
            renamed(FILES, ["git", "git"])

    def test_an_unknown_name_is_refused(self):
        with self.assertRaises(ValueError):
            renamed(FILES, ["debugger"])


class RewrittenTest(unittest.TestCase):
    renames = renamed(FILES)

    def test_references_follow_their_file(self):
        self.assertEqual(
            "See `02-blocks.md` → Appearance and `03-git.md`.",
            rewritten("See `03-blocks.md` → Appearance and `04-git.md`.", self.renames),
        )

    def test_roadmap_items_are_numbered_as_their_file(self):
        self.assertEqual(
            "1. [Known issues](01-known-issues.md) - a\n2. [Blocks](02-blocks.md) - b\n",
            rewritten("1. [Known issues](01-known-issues.md) - a\n3. [Blocks](03-blocks.md) - b\n", self.renames),
        )

    def test_a_name_of_no_plan_file_is_left_alone(self):
        self.assertEqual("`05-settings.md` and `03-other.md`", rewritten("`05-settings.md` and `03-other.md`", self.renames))


if __name__ == "__main__":
    unittest.main()
