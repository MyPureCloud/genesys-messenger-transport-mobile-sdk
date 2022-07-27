package com.genesys.cloud.messenger.uitest.page

import android.app.Activity
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.awaitility.Awaitility
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit.SECONDS


class MessengerPage(activity: Activity) : BasePage(activity) {

    private val title = "Web Messaging Testbed"
    private val sendText = "Send"
    private val messageResultText1 = "direction=Inbound"
    private val messageResultText2 = "Message\$State\$Sent"
    private val buttonClass = "android.widget.Button"
    private val commandClass = "android.widget.EditText"
    private val responseClass = "android.widget.ScrollView"

    // Wait until android compose prototype begins
    fun verifyPageIsVisible(waitTime: Long = 20) {
        waitForElementWithUIAutomator(title, waitTime)
    }

    // Send a command in command field
    fun enterCommand(command: String) {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val commandBox = mDevice.findObject(
            UiSelector().className(commandClass).index(2)
        )
        commandBox.click()
        commandBox.clearTextField()
        commandBox.legacySetText(command)
        sleep(3000)
        val sendButton = mDevice.findObject(
            UiSelector().text(sendText)
        )
        sendButton.click()
    }

    // Grab the client response in client field
    fun getClientResponse(): String {
        val mDevice = UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation()
        )
        val commandBox = mDevice.findObject(
            UiSelector()
                .className(commandClass)
                .index(3)
        )
        return commandBox.getText()
    }

    // Wait for client to return proper response
    fun waitForProperResponse(response: String) {
        Awaitility.await().atMost(waitTime, SECONDS)
            .until {
                getFullResponse().contains(response, ignoreCase = true)
            }
    }

    // Wait for client to be connected
    fun waitForConnected() {
        Awaitility.await().atMost(waitTime, SECONDS)
            .until {
                getClientResponse().contains("Connected", ignoreCase = true)
            }
    }

    // Wait for client to be connected
    fun waitForConfigured() {
        Awaitility.await().atMost(waitTime, SECONDS)
            .until {
                getClientResponse().contains("Configured", ignoreCase = true)
            }
    }

    // Wait for client to be closed
    fun waitForClosed() {
        Awaitility.await().atMost(waitTime, SECONDS)
            .until {
                getClientResponse().contains("Closed", ignoreCase = true)
            }
    }

    // Get text from response field
    fun getFullResponse(): String {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val responseBox = mDevice.findObject(
            UiSelector()
                .className(responseClass)
                .index(4)
        )
        return responseBox.getText()
    }

    // Verify configured full response
    fun checkConfigureFullResponse() {
        val response = getFullResponse()
        if (!(response.contains("\"Connected\": true", ignoreCase = true))) AssertionError("Response does not contain \"Connected\": true")
    }

    // Verify response for sending a message
    fun checkSendMsgFullResponse() {
        val response = getFullResponse()
        if (!(response.contains(messageResultText1, ignoreCase = true))) AssertionError("Response does not contain MessageUpdated")
        if (!(response.contains(messageResultText2, ignoreCase = true))) AssertionError("Response does not contain MessageUpdated")
    }

    // Verify response for the ping command
    fun checkPingFullResponse() {
        val response = getFullResponse()
        if (!(response.contains("\"type\": \"response\"", ignoreCase = true))) AssertionError("Response does not contain \"Type\": \"response\"")
        if (!(response.contains("200", ignoreCase = true))) AssertionError("Response does not contain 200")
        if (!(response.contains("\"text\": \"ping\"", ignoreCase = true))) AssertionError("Response does not contain \"Text\": \"ping\"")
    }

    // Verify response for the history command
    fun checkHistoryFullResponse() {
        val response = getFullResponse()
        if (!(response.contains("HistoryFetched", ignoreCase = true))) AssertionError("Response does not contain \"pageSize\": 1")
    }

    fun pullAttachmentId(response: String): String {
        val string1 = response.split("=")
        val string2 = string1[1].split(",")
        return string2[0]
    }

    // Verify response for the attach command
    fun checkAttachFullResponse(): String {
        val response = getFullResponse()
        if (!(response.contains("Attachment", ignoreCase = true))) AssertionError("Response does not contain: Attachment")
        if (!(response.contains("fileName=", ignoreCase = true))) AssertionError("Response does not contain: filename=")
        if (!(response.contains("state=Uploaded", ignoreCase = true))) AssertionError("Response does not contain: state=Uploaded")
        return pullAttachmentId(response)
    }

    // Verify response for the detach command
    fun checkDetachFullResponse() {
        val response = getFullResponse()
        if (!(response.contains("Attachment", ignoreCase = true))) AssertionError("Response does not contain: Attachment")
        if (!(response.contains("fileName=", ignoreCase = true))) AssertionError("Response does not contain: filename=")
        if (!(response.contains("Deleted", ignoreCase = true))) AssertionError("Response does not contain: Deleted")
    }
}
