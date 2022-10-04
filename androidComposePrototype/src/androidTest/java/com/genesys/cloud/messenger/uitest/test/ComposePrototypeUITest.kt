package com.genesys.cloud.messenger.uitest.test

import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import com.genesys.cloud.messenger.uitest.support.ApiHelper.*

@Suppress("FunctionName")
@LargeTest
@RunWith(AndroidJUnit4::class)
class ComposePrototypeUITest : BaseTests() {

    open val apiHelper by lazy { API() }
    val testBedViewText = "TestBed View"
    val connectText = "connect"
    val sendMsgText = "send"
    val helloText = "hello"
    val healthCheckText = "healthCheck"
    val historyText = "history 1 1"
    val attachImageText = "attach"
    val detachImageText = "detach"
    val byeText = "bye"
    val uploadingText = "Uploading"
    val uploadedText = "Uploaded"
    val deletedText = "Deleted"
    val attachmentSentText = "state=Sent"
    val addAtrributeText = "addAttribute"
    val nameText = "name"
    val newNameText = "Nellie Hay"
    val customAttributeAddedText = "Custom attribute added"
    val oneThousandText = "Code: 1000"
    val healthCheckResponse = "HealthChecked"
    val historyFetchedText = "HistoryFetched"
    val longClosedText = "The user has closed the connection"
    val connectedText = "Connected: true"

    // Send the connect command and wait for connected response
    fun connect() {
        messenger {
            verifyPageIsVisible()
            enterCommand(connectText)
            waitForProperResponse(connectedText)
            waitForConfigured()
        }
    }

    // Send a message, wait for the response, and verify it is correct
    fun sendMsg(messageText: String) {
        messenger {
            verifyPageIsVisible()
            enterCommand("$sendMsgText $messageText")
            waitForProperResponse(messageText)
            checkSendMsgFullResponse()
        }
    }

    // Send a ping command, wait for the response, and verify it is correct
    fun healthcheckTest() {
        messenger {
            verifyPageIsVisible()
            enterCommand(healthCheckText)
            waitForProperResponse(healthCheckResponse)
        }
    }

    // Send a history command, wait for the response, and verify it is correct
    fun history() {
        messenger {
            verifyPageIsVisible()
            enterCommand(historyText)
            waitForProperResponse(historyFetchedText)
            checkHistoryFullResponse()
        }
    }

    // Send an attach command, wait for the response, and verify it is correct
    fun attachImage(): String {
        var attachmentId = ""
        messenger {
            verifyPageIsVisible()
            enterCommand(attachImageText)
            waitForProperResponse(uploadingText)
            waitForProperResponse(uploadedText)
            attachmentId = checkAttachFullResponse()
            enterCommand(sendMsgText)
            waitForProperResponse(attachmentSentText)
            waitForProperResponse("id=$attachmentId")
        }
        return attachmentId
    }

    // Send a detach command, wait for the response, and verify it is correct
    fun detachImage(attachmentId: String) {
        messenger {
            verifyPageIsVisible()
            enterCommand("$detachImageText $attachmentId")
            waitForProperResponse(deletedText)
            checkDetachFullResponse()
        }
    }

    fun addCustomAttribute(key: String, value: String) {
        messenger {
            verifyPageIsVisible()
            enterCommand("$addAtrributeText $key $value")
            waitForProperResponse(customAttributeAddedText)
            waitForProperResponse("$key, $value")
        }
    }

    fun sendTypingIndicator() {
        messenger {
            verifyPageIsVisible()
            enterCommand("typing")
            //verify that an error was not received
        }
        val conversation = apiHelper.waitForConversation()
        val conversationInfo = apiHelper.getConversationInfo(conversation!!.id)
        apiHelper.sendTypingIndicator(conversationInfo)
    }

    // Send the bye command and wait for the closed response
    fun bye() {
        messenger {
            verifyPageIsVisible()
            enterCommand(byeText)
            waitForClosed()
            waitForProperResponse(oneThousandText)
            waitForProperResponse(longClosedText)
        }
    }

    // A test to verify the connect, healthCheck, send message, attach image, add custom attribute, and bye commands
    @Test
    fun testAllCommands() {
        opening {
            verifyPageIsVisible()
            selectView(testBedViewText)
        }
        messenger {
            verifyPageIsVisible()
        }
        connect()
        healthcheckTest()
        sendMsg(helloText)
        val attachmentId = attachImage()
        addCustomAttribute(nameText, newNameText)
        bye()
    }
}
