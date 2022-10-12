package com.genesys.cloud.messenger.transport.shyrka.receive

fun fakeDeploymentConfig() = DeploymentConfig(
    id = "id",
    version = "1",
    languages = listOf("en-us", "zh-cn"),
    defaultLanguage = "en-us",
    apiEndpoint = "api_endpoint",
    messenger = fakeMessenger,
    journeyEvents = JourneyEvents(enabled = false),
    status = DeploymentConfig.Status.Active,
)

private val fakeMessenger = Messenger(
    enabled = true,
    apps = Apps(
        conversations = Conversations(
            messagingEndpoint = "messaging_endpoint",
            showAgentTypingIndicator = true,
            showUserTypingIndicator = true,
        )
    ),
    styles = Styles(primaryColor = "red"),
    launcherButton = LauncherButton(visibility = "On"),
    fileUpload = FileUpload(
        listOf(
            Mode(
                fileTypes = listOf("png"),
                maxFileSizeKB = 100,
            ),
        )
    )
)
