/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp.clients

import kotlin.test.Test
import kotlin.test.assertEquals

class ShellAliasesTest {

    @Test
    fun zshAndBashListingsAreReadWithTheirQuoting() {
        val zsh = """
            claude='echo '\''Use specific commands: claude-flashfood or claude-personal'\'
            claude-personal='CLAUDE_CONFIG_DIR=~/.claude-personal /opt/homebrew/bin/claude'
            run-help=man
        """.trimIndent()
        assertEquals(
            mapOf(
                "claude" to "echo 'Use specific commands: claude-flashfood or claude-personal'",
                "claude-personal" to "CLAUDE_CONFIG_DIR=~/.claude-personal /opt/homebrew/bin/claude",
                "run-help" to "man",
            ),
            ShellAliases.parse(zsh),
        )
        assertEquals(mapOf("ll" to "ls -l", "say" to "echo \"hi\""), ShellAliases.parse("alias ll='ls -l'\nalias say='echo \"hi\"'\nbash: no job control in this shell"))
    }

    @Test
    fun fishListingsAreReadWithTheirQuoting() {
        val fish = """
            alias claude 'echo \'Use specific commands\''
            alias claude-personal 'CLAUDE_CONFIG_DIR=~/.claude-personal /opt/homebrew/bin/claude'
            alias codex-work 'env CODEX_HOME=~/.codex-work codex'
            alias g git
            alias path 'echo C:\\Users'
        """.trimIndent()
        assertEquals(
            mapOf(
                "claude" to "echo 'Use specific commands'",
                "claude-personal" to "CLAUDE_CONFIG_DIR=~/.claude-personal /opt/homebrew/bin/claude",
                "codex-work" to "env CODEX_HOME=~/.codex-work codex",
                "g" to "git",
                "path" to "echo C:\\Users",
            ),
            ShellAliases.parse(fish),
        )
    }

    @Test
    fun theProgramIsTheFirstWordThatRunsSomething() {
        assertEquals("claude", ShellAliases.programOf("CLAUDE_CONFIG_DIR=~/.claude-personal /opt/homebrew/bin/claude"))
        assertEquals("codex", ShellAliases.programOf("env CODEX_HOME=~/.codex-work codex"))
        assertEquals("echo", ShellAliases.programOf("echo 'Use specific commands'"))
        assertEquals("", ShellAliases.programOf(""))
    }

    @Test
    fun quotingIsResolvedAsAShellWould() {
        assertEquals("it's \"here\" \\ \$x", ShellAliases.unquote("'it'\\''s' \"\\\"here\\\"\" \\\\ '\$x'"))
    }

    @Test
    fun environmentsAreTheVariablesInFrontOfTheProgram() {
        assertEquals(
            mapOf("CLAUDE_CONFIG_DIR" to "/Users/me/.claude-personal"),
            ShellAliases.environmentOf("CLAUDE_CONFIG_DIR=~/.claude-personal /opt/homebrew/bin/claude", home = "/Users/me"),
        )
        assertEquals(
            mapOf("CODEX_HOME" to "/Users/me/.codex-work", "MODE" to "fast"),
            ShellAliases.environmentOf("env CODEX_HOME=\$HOME/.codex-work MODE=\"fast\" codex --flag=1", home = "/Users/me"),
        )
        assertEquals(mapOf("A" to "/Users/me"), ShellAliases.environmentOf("A=\${HOME} claude", home = "/Users/me"))
        assertEquals(emptyMap(), ShellAliases.environmentOf("claude --model=opus", home = "/Users/me"))
    }
}
