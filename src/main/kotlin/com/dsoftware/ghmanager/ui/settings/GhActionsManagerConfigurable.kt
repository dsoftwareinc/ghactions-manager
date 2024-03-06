// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.dsoftware.ghmanager.ui.settings

import com.dsoftware.ghmanager.data.GhActionsService
import com.dsoftware.ghmanager.i18n.MessagesBundle.message
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


internal class GhActionsManagerConfigurable internal constructor(project: Project) :
    BoundConfigurable(message("settings.display.name"), "settings.ghactions-manager") {
    private val ghActionsSettingsService = project.service<GhActionsSettingsService>()
    private val state = ghActionsSettingsService.state
    private val ghActionsService = project.service<GhActionsService>()

    override fun apply() {
        super.apply()
        ApplicationManager.getApplication().messageBus.syncPublisher(SETTINGS_CHANGED).settingsChanged()
    }

    override fun createPanel(): DialogPanel {
        val knownRepositories = ghActionsService.knownRepositories
        return panel {
            group(message("settings.group.api-usage.title")) {
                row {
                    intTextField(0..100).bindIntText(state::frequency, state::frequency::set)
                        .label(message("settings.group.api-usage.frequency.label"))
                        .comment(message("settings.group.api-usage.frequency.comment"))
                }
                lateinit var checkbox: Cell<JBCheckBox>
                row {
                    checkbox = checkBox(message("settings.group.github-settings.label"))
                        .comment(message("settings.group.github-settings.comment"))
                        .bindSelected(state::useGitHubSettings, state::useGitHubSettings::set)
                }
                row {
                    label(message("settings.group.github-api-token.label"))
                    passwordField().bindText(state::apiToken, state::apiToken::set)
                }.enabledIf(checkbox.selected.not())
            }

            group(message("settings.group.visual-settings.title")) {
                row {
                    checkBox(message("settings.group.visual-settings.jobs-vertical"))
                        .comment(message("settings.group.visual-settings.jobs-vertical.comment"))
                        .bindSelected(state::jobListAboveLogs, state::jobListAboveLogs::set)
                }

                row {
                    intTextField(0..100).bindIntText(state::pageSize, state::pageSize::set)
                        .label(message("settings.group.visual-settings.number-of-runs"))
                }
            }
            lateinit var projectRepos: Cell<JBCheckBox>
            row {
                projectRepos = checkBox(message("settings.group.custom-repositories.checkbox"))
                    .comment(message("settings.group.custom-repositories.comment"))
                    .bindSelected(state::useCustomRepos, state::useCustomRepos::set)
            }
            group(message("settings.group.custom-repositories-group.title")) {
                row {
                    label(message("settings.group.custom-repositories-group.col-header.repo"))
                    label(message("settings.group.custom-repositories-group.col-header.show"))
                        .comment(message("settings.group.custom-repositories-group.col-header.show.comment"))
                    label(message("settings.group.custom-repositories-group.col-header.tab-name"))
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


