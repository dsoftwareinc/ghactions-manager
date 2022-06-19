package com.dsoftware.ghtoolbar.workflow

import com.dsoftware.ghtoolbar.api.model.GitHubWorkflowRun
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.openapi.Disposable
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlin.properties.Delegates

class WorkflowRunListSelectionHolder {

    @get:RequiresEdt
    @set:RequiresEdt
    var selection: GitHubWorkflowRun? by Delegates.observable(null) { _, _, _ ->
        selectionChangeEventDispatcher.multicaster.eventOccurred()
    }

    private val selectionChangeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

    @RequiresEdt
    fun addSelectionChangeListener(disposable: Disposable, listener: () -> Unit) =
        SimpleEventListener.addDisposableListener(selectionChangeEventDispatcher, disposable, listener)
}

