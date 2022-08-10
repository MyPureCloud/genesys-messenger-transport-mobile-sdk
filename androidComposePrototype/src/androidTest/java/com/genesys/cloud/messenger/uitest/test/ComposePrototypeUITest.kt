package com.genesys.cloud.messenger.uitest.test

import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("FunctionName")
@LargeTest
@RunWith(AndroidJUnit4::class)
class ComposePrototypeUITest : BaseTests() {

    val testBedViewText = "TestBed View"
    val connectText = "connect"
    val configureText = "configure"
    val connectConfigureText = "connectWithConfigure"
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
    val twoHundredText = "200"
    val historyFetchedText = "HistoryFetched"
    val longClosedText = "The user has closed the connection"

    // Send the connect command and wait for connected response
    fun connect() {
        messenger {
            verifyPageIsVisible()
            enterCommand(connectText)
            waitForConnected()
        }
    }

    // Send the configure command, wait for configure response, and verify it is correct
    fun configure() {
        messenger {
            verifyPageIsVisible()
            enterCommand(configureText)
            waitForConfigured()
            checkConfigureFullResponse()
        }
    }

    fun connectWithConfigure() {
        messenger {
            verifyPageIsVisible()
            enterCommand(connectConfigureText)
            waitForConfigured()
            checkConfigureFullResponse()
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
    fun ping() {
        messenger {
            verifyPageIsVisible()
            enterCommand(healthCheckText)
            waitForProperResponse(twoHundredText)
            checkPingFullResponse()
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

    // A test to verify the connect, configure, send message, attach image, add custom attribute, and bye commands
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
        configure()
        sendMsg(helloText)
        val attachmentId = attachImage()
        addCustomAttribute(nameText, newNameText)
        bye()
    }

    // A test to verify the connect with configure command
    @Test
    fun testConnectWithConfigureCommand() {
        opening {
            verifyPageIsVisible()
            selectView(testBedViewText)
        }
        messenger {
            verifyPageIsVisible()
            connectWithConfigure()
        }
        bye()
    }
}
