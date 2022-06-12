package com.dsoftware.ghtoolbar.services

import com.dsoftware.ghtoolbar.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
