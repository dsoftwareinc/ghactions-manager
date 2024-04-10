package com.dsoftware.ghmanager.data.providers

import com.dsoftware.ghmanager.api.GhApiRequestExecutor
import com.dsoftware.ghmanager.api.GithubApi
import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.WorkflowRunJobs
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.util.ProgressIndicatorsProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.EventDispatcher
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.exceptions.GithubStatusCodeException
import java.io.IOException
import java.util.EventListener

open class DataProvider<T>(
    private val requestExecutor: GhApiRequestExecutor,
    private val githubApiRequest: GithubApiRequest<T>,
) {
    private val runChangesEventDispatcher = EventDispatcher.create(DataProviderChangeListener::class.java)
    private val progressManager = ProgressManager.getInstance()
    private val indicatorsProvider: ProgressIndicatorsProvider = ProgressIndicatorsProvider()

    val processValue = progressManager.submitIOTask(indicatorsProvider, true) {
        try {
            LOG.info("Executing ${githubApiRequest.url}")
            val request = githubApiRequest
            val response = requestExecutor.execute(it, request)
            response
        } catch (e: GithubStatusCodeException) {
            LOG.warn("Error when getting $githubApiRequest.url: status code ${e.statusCode}: ${e.message}")
            throw e
        } catch (ioe: IOException) {
            LOG.warn("Error when getting $githubApiRequest.url: $ioe")
            throw ioe
        }
    }

    fun url(): String = githubApiRequest.url

    fun reload() {
        processValue.cancel(true)
        runChangesEventDispatcher.multicaster.changed()
    }

    fun addRunChangesListener(disposable: Disposable, listener: DataProviderChangeListener) =
        runChangesEventDispatcher.addListener(listener, disposable)

    interface DataProviderChangeListener : EventListener {
        fun changed() {}
    }

    companion object {
        private val LOG = thisLogger()
    }
}


class JobsDataProvider(requestExecutor: GhApiRequestExecutor, jobsUrl: String) : DataProvider<WorkflowRunJobs>(
    requestExecutor, GithubApi.getJobsForWorkFlowRun(jobsUrl)
)


class LogDataProvider(requestExecutor: GhApiRequestExecutor, job: Job) : DataProvider<String>(
    requestExecutor, GithubApi.getLogForSingleJob(job)
)