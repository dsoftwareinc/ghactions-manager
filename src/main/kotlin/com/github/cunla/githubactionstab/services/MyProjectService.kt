package com.github.cunla.githubactionstab.services

import com.intellij.openapi.project.Project
import com.github.cunla.githubactionstab.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
