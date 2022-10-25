package com.dsoftware.ghmanager.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor


class OpenWorkflowFileAction : RefreshAction("Open Workflow File", null, AllIcons.General.OpenDisk) {
    override fun update(e: AnActionEvent) {
        val path = e.getData(ActionKeys.SELECTED_WORKFLOW_RUN_FILEPATH)
        e.presentation.isEnabled = path != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.debug("GitHubWorkflowLogReloadAction action performed")
        val path = e.getRequiredData(ActionKeys.SELECTED_WORKFLOW_RUN_FILEPATH)
        val rootDirectory = e.getRequiredData(ActionKeys.ACTION_DATA_CONTEXT).repositoryMapping.gitRepository.root
        val project = e.project ?: return
        path?.let {
            val file = rootDirectory.findFileByRelativePath(it) ?: return
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