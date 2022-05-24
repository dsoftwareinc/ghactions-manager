package com.dsoftware.githubactionstab.services

import com.intellij.openapi.project.Project
import com.dsoftware.githubactionstab.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
