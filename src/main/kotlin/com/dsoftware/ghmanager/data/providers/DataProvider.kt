package com.dsoftware.ghmanager.data.providers

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.EventDispatcher
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.exceptions.GithubStatusCodeException
import org.jetbrains.plugins.github.util.LazyCancellableBackgroundProcessValue
import java.io.IOException
import java.util.EventListener
import java.util.concurrent.CompletableFuture
import kotlin.properties.ReadOnlyProperty

open class DataProvider<T>(
    progressManager: ProgressManager,
    private val requestExecutor: GithubApiRequestExecutor,
    private val githubApiRequest: GithubApiRequest<T>,
    private val errorValue: T?,
) {
    private val runChangesEventDispatcher = EventDispatcher.create(DataProviderChangeListener::class.java)

    private val processValue: LazyCancellableBackgroundProcessValue<T> =
        LazyCancellableBackgroundProcessValue.create(progressManager) {
            try {
                LOG.info("Executing ${githubApiRequest.url}")
                val request = githubApiRequest
                val response = requestExecutor.execute(it, request)
                response
            } catch (e: GithubStatusCodeException) {
                LOG.warn("Error when getting $githubApiRequest.url: $e")
                errorValue ?: throw e
            } catch (ioe: IOException) {
                LOG.warn("Error when getting $githubApiRequest.url: $ioe")
                errorValue ?: throw ioe
            }
        }

    val request by backgroundProcessValue(processValue)

    private fun <T> backgroundProcessValue(backingValue: LazyCancellableBackgroundProcessValue<T>)
        : ReadOnlyProperty<Any?, CompletableFuture<T>> =
        ReadOnlyProperty { _, _ -> backingValue.value }

    fun url(): String = githubApiRequest.url

    fun reload() {
        processValue.drop()
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