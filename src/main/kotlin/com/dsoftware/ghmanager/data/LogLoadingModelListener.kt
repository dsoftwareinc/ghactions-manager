package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.data.providers.LogDataProvider
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
    parentDisposable: Disposable,
    logDataProvider: SingleValueModel<LogDataProvider?>,
    private val jobsSelectionHolder: JobListSelectionHolder,
) : GHLoadingModel.StateChangeListener {
    /*
     * Model for log value.
     * Provider = Execution of the http request to get the log.
     * Value = Log value.
     * When the provider is null, it means no workflow-run/job is selected.
     */
    val logValueModel = SingleValueModel<LogValue?>(null)
    val logsLoadingModel = GHCompletableFutureLoadingModel<String>(parentDisposable)

    init {
        jobsSelectionHolder.addSelectionChangeListener(parentDisposable, this::setLogValue)
        logsLoadingModel.addStateChangeListener(this)
        var listenerDisposable: Disposable? = null
        logDataProvider.addAndInvokeListener { provider ->
            logsLoadingModel.future = null
            logsLoadingModel.future = provider?.processValue
            listenerDisposable = listenerDisposable?.let {
                Disposer.dispose(it)
                null
            }
            provider?.let {
                val newDisposable = Disposer.newDisposable("Log listener disposable").apply {
                    Disposer.register(parentDisposable, this)
                }
                it.addRunChangesListener(newDisposable) {
                    logsLoadingModel.future = it.processValue
                }
                listenerDisposable = newDisposable
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
        logValueModel.value = when {
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
