package com.dsoftware.ghmanager.psi

import com.dsoftware.ghmanager.psi.GitHubWorkflowConfig.FIELD_USES
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex

class ProjectStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.messageBus.connect().subscribe<FileEditorManagerListener>(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    scanWorkflowFile(project, file)
                }
            })
        runReadAction {
            val yamlFiles = FilenameIndex.getAllFilesByExt(project, "yml")
            yamlFiles.addAll(FilenameIndex.getAllFilesByExt(project, "yaml"))
            val workflowFiles = yamlFiles.filter {
                Tools.isActionFile(it) || Tools.isWorkflowFile(it)
            }
            workflowFiles.forEach { file ->
                scanWorkflowFile(project, file)
            }
        }
    }

    fun scanWorkflowFile(project: Project, workflowFile: VirtualFile) {
        if (!Tools.isWorkflowFile(workflowFile) || !Tools.isActionFile(workflowFile)) {
            return
        }
        runReadAction {
            val psiManager = project.service<PsiManager>()
            val gitHubActionCache = project.service<GitHubActionCache>()
            psiManager.findFile(workflowFile)?.let {
                val actionNames = Tools.getYamlElementsWithKey(it, FIELD_USES).map { yamlKeyValue ->
                    yamlKeyValue.valueText.split("@").firstOrNull() ?: return@map null
                }.filterNotNull()
                gitHubActionCache.actionsToResolve.addAll(actionNames)
            }
        }
    }
}
