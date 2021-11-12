package com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.genesys.cloud.messenger.androidcomposeprototype.BuildConfig
import com.genesys.cloud.messenger.transport.Attachment.State.Deleted
import com.genesys.cloud.messenger.transport.Attachment.State.Detached
import com.genesys.cloud.messenger.transport.Configuration
import com.genesys.cloud.messenger.transport.MessageEvent
import com.genesys.cloud.messenger.transport.MessageEvent.AttachmentUpdated
import com.genesys.cloud.messenger.transport.MessagingClient
import com.genesys.cloud.messenger.transport.MessagingClient.State
import com.genesys.cloud.messenger.transport.util.MessageListener
import com.genesys.cloud.messenger.transport.util.MobileMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val ATTACHMENT_FILE_NAME = "test_asset.png"

class TestBedViewModel : ViewModel(), CoroutineScope, MessageListener {

    override val coroutineContext = Dispatchers.IO + Job()

    private val TAG = TestBedViewModel::class.simpleName

    private lateinit var client: MessagingClient
    private lateinit var attachment: ByteArray
    private val attachedIds = mutableListOf<String>()

    var command: String by mutableStateOf("")
        private set
    var commandWaiting: Boolean by mutableStateOf(false)
        private set
    var socketMessage: String by mutableStateOf("")
        private set
    var clientState: State by mutableStateOf(State.Idle)
        private set
    var deploymentId: String by mutableStateOf("")
        private set
    var region: String by mutableStateOf("inindca")
        private set

    val regions = listOf("inindca")

    suspend fun init(context: Context) {
        val defaultDeploymentId = BuildConfig.DEPLOYMENT_ID

        val deploymentIdToUse = if (deploymentId.isNotBlank()) {
            deploymentId
        } else defaultDeploymentId

        val mmsdkConfiguration = Configuration(
            deploymentId = deploymentIdToUse,
            domain = BuildConfig.DEPLOYMENT_DOMAIN,
            tokenStoreKey = "com.genesys.cloud.messenger",
            logging = true
        )
        client = MobileMessenger.createMessagingClient(
            context = context,
            configuration = mmsdkConfiguration,
            listener = this,
        )
        with(client) {
            stateListener = { runBlocking { onClientState(it) } }
            clientState = client.currentState
        }
        withContext(Dispatchers.IO) {
            context.assets.open(ATTACHMENT_FILE_NAME).use { inputStream ->
                inputStream.readBytes().also { attachment = it }
            }
        }
    }

    fun onCommandChanged(newCommand: String) {
        command = newCommand
    }

    fun onDeploymentIdChanged(newDeploymentId: String) {
        deploymentId = newDeploymentId
    }

    fun onRegionChanged(newRegion: String) {
        region = newRegion
    }

