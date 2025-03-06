package com.genesys.cloud.messenger.transport.shyrka.receive

import com.genesys.cloud.messenger.transport.utility.DeploymentConfigValues

fun createDeploymentConfigForTesting(
    messenger: Messenger = createMessengerVOForTesting(),
) = DeploymentConfig(
    id = DeploymentConfigValues.ID,
    version = DeploymentConfigValues.VERSION,
    languages = listOf(
        DeploymentConfigValues.DEFAULT_LANGUAGE,
        DeploymentConfigValues.SECONDARY_LANGUAGE
    ),
    defaultLanguage = DeploymentConfigValues.DEFAULT_LANGUAGE,
    apiEndpoint = DeploymentConfigValues.API_ENDPOINT,
    messenger = messenger,
    journeyEvents = JourneyEvents(enabled = false),
    status = DeploymentConfigValues.Status,
    auth = Auth(
        enabled = true,
        allowSessionUpgrade = true,
    )
)

fun createMessengerVOForTesting(
    apps: Apps = Apps(createConversationsVOForTesting()),
    fileUpload: FileUpload = createFileUploadVOForTesting(),
) = Messenger(
    enabled = true,
    apps = apps,
    styles = Styles(primaryColor = DeploymentConfigValues.PRIMARY_COLOR),
    launcherButton = LauncherButton(visibility = DeploymentConfigValues.LAUNCHER_BUTTON_VISIBILITY),
    fileUpload = fileUpload
)

fun createFileUploadVOForTesting(
    enableAttachments: Boolean? = false,
    modes: List<Mode> = listOf(
        Mode(
            fileTypes = listOf(DeploymentConfigValues.FILE_TYPE),
            maxFileSizeKB = DeploymentConfigValues.MAX_FILE_SIZE,
        )
    ),
): FileUpload {
    return FileUpload(enableAttachments, modes)
}

fun createConversationsVOForTesting(
    autoStart: Conversations.AutoStart = Conversations.AutoStart(),
    conversationDisconnect: Conversations.ConversationDisconnect = Conversations.ConversationDisconnect(),
    conversationClear: Conversations.ConversationClear = Conversations.ConversationClear(enabled = true),
): Conversations = Conversations(
    messagingEndpoint = DeploymentConfigValues.MESSAGING_ENDPOINT,
    autoStart = autoStart,
    conversationDisconnect = conversationDisconnect,
    conversationClear = conversationClear,
)
