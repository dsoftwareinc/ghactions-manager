package com.dsoftware.ghmanager.ui.panels

import com.dsoftware.ghmanager.Constants
import com.dsoftware.ghmanager.data.LogLoadingModelListener
import com.intellij.execution.ConsoleFolding
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.ContextMenuPopupHandler
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.plaf.PanelUI

class GhActionConsoleFolding : ConsoleFolding() {
    override fun shouldBeAttachedToThePreviousLine(): Boolean = true

    override fun getPlaceholderText(project: Project, lines: MutableList<String>): String {
        return "...${lines.size} lines..."
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
    project: Project,
    logValue: String,
    disposable: Disposable,
) : ConsoleViewImpl(project, true), AnsiEscapeDecoder.ColoredTextAcceptor {
    private val ansiEscapeDecoder = AnsiEscapeDecoder()

    // when it's true its save to call editor, otherwise call 'editor' will throw an NPE
    private val objectInitialized = true

    init {
        Disposer.register(disposable, this)
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

private val actionManager = ActionManager.getInstance()
fun createLogConsolePanel(
    project: Project,
    model: LogLoadingModelListener,
    disposable: Disposable,
): JBPanelWithEmptyText {
    val panel = JBPanelWithEmptyText(BorderLayout()).apply {
        isOpaque = false
    }

    model.logModel.addAndInvokeListener {
        if (it.isNullOrBlank()) {
            return@addAndInvokeListener
        }
        val logValue: String = it
        if (Constants.emptyTextMessage(logValue)) {
            panel.emptyText.text = logValue
        } else {
            val console = LogConsolePanel(project, logValue, disposable)
            panel.add(console.component, BorderLayout.CENTER)
            val actionGroup = DefaultActionGroup().apply {
                removeAll()
                add(actionManager.getAction("Github.Workflow.Log.List.Reload"))
                add(
                    object : ToggleUseSoftWrapsToolbarAction(SoftWrapAppliancePlaces.CONSOLE) {
                        override fun getEditor(e: AnActionEvent): Editor? {
                            return console.editor
                        }
                    }
                )
            }
            val contextMenuPopupHandler = ContextMenuPopupHandler.Simple(actionGroup)
            (console.editor as EditorEx).installPopupHandler(contextMenuPopupHandler)
        }
    }

    return panel
}