# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

import os
import subprocess
import tempfile
import unittest

from check_sizes import FILE_LINES, FUNCTION_LINES, changed, checked_tree, functions, judged, measure

COMPOSABLE = """@Composable
internal fun ActorCard(
    modifier: Modifier = Modifier,
    title: String,
) {
    Column(modifier) {
        Text(title)
        Text("}")
    }
}

private fun String.shortened() = take(
    10,
)
"""


def sizes_of(files):
    return measure(list(files), files.__getitem__)


class FunctionsTest(unittest.TestCase):

    def test_a_function_ends_at_its_closing_brace_not_at_its_parameter_list(self):
        self.assertEqual([("ActorCard", 2, 9), ("String.shortened", 12, 3)], functions(COMPOSABLE))

    def test_brackets_in_strings_and_comments_are_not_counted(self):
        text = 'fun a() {\n    val b = "{"\n    // {\n    /* { */\n}\nfun c() = 1\n'
        self.assertEqual([("a", 1, 5), ("c", 6, 1)], functions(text))

    def test_a_chained_expression_body_runs_to_its_last_line(self):
        text = "fun a() = listOf(1)\n    .map { it }\n    .sum()\nfun b() = 2\n"
        self.assertEqual([("a", 1, 3), ("b", 4, 1)], functions(text))

    def test_a_member_function_is_measured_with_its_class_name(self):
        text = "class Renderer {\n    override fun update(delta: Int) {\n        a()\n    }\n}\n"
        self.assertEqual([("Renderer.update", 2, 3)], functions(text))

    def test_members_of_nested_classes_and_companions_are_qualified(self):
        text = (
            "class Outer {\n"
            "    companion object {\n"
            "        fun create() = 1\n"
            "    }\n"
            "\n"
            "    inner class Part {\n"
            "        fun draw() {\n"
            "            a()\n"
            "        }\n"
            "    }\n"
            "}\n"
        )
        self.assertEqual([("Outer.Companion.create", 3, 1), ("Outer.Part.draw", 7, 3)], functions(text))

    def test_a_class_header_spanning_lines_still_names_its_members(self):
        text = "data class Point(val x: Int)\nclass Store(\n    val a: Int,\n) : Base {\n    fun save() {\n    }\n}\n"
        self.assertEqual([("Store.save", 5, 2)], functions(text))

    def test_a_local_function_counts_toward_the_function_around_it(self):
        text = "fun outer() {\n    fun helper() {\n        a()\n    }\n    helper()\n}\n"
        self.assertEqual([("outer", 1, 6)], functions(text))

    def test_a_member_expression_body_runs_to_its_last_continuation_line(self):
        text = "class A {\n    fun a() = listOf(1)\n        .sum()\n    fun b() = 2\n}\n"
        self.assertEqual([("A.a", 2, 2), ("A.b", 4, 1)], functions(text))

    def test_an_abstract_member_is_one_line(self):
        text = "interface Loader {\n    fun load(path: String): Int\n}"
        self.assertEqual([("Loader.load", 2, 1)], functions(text))

    def test_a_fun_interface_is_not_a_function(self):
        text = "internal fun interface Clock {\n    fun now(): Long\n}\nfun interfaceScale() = 1\n"
        self.assertEqual([("Clock.now", 2, 1), ("interfaceScale", 4, 1)], functions(text))

    def test_a_class_literal_or_an_anonymous_object_is_not_a_class(self):
        text = (
            "object Api {\n"
            "    val x by lazy { Api::class.java }\n"
            "    val y = object : Runnable {\n"
            "        override fun run() {\n"
            "        }\n"
            "    }\n"
            "}\n"
        )
        self.assertEqual([("Api.run", 4, 2)], functions(text))


