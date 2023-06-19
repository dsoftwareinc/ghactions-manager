package com.dsoftware.ghmanager.ui.panels.filters

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
            DropDownComponentFactory(vm.reviewStatusState)
                .create(viewScope,
                    filterName = "Status",
                    items = listOf(
                        "completed",
                        "cancelled",
                        "failure",
                        "skipped",
                        "stale",
                        "success",
                        "timed_out",
                        "in_progress",
                        "queued",
                    ),
                    onSelect = {},

                    popupItemPresenter = { ChooserPopupUtil.PopupItemPresentation.Simple(it) })
        )
    }

    override fun WorkflowRunListQuickFilter.getQuickFilterTitle(): String = when (this) {
        is WorkflowRunListQuickFilter.StartedByYou -> "Started by you"
    }

    override fun getShortText(searchValue: WfRunsListSearchValue): @Nls String {
        return searchValue.getShortText()
    }
}