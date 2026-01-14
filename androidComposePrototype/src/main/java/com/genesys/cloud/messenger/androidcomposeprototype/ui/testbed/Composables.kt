package com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genesys.cloud.messenger.transport.core.MessagingClient
import kotlinx.coroutines.launch

@Composable
fun TestBedScreen(testBedViewModel: TestBedViewModel) {
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Web Messaging Testbed") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                scaffoldState.drawerState.open()
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        drawerContent = {
            DrawerContent { selectedCommand ->
                testBedViewModel.onCommandChanged(selectedCommand)
                coroutineScope.launch {
                    scaffoldState.drawerState.close()
                }
            }
        }
    ) { innerPadding ->
        TestBedContent(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                        )
                    ),
            command = testBedViewModel.command,
            onCommandChange = testBedViewModel::onCommandChanged,
            onCommandSend = testBedViewModel::onCommandSend,
            clientState = testBedViewModel.clientState,
            socketMessage = testBedViewModel.socketMessage,
            commandWaiting = testBedViewModel.commandWaiting,
            currentAuthState = testBedViewModel.authState,
        )
    }
}

@Composable
fun TestBedContent(
    modifier: Modifier,
    command: String,
    onCommandChange: (String) -> Unit,
    onCommandSend: () -> Unit,
    clientState: MessagingClient.State,
    socketMessage: String,
    commandWaiting: Boolean,
    currentAuthState: AuthState,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(8.dp)
    ) {
        CommandView(command, onCommandChange, onCommandSend, commandWaiting)
        Spacer(modifier = Modifier.height(16.dp))
        ConnectionStateView(clientState)
        AuthStateView(currentAuthState)
        SocketMessageView(socketMessage)
    }
}

@Composable
fun DrawerContent(onCommandSelected: (String) -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
    ) {
        Text("Commands", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(enumValues<Command>()) { command ->
                TextButton(onClick = { onCommandSelected(command.description) }) {
                    Text(command.description, style = MaterialTheme.typography.body2)
                }
                Divider()
            }
        }
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
        keyboardOptions =
            KeyboardOptions(
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
        modifier =
            Modifier
                .fillMaxWidth()
    )
}

@Composable
fun AuthStateView(authState: AuthState) {
    OutlinedTextField(
        value = " ${authState.javaClass.simpleName}",
        onValueChange = { /* no-op */ },
        label = { Text("Auth state") },
        readOnly = true,
        textStyle = MaterialTheme.typography.body2,
        modifier =
            Modifier
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
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(state = ScrollState(0)),
    )
}

@Preview
@Composable
fun PreviewTestBedScreen() {
    TestBedScreen(TestBedViewModel())
}
