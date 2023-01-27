package com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genesys.cloud.messenger.androidcomposeprototype.ui.theme.WebMessagingTheme
import com.genesys.cloud.messenger.transport.core.MessagingClient

@Composable
fun TestBedScreen(testBedViewModel: TestBedViewModel) {
    WebMessagingTheme {
        Surface(color = MaterialTheme.colors.background) {
            TestBedContent(
                command = testBedViewModel.command,
                onCommandChange = testBedViewModel::onCommandChanged,
                onCommandSend = testBedViewModel::onCommandSend,
                clientState = testBedViewModel.clientState,
                socketMessage = testBedViewModel.socketMessage,
                commandWaiting = testBedViewModel.commandWaiting,
            )
        }
    }
}

@Composable
fun TestBedContent(
    command: String,
    onCommandChange: (String) -> Unit,
    onCommandSend: () -> Unit,
    clientState: MessagingClient.State,
    socketMessage: String,
    commandWaiting: Boolean,
) {
    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        val typography = MaterialTheme.typography
        Text(
            "Web Messaging Testbed",
            style = typography.h5
        )
        Text(
            "Commands: connect, newChat, send <msg>, history, clearConversation, attach, detach, delete <attachmentId> , deployment, bye, healthCheck, addAttribute <key> <value>, typing",
            style = typography.caption,
            softWrap = true
        )
        CommandView(command, onCommandChange, onCommandSend, commandWaiting)
        Spacer(modifier = Modifier.height(16.dp))
        ConnectionStateView(clientState)
        SocketMessageView(socketMessage)
    }
}

@Composable
private fun CommandView(
    command: String,
    onCommandChange: (String) -> Unit,
    onCommandSend: () -> Unit,
    commandWaiting: Boolean,
) {
    OutlinedTextField(
        value = command,
        onValueChange = { onCommandChange(it) },
        label = { Text("Command") },
        keyboardOptions = KeyboardOptions(
            autoCorrect = false,
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(onSend = { onCommandSend() }),
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            if (commandWaiting) {
                CircularProgressIndicator()
            } else {
                TextButton(onClick = { onCommandSend() }) {
                    Text("Send")
                }
            }
        }
    )
}

@Composable
private fun ConnectionStateView(clientState: MessagingClient.State) {
    OutlinedTextField(
        value = clientState.javaClass.simpleName,
        onValueChange = { /* no-op */ },
        label = { Text("Client") },
        readOnly = true,
        textStyle = MaterialTheme.typography.body2,
        modifier = Modifier
            .fillMaxWidth()
    )
}

@Composable
private fun SocketMessageView(socketMessage: String) {
    OutlinedTextField(
        value = socketMessage,
        onValueChange = { /* no-op */ },
        label = { Text("Response") },
        readOnly = true,
        textStyle = MaterialTheme.typography.body2,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = ScrollState(0)),
    )
}

@Preview
@Composable
fun PreviewTestBedScreen() {
    TestBedScreen(TestBedViewModel())
}
