/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import com.pandulapeter.kubrikoCreator.session.JavaArgumentFile
import java.io.File
import kotlin.system.exitProcess

/**
 * A game for the tests of game processes to play, which does what its [MODE_PROPERTY] says.
 */
public object GameProcessTestGame {
    /**
     * The system property naming what the game does: `fail`, `sleep`, `spawn`, or, without it, print one line and end.
     */
    public const val MODE_PROPERTY: String = "kubriko.creator.test.mode"

    /**
     * The system property naming the file a sleeping or spawning game writes its process ID to.
     */
    public const val PID_FILE_PROPERTY: String = "kubriko.creator.test.pidFile"

    /**
     * Plays the game.
     */
    @JvmStatic
    public fun main(args: Array<String>) {
        when (System.getProperty(MODE_PROPERTY)) {
            "fail" -> {
                (1..40).forEach { println("Line $it") }
                exitProcess(3)
            }
            "sleep" -> {
                System.getProperty(PID_FILE_PROPERTY)?.let { File(it).writeText(ProcessHandle.current().pid().toString()) }
                Thread.sleep(60_000)
            }
            "spawn" -> {
                val pidFile = File(System.getProperty(PID_FILE_PROPERTY))
                val child = sleepingJava(File(pidFile.path + ".args"), File(pidFile.path + ".child")).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
                pidFile.writeText("${ProcessHandle.current().pid()} ${child.pid()}")
                Thread.sleep(60_000)
            }
            else -> println("Played.")
        }
    }

    /**
     * A command running this game in its sleeping mode with this JVM's classpath, passed through [argumentFile], since
     * Windows caps a command line at 32,767 characters.
     */
    public fun sleepingJava(argumentFile: File, pidFile: File): ProcessBuilder {
        argumentFile.writeText(JavaArgumentFile.format(listOf("-cp", System.getProperty("java.class.path"))))
        return ProcessBuilder(
            File(System.getProperty("java.home"), "bin/java").path,
            "@${argumentFile.path}",
            "-D$MODE_PROPERTY=sleep",
            "-D$PID_FILE_PROPERTY=${pidFile.path}",
            GameProcessTestGame::class.java.name,
        )
    }
}
