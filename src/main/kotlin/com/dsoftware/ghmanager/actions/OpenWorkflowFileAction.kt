package com.dsoftware.ghmanager.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor


class OpenWorkflowFileAction : RefreshAction("Open Workflow File", null, AllIcons.General.OpenDisk) {
    override fun update(e: AnActionEvent) {
        val selection = e.getData(ActionKeys.ACTION_DATA_CONTEXT)?.runSelectionHolder?.selection
        e.presentation.isEnabled = selection != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.debug("GitHubWorkflowLogReloadAction action performed")
        val selection = e.getRequiredData(ActionKeys.ACTION_DATA_CONTEXT).runSelectionHolder.selection
        val rootDirectory = e.getRequiredData(ActionKeys.ACTION_DATA_CONTEXT).repositoryMapping.gitRepository.root
        val project = e.project ?: return
        selection?.let {
            val file = rootDirectory.findFileByRelativePath(it.path) ?: return
            FileEditorManager.getInstance(project).openTextEditor(
                OpenFileDescriptor(project, file),
                true // request focus to editor
            )

        }
    }

    companion object {
        private val LOG = logger<OpenWorkflowFileAction>()
    }
}