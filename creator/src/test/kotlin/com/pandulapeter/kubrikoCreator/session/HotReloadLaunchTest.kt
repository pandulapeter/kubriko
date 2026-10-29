/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

import com.pandulapeter.kubrikoCreator.scenes.ProjectClasspath
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HotReloadLaunchTest {
    private val launch = GameLaunch(File("/jdk/bin/java"), "game.MainKt", File("/game"), listOf("-Xmx1g"), listOf(File("/game/classes"), File("/libs/session.jar")))
    private val hotReloadLaunch = HotReloadLaunch(File("/jbr/bin/java"), File("/game/build/run/game.argfile"), "hotReloadDesktopMain", listOf(File("/libs/engine-2.jar"), File("/libs/session.jar")))

    @Test
    fun theClasspathFileSaysHowHotReloadWouldRunTheGame() {
        val classpath = ProjectClasspath.parse(
            "kubrikoVersion=1.0.0\n/game/classes\nlaunch.java=/jdk/bin/java\nlaunch.mainClass=game.MainKt\nlaunch.workingDir=/game\nlaunch.classpath=/game/classes\n" +
                "launch.hotJava=/jbr/bin/java\nlaunch.hotArgumentFile=/game/build/run/game.argfile\nlaunch.hotReloadTask=hotReloadDesktopMain\n" +
                "launch.hotLibrary=/libs/engine-2.jar\nlaunch.hotLibrary=/libs/session.jar\n",
        )
        assertEquals(hotReloadLaunch, classpath?.hotReloadLaunch)
        assertEquals(listOf(File("/game/classes")), classpath?.launch?.classpath, "the hot lines aren't part of the plain launch")
        assertNull(ProjectClasspath.parse("kubrikoVersion=1.0.0\nlaunch.hotJava=/jbr/bin/java\n")?.hotReloadLaunch)
    }

    @Test
    fun theGameRunsWithHotReloadsArgumentsAndOwnFilesThenTheSessionLibraries() {
        val hotClasspath = listOf("/game/build/run/classpath/hot", "/game/build/run/classpath/classes", "/libs/engine-1.jar", "/libs/hot-reload-runtime.jar")
        val argumentFile = listOf(
            "\"-Dcompose.reload.pidFile=/game/build/run/game.pid\"",
            "\"-XX:+AllowEnhancedClassRedefinition\"",
            "\"-javaagent:/libs/agent.jar\"",
            "\"-cp\"",
            "\"${hotClasspath.joinToString("${File.pathSeparator}\\\n")}\"",
            "\"game.MainKt\"",
        ).joinToString("\n")
        assertEquals(
            launch.copy(
                java = File("/jbr/bin/java"),
                jvmArguments = listOf(
                    "-Dcompose.reload.pidFile=/game/build/run/game.pid",
                    "-XX:+AllowEnhancedClassRedefinition",
                    "-javaagent:/libs/agent.jar",
                    "-Dcompose.reload.devToolsEnabled=false",
                    "-Xmx1g",
                ),
                classpath = listOf(
                    File("/game/build/run/classpath/hot"),
                    File("/game/build/run/classpath/classes"),
                    File("/libs/hot-reload-runtime.jar"),
                    File("/libs/engine-2.jar"),
                    File("/libs/session.jar"),
                ),
                reloadTaskName = "hotReloadDesktopMain",
            ),
            hotReloadLaunch.applyTo(launch, argumentFile, runtimeFiles = listOf(File("/game/classes"), File("/libs/engine-1.jar"))),
        )
    }

    @Test
    fun anArgumentFileWithoutAClasspathGivesNoLaunch() {
        assertNull(hotReloadLaunch.applyTo(launch, "\"-Da=b\"\n\"game.MainKt\"", emptyList()))
        assertNull(hotReloadLaunch.applyTo(launch, "\"-Da=b\" -cp", emptyList()))
    }
}
