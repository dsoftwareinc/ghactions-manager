package com.dsoftware.ghtoolbar.ui.actions

import com.dsoftware.ghtoolbar.ui.ToolbarUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction

class ShowToolbarSettingsAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(
            e.getProject(), ToolbarUtil.SETTINGS_DISPLAY_NAME
        )
    }
}