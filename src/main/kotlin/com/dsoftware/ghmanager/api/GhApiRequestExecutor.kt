package com.dsoftware.ghmanager.api

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.io.HttpSecurityUtil
import org.jetbrains.plugins.github.api.GHRequestExecutorBreaker
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.util.GithubSettings
import java.io.IOException

/**
 * Executes API requests taking care of authentication, headers, proxies, timeouts, etc.
 */
class GhApiRequestExecutor(
    githubSettings: GithubSettings,
    private val token: String,
    private val useProxy: Boolean,
) : GithubApiRequestExecutor.Base(githubSettings) {

    @Throws(IOException::class, ProcessCanceledException::class)
    override fun <T> execute(indicator: ProgressIndicator, request: GithubApiRequest<T>): T {
        check(!service<GHRequestExecutorBreaker>().isRequestsShouldFail) {
            "Request failure was triggered by user action. This a pretty long description of this failure that should resemble some long error which can go out of bounds."
        }

        indicator.checkCanceled()
        return createRequestBuilder(request)
            .tuner { connection ->
                request.additionalHeaders.forEach(connection::addRequestProperty)
                if (request.url.contains(connection.url.host)) {
                    connection.addRequestProperty(HttpSecurityUtil.AUTHORIZATION_HEADER_NAME, "Bearer ${token}")
                }
            }
            .useProxy(useProxy)
            .execute(request, indicator)
    }


    companion object {
        fun create(token: String): GhApiRequestExecutor =
            GhApiRequestExecutor(GithubSettings.getInstance(), token, true)
    }
}