    fun onCommandSend() = launch(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            commandWaiting = true
        }
        val components = command.split(" ", limit = 2)
        when (components.firstOrNull()) {
            "connect" -> doConnect()
            "bye" -> doDisconnect()
            "configure" -> doConfigureSession()
            "send" -> doSendMessage(components)
            "history" -> fetchNextPage()
            "healthCheck" -> doSendHealthCheck()
            "attach" -> doAttach()
            "detach" -> doDetach()
            "delete" -> doDeleteAttachment(components)
            "deployment" -> doDeployment()
            else -> {
                Log.e(TAG, "Invalid command")
                withContext(Dispatchers.Main) {
                    commandWaiting = false
                }
            }
        }
    }

    private suspend fun doDeployment() {
        try {
            onSocketMessageReceived(client.fetchDeploymentConfig().toString())
        } catch (t: Throwable) {
            handleException(t, "fetch deployment config")
        }
    }

    private suspend fun doConnect() {
        try {
            client.connect()
        } catch (t: Throwable) {
            handleException(t, "connect")
        }
    }

    private suspend fun doDisconnect() {
        try {
            client.disconnect()
        } catch (t: Throwable) {
            handleException(t, "disconnect")
        }
    }

    private suspend fun doConfigureSession() {
        try {
            client.configureSession(
                email = "peter.parker@marvel.com",
                phoneNumber = null,
                firstName = "Peter",
                lastName = "Parker"
            )
        } catch (t: Throwable) {
            handleException(t, "configure session")
        }
    }

    private suspend fun doSendMessage(components: List<String>) {
        try {
            val message = components.getOrNull(1) ?: ""
            client.sendMessage(message)
        } catch (t: Throwable) {
            handleException(t, "send message")
        }
    }

    private suspend fun fetchNextPage() {
        try {
            client.fetchNextPage()
            commandWaiting = false
        } catch (t: Throwable) {
            handleException(t, "request history")
        }
    }

    private suspend fun doSendHealthCheck() {
        try {
            client.sendHealthCheck()
        } catch (t: Throwable) {
            handleException(t, "send health check")
        }
    }

    private suspend fun doAttach() {
        try {
            client.attach(
                attachment,
                ATTACHMENT_FILE_NAME
            ) { progress -> println("Attachment upload progress: $progress") }.also {
                attachedIds.add(it)
            }
        } catch (t: Throwable) {
            handleException(t, "attach")
        }
    }

    private suspend fun doDetach() {
        try {
            if (attachedIds.isEmpty()) {
                socketMessage = "No attachments to detach"
                commandWaiting = false
                return
            }
            client.detach(attachmentId = attachedIds.first())
        } catch (t: Throwable) {
            handleException(t, "detach")
        }
    }

    private suspend fun doDeleteAttachment(components: List<String>) {
        try {
            val attachmentId = components.getOrNull(1) ?: ""
            client.deleteAttachment(attachmentId)
        } catch (t: Throwable) {
            handleException(t, "detach")
        }
    }

    private suspend fun onClientState(state: State) {
        Log.v(TAG, "onClientState(state = $state)")
        clientState = state
        val statePayloadMessage = when (state) {
            is State.Configured -> "connected: ${state.connected}, newSession: ${state.newSession}"
            is State.Closing -> "code: ${state.code}, reason: ${state.reason}"
            is State.Closed -> "code: ${state.code}, reason: ${state.reason}"
            is State.Error -> "code: ${state.code}, message: ${state.message}"
            else -> ""
        }
        onSocketMessageReceived(statePayloadMessage)
        withContext(Dispatchers.Main) {
            commandWaiting = false
        }
    }

    private suspend fun onSocketMessageReceived(message: String) {
        Log.v(TAG, "onSocketMessageReceived(message = $message)")
        clearCommand()
        withContext(Dispatchers.Main) {
            socketMessage = message
        }
    }

    private suspend fun clearCommand() {
        withContext(Dispatchers.Main) {
            command = ""
            commandWaiting = false
        }
    }

    private suspend fun handleException(t: Throwable, action: String) {
        val failMessage = "Failed to $action"
        Log.e(TAG, failMessage, t)
        onSocketMessageReceived(t.message ?: failMessage)
        withContext(Dispatchers.Main) {
            commandWaiting = false
        }
    }

    override fun onEvent(event: MessageEvent) {
        val eventMessage = when (event) {
            is MessageEvent.MessageUpdated -> event.message.toString()
            is MessageEvent.MessageInserted -> event.message.toString()
            is MessageEvent.HistoryFetched -> "start of conversation: ${event.startOfConversation}, messages: ${event.messages}"
            is AttachmentUpdated -> {
                when (event.attachment.state) {
                    Detached -> {
                        attachedIds.remove(event.attachment.id)
                        event.attachment.toString()
                    }
                    Deleted -> {
                        attachedIds.remove(event.attachment.id)
                        event.attachment.toString()
                    }
                    else -> event.attachment.toString()
                }
            }
            else -> event.toString()
        }
        launch {
            onSocketMessageReceived(eventMessage)
        }
    }
}
