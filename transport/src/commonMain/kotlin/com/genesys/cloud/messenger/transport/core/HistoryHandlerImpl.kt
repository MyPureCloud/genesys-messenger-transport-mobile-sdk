package com.genesys.cloud.messenger.transport.core

import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.MessageEntityList
import com.genesys.cloud.messenger.transport.util.extensions.toMessageList
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages

internal class HistoryHandlerImpl(
    private val messageStore: MessageStore,
    private val api: WebMessagingApi,
    private val eventHandler: EventHandler,
    private val log: Log,
    private val tokenProvider: suspend () -> String,
) : HistoryHandler {

    override suspend fun fetchNextPage() {
        if (messageStore.startOfConversation) {
            handleStartOfConversation()
            return
        }

        fetchHistoryPage()
    }

    private fun handleStartOfConversation() {
        log.i { LogMessages.ALL_HISTORY_FETCHED }
        messageStore.updateMessageHistory(emptyList(), messageStore.getConversation().size)
    }

    private suspend fun fetchHistoryPage() {
        log.i { LogMessages.fetchingHistory(messageStore.nextPage) }
        val token = tokenProvider()
        val result = api.getMessages(token, messageStore.nextPage)
        handleApiResult(result)
    }

    private fun handleApiResult(result: Result<MessageEntityList>) {
        when (result) {
            is Result.Success -> handleSuccess(result)
            is Result.Failure -> handleFailure(result)
        }
    }

    private fun handleSuccess(result: Result.Success<MessageEntityList>) {
        messageStore.updateMessageHistory(
            result.value.entities.toMessageList(),
            result.value.total,
        )
    }

    private fun handleFailure(result: Result.Failure) {
        if (result.errorCode is ErrorCode.CancellationError) {
            log.w { LogMessages.CANCELLATION_EXCEPTION_GET_MESSAGES }
            return
        }

        log.w { LogMessages.historyFetchFailed(result) }
        eventHandler.onEvent(
            Event.Error(
                ErrorCode.HistoryFetchFailure,
                result.message,
                result.errorCode.toCorrectiveAction()
            )
        )
    }
}
