package com.genesys.cloud.messenger.uitest.page

import android.app.Activity
import android.content.ContentValues.TAG
import android.util.Log
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import org.awaitility.Awaitility
import org.awaitility.Awaitility.await
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit.SECONDS

class MessengerPage(activity: Activity) : BasePage(activity) {

    private val title = "Web Messaging Testbed"
    private val sendText = "Send"
    private val messageResultText1 = "direction=Inbound"
    private val messageResultText2 = "Message\$State\$Sent"
    private val commandClass = "android.widget.EditText"
    private val responseClass = "android.widget.ScrollView"
    private val autostartEventText = "Event\$ConversationAutostart"
    private val disconnectEventText = "Event\$ConversationDisconnect"
    private val newChatText = "newChat"
    private val readOnlyText = "ReadOnly"
    private val noUAuthText = "NoAuth"
    private val closeButton = "Close tab"
    private val welcomeText = "Welcome to Chrome"
    private val acceptText = "Accept & continue"
    private val noThanksText = "No thanks"
    private val signInText = "Sign In"
    private val signInUserNameId = "okta-signin-username"
    private val signInPasswordId = "okta-signin-password"

    // Wait until android compose prototype begins
    fun verifyPageIsVisible(waitTime: Long = 20) {
        waitForElementWithUIAutomator(title, waitTime)
    }

    // Send a command in command field
    fun enterCommand(command: String) {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val commandBox = mDevice.findObject(
            UiSelector().className(commandClass).index(1)
        )
        commandBox.click()
        commandBox.clearTextField()
        mDevice.executeShellCommand("input text $command")
        UiObject(UiSelector().className(commandClass)).setText(command)

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
                .index(1)
        )
        return commandBox.getText()
    }

    fun getAuthStateResponse(): String {
        val mDevice = UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation()
        )
        val commandBox = mDevice.findObject(
            UiSelector()
                .className(commandClass)
                .index(2)
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

    fun grabAttachmentId(): String {
        val fullResponse = getFullResponse()
        val attachIdIndex = fullResponse.indexOf("Attachment(id=", 0)
        val attachmentIdLength = "Attachment(id=".length
        val startIndex = attachIdIndex + attachmentIdLength
        Log.i(TAG, "attachment id: ${fullResponse.substring(startIndex, (startIndex + 36))}")
        return fullResponse.substring(startIndex, (startIndex + 36))
    }

    fun checkForUnAuthenticatedResponse(rejectionText: String) {
        waitForElementWithUIAutomator(rejectionText)
        tapWithUIAutomator(closeButton)
        waitForAuthMsgReceived(noUAuthText)
    }

    fun waitForAuthMsgReceived(messageToBeReceived: String) {
        await().atMost(waitTime, SECONDS)
            .until {
                getAuthStateResponse().contains(
                    messageToBeReceived,
                    ignoreCase = true
                )
            }
    }

    // Wait for configure response
    fun waitForConfigured() {
        var clientResponse: String = ""
        await().atMost(waitTime, SECONDS)
            .until {
                clientResponse = getClientResponse()
                (clientResponse.contains("Configured", ignoreCase = true) || (clientResponse.contains("ReadOnly", ignoreCase = true)))
            }
        if (clientResponse.contains("ReadOnly", ignoreCase = true)) {
            enterCommand(newChatText)
            await().atMost(waitTime, SECONDS)
                .until {
                    clientResponse = getClientResponse()
                    clientResponse.contains("Configured", ignoreCase = true)
                }
        }
    }

    fun waitForReadOnly() {
        await().atMost(waitTime, SECONDS)
            .until {
                getClientResponse().contains("ReadOnly", ignoreCase = true)
            }
    }

    // Wait for client to be closed
    fun waitForClosed() {
        await().atMost(waitTime, SECONDS)
            .until {
                getClientResponse().contains("Closed", ignoreCase = true)
            }
    }

    // Get text from response field
    fun getFullResponse(): String {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val responseBox = mDevice.findObject(
            UiSelector().index(3)
        )
        return responseBox.getText()
    }

    // Verify response for sending a message
    fun checkSendMsgFullResponse() {
        val response = getFullResponse()
        if (!(response.contains(messageResultText1, ignoreCase = true))) AssertionError("Response does not contain MessageUpdated")
        if (!(response.contains(messageResultText2, ignoreCase = true))) AssertionError("Response does not contain MessageUpdated")
    }

    // Verify response contains events for autostart and conversationDisconnect
    fun checkHistoryForAutoStartAndDisconnectEventsResponse() {
        val response = getFullResponse()
        if (!(response.contains(autostartEventText, ignoreCase = true))) AssertionError("Response does not contain Autostart event")
        if (!(response.contains(disconnectEventText, ignoreCase = true))) AssertionError("Response does not contain conversationDisconnect event")
    }

    // Verify response does not contain readOnly or an event for conversationDisconnect
    fun checkHistoryDoesNotContainDisconnectEventOrReadOnlyResponse() {
        val response = getFullResponse()
        if (response.contains(disconnectEventText, ignoreCase = true)) AssertionError("Response does contain conversationDisconnect event but should not")
        val clientResponse = getClientResponse()
        if (clientResponse == readOnlyText) AssertionError("Client response is in ReadOnly but should not be.")
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

    fun loginWithOkta(email: String, password: String) {
        waitForElementWithUIAutomator(acceptText, shortWaitTime)
        if (hasTextView(acceptText)) {
            tapTextWithUIAutomator(acceptText)
            waitForElementWithUIAutomator(noThanksText, shortWaitTime)
            tapTextWithUIAutomator(noThanksText)
        }
        waitForElementWithUIAutomator(signInText)
        typeIndexWithUIAutomator(2, email)
        typeIndexWithUIAutomator(4, password)
        pressEnterKey()
    }

    protected fun typeWithUIAutomator(id: String, text: String) {
        val uiAutomatorInstance = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val target = uiAutomatorInstance.findObject(UiSelector().resourceId(id))
        target.clearTextField()
        target.click()
        target.setText(text)
    }

    protected fun typeIndexWithUIAutomator(index: Int, text: String) {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val responseBox = mDevice.findObject(
            UiSelector().index(index)
        )
        responseBox.click()
        responseBox.clearTextField()
        mDevice.executeShellCommand("input text $text")
        UiObject(UiSelector().index(index)).setText(text)
    }

    protected fun pressTab() {
        await().atMost(3, SECONDS).ignoreExceptions()
            .untilAsserted {
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()) != null
            }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressKeyCode(KeyEvent.KEYCODE_TAB)
    }

    protected fun pressEnterKey() {
        await().atMost(3, SECONDS).ignoreExceptions()
            .untilAsserted {
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()) != null
            }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressEnter()
    }

    fun pressBackKey() {
        await().atMost(3, SECONDS).ignoreExceptions()
            .untilAsserted {
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()) != null
            }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
    }

    protected fun tapWithUIAutomator(contentDescription: String) {
        val uiAutomatorInstance = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val itemToTap = uiAutomatorInstance.findObject(UiSelector().descriptionContains(contentDescription))
        itemToTap.click()
    }
    protected fun hasTextView(text: String): Boolean {
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        return uiDevice.hasObject(By.textContains(text))
    }
    protected fun tapTextWithUIAutomator(text: String) {
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val itemToTap = uiDevice.findObject(UiSelector().text(text))
        itemToTap.waitForExists(waitTime)
        itemToTap.click()
    }
}
