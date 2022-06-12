package com.dsoftware.ghtoolbar.ui

import com.dsoftware.ghtoolbar.workflow.LoadingErrorHandler
import com.dsoftware.ghtoolbar.workflow.data.WorkflowRunListLoader
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.ComponentWithEmptyText
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.Panels.simplePanel
import com.intellij.util.ui.StatusText
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.plugins.github.exceptions.GithubStatusCodeException
import org.jetbrains.plugins.github.ui.HtmlInfoPanel
import java.awt.event.ActionEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

internal abstract class ListLoaderPanel(
    protected val listLoader: WorkflowRunListLoader,
    private val contentComponent: JComponent,
    private val loadAllAfterFirstScroll: Boolean = false
) : BorderLayoutPanel(), Disposable {

    private var userScrolled = false
    val scrollPane = ScrollPaneFactory.createScrollPane(
        contentComponent,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    ).apply {
        isOpaque = false
        viewport.isOpaque = false
        border = JBUI.Borders.empty()
        verticalScrollBar.model.addChangeListener { potentiallyLoadMore() }
        verticalScrollBar.model.addChangeListener {
            if (!userScrolled && verticalScrollBar.value > 0) userScrolled = true
        }
    }

    protected val infoPanel = HtmlInfoPanel()

    protected open val loadingText
        get() = "Loading..."

    var errorHandler: LoadingErrorHandler? = null

    init {
        LOG.info("Initialize ListLoaderPanel")
        addToCenter(createCenterPanel(simplePanel(scrollPane).addToTop(infoPanel).apply {
            isOpaque = false
        }))

        listLoader.addLoadingStateChangeListener(this) {
            setLoading(listLoader.loading)
            updateEmptyText()
        }

        listLoader.addErrorChangeListener(this) {
            updateInfoPanel()
            updateEmptyText()
        }

        setLoading(listLoader.loading)
        updateInfoPanel()
        updateEmptyText()
    }

    abstract fun createCenterPanel(content: JComponent): JPanel

    abstract fun setLoading(isLoading: Boolean)

    private fun updateEmptyText() {
        val emptyText = (contentComponent as? ComponentWithEmptyText)?.emptyText ?: return
        emptyText.clear()
        if (listLoader.loading) {
            emptyText.text = loadingText
        } else {
            val error = listLoader.error
            if (error != null) {
                displayErrorStatus(emptyText, error)
            } else {
                displayEmptyStatus(emptyText)
            }
        }
    }

    private fun displayErrorStatus(emptyText: StatusText, error: Throwable) {
        LOG.info("Display error status")
        emptyText.appendText(getErrorPrefix(listLoader.loadedData.isEmpty()), SimpleTextAttributes.ERROR_ATTRIBUTES)
            .appendSecondaryText(getLoadingErrorText(error), SimpleTextAttributes.ERROR_ATTRIBUTES, null)

        errorHandler?.getActionForError()?.let {
            emptyText.appendSecondaryText(" ${it.getValue("Name")}", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES, it)
        }
    }

    protected open fun displayEmptyStatus(emptyText: StatusText) {
        LOG.info("Display empty status")
        emptyText.text = "List is empty "
        emptyText.appendSecondaryText("Refresh", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
            listLoader.reset()
        }
    }

    protected open fun updateInfoPanel() {
        val error = listLoader.error
        if (error != null && listLoader.loadedData.isNotEmpty()) {
            val errorPrefix = getErrorPrefix(listLoader.loadedData.isEmpty())
            val errorText = getLoadingErrorText(error, "<br/>")
            val action = errorHandler?.getActionForError()
            if (action != null) {
                //language=HTML
                infoPanel.setInfo(
                    """<html><body>$errorPrefix<br/>$errorText<a href=''>&nbsp;${action.getValue("Name")}</a></body></html>""",
                    HtmlInfoPanel.Severity.ERROR
                ) {
                    action.actionPerformed(
                        ActionEvent(
                            infoPanel,
                            ActionEvent.ACTION_PERFORMED,
                            it.eventType.toString()
                        )
                    )
                }

            } else {
                //language=HTML
                infoPanel.setInfo(
                    """<html><body>$errorPrefix<br/>$errorText</body></html>""",
                    HtmlInfoPanel.Severity.ERROR
                )
            }
        } else infoPanel.setInfo(null)
    }

    protected open fun getErrorPrefix(listEmpty: Boolean) = if (listEmpty) "Can't load list" else "Can't load full list"

    private fun potentiallyLoadMore() {
        LOG.info("Potentially loading more")
        if (listLoader.canLoadMore() && ((userScrolled && loadAllAfterFirstScroll) || isScrollAtThreshold())) {
            LOG.info("Load more")
            listLoader.loadMore()
        }
    }

    private fun isScrollAtThreshold(): Boolean {
        val verticalScrollBar = scrollPane.verticalScrollBar
        val visibleAmount = verticalScrollBar.visibleAmount
        val value = verticalScrollBar.value
        val maximum = verticalScrollBar.maximum
        if (maximum == 0) return false
        val scrollFraction = (visibleAmount + value) / maximum.toFloat()
        if (scrollFraction < 0.5) return false
        return true
    }

    override fun dispose() {}

    companion object {
        private val LOG = logger<ListLoaderPanel>()

        private fun getLoadingErrorText(error: Throwable, newLineSeparator: String = "\n"): String {
            if (error is GithubStatusCodeException && error.error != null) {
                val githubError = error.error!!
                val builder = StringBuilder(githubError.message)
                if (githubError.errors?.isNotEmpty()!!) {
                    builder.append(": ").append(newLineSeparator)
                    for (e in githubError.errors!!) {
                        builder.append(
                            e.message
                                ?: "${e.code} error in ${e.resource} field ${e.field}"
                        ).append(newLineSeparator)
                    }
                }
                return builder.toString()
            }

            return error.message?.let { addDotIfNeeded(it) } ?: "Unknown loading error."
        }

        private fun addDotIfNeeded(line: String) = if (line.endsWith('.')) line else "$line."
    }
}