package com.genesys.cloud.messenger.uitest.page

import android.app.Activity
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.genesys.cloud.messenger.uitest.support.testConfig
import java.lang.Thread.sleep


class OpeningPage(activity: Activity) : BasePage(activity) {

    val title = "Deployment ID"
    val regionDefault = "inindca.com"
    val prodRegion = ""

    // Wait until android compose prototype begins
    fun verifyPageIsVisible(waitTime: Long = 20) {
        waitForElementWithUIAutomator(title, waitTime)
    }

    // Select appropriate view from opening page
    fun selectView(viewName: String) {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val androidUiViewButton = mDevice.findObject(UiSelector().text(viewName))
        androidUiViewButton.click()
    }

    // Enter the deployment ID contained in the assets>testConfig.json file
    fun enterDeploymentID(deploymentId: String) {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val deploymentIdField = mDevice.findObject(UiSelector().text(title))
        deploymentIdField.click()
        deploymentIdField.clearTextField()
        deploymentIdField.legacySetText(deploymentId)
        val regionField = mDevice.findObject(UiSelector().text(regionDefault))
        regionField.click()
        sleep(2000)
        if (testConfig.domain != regionDefault) {
            val prodRegionField = mDevice.findObject(UiSelector().text(testConfig.domain))
            prodRegionField.click()
        }
        else {
            val dcaRegionField = mDevice.findObject(UiSelector().text(regionDefault))
            dcaRegionField.click()
        }
    }
}
