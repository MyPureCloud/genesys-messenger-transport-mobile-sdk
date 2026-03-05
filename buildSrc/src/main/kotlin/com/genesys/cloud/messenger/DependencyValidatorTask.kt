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
            // OkHttp 5.x pulls in androidx on the test classpath; skip check for test configs only.
            // Production configs are still validated (no androidx in published/runtime deps).
            val isTestConfiguration = name.contains("UnitTest", ignoreCase = true) ||
                name.contains("AndroidTest", ignoreCase = true)
            if (isTestConfiguration) return@all
            resolutionStrategy.eachDependency {
                if (this.requested.group.contains(excludeGroup)) {
                    throw IllegalArgumentException("Sorry, $excludeGroup libraries are not welcomed here.")
                }
            }
        }
    }
}
