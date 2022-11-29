package com.genesys.cloud.messenger.uitest.test

import android.util.Log
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed.TestBedViewModel
import com.genesys.cloud.messenger.transport.util.DefaultTokenStore
import com.genesys.cloud.messenger.uitest.support.ApiHelper.API
import com.genesys.cloud.messenger.uitest.support.ApiHelper.answerNewConversation
import com.genesys.cloud.messenger.uitest.support.ApiHelper.sendConnectOrDisconnect
import com.genesys.cloud.messenger.uitest.support.ApiHelper.sendTypingIndicatorFromAgentToUser
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@Suppress("FunctionName")
@LargeTest
@RunWith(AndroidJUnit4::class)
class ComposePrototypeUITest : BaseTests() {
    val apiHelper by lazy { API() }
    private val testBedViewText = "TestBed View"
    private val connectText = "connect"
    private val sendMsgText = "send"
    private val helloText = "hello"
    private val healthCheckText = "healthCheck"
    private val historyText = "history 1 1"
    private val attachImageText = "attach"
    private val detachImageText = "detach"
    private val byeText = "bye"
    private val uploadingText = "Uploading"
    private val uploadedText = "Uploaded"
    private val deletedText = "Deleted"
    private val attachmentSentText = "state=Sent"
    private val addAtrributeText = "addAttribute"
    private val nameText = "name"
    private val newNameText = "Nellie Hay"
    private val customAttributeAddedText = "Custom attribute added"
    private val oneThousandText = "Code: 1000"
    private val healthCheckResponse = "HealthChecked"
    private val historyFetchedText = "HistoryFetched"
    private val longClosedText = "The user has closed the connection"
    private val connectedText = "Connected: true"
    private val typingIndicatorResponse = "AgentTyping"
    private val autoStartEnabledText = "ConversationAutostart"
    private val TAG = TestBedViewModel::class.simpleName

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

    // Send a healthCheck command, wait for the response, and verify it is correct
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

    // Send an attach image command, wait for the response, and verify it is correct
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

    // Send a detach image command, wait for the response, and verify it is correct
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

    fun verifyAutoStartResponse() {
        messenger {
            waitForProperResponse(autoStartEnabledText)
        }
    }

    @Test
    fun sendTypingIndicator() {
        opening {
            verifyPageIsVisible()
            enterDeploymentID()
            selectView(testBedViewText)
        }
        messenger {
            verifyPageIsVisible()
        }
        connect()
        sendMsg("howdy")
        val conversationInfo = apiHelper.answerNewConversation()
        if (conversationInfo != null) {
            apiHelper.sendTypingIndicatorFromAgentToUser(conversationInfo)
            messenger {
                waitForProperResponse(typingIndicatorResponse)
            }
            apiHelper.sendConnectOrDisconnect(conversationInfo, false, true)
        } else AssertionError("Agent did not answer conversation.")
        messenger {
            enterCommand(byeText)
            waitForClosed()
        }
    }

    @Test
    fun verifyAutoStart() {
        opening {
            verifyPageIsVisible()
            enterDeploymentID()
            selectView(testBedViewText)
        }
        messenger {
            verifyPageIsVisible()
        }
        // Force a new session. AutoStart is enabled and newSession is true
        DefaultTokenStore("com.genesys.cloud.messenger").store(UUID.randomUUID().toString())
        connect()
        verifyAutoStartResponse()
        val conversationInfo = apiHelper.answerNewConversation()
        if (conversationInfo == null) AssertionError("Unable to answer conversation with autoStart enabled.")
        else {
            Log.i(TAG, "Conversation started successfully with autoStart enabled.")
            apiHelper.sendConnectOrDisconnect(conversationInfo, false, true)
        }
        messenger {
            enterCommand(byeText)
            waitForClosed()
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
