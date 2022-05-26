package com.dsoftware.githubactionstab.ui

import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key

class GitHubWorkflowRunLogConsole(
    project: Project,
    logModel: SingleValueModel<String?>,
    disposable: Disposable,
) : ConsoleViewImpl(project, true), AnsiEscapeDecoder.ColoredTextAcceptor {

    init {
        LOG.debug("Create console")
        val myTextAnsiEscapeDecoder = AnsiEscapeDecoder()
        logModel.addListener {
            this.clear()
            if (!logModel.value.isNullOrBlank()) {
                myTextAnsiEscapeDecoder.escapeText(logModel.value!!, ProcessOutputTypes.STDOUT, this)
            }
        }

        Disposer.register(disposable) {
            Disposer.dispose(this)
        }
    }

    override fun coloredTextAvailable(text: String, attributes: Key<*>) {
        this.print(text, ConsoleViewContentType.getConsoleViewType(attributes))
    }

    companion object {
        private val LOG = logger<GitHubWorkflowRunLogConsole>()
    }
}
