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

import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionInitScriptTest {
    private val script = SessionInitScript.file.readText()

    @Test
    fun theScriptRegistersTheTaskKubrikoCreatorRuns() {
        assertTrue("register<JavaExec>(\"${SessionInitScript.SESSION_TASK_NAME}\")" in script)
    }

    @Test
    fun theScriptRegistersTheClasspathTaskKubrikoCreatorRuns() {
        assertTrue("register(\"${SessionInitScript.CLASSPATH_TASK_NAME}\")" in script)
    }

    @Test
    fun theScriptStartsTheSessionLauncherThatShipsWithCreatorSession() {
        val launcherClassName = Regex("mainClass.set\\(\"([^\"]+)\"\\)").find(script)!!.groupValues[1]
        Class.forName(launcherClassName).getMethod("main", Array<String>::class.java)
    }

    @Test
    fun theArgumentsPassTheScriptAndEveryPropertyAsAProjectProperty() {
        assertEquals(
            listOf("--init-script", SessionInitScript.file.absolutePath, "--no-configuration-cache", "-Pa=1", "-Pb=two words"),
            SessionInitScript.gradleArguments(mapOf("a" to "1", "b" to "two words")),
        )
    }

    @Test
    fun theScriptPassesThePortInThePropertyTheSessionReads() {
        assertTrue("systemProperty(\"${SessionProtocol.PORT_PROPERTY}\"" in script)
    }

    @Test
    fun theScriptInjectsCreatorSessionAtTheProjectsOwnKubrikoVersion() {
        assertTrue("io.github.pandulapeter.kubriko:tool-creator-session:\${kubrikoVersion(runtime)}" in script)
        assertTrue("it.group == \"io.github.pandulapeter.kubriko\" && it.module == \"engine\"" in script)
    }
}
