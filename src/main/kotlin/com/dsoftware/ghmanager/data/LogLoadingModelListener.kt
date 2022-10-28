package com.dsoftware.ghmanager.data

import WorkflowRunJob
import WorkflowRunJobSteps
import com.dsoftware.ghmanager.ui.WorkflowToolWindowTabController
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
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
        jobsSelectionHolder.addSelectionChangeListener(disposable) {
            setLogValue()
        }
        logsLoadingModel.addStateChangeListener(this)
        var listenerDisposable: Disposable? = null

        dataProviderModel.addListener {
            val provider = dataProviderModel.value
            logsLoadingModel.future = provider?.request
            listenerDisposable = listenerDisposable?.let {
                Disposer.dispose(it)
                null
            }
            if (provider != null) {
                val disposable = Disposer.newDisposable().apply {
                    Disposer.register(disposable, this)
                }
                provider.addRunChangesListener(disposable,
                    object : DataProvider.DataProviderChangeListener {
                        override fun changed() {
                            LOG.debug("Log changed ${provider.request}")
                            logsLoadingModel.future = provider.request
                            logModel.value = null
                        }
                    })
                listenerDisposable = disposable
            }
        }
    }

    private fun stepsAsLog(stepLogs: Map<Int, String>, selection: WorkflowRunJob): String {
        val stepsResult: Map<Int, WorkflowRunJobSteps> = if (selection.steps == null) {
            emptyMap()
        } else {
            selection.steps.associate { it.number to it }
        }
        return stepLogs.entries
            .map { (index, logs) ->
                val stepInfo = stepsResult[index]
                val color = if (stepInfo?.conclusion == "failure") "\u001b[31m" else "\u001B[1;97m"
                "$color---- Step: ${index}_${stepInfo?.name} ----\u001b[0m\n${logs}"
            }
            .joinToString("\n")
    }

    private fun setLogValue() {
        val removeChars = setOf('<', '>', '/', ':')
        val jobSelection = jobsSelectionHolder.selection
        val jobName = jobSelection?.name?.filterNot {
            removeChars.contains(it)
        }?.trim()
        val logs = if (jobName == null) null else logsLoadingModel.result?.get(jobName)
        logModel.value = when {
            logsLoadingModel.result == null -> null
            jobName == null -> "Pick a job to view logs"
            logs == null -> "Job ${jobSelection?.name} logs missing"
            else -> stepsAsLog(logs, jobSelection)
        }
    }

    override fun onLoadingCompleted() = setLogValue()
    override fun onLoadingStarted() = setLogValue()
    override fun onReset() = setLogValue()
    companion object{
        private val LOG = logger<LogLoadingModelListener>()
    }
}
