package com.genesys.cloud.messenger.transport.core

internal interface HistoryHandler {
    suspend fun fetchNextPage()
}
