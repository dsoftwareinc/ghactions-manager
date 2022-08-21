package com.dsoftware.ghtoolbar.ui.consolepanel

import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.ui.UIUtil
import javax.swing.plaf.PanelUI

class LogConsolePanel(
    project: Project,
    logModel: SingleValueModel<String?>,
    disposable: Disposable,
) : ConsoleViewImpl(project, true), AnsiEscapeDecoder.ColoredTextAcceptor {
    private val ansiEscapeDecoder = AnsiEscapeDecoder()

    // when it's true its save to call editor, otherwise call 'editor' will throw an NPE
    private val objectInitialized = true;

    init {
        LOG.info("Create console")
        if (!logModel.value.isNullOrBlank()) {
            this.setData(logModel.value!!)
        }
        logModel.addListener {
            if (!logModel.value.isNullOrBlank()) {
                this.setData(logModel.value!!)
            }
        }

        Disposer.register(disposable) {
            Disposer.dispose(this)
        }
    }

    private fun setData(message: String) {
        this.clear()
        ansiEscapeDecoder.escapeText(message, ProcessOutputType.STDOUT, this)
    }

    override fun coloredTextAvailable(text: String, attributes: Key<*>) {
        this.print(text, ConsoleViewContentType.getConsoleViewType(attributes))
    }

    override fun setUI(ui: PanelUI?) {
        super.setUI(ui)
        if (objectInitialized && editor != null) {
            (editor as EditorImpl).backgroundColor = UIUtil.getPanelBackground()
        }
    }

    companion object {
        private val LOG = logger<LogConsolePanel>()
    }
}

