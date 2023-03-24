package com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesys.cloud.messenger.androidcomposeprototype.BuildConfig
import com.genesys.cloud.messenger.androidcomposeprototype.util.getSharedPreferences
import com.genesys.cloud.messenger.transport.core.Attachment.State.Detached
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.core.MessageEvent
import com.genesys.cloud.messenger.transport.core.MessageEvent.AttachmentUpdated
import com.genesys.cloud.messenger.transport.core.MessagingClient
import com.genesys.cloud.messenger.transport.core.MessagingClient.State
import com.genesys.cloud.messenger.transport.core.MessengerTransport
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.model.AuthJwt
import com.genesys.cloud.messenger.transport.util.DefaultTokenStore
import io.ktor.http.URLBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val ATTACHMENT_FILE_NAME = "test_asset.png"

class TestBedViewModel : ViewModel(), CoroutineScope {

    override val coroutineContext = Dispatchers.IO + Job()

    private val TAG = TestBedViewModel::class.simpleName

    private lateinit var messengerTransport: MessengerTransport
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
    var region: String by mutableStateOf("inintca.com")
        private set
    var authState: AuthState by mutableStateOf(AuthState.NoAuth)
        private set
    var pkceEnabled by mutableStateOf(false)

    val regions = listOf("inindca.com", "inintca.com", "mypurecloud.com")
    private val customAttributes = mutableMapOf<String, String>()
    private lateinit var onOktaSingIn: (url: String) -> Unit
    private lateinit var onOktaLogout: () -> Unit

