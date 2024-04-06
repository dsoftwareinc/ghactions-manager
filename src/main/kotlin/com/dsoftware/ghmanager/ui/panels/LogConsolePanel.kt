package com.dsoftware.ghmanager.ui.panels

import com.dsoftware.ghmanager.data.LogLoadingModelListener
import com.dsoftware.ghmanager.data.LogValue
import com.dsoftware.ghmanager.data.LogValueStatus
import com.dsoftware.ghmanager.i18n.MessagesBundle.message
import com.intellij.execution.ConsoleFolding
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.ContextMenuPopupHandler
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.plaf.PanelUI

class GhActionConsoleFolding : ConsoleFolding() {
    override fun shouldBeAttachedToThePreviousLine(): Boolean = true

    override fun getPlaceholderText(project: Project, lines: MutableList<String>): String {
        return message("panel.log.lines-folded", lines.size)
    }

    override fun shouldFoldLine(project: Project, line: String): Boolean {
        return !(line.startsWith("====")
            || line.startsWith("---- Step"))
    }

    override fun isEnabledForConsole(consoleView: ConsoleView): Boolean {
        return consoleView is LogConsolePanel
    }

}

class LogConsolePanel(
    project: Project, logValue: String, parentDisposable: Disposable,
) : ConsoleViewImpl(project, true), AnsiEscapeDecoder.ColoredTextAcceptor {
    private val ansiEscapeDecoder = AnsiEscapeDecoder()

    // when it's true its save to call editor, otherwise call 'editor' will throw an NPE
    private val objectInitialized = true

    init {
        Disposer.register(parentDisposable, this)
        this.clear()
        ansiEscapeDecoder.escapeText(logValue, ProcessOutputType.STDOUT, this)
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
}


fun createLogConsolePanel(
    project: Project, model: LogLoadingModelListener, parentDisposable: Disposable,
): JBPanelWithEmptyText {
    val panel = JBPanelWithEmptyText(BorderLayout()).apply {
        isOpaque = false
    }
    val actionManager = ActionManager.getInstance()

    @RequiresEdt
    fun addConsole(logValue: LogValue?) {
        if (logValue == null) return
        panel.removeAll()
        if (logValue.status == LogValueStatus.LOG_EXIST) {
            val console = LogConsolePanel(project, logValue.log!!, parentDisposable)
            panel.add(console.component, BorderLayout.CENTER)
            (console.editor as EditorEx).installPopupHandler(
                ContextMenuPopupHandler.Simple(
                    DefaultActionGroup().apply {
                        removeAll()
                        add(actionManager.getAction("Github.Workflow.Log.List.Reload"))
                        add(object : ToggleUseSoftWrapsToolbarAction(SoftWrapAppliancePlaces.CONSOLE) {
                            override fun getEditor(e: AnActionEvent): Editor? = console.editor
                        })
                    })
            )
        } else {
            panel.emptyText.text = when (logValue.status) {
                LogValueStatus.LOG_MISSING -> message("panel.log.logs-missing", logValue.jobName ?: "")
                LogValueStatus.NO_JOB_SELECTED -> message("panel.log.no-job-selected")
                LogValueStatus.JOB_IN_PROGRESS -> message("panel.log.job-in-progress")
                else -> ""
            }
            if (logValue.status == LogValueStatus.JOB_IN_PROGRESS) {
                panel.emptyText.appendSecondaryText(
                    message("panel.log.job-in-progress-upvote-issue"),
                    SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES
                ) {
                    BrowserUtil.browse("https://github.com/orgs/community/discussions/75518")
                }
            }
        }
    }

    model.logValueModel.addAndInvokeListener {
        runInEdt { addConsole(it) }
    }
    ApplicationManager.getApplication().messageBus.connect(parentDisposable)
        .subscribe(EditorColorsManager.TOPIC, EditorColorsListener {
            runInEdt { addConsole(model.logValueModel.value) }
        })
    return panel
}