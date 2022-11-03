package com.genesys.cloud.messenger.transport.shyrka.receive

fun createDeploymentConfigForTesting(
    messenger: Messenger = createMessengerVOForTesting()
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

fun createMessengerVOForTesting(
    apps: Apps = Apps(createConversationsVOForTesting())
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

fun createConversationsVOForTesting(
    autoStart: Conversations.AutoStart = Conversations.AutoStart()
): Conversations = Conversations(
    messagingEndpoint = "messaging_endpoint",
    autoStart = autoStart,
)