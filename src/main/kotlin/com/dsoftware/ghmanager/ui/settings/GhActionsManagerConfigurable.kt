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
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.not
import com.intellij.util.messages.Topic
import org.jetbrains.plugins.github.util.GHHostedRepositoriesManager


internal class GhActionsManagerConfigurable internal constructor(project: Project) :
    BoundConfigurable(ToolbarUtil.SETTINGS_DISPLAY_NAME, "settings.ghactions-manager") {
    private val ghActionsSettingsService = GhActionsSettingsService.getInstance(project)
    private val state = ghActionsSettingsService.state
    private val repoManager = project.service<GHHostedRepositoriesManager>()

    override fun apply() {
        super.apply()
        ApplicationManager.getApplication().messageBus.syncPublisher(SETTINGS_CHANGED).settingsChanged()
    }

    override fun createPanel(): DialogPanel {
        val knownRepositories = repoManager.knownRepositoriesState.value
        return panel {
            group("API Usage") {
                row {
                    intTextField(0..100).bindIntText(state::frequency, state::frequency::set)
                        .label("How often Should the list of workflows be updated")
                        .comment("In seconds")
                }
                lateinit var checkbox: Cell<JBCheckBox>
                row {
                    checkbox = checkBox("Use GitHub accounts settings?")
                        .comment("GHActions-Manager can use either the GitHub-Settings or a custom token")
                        .bindSelected(state::useGitHubSettings, state::useGitHubSettings::set)
                }
                row {
                    label("GitHub API token")
                    passwordField().bindText(state::apiToken, state::apiToken::set)
                }.enabledIf(checkbox.selected.not())
            }

            group("Visual Settings") {
                row {
                    checkBox("Show jobs list above logs?")
                        .comment("If this is unchecked, it will show side by side")
                        .bindSelected(state::jobListAboveLogs, state::jobListAboveLogs::set)
                }

                row {
                    intTextField(0..100).bindIntText(state::pageSize, state::pageSize::set)
                        .label("How many workflow runs to present")
                }
            }
            lateinit var projectRepos: Cell<JBCheckBox>
            group("Repositories") {
                row {
                    projectRepos = checkBox("Use custom repositories")
                        .comment("Select which repositories will be shown on GHActions-Manager")
                        .bindSelected(state::useCustomRepos, state::useCustomRepos::set)
                }
                row {
                    label("Repository")
                    label("Show").comment("Show tab for repository")
                    label("Tab name")
                }.layout(RowLayout.PARENT_GRID)
                knownRepositories
                    .map { it.remote.url }
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


