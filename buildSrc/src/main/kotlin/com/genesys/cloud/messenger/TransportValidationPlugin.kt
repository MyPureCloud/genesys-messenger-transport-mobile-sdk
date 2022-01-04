package com.genesys.cloud.messenger

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

private const val ANDROIDX_VALIDATION_TASK_NAME = "checkForAndroidxDependencies"

class TransportValidationPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.tasks.create<DependencyValidatorTask>(ANDROIDX_VALIDATION_TASK_NAME) {
            dependsOn("androidDependencies")
            failIfPresent("androidx")
        }
    }
}
