package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.api.model.WorkflowRunJobs
import com.dsoftware.ghmanager.data.providers.JobsDataProvider
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.plugins.github.pullrequest.ui.GHCompletableFutureLoadingModel
import org.jetbrains.plugins.github.pullrequest.ui.GHLoadingModel


class JobsLoadingModelListener(
    parentDisposable: Disposable,
    dataProviderModel: SingleValueModel<JobsDataProvider?>
) : GHLoadingModel.StateChangeListener {
    val jobsLoadingModel = GHCompletableFutureLoadingModel<WorkflowRunJobs>(parentDisposable)

    init {
        var listenerDisposable: Disposable? = null
        dataProviderModel.addAndInvokeListener { provider ->
            jobsLoadingModel.future = null
            listenerDisposable?.let { Disposer.dispose(it) }
            listenerDisposable = null

            provider?.let {
                jobsLoadingModel.future = it.processValue
                val disposable = Disposer.newDisposable().apply {
                    Disposer.register(jobsLoadingModel, this)
                }

                it.addRunChangesListener(disposable){
                    jobsLoadingModel.future = it.processValue
                }

                listenerDisposable = disposable
            }
        }

    }

}
