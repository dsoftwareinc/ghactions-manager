package com.dsoftware.ghtoolbar.ui

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.SpinningProgressIcon
import javax.swing.Icon

object Icons {
    private fun load(path: String): Icon {
        return IconLoader.getIcon(path, Icons::class.java)
    }

    @JvmField
    val InProgress = load("/icons/in-progress.svg")

    @JvmField
    val PrimitiveDot = load("/icons/primitive-dot.svg")

    @JvmField
    val Watch = load("/icons/watch.svg")

    @JvmField
    val Workflow = load("/icons/workflow.svg")

    @JvmField
    val WorkflowAll = load("/icons/workflow-all.svg")

    @JvmField
    val WorkflowAllToolbar = load("/icons/workflow-all-toolbar.svg")

    @JvmField
    val X = load("/icons/x.svg")
}