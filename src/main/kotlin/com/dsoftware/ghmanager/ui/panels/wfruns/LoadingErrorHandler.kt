package com.dsoftware.ghmanager.ui.panels.wfruns

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.github.authentication.AuthorizationType
import org.jetbrains.plugins.github.authentication.GHAccountsUtil
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.exceptions.GithubAuthenticationException
import org.jetbrains.plugins.github.i18n.GithubBundle
import org.jetbrains.plugins.github.pullrequest.ui.GHRetryLoadingErrorHandler
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action


open class LoadingErrorHandler(
    private val project: Project,
    private val account: GithubAccount,
    resetRunnable: () -> Unit
) : GHRetryLoadingErrorHandler(resetRunnable) {

    override fun getActionForError(error: Throwable): Action {
        return when (error) {
            is GithubAuthenticationException -> ReLoginAction()
            else -> super.getActionForError(error)
        }
    }

    private inner class ReLoginAction : AbstractAction(GithubBundle.message("accounts.relogin")) {
        override fun actionPerformed(e: ActionEvent?) {
            if (GHAccountsUtil.requestReLogin(account, project, authType = AuthorizationType.UNDEFINED) != null) {
                resetRunnable()
            }
        }
    }
}