// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.dsoftware.ghtoolbar.ui.settings

import com.dsoftware.ghtoolbar.ui.ToolbarUtil
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel

internal class GhActionsToolbarConfigurable internal constructor(
    private val project: Project
) :
    BoundConfigurable(ToolbarUtil.SETTINGS_DISPLAY_NAME, "settings.ghactions-toolbar") {
    override fun createPanel(): DialogPanel {

        return panel {
            row {
                checkBox("Use projects repositories")
//                    .bindSelected(state::useProjectRepos, state::useProjectRepos::set)
            }.resizableRow()

            row {

            }

        }
    }
}
