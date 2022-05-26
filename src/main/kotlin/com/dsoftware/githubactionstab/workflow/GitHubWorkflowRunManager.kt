package com.dsoftware.githubactionstab.workflow

import com.intellij.dvcs.repo.VcsRepositoryMappingListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import org.jetbrains.plugins.github.authentication.accounts.AccountRemovedListener
import org.jetbrains.plugins.github.authentication.accounts.AccountTokenChangedListener
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.pullrequest.config.GithubPullRequestsProjectUISettings
import org.jetbrains.plugins.github.util.CollectionDelta
import org.jetbrains.plugins.github.util.GitRemoteUrlCoordinates
import kotlin.properties.Delegates.observable

@Service
internal class GitHubWorkflowRunManager(private val project: Project) {
    private val settings = GithubPullRequestsProjectUISettings.getInstance(project)

//    private val contentManager by lazy(LazyThreadSafetyMode.NONE) {
//        GitHubWorkflowToolTabsContentManager(project, ChangesViewContentManager.getInstance(project))
//    }

    private var remoteUrls by observable(setOf<GitRemoteUrlCoordinates>()) { _, oldValue, newValue ->
        LOG.debug("Remote URLs changed")
        val delta = CollectionDelta(oldValue, newValue)
//        for (item in delta.removedItems) {
//            contentManager.removeTab(item)
//        }
//        for (item in delta.newItems) {
//            contentManager.addTab(item, Disposable {
//                ApplicationManager.getApplication().invokeLater(::updateRemoteUrls) { project.isDisposed }
//            })
//        }
    }

    @RequiresEdt
    fun showTab(remoteUrl: GitRemoteUrlCoordinates) {
        LOG.debug("Show Tab")
        updateRemoteUrls()

//        contentManager.focusTab(remoteUrl)
    }

    private fun updateRemoteUrls() {
        LOG.debug("Update remote urls")
        remoteUrls = emptySet()
//        remoteUrls = project.service<GHProjectRepositoriesManager>().knownRepositories
//            .filter { !settings.getHiddenUrls().contains(it.gitRemote.url) }
//            .map { it.gitRemote }
//            .toSet()
    }

    class RemoteUrlsListener(private val project: Project) : VcsRepositoryMappingListener, GitRepositoryChangeListener {

        override fun mappingChanged() = runInEdt(project) {
            LOG.debug("mappingChanged")
            updateRemotes(project)
        }

        override fun repositoryChanged(repository: GitRepository) = runInEdt(project) {
            LOG.debug("repositoryChanged")
            updateRemotes(project)
        }
    }

    class AccountsListener : AccountRemovedListener, AccountTokenChangedListener {
        override fun accountRemoved(removedAccount: GithubAccount) = updateRemotes()
        override fun tokenChanged(account: GithubAccount) = updateRemotes()

        private fun updateRemotes() = runInEdt {
            LOG.debug("updateRemotes")
            for (project in ProjectManager.getInstance().openProjects) {
                updateRemotes(project)
            }
        }
    }

    companion object {
        private val LOG = logger<GitHubWorkflowRunManager>()

        private inline fun runInEdt(project: Project, crossinline runnable: () -> Unit) {
            val application = ApplicationManager.getApplication()
            if (application.isDispatchThread) runnable()
            else application.invokeLater({ runnable() }) { project.isDisposed }
        }

        private fun updateRemotes(project: Project) = project.service<GitHubWorkflowRunManager>().updateRemoteUrls()
    }
}
