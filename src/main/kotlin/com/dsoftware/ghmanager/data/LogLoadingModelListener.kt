package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.Constants.LOG_MSG_JOB_IN_PROGRESS
import com.dsoftware.ghmanager.Constants.LOG_MSG_MISSING
import com.dsoftware.ghmanager.Constants.LOG_MSG_PICK_JOB
import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.JobStep
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.plugins.github.pullrequest.ui.GHCompletableFutureLoadingModel
import org.jetbrains.plugins.github.pullrequest.ui.GHLoadingModel


class LogLoadingModelListener(
    disposable: Disposable,
    dataProviderModel: SingleValueModel<WorkflowRunLogsDataProvider?>,
    private val jobsSelectionHolder: JobListSelectionHolder,
) : GHLoadingModel.StateChangeListener {
    val logModel = SingleValueModel<String?>(null)
    val logsLoadingModel = GHCompletableFutureLoadingModel<Map<String, Map<Int, String>>>(disposable)

    init {
        jobsSelectionHolder.addSelectionChangeListener(disposable, this::setLogValue)
        logsLoadingModel.addStateChangeListener(this)
        var listenerDisposable: Disposable? = null
        dataProviderModel.addAndInvokeListener {
            val provider = dataProviderModel.value
            logsLoadingModel.future = null
            logsLoadingModel.future = provider?.request
            listenerDisposable = listenerDisposable?.let {
                Disposer.dispose(it)
                null
            }
            if (provider != null) {
                val disposable2 = Disposer.newDisposable("Log listener disposable")
                    .apply {
                        Disposer.register(disposable, this)
                    }
                provider.addRunChangesListener(disposable2,
                    object : DataProvider.DataProviderChangeListener {
                        override fun changed() {
                            logsLoadingModel.future = provider.request
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
        return stepLogs.entries.joinToString("\n") { (index, logs) ->
            val stepInfo = stepsResult[index]
            val color = if (stepInfo?.conclusion == "failure") "\u001b[31m" else "\u001B[1;97m"
            "$color---- Step: ${index}_${stepInfo?.name} ----\u001b[0m\n${logs}"
        }
    }

    private fun setLogValue() {
        val removeChars = setOf('<', '>', '/', ':')
        val jobSelection = jobsSelectionHolder.selection
        val jobName = jobSelection?.name?.filterNot {
            removeChars.contains(it)
        }?.trim()
        val logs =
            if (jobName == null || !logsLoadingModel.resultAvailable)
                null
            else
                logsLoadingModel.result?.get(jobName)
        logModel.value = when {
            logsLoadingModel.result == null -> null
            jobName == null -> LOG_MSG_PICK_JOB
            logs == null && jobSelection.status == "in_progress" -> LOG_MSG_JOB_IN_PROGRESS
            logs == null -> LOG_MSG_MISSING + jobSelection.name
            else -> stepsAsLog(logs, jobSelection)
        }
    }

    override fun onLoadingCompleted() = setLogValue()
    override fun onLoadingStarted() = setLogValue()
    override fun onReset() = setLogValue()

}
