package com.genesys.cloud.messenger.transport.shyrka.receive

import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations.AutoStart
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations.ConversationClear
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations.ConversationDisconnect
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations.Notifications
import com.genesys.cloud.messenger.transport.utility.DeploymentConfigValues

fun createDeploymentConfigForTesting(
    messenger: Messenger = createMessengerVOForTesting(),
) = DeploymentConfig(
    id = DeploymentConfigValues.Id,
    version = DeploymentConfigValues.Version,
    languages = listOf(
        DeploymentConfigValues.DefaultLanguage,
        DeploymentConfigValues.SecondaryLanguage
    ),
    defaultLanguage = DeploymentConfigValues.DefaultLanguage,
    apiEndpoint = DeploymentConfigValues.ApiEndPoint,
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
    styles = Styles(primaryColor = DeploymentConfigValues.PrimaryColor),
    launcherButton = LauncherButton(visibility = DeploymentConfigValues.LauncherButtonVisibility),
    fileUpload = fileUpload
)

fun createFileUploadVOForTesting(
    enableAttachments: Boolean? = false,
    modes: List<Mode> = listOf(
        Mode(
            fileTypes = listOf(DeploymentConfigValues.FileType),
            maxFileSizeKB = DeploymentConfigValues.MaxFileSize,
        )
    ),
): FileUpload {
    return FileUpload(enableAttachments, modes)
}

fun createConversationsVOForTesting(
    autoStart: AutoStart = AutoStart(),
    conversationDisconnect: ConversationDisconnect = ConversationDisconnect(),
    conversationClear: ConversationClear = ConversationClear(enabled = true),
    notifications: Notifications = Notifications(enabled = true, notificationContentType = Notifications.NotificationContentType.IncludeMessagesContent),
): Conversations = Conversations(
    messagingEndpoint = DeploymentConfigValues.MessagingEndpoint,
    autoStart = autoStart,
    conversationDisconnect = conversationDisconnect,
    conversationClear = conversationClear,
    notifications = notifications,
)
