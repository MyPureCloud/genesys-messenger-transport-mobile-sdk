package com.genesys.cloud.messenger

import org.gradle.api.DefaultTask
import java.lang.IllegalArgumentException

abstract class DependencyValidatorTask: DefaultTask() {

    init {
        group = "verification"
        description = "Verify dependencies."
    }

    fun failIfPresent(excludeGroup: String) {
        project.configurations.all {
            resolutionStrategy.eachDependency {
                if (this.requested.group.contains(excludeGroup)) {
                    throw IllegalArgumentException("Sorry, $excludeGroup libraries are not welcomed here.")
                }
            }
        }
    }
}
