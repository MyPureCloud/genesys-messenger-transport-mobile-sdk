package com.genesys.cloud.messenger.uitest.page

import android.app.Activity
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

open class BasePage(val activity: Activity) {

    val waitTime = 20.toLong()

    // Initialize activity
    init {
        ActivityLifecycleMonitorRegistry.getInstance()
            .addLifecycleCallback { activity, stage ->
                if (stage == Stage.PRE_ON_CREATE) {
                    activity.window.addFlags(FLAG_KEEP_SCREEN_ON)
                }
            }
    }

    // Wait for the text string to appear using UIAutomator
    protected fun waitForElementWithUIAutomator(text: String, waitTime: Long = 60000) {
        println("Waiting for element with text: $text")
        val uiAutomatorInstance = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        uiAutomatorInstance.wait(Until.findObject(By.text(text)), waitTime)
    }
}
