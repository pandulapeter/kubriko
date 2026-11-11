/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.AudioFolderIndex
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypes
import com.pandulapeter.kubrikoCreator.commands.folders.FolderMoves
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.console.ConsoleLines
import com.pandulapeter.kubrikoCreator.context.KeyedCache
import com.pandulapeter.kubrikoCreator.context.ScreenplayTexts
import com.pandulapeter.kubrikoCreator.git.GitController
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.screenplay.DocumentStore
import com.pandulapeter.kubrikoCreator.screenplay.MovedFiles
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStore
import com.pandulapeter.kubrikoCreator.sprites.SpriteFolderStore
import com.pandulapeter.kubrikoCreator.sprites.SpritePalette
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * What commands read and change: the parts of the open project.
 *
 * @param blockTypes The project's block-built types, or null if the project hasn't opted in to them.
 * @param scannedFiles What the last resource scan found of the files commands check against.
 * @param report Adds an entry to the console, for what the user should know after a command.
 * @param buildFiles The project's build files that came from its template: the upgrade they need, as last checked in the
 *   background, so that validating reads no files, who to tell once a command changed them, and the game's targets.
 * @param rejectChanges Why no change may be made to the project at all right now (it was saved by a newer Kubriko
 *   Creator), or null if changes may be made.
 */
