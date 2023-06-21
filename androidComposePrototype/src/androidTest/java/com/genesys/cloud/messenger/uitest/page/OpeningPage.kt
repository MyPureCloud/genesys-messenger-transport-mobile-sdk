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
    val tcaEnvironment = "inintca.com"
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
        sleep(2000)
        val regionField = mDevice.findObject(UiSelector().className("android.widget.Button").index(1))
        regionField.click()
        sleep(2000)
        val listOfSrollView = mDevice.findObject(UiSelector().className("android.widget.ScrollView"))
        if (testConfig.domain == regionDefault) {
            val dcaView = listOfSrollView.getChild(UiSelector().className("android.view.View").index(0))
            if (dcaView != null) {
                dcaView.click()
            }
        } else if (testConfig.domain == tcaEnvironment) {
            val tcaView = listOfSrollView.getChild(UiSelector().className("android.view.View").index(1))
            if (tcaView != null) {
                tcaView.click()
            }
        } else {
            val prodView = listOfSrollView.getChild(UiSelector().className("android.view.View").index(2))
            if (prodView != null) {
                prodView.click()
            }
        }
    }
}
