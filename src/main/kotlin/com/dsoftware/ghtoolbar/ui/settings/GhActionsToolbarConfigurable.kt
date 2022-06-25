// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.dsoftware.ghtoolbar.ui.settings

import com.dsoftware.ghtoolbar.ui.ToolbarUtil
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import git4idea.GitUtil


internal class GhActionsToolbarConfigurable internal constructor(
    project: Project
) : BoundConfigurable(ToolbarUtil.SETTINGS_DISPLAY_NAME, "settings.ghactions-toolbar") {
    private val toolbarSettings = ToolbarSettings.getInstance(project)

    private val state = toolbarSettings.state
    private val repoManager = GitUtil.getRepositoryManager(project)


    override fun createPanel(): DialogPanel {
        val knownRepositories = repoManager.repositories
        return panel {
            lateinit var projectRepos: Cell<JBCheckBox>
            row {
                projectRepos = checkBox("Use custom repositories")
                    .comment("Do not use all repositories in the project")
                    .bindSelected(state::useCustomRepos, state::useCustomRepos::set)
            }

            group {
                twoColumnsRow({ label("Repository") }, { label("Selected") })
                knownRepositories
                    .map { it.presentableUrl }
                    .forEach { repo ->
                        val settingsValue = state.customRepos.getOrDefault(repo, ToolbarState.RepoSettings())
                        twoColumnsRow({ label(repo) }, {
                            checkBox("")
                                .bindSelected(settingsValue::getIncluded, settingsValue::setIncluded)
                        })
                    }

            }.enabledIf(projectRepos.selected)
        }

    }
}


