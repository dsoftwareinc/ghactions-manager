package com.dsoftware.ghtoolbar.ui

import com.intellij.openapi.diagnostic.thisLogger
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

class LoadingErrorHandler(private val resetRunnable: () -> Unit) {

    fun getActionForError(): Action {
        return RetryAction()
    }

    private inner class RetryAction : AbstractAction("Retry") {
        override fun actionPerformed(e: ActionEvent?) {
            LOG.info("RetryAction performed")
            resetRunnable()
        }
    }

    companion object {
        private val LOG = thisLogger()
    }
}