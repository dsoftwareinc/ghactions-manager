// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.dsoftware.ghmanager.data

import com.intellij.collaboration.async.CompletableFutureUtil
import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.plugins.github.pullrequest.data.GHListLoader
import org.jetbrains.plugins.github.util.NonReusableEmptyProgressIndicator
import java.util.concurrent.CompletableFuture
import kotlin.properties.Delegates

abstract class GHListLoaderBase<T : Comparable<T>>(
    private val progressManager: ProgressManager
) : Disposable {


    private var lastFuture = CompletableFuture.completedFuture(emptyList<T>())
    private var progressIndicator = NonReusableEmptyProgressIndicator()

    private val loadingStateChangeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

    @get:RequiresEdt
    var loading: Boolean by Delegates.observable(false) { _, _, _ ->
        loadingStateChangeEventDispatcher.multicaster.eventOccurred()
    }

    private val errorChangeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

    @get:RequiresEdt
    var error: Throwable? by Delegates.observable(null) { _, _, _ ->
        errorChangeEventDispatcher.multicaster.eventOccurred()
    }

    protected val dataEventDispatcher = EventDispatcher.create(GHListLoader.ListDataListener::class.java)

    @get:RequiresEdt
    val loadedData = ArrayList<T>()

    @RequiresEdt
    open fun canLoadMore() = !loading && (error != null)

    @RequiresEdt
    fun loadMore(update: Boolean = false) {
        val indicator = progressIndicator
        if (canLoadMore() || update) {
            loading = true
            requestLoadMore(indicator, update).handleOnEdt { list, error ->
                if (indicator.isCanceled) return@handleOnEdt
                loading = false
                if (error != null) {
                    if (!CompletableFutureUtil.isCancellation(error)) this.error = error
                } else if (!list.isNullOrEmpty()) {
                    val startIdx = loadedData.size
                    loadedData.addAll(list)
                    dataEventDispatcher.multicaster.onDataAdded(startIdx)
                }
            }
        }
    }

    private fun requestLoadMore(indicator: ProgressIndicator, update: Boolean): CompletableFuture<List<T>> {
        lastFuture = lastFuture.thenCompose {
            progressManager.submitIOTask(indicator) {
                doLoadMore(indicator, update)
            }
        }
        return lastFuture
    }

    protected abstract fun doLoadMore(indicator: ProgressIndicator, update: Boolean): List<T>?


    @RequiresEdt
    open fun reset() {
        lastFuture = lastFuture.handle { _, _ ->
            listOf()
        }
        progressIndicator.cancel()
        progressIndicator = NonReusableEmptyProgressIndicator()
        error = null
        loading = false
        loadedData.clear()
        dataEventDispatcher.multicaster.onAllDataRemoved()
    }

    @RequiresEdt
    fun addLoadingStateChangeListener(disposable: Disposable, listener: () -> Unit) =
        SimpleEventListener.addDisposableListener(loadingStateChangeEventDispatcher, disposable, listener)

    @RequiresEdt
    fun addDataListener(disposable: Disposable, listener: GHListLoader.ListDataListener) =
        dataEventDispatcher.addListener(listener, disposable)

    @RequiresEdt
    fun addErrorChangeListener(disposable: Disposable, listener: () -> Unit) =
        SimpleEventListener.addDisposableListener(errorChangeEventDispatcher, disposable, listener)

    override fun dispose() = progressIndicator.cancel()
}