class SizesTest(unittest.TestCase):

    def test_tests_and_other_files_are_not_measured(self):
        sizes = sizes_of({
            "m/src/commonTest/kotlin/A.kt": "x\n" * 900,
            "m/src/desktopTest/kotlin/B.kt": "x\n" * 900,
            "CLAUDE.md": "w " * 9000,
            "build.gradle.kts": "x\n" * 900,
        })
        self.assertEqual({}, sizes)

    def test_test_fixtures_are_not_measured(self):
        self.assertEqual({}, sizes_of({"m/src/testFixtures/kotlin/A.kt": "x\n" * 900}))

    def test_a_main_file_and_its_functions_are_measured(self):
        sizes = sizes_of({"engine/src/commonMain/kotlin/A.kt": COMPOSABLE})
        self.assertEqual((14, FILE_LINES, "lines"), sizes["engine/src/commonMain/kotlin/A.kt"])
        self.assertEqual((9, FUNCTION_LINES, "lines"), sizes["engine/src/commonMain/kotlin/A.kt::ActorCard"])

    def test_a_long_member_crossing_the_limit_fails(self):
        def renderer(body_lines):
            return "class Renderer {\n    fun update() {\n" + "        a()\n" * body_lines + "    }\n}\n"

        path = "m/src/commonMain/kotlin/Renderer.kt"
        failures, _ = judged(
            sizes_of({path: renderer(FUNCTION_LINES - 2)}),
            sizes_of({path: renderer(FUNCTION_LINES - 1)}),
        )
        self.assertEqual(1, len(failures))
        self.assertIn("::Renderer.update", failures[0])


class JudgedTest(unittest.TestCase):

    def test_crossing_a_limit_fails(self):
        before = {"A.kt": (FILE_LINES, FILE_LINES, "lines")}
        failures, _ = judged(before, {"A.kt": (FILE_LINES + 1, FILE_LINES, "lines")})
        self.assertEqual(1, len(failures))

    def test_a_new_file_past_a_limit_fails(self):
        failures, _ = judged({}, {"A.kt::f": (FUNCTION_LINES + 1, FUNCTION_LINES, "lines")})
        self.assertEqual(1, len(failures))

    def test_what_was_already_past_a_limit_may_grow_with_a_note(self):
        failures, notes = judged({"A.kt": (600, 500, "lines")}, {"A.kt": (650, 500, "lines")})
        self.assertEqual(([], 1), (failures, len(notes)))

    def test_a_function_moved_to_another_file_keeps_its_past(self):
        failures, _ = judged({"A.kt::f": (200, 120, "lines")}, {"B.kt::f": (200, 120, "lines")})
        self.assertEqual([], failures)

    def test_a_long_member_of_another_class_is_not_its_past(self):
        failures, _ = judged({"A.kt::A.update": (200, 120, "lines")}, {"B.kt::B.update": (200, 120, "lines")})
        self.assertEqual(1, len(failures))


class ChangedTest(unittest.TestCase):

    def test_a_renamed_file_is_judged_against_its_old_path(self):
        with tempfile.TemporaryDirectory() as directory:
            def run(*arguments):
                subprocess.run(["git", "-C", directory, *arguments], check=True, capture_output=True)

            run("init", "-q")
            with open(os.path.join(directory, "Old.kt"), "w") as file:
                file.write("x\n" * 600)
            with open(os.path.join(directory, "Edited.kt"), "w") as file:
                file.write("y\n")
            run("add", ".")
            run("-c", "user.name=t", "-c", "user.email=t@t", "commit", "-qm", "base")
            run("mv", "Old.kt", "New.kt")
            with open(os.path.join(directory, "Edited.kt"), "a") as file:
                file.write("z\n")
            with open(os.path.join(directory, "Added.kt"), "w") as file:
                file.write("w\n")
            run("add", ".")
            working_directory = os.getcwd()
            os.chdir(directory)
            try:
                paths = changed("HEAD", True)
            finally:
                os.chdir(working_directory)
            self.assertEqual({"New.kt": "Old.kt", "Edited.kt": "Edited.kt", "Added.kt": None}, paths)

    def test_the_shell_hook_judges_files_git_does_not_track_yet(self):
        with tempfile.TemporaryDirectory() as directory:
            subprocess.run(["git", "-C", directory, "init", "-q"], check=True, capture_output=True)
            subprocess.run(
                ["git", "-C", directory, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "--allow-empty", "-m", "base"],
                check=True,
                capture_output=True,
            )
            os.makedirs(os.path.join(directory, "module", "src", "commonMain"))
            with open(os.path.join(directory, "module", "src", "commonMain", "Untracked.kt"), "w") as file:
                file.write("x\n" * (FILE_LINES + 1))
            working_directory = os.getcwd()
            os.chdir(directory)
            try:
                failures = checked_tree()
            finally:
                os.chdir(working_directory)
            self.assertEqual(1, len(failures))
            self.assertIn("Untracked.kt", failures[0])


if __name__ == "__main__":
    unittest.main()
