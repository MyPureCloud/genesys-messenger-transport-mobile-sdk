package com.genesys.cloud.messenger.transport.core

import assertk.Assert
import assertk.assertions.isEqualTo

internal fun Assert<CustomAttributesStoreImpl.State>.isPending() = this.isEqualTo(CustomAttributesStoreImpl.State.PENDING)

internal fun Assert<CustomAttributesStoreImpl.State>.isSending() = this.isEqualTo(CustomAttributesStoreImpl.State.SENDING)

internal fun Assert<CustomAttributesStoreImpl.State>.isSent() = this.isEqualTo(CustomAttributesStoreImpl.State.SENT)

internal fun Assert<CustomAttributesStoreImpl.State>.isError() = this.isEqualTo(CustomAttributesStoreImpl.State.ERROR)
