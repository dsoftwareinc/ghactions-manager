package com.dsoftware.ghmanager.ui

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object Icons {
    private fun load(path: String): Icon {
        return IconLoader.getIcon(path, Icons::class.java)
    }

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

    @JvmField
    val Skipped = load("/icons/skipped.svg")

    @JvmField
    val Checkmark = load("/icons/checkmark.svg")
}