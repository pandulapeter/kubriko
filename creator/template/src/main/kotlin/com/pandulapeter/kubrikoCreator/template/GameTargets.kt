/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.foundation.AndroidVersions
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.project.ProjectInspector
import com.pandulapeter.kubrikoCreator.project.withLineBreaksOf
import com.pandulapeter.kubrikoCreator.project.withUnixLineBreaks
import java.io.File

/**
 * The platforms the game in [rootDirectory] is built for ([GameTarget]), turned on and off as changes to its build files
 * that are shown before they are made, since those files belong to the user. The desktop target is always on, since
 * Kubriko Creator plays and edits the game through it. Reads and writes files, so it is used off the main thread.
 */
public class GameTargets(
    private val rootDirectory: File,
    private val android: AndroidVersions,
    private val host: TargetHost = TargetHost.System,
) {
    /**
     * Every target, whether it is on, and what stands in its way here, or null if the project isn't opted in, so its build
     * files aren't the template's.
     */
    public fun read(): List<TargetState>? {
        val files = files() ?: return null
        val androidSdk = files.androidSdk
        val invalidLocalSdk = AndroidTarget.localSdkDirectory(files)?.takeIf { !SystemTargetHost.isSdk(File(it)) }
        return GameTarget.entries.map { target ->
            val isOn = isOn(target, files)
            TargetState(
                target = target,
                isOn = isOn,
                problem = when (target) {
                    GameTarget.DESKTOP -> TargetProblem.DESKTOP_ALWAYS_ON
                    GameTarget.WEB -> null
                    GameTarget.ANDROID -> when {
                        isOn && invalidLocalSdk != null -> TargetProblem.ANDROID_SDK_DIRECTORY_INVALID
                        androidSdk != null -> null
                        isOn -> TargetProblem.ANDROID_SDK_MISSING_WHILE_ON
                        else -> TargetProblem.ANDROID_SDK_MISSING
                    }
                    GameTarget.IOS -> if (isOn || host.canBuildIos) null else TargetProblem.XCODE_MISSING
                },
                arguments = if (target == GameTarget.ANDROID && isOn && invalidLocalSdk != null) listOf(invalidLocalSdk) else emptyList(),
            )
        }
    }

    /**
     * What turning [target] on (or off, as [isOn] says) takes, without changing anything.
     */
    public fun change(target: GameTarget, isOn: Boolean): TargetChange {
        val edits = TargetEdits.ALL[target] ?: return TargetChange.Refused(TargetProblem.DESKTOP_ALWAYS_ON)
        val files = files() ?: return TargetChange.Refused(TargetProblem.CHANGED_BY_HAND, listOf(target.key))
        if (edits.isOn(files) == isOn) return TargetChange.Refused(if (isOn) TargetProblem.ALREADY_ON else TargetProblem.ALREADY_OFF, listOf(target.key))
        if (ProjectUpgrader(rootDirectory).check() != TemplateUpgrade.UpToDate) return TargetChange.Refused(TargetProblem.TEMPLATE_OUTDATED)
        if (isOn) refusalOfTurningOn(target, files)?.let { return it }
        val changed = (if (isOn) edits.turnedOn(files) else edits.turnedOff(files))
            ?: return TargetChange.Refused(TargetProblem.CHANGED_BY_HAND, listOf(target.key))
        val changes = changed.entries.sortedBy { it.key }.mapNotNull { (path, text) ->
            val before = files.read(path)
            if (before == text) null else ProposedFileChange(path = path, before = before.orEmpty(), after = text, isNew = before == null)
        }
        return TargetChange.Proposed(target = target, isOn = isOn, changes = changes, isReversible = isReversible(edits, isOn, files, changes))
    }

    /**
     * Whether turning [edits]' target back after [changes] brings every file they change back as [files] has it, keeping only
     * the files they create and what they add to the kept paths.
     */
    private fun isReversible(edits: TargetEdits, isOn: Boolean, files: TargetFiles, changes: List<ProposedFileChange>): Boolean {
        val changed = changes.associate { it.path to it.after }
        val after = files.copy { path -> if (path in changed) changed[path] else files.read(path) }
        val reverted = (if (isOn) edits.turnedOff(after) else edits.turnedOn(after)) ?: return false
        return reverted.all { (path, text) -> text == files.read(path) } &&
            changes.all { it.isNew || it.path in edits.keptPaths || reverted[it.path] == it.before }
    }

    /**
     * Makes [change], if the files are still as it found them.
     *
     * @return Whether it was made.
     */
    @EditorMutation
    public fun apply(change: TargetChange.Proposed): Boolean = applyAll(rootDirectory, change.changes)

    /**
     * Why the game can't take [applicationId], which iOS refuses while it is built for iOS, or null if it can.
     */
    public fun refusalOfApplicationId(applicationId: String): TargetChange.Refused? {
        val files = files() ?: return null
        return if ('_' in applicationId && IosTarget.isOn(files)) TargetChange.Refused(TargetProblem.APPLICATION_ID_NOT_FOR_IOS, listOf(applicationId)) else null
    }

    /**
     * Gives the Xcode project the game's [applicationId] as its bundle ID, which Xcode reads before anything Gradle could
     * hand it, if the project has one.
     */
    @EditorMutation
    public fun writeApplicationId(applicationId: String) {
        val file = File(rootDirectory, IosTarget.XCODE_CONFIGURATION)
        if (!file.isFile) return
        val raw = file.readText()
        val changed = IosTarget.withBundleIdentifier(raw.withUnixLineBreaks(), applicationId)
        if (changed != raw.withUnixLineBreaks()) file.writeTextAtomically(changed.withLineBreaksOf(raw))
    }

    private fun refusalOfTurningOn(target: GameTarget, files: TargetFiles) = when (target) {
        GameTarget.ANDROID -> if (files.androidSdk == null) TargetChange.Refused(TargetProblem.ANDROID_SDK_MISSING) else null
        GameTarget.IOS -> when {
            !host.canBuildIos -> TargetChange.Refused(TargetProblem.XCODE_MISSING)
            '_' in files.applicationId -> TargetChange.Refused(TargetProblem.APPLICATION_ID_NOT_FOR_IOS, listOf(files.applicationId))
            else -> null
        }
        else -> null
    }

    private fun isOn(target: GameTarget, files: TargetFiles) = TargetEdits.ALL[target]?.isOn(files) ?: true

    /**
     * The project's files, or null if it isn't opted in, so its build files aren't the template's.
     */
    private fun files(): TargetFiles? {
        val packageName = ProjectUpgrader(rootDirectory).packageName ?: return null
        return TargetFiles(
            packageName = packageName,
            projectId = ProjectInspector.nameOf(rootDirectory),
            applicationId = GameIdentity.read(rootDirectory)?.applicationId ?: packageName,
            android = android,
            androidSdk = host.androidSdk(rootDirectory),
            reader = { path -> File(rootDirectory, path).takeIf { it.isFile }?.readText()?.withUnixLineBreaks() },
        )
    }
}
