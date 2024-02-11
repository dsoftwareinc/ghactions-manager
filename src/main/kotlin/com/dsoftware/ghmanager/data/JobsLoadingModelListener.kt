package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.api.model.WorkflowRunJobs
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.plugins.github.pullrequest.ui.GHCompletableFutureLoadingModel
import org.jetbrains.plugins.github.pullrequest.ui.GHLoadingModel


class JobsLoadingModelListener(
    workflowRunDisposable: Disposable,
    dataProviderModel: SingleValueModel<WorkflowRunJobsDataProvider?>,
    runSelectionHolder: WorkflowRunListSelectionHolder,
) : GHLoadingModel.StateChangeListener {
    val jobsModel = SingleValueModel<WorkflowRunJobs?>(null)
    val jobsLoadingModel = GHCompletableFutureLoadingModel<WorkflowRunJobs>(workflowRunDisposable)

    init {
        runSelectionHolder.addSelectionChangeListener(workflowRunDisposable, this::setValue)
        jobsLoadingModel.addStateChangeListener(this)
        var listenerDisposable: Disposable? = null
        dataProviderModel.addAndInvokeListener { provider ->
            jobsLoadingModel.future = null
            listenerDisposable?.let { Disposer.dispose(it) }
            listenerDisposable = null

            provider?.let {
                jobsLoadingModel.future = it.request
                val disposable = Disposer.newDisposable().apply {
                    Disposer.register(jobsLoadingModel, this)
                }
                it.addRunChangesListener(disposable,
                    object : DataProvider.DataProviderChangeListener {
                        override fun changed() {
                            jobsLoadingModel.future = it.request
                        }
                    })
                listenerDisposable = disposable
            }
        }

    }

    private fun setValue() {
        jobsModel.value = jobsLoadingModel.result
    }

    override fun onLoadingCompleted() = setValue()
    override fun onLoadingStarted() = setValue()
    override fun onReset() = setValue()

}
