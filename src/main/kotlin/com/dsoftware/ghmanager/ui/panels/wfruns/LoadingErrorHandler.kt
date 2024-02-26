package com.dsoftware.ghmanager.ui.panels.wfruns

import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

class LoadingErrorHandler(private val resetRunnable: () -> Unit) {
    fun getActionForError(): Action {
        return RetryAction()
    }

    private inner class RetryAction : AbstractAction("Retry") {
        override fun actionPerformed(e: ActionEvent?) = resetRunnable()
    }
}