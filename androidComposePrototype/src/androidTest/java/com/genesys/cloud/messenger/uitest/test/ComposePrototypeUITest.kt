package com.genesys.cloud.messenger.uitest.test

import android.util.Log
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed.TestBedViewModel
import com.genesys.cloud.messenger.transport.util.DefaultVault
import com.genesys.cloud.messenger.uitest.support.ApiHelper.API
import com.genesys.cloud.messenger.uitest.support.ApiHelper.answerNewConversation
import com.genesys.cloud.messenger.uitest.support.ApiHelper.disconnectAllConversations
import com.genesys.cloud.messenger.uitest.support.ApiHelper.sendConnectOrDisconnect
import com.genesys.cloud.messenger.uitest.support.ApiHelper.sendOutboundMessageFromAgentToUser
import com.genesys.cloud.messenger.uitest.support.ApiHelper.sendTypingIndicatorFromAgentToUser
import com.genesys.cloud.messenger.uitest.support.ApiHelper.waitForParticipantToConnectOrDisconnect
import com.genesys.cloud.messenger.uitest.support.testConfig
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.lang.Thread.sleep
import java.util.UUID

@Suppress("FunctionName")
@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ComposePrototypeUITest : BaseTests() {
    val apiHelper by lazy { API() }
    private val testBedViewText = "TestBed View"
    private val connectText = "connect"
    private val sendMsgText = "send"
    private val helloText = "hello"
    private val healthCheckText = "healthCheck"
    private val historyText = "history"
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
    private val longClosedText = "The user has closed the connection"
    private val connectedText = "Connected: true"
    private val newChatText = "newChat"
    private val typingIndicatorResponse = "AgentTyping"
    private val outboundMessage = "Right back at you"
    private val autoStartEnabledText = "ConversationAutostart"
    private var humanNameText = "name=Nellie Hay"
    private val prodHumanNameText = "name=Beagle Puppy"
    private var avatarText = "imageUrl=https://dev-inin-directory-service-profile.s3.amazonaws.com"
    private val prodAvatorText = "imageUrl=https://prod-inin-directory-service-profile.s3.amazonaws.com"
    private val humanText = "originatingEntity=Human"
    private val deploymentText = "Deployment"
    private val humanizeDisabledText = "from=Participant(name=null, imageUrl=null"
    private val botImageName = "from=Participant(name=Test-Bot-Name, imageUrl=null"
    private val botEntity = "originatingEntity=Bot"
    private val yesText = "Yes"
    private val anotherBotMessage = "Would you like to continue"
    private val startOfConversationText = "Start of conversation"
    private val oktaSignInWithPKCEText = "oktaSignInWithPKCE"
    private val oktaLogoutText = "oktaLogout"
    private val authCodeReceivedText = "AuthCodeReceived"
    private val loggedOutText = "LoggedOut"
    private val authenticateText = "authenticate"
    private val authenticatedText = "authenticated"
    private val authenticateConnectText = "connectAuthenticated"
    private val notAuthenticateText = "Unable to sign in"
    private val fakeAuthUserName = "daffy.duck@looneytunes.com"
    private val fakeAuthPassword = "xxxxxxxxxx"
    private val TAG = TestBedViewModel::class.simpleName

    fun enterDeploymentInfo(deploymentId: String) {
        opening {
            verifyPageIsVisible()
            enterDeploymentID(deploymentId)
            selectView(testBedViewText)
        }
        messenger {
            verifyPageIsVisible()
        }
    }

    // Send the connect command and wait for connected response
    fun connect(connectCommand: String = connectText) {
        messenger {
            verifyPageIsVisible()
            enterCommand(connectCommand)
            waitForConfigured()
        }
    }

    fun oktaSignInWithPKCE(userName: String, password: String, validSignIn: Boolean = true) {
        messenger {
            verifyPageIsVisible()
            enterCommand(oktaSignInWithPKCEText)
            loginWithOkta(userName, password)
            if (validSignIn) waitForAuthMsgReceived(authCodeReceivedText)
        }
    }

    fun oktaLogout() {
        messenger {
            verifyPageIsVisible()
            enterCommand(oktaLogoutText)
            waitForAuthMsgReceived(loggedOutText)
            waitForClosed()
            pressBackKey()
        }
    }

    fun authenticate() {
        messenger {
            verifyPageIsVisible()
            enterCommand(authenticateText)
            waitForAuthMsgReceived(authenticatedText)
        }
    }

    fun clearBrowser() {
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("pm clear com.android.chrome").close()
    }

    fun checkForReadOnly() {
        messenger {
            waitForReadOnly()
        }
    }

    fun reconnectReadOnly() {
        messenger {
            enterCommand(connectText)
            waitForReadOnly()
        }
    }

    fun startNewChat() {
        messenger {
            enterCommand(newChatText)
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

    fun sendBotResponseAndCheckReply(messageText: String) {
        messenger {
            verifyPageIsVisible()
            enterCommand("$sendMsgText $messageText")
            verifyResponse(anotherBotMessage)
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
    fun history(withConversationDisconnectEvent: Boolean = true) {
        messenger {
            verifyPageIsVisible()
            enterCommand(historyText)
            waitForProperResponse(startOfConversationText)
            if (withConversationDisconnectEvent) checkHistoryForAutoStartAndDisconnectEventsResponse()
            else checkHistoryDoesNotContainDisconnectEventOrReadOnlyResponse()
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
            sleep(2000)
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

    fun enterDeploymentCommand(responseLookingFor: String) {
        messenger {
            verifyPageIsVisible()
            enterCommand(deploymentText)
            waitForProperResponse(responseLookingFor)
        }
    }

    fun verifyResponse(response: String) {
        messenger {
            waitForProperResponse(response)
        }
    }

    fun verifyNotAuthenticated(rejectText: String) {
        messenger {
            checkForUnAuthenticatedResponse(rejectText)
        }
    }

    @Test
    fun testSendTypingIndicator() {
        apiHelper.disconnectAllConversations()
        enterDeploymentInfo(testConfig.deploymentId)
        DefaultVault().store("token", UUID.randomUUID().toString())
        connect()
        val conversationInfo = apiHelper.answerNewConversation()
        if (conversationInfo != null) {
            apiHelper.sendTypingIndicatorFromAgentToUser(conversationInfo)
            verifyResponse(typingIndicatorResponse)
            apiHelper.sendConnectOrDisconnect(conversationInfo)
        } else AssertionError("Agent did not answer conversation.")
        apiHelper.disconnectAllConversations()
    }

    @Test
    // Adjusting the test name to force this test to run first
    fun test3VerifyAutoStart() {
        apiHelper.disconnectAllConversations()
        enterDeploymentInfo(testConfig.deploymentId)
        // Force a new session. AutoStart is enabled and newSession is true
        DefaultVault().store("token", UUID.randomUUID().toString())
        connect()
        verifyResponse(autoStartEnabledText)
        val conversationInfo = apiHelper.answerNewConversation()
        if (conversationInfo == null) AssertionError("Unable to answer conversation with autoStart enabled.")
        else {
            Log.i(TAG, "Conversation started successfully with autoStart enabled.")
            apiHelper.sendConnectOrDisconnect(conversationInfo)
        }
        apiHelper.disconnectAllConversations()
    }

    @Test
    fun testHealthCheck() {
        apiHelper.disconnectAllConversations()
        enterDeploymentInfo(testConfig.deploymentId)
        connect()
        val conversationInfo = apiHelper.answerNewConversation()
        if (conversationInfo == null) AssertionError("Unable to answer conversation with autoStart enabled.")
        else {
            Log.i(TAG, "Conversation started successfully with autoStart enabled.")
            healthcheckTest()
            apiHelper.sendConnectOrDisconnect(conversationInfo)
        }
        bye()
    }

    @Test
    fun testSendAndReceiveMessage() {
        apiHelper.disconnectAllConversations()
        enterDeploymentInfo(testConfig.deploymentId)
        connect()
        val conversationInfo = apiHelper.answerNewConversation()
        if (conversationInfo == null) AssertionError("Unable to answer conversation.")
        else {
            Log.i(TAG, "Conversation started successfully.")
            sendMsg(helloText)
            sleep(3000)
            apiHelper.sendOutboundMessageFromAgentToUser(conversationInfo, outboundMessage)
            verifyResponse(outboundMessage)
            if (testConfig.domain == "mypurecloud.com") {
                humanNameText = prodHumanNameText
                avatarText = prodAvatorText
            }
            verifyResponse(humanNameText)
            verifyResponse(avatarText)
            verifyResponse(humanText)
            apiHelper.sendConnectOrDisconnect(conversationInfo)
        }
        bye()
    }

    @Test
    fun testAttachments() {
        apiHelper.disconnectAllConversations()
        enterDeploymentInfo(testConfig.deploymentId)
        connect()
        sendMsg(helloText)
        val conversationInfo = apiHelper.answerNewConversation()
        if (conversationInfo == null) AssertionError("Unable to answer conversation.")
        else {
            Log.i(TAG, "Conversation started successfully.")
            attachImage()
            // wait for image to load
            sleep(3000)
            apiHelper.sendConnectOrDisconnect(conversationInfo)
        }
        bye()
    }

    @Test
    fun testCustomAttributes() {
        apiHelper.disconnectAllConversations()
        enterDeploymentInfo(testConfig.deploymentId)
        connect()
        val conversationInfo = apiHelper.answerNewConversation()
        if (conversationInfo == null) AssertionError("Unable to answer conversation.")
        else {
            Log.i(TAG, "Conversation started successfully.")
            addCustomAttribute(nameText, newNameText)
            sleep(3000)
            apiHelper.sendConnectOrDisconnect(conversationInfo)
        }
        bye()
    }

    @Test
    fun testUnknownAgent() {
        apiHelper.disconnectAllConversations()
        enterDeploymentInfo(testConfig.humanizeDisableDeploymentId)
        connect()
        sendMsg(helloText)
        val conversationInfo = apiHelper.answerNewConversation()
        if (conversationInfo == null) AssertionError("Unable to answer conversation.")
        else {
            Log.i(TAG, "Conversation started successfully.")
            apiHelper.sendOutboundMessageFromAgentToUser(conversationInfo, outboundMessage)
            verifyResponse(outboundMessage)
            verifyResponse(humanizeDisabledText)
            verifyResponse(humanText)
            apiHelper.sendConnectOrDisconnect(conversationInfo)
        }
        bye()
    }

    @Test
    fun testBotAgent() {
        apiHelper.disconnectAllConversations()
        enterDeploymentInfo(testConfig.botDeploymentId)
        connect()
        verifyResponse(botImageName)
        verifyResponse(botEntity)
        sleep(3000)
        sendBotResponseAndCheckReply(yesText)
        sleep(2000)
        history(withConversationDisconnectEvent = false)
        bye()
    }

    @Test
    fun testDisconnectAgent_ReadOnly() {
        apiHelper.disconnectAllConversations()
        enterDeploymentInfo(testConfig.agentDisconnectDeploymentId)
        connect()
        sendMsg(helloText)
        val conversationInfo = apiHelper.answerNewConversation()
        if (conversationInfo == null) AssertionError("Unable to answer conversation.")
        else {
            Log.i(TAG, "Conversation started successfully.")
            // conversationInfo: Conversation, connecting: Boolean, wrapup: Boolean = true
            apiHelper.sendConnectOrDisconnect(conversationInfo)
            // wait for agent to disconnect
            apiHelper.waitForParticipantToConnectOrDisconnect(conversationInfo.id)
            checkForReadOnly()
            bye()
            sleep(2000)
            // connect again and check for readOnly
            reconnectReadOnly()
            bye()
        }
    }

    @Test
    fun testDisconnectAgent_NotReadOnly() {
        apiHelper.disconnectAllConversations()
        enterDeploymentInfo(testConfig.deploymentId)
        connect()
        sendMsg(helloText)
        val conversationInfo = apiHelper.answerNewConversation()
        if (conversationInfo == null) AssertionError("Unable to answer conversation.")
        else {
            Log.i(TAG, "Conversation started successfully.")
            apiHelper.sendOutboundMessageFromAgentToUser(conversationInfo, outboundMessage)
            verifyResponse(outboundMessage)
            apiHelper.sendConnectOrDisconnect(conversationInfo)
            // wait for agent to disconnect
            apiHelper.waitForParticipantToConnectOrDisconnect(conversationInfo.id)
            sendMsg(helloText)
            val conversation2Info = apiHelper.answerNewConversation()
            if (conversation2Info == null) AssertionError("Unable to answer conversation.")
            else {
                Log.i(TAG, "Conversation started successfully.")
                if (conversationInfo.id != conversation2Info.id) AssertionError("Reconnecting a conversation may not have matching conversation IDs.")
                else (Log.i(TAG, "Conversation ids matched as expected."))
                apiHelper.sendConnectOrDisconnect(conversation2Info)
                // wait for agent to disconnect
                apiHelper.waitForParticipantToConnectOrDisconnect(conversation2Info.id)
            }
        }
    }

    @Test
    fun testHistoryPull() {
        apiHelper.disconnectAllConversations()
        enterDeploymentInfo(testConfig.agentDisconnectDeploymentId)
        connect()
        sendMsg(helloText)
        val conversationInfo = apiHelper.answerNewConversation()
        if (conversationInfo == null) AssertionError("Unable to answer conversation.")
        else {
            Log.i(TAG, "Conversation started successfully.")
            apiHelper.sendConnectOrDisconnect(conversationInfo)
            // wait for agent to disconnect
            apiHelper.waitForParticipantToConnectOrDisconnect(conversationInfo.id)
            checkForReadOnly()
            // enter history and check for ConversationDisconnect and ConversationAutostart events
            history()
            // Just to clear things up, let's start a new chat, wait for configured
            startNewChat()
            val conversation2Info = apiHelper.answerNewConversation()
            if (conversation2Info == null) AssertionError("Unable to answer conversation.")
            else {
                Log.i(TAG, "Conversation started successfully.")
                apiHelper.sendConnectOrDisconnect(conversation2Info)
                // wait for agent to disconnect
                apiHelper.waitForParticipantToConnectOrDisconnect(conversation2Info.id)
            }
            bye()
        }
    }

    @Test
    fun test2AuthenticatedUser() {
        apiHelper.disconnectAllConversations()
        enterDeploymentInfo(testConfig.authDeploymentId)
        oktaSignInWithPKCE(testConfig.oktaUsername, testConfig.oktaPassword)
        authenticate()
        connect(authenticateConnectText)
        val conversationInfo = apiHelper.answerNewConversation()
        if (conversationInfo == null) AssertionError("Unable to answer conversation.")
        else {
            Log.i(TAG, "Conversation started successfully.")
            sendMsg(helloText)
            sleep(3000)
            apiHelper.sendOutboundMessageFromAgentToUser(conversationInfo, outboundMessage)
            verifyResponse(outboundMessage)
            apiHelper.sendConnectOrDisconnect(conversationInfo)
            oktaLogout()
            clearBrowser()
            oktaSignInWithPKCE(testConfig.oktaUser2name, testConfig.oktaPassword2)
            authenticate()
            connect(authenticateConnectText)
            val conversation2Info = apiHelper.answerNewConversation()
            if (conversation2Info == null) AssertionError("Unable to answer conversation.")
            else {
                Log.i(TAG, "Conversation started successfully.")
                sendMsg(helloText)
                sleep(3000)
                apiHelper.sendOutboundMessageFromAgentToUser(conversation2Info, outboundMessage)
                verifyResponse(outboundMessage)
                apiHelper.sendConnectOrDisconnect(conversation2Info)
            }
        }
        oktaLogout()
        clearBrowser()
    }

    @Test
    fun test1UnAuthenticatedUser() {
        enterDeploymentInfo(testConfig.authDeploymentId)
        // Enter an invalid password to see if noAuth will persist
        oktaSignInWithPKCE(fakeAuthUserName, fakeAuthPassword, false)
        verifyNotAuthenticated(notAuthenticateText)
    }
}
