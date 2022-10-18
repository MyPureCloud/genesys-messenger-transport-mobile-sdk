package com.genesys.cloud.messenger.transport.shyrka.receive

fun testDeploymentConfig(
    messenger: Messenger = testMessenger()
) = DeploymentConfig(
    id = "id",
    version = "1",
    languages = listOf("en-us", "zh-cn"),
    defaultLanguage = "en-us",
    apiEndpoint = "api_endpoint",
    messenger = messenger,
    journeyEvents = JourneyEvents(enabled = false),
    status = DeploymentConfig.Status.Active,
)

fun testMessenger(
    apps: Apps = Apps(testConversations())
) = Messenger(
    enabled = true,
    apps = apps,
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

fun testConversations(
    autoStart: Conversations.AutoStart = Conversations.AutoStart()
): Conversations = Conversations(
    messagingEndpoint = "messaging_endpoint",
    autoStart = autoStart,
)
