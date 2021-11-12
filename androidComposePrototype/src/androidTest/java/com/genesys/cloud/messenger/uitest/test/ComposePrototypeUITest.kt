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
    val sendMsgText = "send hello"
    val healthCheckText = "healthCheck"
    val historyText = "history 1 1"
    val attachImageText = "attach"
    val detachImageText = "detach"
    val deleteText = "delete"
    val byeText = "bye"
    val messageText = "How can I help?"
    val messageResultText = "MessageUpdated"
    val uploadingText = "Uploading"
    val uploadedText = "Uploaded"
    val deletedText = "Deleted"

    val twoHundredText = "200"
    val historyFetchedText = "HistoryFetched"
    private val randomFileName = "a" + java.util.UUID.randomUUID().toString().substring(0, 15) + ".jpg"

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
            waitForProperResponse(twoHundredText)
            checkConfigureFullResponse()
        }
    }

    // Send a message, wait for the response, and verify it is correct
    fun sendMsg() {
        messenger {
            verifyPageIsVisible()
            enterCommand(sendMsgText)
            waitForProperResponse(messageResultText)
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
        var attachmentId: String = ""
        messenger {
            verifyPageIsVisible()
            enterCommand(attachImageText)
            waitForProperResponse(uploadingText)
            waitForProperResponse(uploadedText)
            attachmentId = checkAttachFullResponse()
        }
        return attachmentId
    }

    // Send a detach command, wait for the response, and verify it is correct
    fun detachImage() {
        messenger {
            verifyPageIsVisible()
            enterCommand(detachImageText)
            waitForProperResponse(deletedText)
            checkDetachFullResponse()
        }
    }

    // Send a detach command, wait for the response, and verify it is correct
    fun deleteImage(attachmentId: String) {
        messenger {
            verifyPageIsVisible()
            enterCommand("$deleteText $attachmentId")
            waitForProperResponse(deletedText)
            checkDetachFullResponse()
        }
    }

    // Send the bye command and wait for the closed response
    fun bye() {
        messenger {
            verifyPageIsVisible()
            enterCommand(byeText)
            waitForClosed()
        }
    }

    // A test to verify the connect, configure, send message, ping, history, attach image, detach image, and bye commands
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
        sendMsg()
        ping()
        history()
        attachImage()
        detachImage()
        bye()
    }

    // A test to verify the connect, configure, attach image, delete image, and bye commands
    @Test
    fun testDeleteCommand() {
        opening {
            verifyPageIsVisible()
            selectView(testBedViewText)
        }
        messenger {
            verifyPageIsVisible()
        }
        connect()
        configure()
        val attachmentId = attachImage()
        deleteImage(attachmentId)
        bye()
    }
}
