// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.dsoftware.ghmanager.ui.settings

import com.dsoftware.ghmanager.ui.ToolbarUtil
import com.dsoftware.ghmanager.ui.settings.GithubActionsManagerSettings.RepoSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.util.messages.Topic
import org.jetbrains.plugins.github.util.GHProjectRepositoriesManager


internal class GhActionsManagerConfigurable internal constructor(
    project: Project
) : BoundConfigurable(ToolbarUtil.SETTINGS_DISPLAY_NAME, "settings.ghactions-manager") {
    private val ghActionsSettingsService = GhActionsSettingsService.getInstance(project)

    private val state = ghActionsSettingsService.state

    private val repoManager = project.service<GHProjectRepositoriesManager>()

    override fun apply() {
        super.apply()
        ApplicationManager.getApplication().messageBus.syncPublisher(SETTINGS_CHANGED).settingsChanged()
    }

    override fun createPanel(): DialogPanel {
        val knownRepositories = repoManager.knownRepositories
        return panel {
            row {
                intTextField(0..100).bindIntText(state::frequency, state::frequency::set)
                    .label("How often Should the list of workflows be updated")
                    .comment("In secs")
            }
            row {
                checkBox("Show jobs list above logs?")
                    .comment("If this is unchecked, it will show side by side")
                    .bindSelected(state::jobListAboveLogs, state::jobListAboveLogs::set)
            }
            lateinit var projectRepos: Cell<JBCheckBox>
            row {
                projectRepos = checkBox("Use custom repositories")
                    .comment("Do not use all repositories in the project")
                    .bindSelected(state::useCustomRepos, state::useCustomRepos::set)
            }
            group("Repositories") {
                row {
                    label("Repository")
                    label("Show").comment("Show tab for repository")
                    label("Tab name")
                }.layout(RowLayout.PARENT_GRID)
                knownRepositories
                    .map { it.gitRemoteUrlCoordinates.url }
                    .forEach {
                        val settingsValue = state.customRepos.getOrPut(it) { RepoSettings() }
                        row(it) {
                            checkBox("")
                                .bindSelected(settingsValue::included, settingsValue::included::set)
                            textField()
                                .bindText(settingsValue::customName, settingsValue::customName::set)
                        }.layout(RowLayout.PARENT_GRID)
                    }
            }.enabledIf(projectRepos.selected)
        }

    }

    interface SettingsChangedListener {
        fun settingsChanged()
    }

    companion object {
        @JvmField
        @Topic.AppLevel
        val SETTINGS_CHANGED = Topic(SettingsChangedListener::class.java, Topic.BroadcastDirection.NONE)

    }
}


