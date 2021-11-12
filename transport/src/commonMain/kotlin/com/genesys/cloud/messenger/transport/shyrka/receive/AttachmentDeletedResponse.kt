package com.genesys.cloud.messenger.transport.shyrka.receive

import kotlinx.serialization.Serializable

@Serializable
internal data class AttachmentDeletedResponse(
    val attachmentId: String
)
