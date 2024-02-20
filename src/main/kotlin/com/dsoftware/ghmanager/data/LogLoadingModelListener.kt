package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.data.providers.DataProvider
import com.dsoftware.ghmanager.data.providers.JobLogDataProvider
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import org.jetbrains.plugins.github.pullrequest.ui.GHCompletableFutureLoadingModel
import org.jetbrains.plugins.github.pullrequest.ui.GHLoadingModel

enum class LogValueStatus {
    LOG_EXIST, LOG_MISSING, JOB_IN_PROGRESS, NO_JOB_SELECTED,
}

data class LogValue(val log: String?, val status: LogValueStatus, val jobName: String? = null)

class LogLoadingModelListener(
    workflowRunDisposable: Disposable,
    dataProviderModel: SingleValueModel<JobLogDataProvider?>,
    private val jobsSelectionHolder: JobListSelectionHolder,
) : GHLoadingModel.StateChangeListener {
    val logModel = SingleValueModel<LogValue?>(null)
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
            jobSelection == null -> LogValue(null, LogValueStatus.NO_JOB_SELECTED)
            jobSelection.status == "in_progress" -> LogValue(null, LogValueStatus.JOB_IN_PROGRESS)
            logs == null -> LogValue(null, LogValueStatus.LOG_MISSING, jobSelection.name)
            else -> LogValue(logs, LogValueStatus.LOG_EXIST)
        }
    }

    override fun onLoadingCompleted() = setLogValue()
    override fun onLoadingStarted() = setLogValue()
    override fun onReset() = setLogValue()

    companion object {
        private val LOG = logger<LogLoadingModelListener>()
    }
}
