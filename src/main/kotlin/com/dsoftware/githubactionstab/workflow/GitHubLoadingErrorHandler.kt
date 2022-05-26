package com.dsoftware.githubactionstab.workflow

import com.intellij.openapi.diagnostic.logger
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

class GitHubLoadingErrorHandler(private val resetRunnable: () -> Unit) {

    fun getActionForError(): Action {
        return RetryAction()
    }

    private inner class RetryAction : AbstractAction("Retry") {
        override fun actionPerformed(e: ActionEvent?) {
            LOG.debug("RetryAction performed")
            resetRunnable()
        }
    }

    companion object {
        private val LOG = logger<GitHubLoadingErrorHandler>()
    }
}