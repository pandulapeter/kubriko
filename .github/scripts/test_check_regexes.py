# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

import unittest

from check_regexes import findings


def lines(source):
    return [line for line, _ in findings(source)]


class FindingsTest(unittest.TestCase):

    def test_a_bare_closing_brace_is_reported(self):
        self.assertEqual([1], lines('private val RULE = Regex("""\\.(\\w+)\\s*\\{([^}]*)}""")'))

    def test_a_bare_opening_brace_is_reported(self):
        self.assertEqual([1], lines('val title = Regex("{title: (.*)")'))

    def test_escaped_braces_counts_and_braces_in_a_class_are_accepted(self):
        self.assertEqual([], lines('val a = Regex("""\\{([^}]*)\\}[0-9]{1,2}x{3}y{2,}[{}]""")'))
        self.assertEqual([], lines('val b = Regex("[\\\\r\\\\n{}]+")'))

    def test_braces_of_a_property_name_or_code_point_escape_are_accepted(self):
        self.assertEqual([], lines('val p = Regex("""\\p{L}+\\P{Lu}\\N{SPACE}\\x{1F600}""")'))
        self.assertEqual([], lines('val q = Regex("\\\\p{Alpha}")'))

    def test_a_literal_with_to_regex_is_checked(self):
        self.assertEqual([1], lines('val c = """a}""".toRegex()'))
        self.assertEqual([1], lines('val c = """\n    a}\n""".trimIndent().toRegex()'))
        self.assertEqual([1], lines('val c = "a}".toPattern()'))

    def test_every_literal_of_a_concatenated_pattern_is_checked(self):
        self.assertEqual([3], lines('\n\nval d = Regex(\n    "^(a|b)" +\n        "c}$",\n    RegexOption.IGNORE_CASE,\n)'))

    def test_templates_strings_outside_a_regex_and_comments_are_not_read_as_braces(self):
        self.assertEqual([], lines('val e = Regex("""a${OTHER.pattern}b""")\nval f = "{title}"\n// Regex("}")'))


if __name__ == "__main__":
    unittest.main()
