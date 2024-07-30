package com.genesys.cloud.messenger.uitest.page

import android.app.Activity
import android.content.ContentValues.TAG
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import com.genesys.cloud.messenger.uitest.support.testConfig
import java.lang.Thread.sleep

class OpeningPage(activity: Activity) : BasePage(activity) {

    val title = "Deployment ID"
    val regionDefault = "inindca.com"
    val tcaEnvironment = "inintca.com"
    val prodEnvironment = "mypurecloud.com"
    val usWestEnvironment = "usw2.pure.cloud"
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
        mDevice.executeShellCommand("input text $deploymentId")
        //deploymentIdField.legacySetText(deploymentId)
        //UiObject(UiSelector().text(title)).setText(deploymentId)
        sleep(2000)
        val regionField = mDevice.findObject(UiSelector().className("android.widget.Button").index(1))
        regionField.click()
        sleep(2000)
        val listOfSrollView = mDevice.findObject(UiSelector().className("android.widget.ScrollView"))
        when (testConfig.domain) {
            regionDefault -> {
                val envView =
                    listOfSrollView.getChild(UiSelector().className("android.view.View").index(0))!!
                        .click()
            }

            tcaEnvironment -> {
                val envView =
                    listOfSrollView.getChild(UiSelector().className("android.view.View").index(1))!!
                        .click()
            }

            prodEnvironment -> {
                val envView =
                    listOfSrollView.getChild(UiSelector().className("android.view.View").index(2))!!
                        .click()
            }

            usWestEnvironment -> {
                val envView =
                    listOfSrollView.getChild(UiSelector().className("android.view.View").index(3))!!
                        .click()
            }
            // to do add other regions as needed
            else -> {
                Log.e(TAG, "region not found")
            }
        }
    }
}
