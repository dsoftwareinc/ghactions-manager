package com.dsoftware.githubactionstab.services

import com.dsoftware.githubactionstab.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