public class CommandContext(
    public val project: CreatorProject,
    public val console: StateFlow<ConsoleLines>,
    public val blockTypes: BlockTypes?,
    public val git: GitController,
    public val scenes: SceneStore,
    public val runner: ProjectRunner,
    public val ioDispatcher: CoroutineDispatcher,
    scannedFiles: ScannedFiles = ScannedFiles(),
    public val report: (ConsoleEntry) -> Unit = {},
    public val buildFiles: ProjectBuildFiles = ProjectBuildFiles.UpToDate,
    public val rejectChanges: () -> Rejection? = { null },
) {
    /**
     * The questions an AI asks the user ([com.pandulapeter.kubrikoCreator.commands.project.AskUser]) that wait for an answer.
     */
    public val questions: UserQuestions = UserQuestions()

    /**
     * What the assistant's conversations noted for the next one ([com.pandulapeter.kubrikoCreator.commands.project.RememberForNextTime]).
     */
    public val summaries: ConversationSummaries = ConversationSummaries()

    /**
     * The plans the assistant's conversations laid out ([ConversationPlans]).
     */
    public val plans: ConversationPlans = ConversationPlans()

    /**
     * Where Kubriko Creator moved pages and documents to, so that the page editor follows them.
     */
    public val movedFiles: MovedFiles = MovedFiles()

    /**
     * Where commands move folders to, so that the sidebar's open folders follow them.
     */
    public val folderMoves: FolderMoves = FolderMoves()

    /**
     * The folders of the game's own `files` folders, which sounds and music are kept in, as of the last resource scan
     * and the commands' changes since.
     */
    public val audioFolders: AudioFolderIndex = AudioFolderIndex { scenes.resourcesDirectory }

    /**
     * The sidebar's folders of sprites, whose writes are recorded as Kubriko Creator's own with the Scenes'.
     */
    public val spriteFolders: SpriteFolderStore = SpriteFolderStore(project.rootDirectory, scenes.ownWrites)

    /**
     * The colors the project keeps for drawing its sprites, whose writes are recorded as Kubriko Creator's own with the Scenes'.
     */
    public val palette: SpritePalette = SpritePalette(project.rootDirectory, scenes.ownWrites)

    /**
     * The screenplay's pages, whose writes are recorded as Kubriko Creator's own with the Scenes'.
     */
    public val screenplay: ScreenplayStore = ScreenplayStore(project.rootDirectory, scenes.ownWrites, movedFiles)

    /**
     * The sounds and music of the project, or null if they aren't known, so they aren't checked.
     */
    public val audioFiles: () -> AudioFiles? = scannedFiles.audioFiles

    /**
     * The images Actor types can be drawn as, or null if they aren't known, so they aren't checked.
     */
    public val spriteFiles: () -> SpriteFiles? = scannedFiles.spriteFiles

    /**
     * Every file the last resource scan found, by group, or null before the first scan.
     */
    internal val scannedResources: () -> Map<ResourceGroup, List<ProjectResource>>? = scannedFiles.resources

    private val _filesRevision = MutableStateFlow(0)

    /**
     * Raised whenever a command created, deleted, or moved a file no store reports on its own (a sprite, a sound, music,
     * or a page's image), which the watcher never reports either, being Kubriko Creator's own write, so that the
     * resource tree is scanned again.
     */
    public val filesRevision: StateFlow<Int> = _filesRevision.asStateFlow()

    internal fun onFilesChanged() = _filesRevision.update { it + 1 }

    private val _spriteImagesRevision = MutableStateFlow(0)

    /**
     * Raised whenever a command wrote, deleted, or renamed a sprite's image, which the watcher never reports, being Kubriko
     * Creator's own write, so that the Scene view and the playing game follow it as they follow one made outside.
     */
    public val spriteImagesRevision: StateFlow<Int> = _spriteImagesRevision.asStateFlow()

    internal fun onSpriteImagesChanged() = _spriteImagesRevision.update { it + 1 }

    /**
     * The project's Markdown files that aren't pages, whose writes are recorded as Kubriko Creator's own with the Scenes'.
     */
    public val documents: DocumentStore = DocumentStore(project.rootDirectory, scenes.ownWrites, movedFiles)

    /**
     * The project's Markdown as last read ([com.pandulapeter.kubrikoCreator.context.screenplayTexts]).
     */
    internal val screenplayTextsCache = KeyedCache<ScreenplayTexts>()

    /**
     * The hand-written Kotlin read once for a change made of many deletes
     * ([com.pandulapeter.kubrikoCreator.context.withKotlinFilesHeld]), or null outside one.
     */
    internal var heldKotlinFiles: Map<String, String>? = null

    /**
     * Returns once every change made so far is on disk, or writing them failed, for what reads the files (git, and builds);
     * whether they all are on disk.
     */
    internal suspend fun awaitWrites() = blockTypes?.awaitWrites() ?: true

    /**
     * Why a command that reads the files is refused while [awaitWrites] says they aren't all on disk.
     */
    internal fun unsavedWrite(): Rejection {
        val unsaved = checkNotNull(blockTypes).unsavedWrites()
        return when (val subjects = unsaved.subjects) {
            null -> Rejection(CommandProblem.GENERATED_CODE_NOT_WRITTEN, unsaved.reason)
            else -> Rejection(CommandProblem.CHANGES_NOT_WRITTEN, subjects, unsaved.reason)
        }
    }

    /**
     * Reads the types again after a change rewrote their files, returning once they are in memory, so that the next
     * command doesn't write the old ones back.
     */
    internal suspend fun reloadTypes() {
        blockTypes?.reload()
    }

    /**
     * A hash of the content of each type, Scene, page, and document in [targets], by its target without Event or Block indices, for telling
     * whether it changed since. A deleted type or Scene hashes as 0, so one that is still deleted matches. A Scene that
     * exists but can't be read right now (the running game holds it open on Windows) gets null, and so does every other
     * kind of target: a null hash isn't compared.
     */
    internal suspend fun contentHashesOf(targets: Collection<CommandTarget>): Map<CommandTarget, Int?> {
        val hashes = LinkedHashMap<CommandTarget, Int?>()
        val sceneNames = ArrayList<String>()
        val pageNames = ArrayList<String>()
        val documentPaths = ArrayList<String>()
        targets.forEach { target ->
            when (target) {
                is CommandTarget.BlockType -> hashes[CommandTarget.BlockType(target.name)] = blockTypes?.let { it[target.name].hashCode() }
                is CommandTarget.Scene -> sceneNames += target.name
                is CommandTarget.Page -> pageNames += target.name
                is CommandTarget.Document -> documentPaths += target.path
                else -> Unit
            }
        }
        if (pageNames.isNotEmpty()) {
            withContext(ioDispatcher) { pageNames.forEach { name -> hashes[CommandTarget.Page(name)] = screenplay.read(name)?.hashCode() ?: 0 } }
        }
        if (documentPaths.isNotEmpty()) {
            withContext(ioDispatcher) { documentPaths.forEach { path -> hashes[CommandTarget.Document(path)] = documents.fileOf(path).takeIf { it.isFile }?.readBytes()?.contentHashCode() ?: 0 } }
        }
        if (sceneNames.isNotEmpty()) {
            withContext(ioDispatcher) {
                sceneNames.forEach { name -> hashes[CommandTarget.Scene(name)] = if (scenes.fileOf(name).isFile) scenes.read(name)?.hashCode() else 0 }
            }
        }
        return hashes
    }
}
