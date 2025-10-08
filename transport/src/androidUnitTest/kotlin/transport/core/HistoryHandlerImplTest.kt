package transport.core

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.HistoryHandlerImpl
import com.genesys.cloud.messenger.transport.core.MessageStore
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.EventHandler
import com.genesys.cloud.messenger.transport.core.toCorrectiveAction
import com.genesys.cloud.messenger.transport.network.TestWebMessagingApiResponses
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.util.extensions.toMessageList
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.utility.InvalidValues
import com.genesys.cloud.messenger.transport.utility.TestValues
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HistoryHandlerImplTest {

    private val messageStore = mockk<MessageStore>(relaxed = true)
    private val api = mockk<WebMessagingApi>(relaxed = true)
    private val eventHandler = mockk<EventHandler>(relaxed = true)
    private val log = mockk<Log>(relaxed = true)
    private val tokenProvider = mockk<suspend () -> String>()
    private val logSlot = mutableListOf<() -> String>()
    private val subject = HistoryHandlerImpl(
        messageStore = messageStore,
        api = api,
        eventHandler = eventHandler,
        log = log,
        tokenProvider = tokenProvider
    )

    @Test
    fun `when all history fetched`() =
        runTest {
            every { messageStore.startOfConversation } returns true

            subject.fetchNextPage()

            verify { log.i(capture(logSlot)) }
            verify { messageStore.updateMessageHistory(emptyList(), any()) }
            assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.ALL_HISTORY_FETCHED)
            assertThat(logSlot.size).isEqualTo(1)
        }

    @Test
    fun `when fetch history success`() =
        runTest {
            val token = TestValues.TOKEN
            val pageNumber = 1
            val response = TestWebMessagingApiResponses.testMessageEntityList

            every { messageStore.startOfConversation } returns false
            every { messageStore.nextPage } returns pageNumber
            coEvery { tokenProvider() } returns token
            coEvery { api.getMessages(token, pageNumber) } returns Result.Success(response)

            subject.fetchNextPage()

            verify { log.i(capture(logSlot)) }
            verify { messageStore.updateMessageHistory(response.entities.toMessageList(), response.total) }
            assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.fetchingHistory(pageNumber))
        }

    @Test
    fun `when fetch history fails with cancellation error`() =
        runTest {
            val token = TestValues.TOKEN
            val pageNumber = 1
            val errorMessage = InvalidValues.CANCELLATION_EXCEPTION

            every { messageStore.startOfConversation } returns false
            every { messageStore.nextPage } returns pageNumber
            coEvery { tokenProvider() } returns token
            coEvery { api.getMessages(token, pageNumber) } returns Result.Failure(ErrorCode.CancellationError, errorMessage)

            subject.fetchNextPage()

            verify { log.i(capture(logSlot)) }
            verify { log.w(capture(logSlot)) }
            verify(exactly = 0) { eventHandler.onEvent(any()) }
            assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.fetchingHistory(pageNumber))
            assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.CANCELLATION_EXCEPTION_GET_MESSAGES)
        }

    @Test
    fun `when fetch history fails with other error`() =
        runTest {
            val token = "test-token"
            val pageNumber = 1
            val errorCode = ErrorCode.ServerResponseError(500)
            val errorMessage = "Server error"
            val failure = Result.Failure(errorCode, errorMessage)

            every { messageStore.startOfConversation } returns false
            every { messageStore.nextPage } returns pageNumber
            coEvery { tokenProvider() } returns token
            coEvery { api.getMessages(token, pageNumber) } returns failure

            subject.fetchNextPage()

            verify { log.i(capture(logSlot)) }
            verify { log.w(capture(logSlot)) }
            verify {
                eventHandler.onEvent(
                    Event.Error(
                        ErrorCode.HistoryFetchFailure,
                        errorMessage,
                        errorCode.toCorrectiveAction()
                    )
                )
            }
            assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.fetchingHistory(pageNumber))
            assertThat(logSlot[1].invoke()).isEqualTo(LogMessages.historyFetchFailed(failure))
        }
}
