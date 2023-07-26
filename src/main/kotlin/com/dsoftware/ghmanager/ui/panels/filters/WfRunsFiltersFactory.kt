package com.dsoftware.ghmanager.ui.panels.filters

import com.dsoftware.ghmanager.ui.ToolbarUtil
import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil
import com.intellij.collaboration.ui.codereview.list.search.DropDownComponentFactory
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory
import com.intellij.collaboration.ui.icon.AsyncImageIconsProvider
import com.intellij.collaboration.ui.icon.CachingIconsProvider
import com.intellij.util.IconUtil
import com.intellij.util.ui.ImageUtil
import icons.CollaborationToolsIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.util.CachingGHUserAvatarLoader
import java.awt.Image
import javax.swing.Icon
import javax.swing.JComponent

internal class WfRunsFiltersFactory(vm: WfRunsSearchPanelViewModel) :
    ReviewListSearchPanelFactory<WfRunsListSearchValue, WorkflowRunListQuickFilter, WfRunsSearchPanelViewModel>(vm) {

    fun createWfRunsFiltersPanel(viewScope: CoroutineScope): JComponent {
        val searchPanel = create(viewScope)
        searchPanel.remove(0)
        return searchPanel
    }

    private class AvatarLoader(private val requestExecutor: GithubApiRequestExecutor) :
        AsyncImageIconsProvider.AsyncImageLoader<String> {

        private val avatarsLoader = CachingGHUserAvatarLoader.getInstance()

        override suspend fun load(key: String): Image? =
            avatarsLoader.requestAvatar(requestExecutor, key).await()

        override fun createBaseIcon(key: String?, iconSize: Int): Icon =
            IconUtil.resizeSquared(CollaborationToolsIcons.Review.DefaultAvatar, iconSize)

        override suspend fun postProcess(image: Image): Image =
            ImageUtil.createCircleImage(ImageUtil.toBufferedImage(image))
    }

    override fun createFilters(viewScope: CoroutineScope): List<JComponent> {
        val avatarIconsProvider = CachingIconsProvider(
            AsyncImageIconsProvider(viewScope, AvatarLoader(vm.context.requestExecutor))
        )
        return listOf(
            DropDownComponentFactory(vm.userFilterState)
                .create(viewScope,
                    filterName = "Actor",
                    items = vm.collaborators,
                    onSelect = {},
                    valuePresenter = { it.shortName },
                    popupItemPresenter = {
                        ChooserPopupUtil.PopupItemPresentation.Simple(
                            it.shortName, avatarIconsProvider.getIcon(it.avatarUrl, AVATAR_SIZE), it.name
                        )
                    }),
            DropDownComponentFactory(vm.statusState)
                .create(viewScope,
                    filterName = "Status",
                    items = WfRunsListSearchValue.Status.values().asList(),
                    onSelect = {},
                    valuePresenter = Companion::getStatusText,
                    popupItemPresenter = {
                        ChooserPopupUtil.PopupItemPresentation.Simple(
                            getStatusText(it), ToolbarUtil.statusIcon(it.name.lowercase(), null)
                        )
                    }),
            DropDownComponentFactory(vm.branchFilterState)
                .create(viewScope,
                    filterName = "Branch",
                    items = vm.branches,
                    onSelect = {},
                    popupItemPresenter = {
                        ChooserPopupUtil.PopupItemPresentation.Simple(it)
                    }),
            DropDownComponentFactory(vm.eventFilterState)
                .create(viewScope,
                    filterName = "Event",
                    items = WfRunsListSearchValue.Event.values().asList(),
                    onSelect = {},
                    valuePresenter = Companion::getEventText,
                    popupItemPresenter = {
                        ChooserPopupUtil.PopupItemPresentation.Simple(getEventText(it))
                    }),
        )
    }

    override fun WorkflowRunListQuickFilter.getQuickFilterTitle(): String = this.title

    override fun getShortText(searchValue: WfRunsListSearchValue): @Nls String {
        return searchValue.getShortText()
    }

    companion object {
        const val AVATAR_SIZE = 20
        private fun getStatusText(status: WfRunsListSearchValue.Status): @Nls String = when (status) {
            WfRunsListSearchValue.Status.COMPLETED -> "Completed"
            WfRunsListSearchValue.Status.FAILURE -> "Failed"
            WfRunsListSearchValue.Status.SKIPPED -> "Skipped"
            WfRunsListSearchValue.Status.STALE -> "Stale"
            WfRunsListSearchValue.Status.SUCCESS -> "Succeeded"
            WfRunsListSearchValue.Status.TIMED_OUT -> "Timed out"
            WfRunsListSearchValue.Status.IN_PROGRESS -> "In progress"
            WfRunsListSearchValue.Status.QUEUED -> "Queued"
            WfRunsListSearchValue.Status.CANCELLED -> "Cancelled"
        }

        private fun getEventText(status: WfRunsListSearchValue.Event): @Nls String = when (status) {
            WfRunsListSearchValue.Event.PULL_REQUEST -> "pull_request"
            WfRunsListSearchValue.Event.PULL_REQUEST_TARGET -> "pull_request_target"
            WfRunsListSearchValue.Event.PUSH -> "push"
            WfRunsListSearchValue.Event.RELEASE -> "release"
        }
    }
}