    suspend fun init(
        context: Context,
        onOktaSignIn: (url: String) -> Unit,
        onOktaLogout: () -> Unit,
    ) {
        this.onOktaSingIn = onOktaSignIn
        this.onOktaLogout = onOktaLogout
        val mmsdkConfiguration = Configuration(
            deploymentId = deploymentId.ifEmpty { BuildConfig.DEPLOYMENT_ID },
            domain = region.ifEmpty { BuildConfig.DEPLOYMENT_DOMAIN },
            logging = true
        )
        DefaultTokenStore.context = context
        messengerTransport = MessengerTransport(mmsdkConfiguration)
        client = messengerTransport.createMessagingClient()
        with(client) {
            stateChangedListener = {
                runBlocking {
                    onClientStateChanged(
                        oldState = it.oldState,
                        newState = it.newState,
                    )
                }
            }
            messageListener = { onMessage(it) }
            eventListener = { onEvent(it) }
            clientState = client.currentState
        }
        withContext(Dispatchers.IO) {
            context.assets.open(ATTACHMENT_FILE_NAME).use { inputStream ->
                inputStream.readBytes().also { attachment = it }
            }
        }

        val authCode = context.getSharedPreferences().getString("authCode", null)
        viewModelScope.launch {
            authState = if (authCode != null) {
                onSocketMessageReceived("AuthCode: $authCode")
                AuthState.AuthCodeReceived(authCode)
            } else {
                AuthState.NoAuth
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
        val command = components.firstOrNull()
        val input = components.getOrNull(1) ?: ""
        when (command) {
            "connect" -> doConnect()
            "connectAuthenticated" -> doConnectAuthenticated()
            "bye" -> doDisconnect()
            "send" -> doSendMessage(input)
            "history" -> fetchNextPage()
            "healthCheck" -> doSendHealthCheck()
            "attach" -> doAttach()
            "detach" -> doDetach(input)
            "deployment" -> doDeployment()
            "clearConversation" -> doClearConversation()
            "addAttribute" -> doAddCustomAttributes(input)
            "typing" -> doIndicateTyping()
            "oktaSignIn" -> doOktaSignIn(false)
            "oktaSignInWithPKCE" -> doOktaSignIn(true)
            "oktaLogout" -> doOktaLogout()
            "fetchAuthJwt" -> doFetchAuthJwt()
            else -> {
                Log.e(TAG, "Invalid command")
                withContext(Dispatchers.Main) {
                    commandWaiting = false
                }
            }
        }
    }

    private fun doOktaSignIn(withPKCE: Boolean) {
        pkceEnabled = withPKCE
        onOktaSingIn(buildOktaAuthorizeUrl())
        commandWaiting = false
    }

    private suspend fun doDeployment() {
        try {
            onSocketMessageReceived(
                messengerTransport.fetchDeploymentConfig().toString()
            )
        } catch (t: Throwable) {
            handleException(t, "fetch deployment config")
        }
    }

    private suspend fun doOktaLogout() {
        try {
            if (authState is AuthState.JwtFetched) {
                (authState as AuthState.JwtFetched).authJwt.let {
                    client.logoutFromAuthenticatedSession()
                    onOktaLogout()
                    authState = AuthState.LoggedOut
                }
                commandWaiting = false
            } else {
                throw IllegalStateException("Can not perform logout. Auth jwt was not fetched. Please, fetch and try again.")
            }
        } catch (t: Throwable) {
            handleException(t, "okta logout")
        }
    }

    private suspend fun doConnect() {
        try {
            client.connect()
        } catch (t: Throwable) {
            handleException(t, "connect")
        }
    }

    private suspend fun doConnectAuthenticated() {
        try {
            if (authState is AuthState.JwtFetched) {
                client.connectAuthenticatedSession((authState as AuthState.JwtFetched).authJwt)
            } else {
                throw IllegalStateException("Auth jwt was not fetched. Please, fetch and try again.")
            }
        } catch (t: Throwable) {
            handleException(t, "connectAuthenticated")
        }
    }

    private suspend fun doDisconnect() {
        try {
            client.disconnect()
        } catch (t: Throwable) {
            handleException(t, "disconnect")
        }
    }

    private suspend fun doSendMessage(message: String) {
        try {
            client.sendMessage(message, customAttributes = customAttributes)
            customAttributes.clear()
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
            commandWaiting = false
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

    private suspend fun doDetach(attachmentId: String) {
        try {
            client.detach(attachmentId)
        } catch (t: Throwable) {
            handleException(t, "detach")
        }
    }

    private suspend fun doClearConversation() {
        client.invalidateConversationCache()
        clearCommand()
    }

    private suspend fun doAddCustomAttributes(customAttributes: String) {
        clearCommand()
        val keyValue = customAttributes.toKeyValuePair()
        val consoleMessage = if (keyValue.first.isNotEmpty()) {
            this.customAttributes[keyValue.first] = keyValue.second
            "Custom attribute added: $keyValue"
        } else {
            "Custom attribute key can not be null or empty!"
        }
        onSocketMessageReceived(consoleMessage)
    }

    private suspend fun doIndicateTyping() {
        try {
            client.indicateTyping()
            commandWaiting = false
        } catch (t: Throwable) {
            handleException(t, "indicate typing.")
        }
    }

    private suspend fun doFetchAuthJwt() {
        if (authState is AuthState.AuthCodeReceived) {
            try {
                client.fetchAuthJwt(
                    authCode = (authState as AuthState.AuthCodeReceived).authCode,
                    redirectUri = BuildConfig.SIGN_IN_REDIRECT_URI,
                    codeVerifier = if (pkceEnabled) BuildConfig.CODE_VERIFIER else null,
                ).let {
                    authState = AuthState.JwtFetched(it)
                    onSocketMessageReceived("AuthJwt: $it")
                }
            } catch (t: Throwable) {
                handleException(t, "failed to fetch auth jwt.")
                authState = AuthState.Error(t.cause, t.message)
            }
        } else {
            onSocketMessageReceived("Please, first obtain authCode from login.")
        }

        commandWaiting = false
    }

    private suspend fun onClientStateChanged(oldState: State, newState: State) {
        Log.v(TAG, "onClientStateChanged(oldState = $oldState, newState = $newState)")
        clientState = newState
        val statePayloadMessage = when (newState) {
            is State.Configured -> "connected: ${newState.connected}, newSession: ${newState.newSession}, wasReconnecting: ${oldState is State.Reconnecting}"
            is State.Closing -> "code: ${newState.code}, reason: ${newState.reason}"
            is State.Closed -> "code: ${newState.code}, reason: ${newState.reason}"
            is State.Error -> "code: ${newState.code}, message: ${newState.message}"
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

    private fun onMessage(event: MessageEvent) {
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
                    else -> event.attachment.toString()
                }
            }
            else -> event.toString()
        }
        launch {
            onSocketMessageReceived(eventMessage)
        }
    }

    private fun onEvent(event: Event) {
        launch { onSocketMessageReceived(event.toString()) }
    }

    private fun buildOktaAuthorizeUrl(): String {
        val builder =
            URLBuilder("https://${BuildConfig.OKTA_DOMAIN}/oauth2/default/v1/authorize").apply {
                parameters.append("client_id", BuildConfig.CLIENT_ID)
                parameters.append("response_type", "code")
                parameters.append("scope", "openid profile")
                parameters.append("redirect_uri", BuildConfig.SIGN_IN_REDIRECT_URI)
                parameters.append("state", BuildConfig.OKTA_STATE)
                if (pkceEnabled) {
                    parameters.append("code_challenge_method", BuildConfig.CODE_CHALLENGE_METHOD)
                    parameters.append("code_challenge", BuildConfig.CODE_CHALLENGE)
                }
            }
        return builder.build().toString()
    }
}

private fun String.toKeyValuePair(): Pair<String, String> {
    return this.split(" ", limit = 2).run {
        val key = firstOrNull() ?: ""
        val value = getOrNull(1) ?: ""
        Pair(key, value)
    }
}

sealed class AuthState {
    object NoAuth : AuthState()
    data class AuthCodeReceived(val authCode: String) : AuthState()
    data class Error(val cause: Throwable? = null, val message: String? = null) : AuthState()
    data class JwtFetched(val authJwt: AuthJwt) : AuthState()
    object LoggedOut : AuthState()
}
