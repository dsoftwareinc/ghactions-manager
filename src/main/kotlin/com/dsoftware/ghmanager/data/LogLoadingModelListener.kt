package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.Constants.LOG_MSG_JOB_IN_PROGRESS
import com.dsoftware.ghmanager.Constants.LOG_MSG_MISSING
import com.dsoftware.ghmanager.Constants.LOG_MSG_PICK_JOB
import com.dsoftware.ghmanager.api.JobLog
import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.JobStep
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import org.jetbrains.plugins.github.pullrequest.ui.GHCompletableFutureLoadingModel
import org.jetbrains.plugins.github.pullrequest.ui.GHLoadingModel


class LogLoadingModelListener(
    workflowRunDisposable: Disposable,
    dataProviderModel: SingleValueModel<JobLogDataProvider?>,
    private val jobsSelectionHolder: JobListSelectionHolder,
) : GHLoadingModel.StateChangeListener {
    val logModel = SingleValueModel<String?>(null)
    val logsLoadingModel = GHCompletableFutureLoadingModel<String>(workflowRunDisposable)

    init {
        jobsSelectionHolder.addSelectionChangeListener(workflowRunDisposable, this::setLogValue)
        logsLoadingModel.addStateChangeListener(this)
        var listenerDisposable: Disposable? = null
        dataProviderModel.addAndInvokeListener { provider ->
            logsLoadingModel.future = null
            logsLoadingModel.future = provider?.request
            listenerDisposable = listenerDisposable?.let {
                Disposer.dispose(it)
                null
            }
            provider?.let {
                val disposable2 = Disposer.newDisposable("Log listener disposable")
                    .apply {
                        Disposer.register(workflowRunDisposable, this)
                    }
                it.addRunChangesListener(disposable2,
                    object : DataProvider.DataProviderChangeListener {
                        override fun changed() {
                            logsLoadingModel.future = it.request
                        }
                    })
                listenerDisposable = disposable2
            }
        }

    }



    private fun setLogValue() {
        val jobSelection = jobsSelectionHolder.selection
        val logs =
            if (jobSelection == null || !logsLoadingModel.resultAvailable)
                null
            else
                logsLoadingModel.result
        logModel.value = when {
            logsLoadingModel.result == null -> null
            jobSelection == null -> LOG_MSG_PICK_JOB
            jobSelection.status == "in_progress" -> LOG_MSG_JOB_IN_PROGRESS
            logs == null -> LOG_MSG_MISSING + jobSelection.name
            else -> logs
        }
    }

    override fun onLoadingCompleted() = setLogValue()
    override fun onLoadingStarted() = setLogValue()
    override fun onReset() = setLogValue()

    companion object {
        private val LOG = logger<LogLoadingModelListener>()
    }
}
