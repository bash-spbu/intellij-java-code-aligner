package com.github.bashspbu.intellijjavacodealigner.services

import com.intellij.openapi.project.Project
import com.github.bashspbu.intellijjavacodealigner.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
