package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.Constants.LOG_MSG_JOB_IN_PROGRESS
import com.dsoftware.ghmanager.Constants.LOG_MSG_MISSING
import com.dsoftware.ghmanager.Constants.LOG_MSG_PICK_JOB
import com.dsoftware.ghmanager.api.JobLog
import com.dsoftware.ghmanager.api.WorkflowRunLog
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
    val logsLoadingModel = GHCompletableFutureLoadingModel<JobLog>(workflowRunDisposable)

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

    private fun stepsAsLog(stepLogs: Map<Int, String>, selection: Job): String {
        val stepsResult: Map<Int, JobStep> = if (selection.steps == null) {
            emptyMap()
        } else {
            selection.steps.associateBy { it.number }
        }
        val stepNumbers = stepsResult.keys.sorted()
        if (!stepNumbers.containsAll(stepLogs.keys)) {
            LOG.warn(
                "Some logs do not have a step-result associated " +
                    "[steps in results=$stepNumbers, step with logs=${stepLogs.keys}] "
            )
        }
        val res = StringBuilder()
        for (index in stepNumbers) {
            val stepInfo = stepsResult[index]!!
            val logs = if (stepLogs.containsKey(index)) stepLogs[index] else ""
            val indexStr = "%3d".format(index)
            res.append(
                when (stepInfo.conclusion) {
                    "skipped" -> "\u001B[0m\u001B[37m---- Step ${indexStr}: ${stepInfo.name} (skipped) ----\u001b[0m\n"
                    "failure" -> "\u001B[0m\u001B[31m---- Step ${indexStr}: ${stepInfo.name} (failed) ----\u001b[0m\n${logs}"
                    else -> "\u001B[0m\u001B[32m---- Step ${indexStr}: ${stepInfo.name} ----\u001b[0m\n${logs}"
                }
            )
        }
        return res.toString()
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
            logs == null && jobSelection.status == "in_progress" -> LOG_MSG_JOB_IN_PROGRESS
            logs == null -> LOG_MSG_MISSING + jobSelection.name
            else -> stepsAsLog(logs, jobSelection)
        }
    }

    override fun onLoadingCompleted() = setLogValue()
    override fun onLoadingStarted() = setLogValue()
    override fun onReset() = setLogValue()

    companion object {
        private val LOG = logger<LogLoadingModelListener>()
    }
}
