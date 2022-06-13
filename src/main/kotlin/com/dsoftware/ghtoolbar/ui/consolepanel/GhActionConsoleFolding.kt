package com.dsoftware.ghtoolbar.ui.consolepanel

import com.intellij.execution.ConsoleFolding
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project

class GhActionConsoleFolding : ConsoleFolding() {
    override fun shouldBeAttachedToThePreviousLine(): Boolean = true

    override fun getPlaceholderText(project: Project, lines: MutableList<String>): String? {
        return "...${lines.size} lines..."
    }

    override fun shouldFoldLine(project: Project, line: String): Boolean {
        return !(line.startsWith("==== Job")
            || line.startsWith("---- Step"))
    }

    override fun isEnabledForConsole(consoleView: ConsoleView): Boolean {
        return true
    }

}