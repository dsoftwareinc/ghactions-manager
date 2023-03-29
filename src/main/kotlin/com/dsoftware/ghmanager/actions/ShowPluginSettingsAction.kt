package com.dsoftware.ghmanager.actions

import com.dsoftware.ghmanager.ui.ToolbarUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction

class ShowPluginSettingsAction : DumbAwareAction(
    "Open GHActions-Manager Settings",
    null,
    AllIcons.General.Settings
) {
    override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(
            e.project, ToolbarUtil.SETTINGS_DISPLAY_NAME
        )
    }
}