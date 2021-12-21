package com.genesys.cloud.messenger.transport.shyrka.receive

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeploymentConfig(
    val id: String,
    val version: String,
    val languages: List<String>,
    val defaultLanguage: String,
    val apiEndpoint: String,
    val messenger: Messenger,
    val journeyEvents: JourneyEvents,
    val status: Status,
)

@Serializable
data class Messenger(
    val enabled: Boolean,
    val apps: Apps,
    val styles: Styles,
    val launcherButton: LauncherButton,
    val fileUpload: FileUpload,
)

@Serializable
data class Apps(val conversations: Conversations)

@Serializable
data class Conversations(val messagingEndpoint: String)

@Serializable
data class Styles(val primaryColor: String)

@Serializable
data class LauncherButton(val visibility: String)

@Serializable
data class FileUpload(val modes: List<Mode>)

@Serializable
data class Mode(
    val fileTypes: List<String>,
    val maxFileSizeKB: Long,
)

@Serializable
data class JourneyEvents(val enabled: Boolean)

enum class Status {
    @SerialName("Active")
    Active,
    @SerialName("Inactive")
    Inactive,
}
