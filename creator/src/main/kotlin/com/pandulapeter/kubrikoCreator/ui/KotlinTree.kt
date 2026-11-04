/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import java.util.TreeMap

/**
 * A row of the sidebar's tree of Kotlin files.
 */
internal sealed interface KotlinTreeRow {
    val depth: Int

    /**
     * What tells this row apart from every other folder or container of the tree, and what [kotlinTreeRows] takes as
     * toggled.
     */
    val key: String

    /**
     * The files of one platform, or of a source set that is no platform's (such as `commonTest`), which are named by it.
     */
    data class Container(
        val platform: SourcePlatform?,
        val sourceSet: String?,
        val isExpanded: Boolean,
    ) : KotlinTreeRow {
        override val depth get() = 0
        override val key get() = containerKey(platform, sourceSet)
    }

    /**
     * A package, below the one every Kotlin file of the project is in, named by what it adds to its parent's
     * (`ui.menu` when `ui` holds nothing else).
     */
    data class Package(
        override val key: String,
        val name: String,
        override val depth: Int,
        val isExpanded: Boolean,
    ) : KotlinTreeRow

    /**
     * @param detail The source set of a file another one of the same name is next to, which tells them apart.
     */
    data class File(
        val resource: ProjectResource,
        override val depth: Int,
        val detail: String?,
    ) : KotlinTreeRow {
        override val key get() = resource.relativePath
    }
}

/**
 * The package every one of [resources] is in, the longest one they share (empty when one is in the default package).
 */
internal fun rootPackageOf(resources: List<ProjectResource>): String {
    var root: List<String>? = null
    resources.forEach { resource ->
        val segments = packageSegments(resource.kotlinPackage.orEmpty())
        root = root?.let { shared -> shared.subList(0, shared.zip(segments).takeWhile { (a, b) -> a == b }.size) } ?: segments
    }
    return root.orEmpty().joinToString(".")
}

/**
 * The rows of the Kotlin files in [resources]: the files of each platform's source sets in a container of their own
 * (those of a source set that is no platform's in one named after it), followed by the shared ones, and in each, a
 * folder for every package below [rootPackage] that holds something, merged with the one it holds while that is all it
 * holds. At every level, containers come first, then folders, then files, each sorted by name. Containers start closed
 * and folders open, and each one whose key is in [toggled] the other way, unless [isEverythingOpen].
 */
internal fun kotlinTreeRows(
    resources: List<ProjectResource>,
    rootPackage: String,
    toggled: Set<String>,
    isEverythingOpen: Boolean = false,
): List<KotlinTreeRow> {
    val collapsed = if (isEverythingOpen) emptySet() else toggled
    val rootSegments = packageSegments(rootPackage)
    val shared = PackageNode()
    val containers = TreeMap<ContainerId, PackageNode>()
    resources.forEach { resource ->
        val sourceSet = ResourceScanner.sourceSetOf(resource)
        val node = if (sourceSet == null || sourceSet in SHARED_SOURCE_SETS) {
            shared
        } else {
            val platform = sourcePlatformOf(sourceSet)
            containers.getOrPut(ContainerId(platform, if (platform == null) sourceSet else null)) { PackageNode() }
        }
        val segments = packageSegments(resource.kotlinPackage.orEmpty())
        val relative = if (segments.take(rootSegments.size) == rootSegments) segments.drop(rootSegments.size) else segments
        relative.fold(node) { parent, segment -> parent.children.getOrPut(segment) { PackageNode() } }.files += resource
    }
    val rows = mutableListOf<KotlinTreeRow>()
    containers.forEach { (id, node) ->
        val container = KotlinTreeRow.Container(id.platform, id.sourceSet, isExpanded = isEverythingOpen || containerKey(id.platform, id.sourceSet) in toggled)
        rows += container
        if (container.isExpanded) rows.addLevel(node, container.key, "", depth = 1, collapsed)
    }
    rows.addLevel(shared, SHARED_KEY, "", depth = 0, collapsed)
    return rows
}

/**
 * The platform whose code a source set holds (`androidMain`, `iosArm64Main`), or null for one that is shared between
 * them, or holds tests.
 */
internal fun sourcePlatformOf(sourceSet: String): SourcePlatform? {
    if (!sourceSet.endsWith("Main")) return null
    val target = sourceSet.removeSuffix("Main").lowercase()
    return when {
        target.startsWith("android") -> SourcePlatform.ANDROID
        target.startsWith("desktop") || target.startsWith("jvm") -> SourcePlatform.DESKTOP
        target.startsWith("ios") -> SourcePlatform.IOS
        target.startsWith("wasm") || target.startsWith("js") || target == "web" -> SourcePlatform.WEB
        else -> null
    }
}

private fun MutableList<KotlinTreeRow>.addLevel(node: PackageNode, containerKey: String, parentPath: String, depth: Int, collapsed: Set<String>) {
    node.children.forEach { (segment, child) ->
        var name = segment
        var folder = child
        while (folder.files.isEmpty() && folder.children.size == 1) {
            val (onlySegment, onlyChild) = folder.children.entries.single()
            name = "$name.$onlySegment"
            folder = onlyChild
        }
        val path = if (parentPath.isEmpty()) name else "$parentPath.$name"
        val row = KotlinTreeRow.Package("$containerKey/$path", name, depth, isExpanded = "$containerKey/$path" !in collapsed)
        this += row
        if (row.isExpanded) addLevel(folder, containerKey, path, depth + 1, collapsed)
    }
    val duplicateNames = node.files.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
    node.files.sortedWith(compareBy<ProjectResource> { it.name.lowercase() }.thenBy { it.relativePath }).forEach { resource ->
        this += KotlinTreeRow.File(resource, depth, detail = if (resource.name in duplicateNames) ResourceScanner.sourceSetOf(resource) else null)
    }
}

private fun packageSegments(kotlinPackage: String) = if (kotlinPackage.isEmpty()) emptyList() else kotlinPackage.split('.')

private fun containerKey(platform: SourcePlatform?, sourceSet: String?) = "container:${platform?.name ?: sourceSet}"

private class PackageNode {
    val children = TreeMap<String, PackageNode>(String.CASE_INSENSITIVE_ORDER.thenComparing(naturalOrder()))
    val files = mutableListOf<ProjectResource>()
}

/**
 * A container of the tree: a [platform]'s, or, for none, a [sourceSet]'s. Platforms come first, in their order, then
 * source sets by name.
 */
private data class ContainerId(val platform: SourcePlatform?, val sourceSet: String?) : Comparable<ContainerId> {
    override fun compareTo(other: ContainerId) = compareValuesBy(this, other, { it.platform == null }, { it.platform }, { it.sourceSet?.lowercase() }, { it.sourceSet })
}

/**
 * The source sets whose files are shown outside every container: the shared code of a multiplatform project, and the
 * only code of a single-platform one.
 */
private val SHARED_SOURCE_SETS = setOf("commonMain", "main")

private const val SHARED_KEY = "shared"
