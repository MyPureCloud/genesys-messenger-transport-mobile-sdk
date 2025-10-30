package transport.core.events

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.CorrectiveAction
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.events.Event
import com.genesys.cloud.messenger.transport.core.events.Event.AgentTyping
import com.genesys.cloud.messenger.transport.core.events.Event.Authorized
import com.genesys.cloud.messenger.transport.core.events.Event.ConnectionClosed
import com.genesys.cloud.messenger.transport.core.events.Event.ConversationAutostart
import com.genesys.cloud.messenger.transport.core.events.Event.ConversationCleared
import com.genesys.cloud.messenger.transport.core.events.Event.ConversationDisconnect
import com.genesys.cloud.messenger.transport.core.events.Event.Error
import com.genesys.cloud.messenger.transport.core.events.Event.ExistingAuthSessionCleared
import com.genesys.cloud.messenger.transport.core.events.Event.HealthChecked
import com.genesys.cloud.messenger.transport.core.events.Event.Logout
import com.genesys.cloud.messenger.transport.core.events.Event.SignedIn
import com.genesys.cloud.messenger.transport.core.events.EventHandlerImpl
import com.genesys.cloud.messenger.transport.core.events.toTransportEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.PresenceEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessage
import com.genesys.cloud.messenger.transport.shyrka.receive.StructuredMessageEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TypingEvent
import com.genesys.cloud.messenger.transport.shyrka.receive.TypingEvent.Typing
import com.genesys.cloud.messenger.transport.util.logs.Log
import com.genesys.cloud.messenger.transport.util.logs.LogMessages
import com.genesys.cloud.messenger.transport.util.logs.LogTag
import com.genesys.cloud.messenger.transport.utility.ErrorTest
import com.genesys.cloud.messenger.transport.utility.MessageValues
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertNull

class EventHandlerTest {
    internal val mockLogger: Log = mockk(relaxed = true)
    internal val logSlot = mutableListOf<() -> String>()
    private val eventSlot = mutableListOf<Event>()
    private val mockEventListener: ((Event) -> Unit) = mockk(relaxed = true)
    private val subject =
        EventHandlerImpl(mockLogger).also {
            it.eventListener = mockEventListener
        }

    @Test
    fun `when onEvent()`() {
        val events =
            listOf(
                AgentTyping(3000),
                HealthChecked,
                Error(
                    errorCode = ErrorCode.ClientResponseError(403),
                    message = "some message",
                    correctiveAction = CorrectiveAction.Forbidden,
                ),
                ConversationAutostart,
                ConversationDisconnect,
                ConnectionClosed(ConnectionClosed.Reason.UserSignedIn),
                Authorized,
                Logout,
                ConversationCleared,
                SignedIn(MessageValues.PARTICIPANT_NAME, MessageValues.PARTICIPANT_LAST_NAME),
                ExistingAuthSessionCleared,
            )

        events.forEach {
            subject.onEvent(it)

            verify {
                mockLogger.i(capture(logSlot))
                mockEventListener.invoke(eq(it))
            }
        }
    }

    @Test
    fun `when no eventListener set and onEvent() invoked`() {
        subject.eventListener = null

        subject.onEvent(HealthChecked)

        verify(exactly = 0) { mockEventListener.invoke(any()) }
    }

    @Test
    fun `when TypingEvent toTransportEvent()`() {
        val expectedEvent = AgentTyping(3000)

        val result =
            TypingEvent(
                eventType = StructuredMessageEvent.Type.Typing,
                typing = Typing(type = "On", duration = 3000)
            ).toTransportEvent()

        assertThat(result).isEqualTo(expectedEvent)
    }

    @Test
    fun `when TypingEvent with null duration toTransportEvent()`() {
        val expectedEvent = AgentTyping(5000)

        val result =
            TypingEvent(
                eventType = StructuredMessageEvent.Type.Typing,
                typing = Typing(type = "On", duration = null)
            ).toTransportEvent()

        assertThat(result).isEqualTo(expectedEvent)
    }

    @Test
    fun `when PresenceEvent Join toTransportEvent()`() {
        val expectedEvent = ConversationAutostart

        val result =
            PresenceEvent(
                StructuredMessageEvent.Type.Presence,
                PresenceEvent.Presence(
                    PresenceEvent.Presence.Type.Join
                )
            ).toTransportEvent()

        assertThat(result).isEqualTo(expectedEvent)
    }

    @Test
    fun `when PresenceEvent Disconnect toTransportEvent()`() {
        val expectedEvent = ConversationDisconnect

        val result =
            PresenceEvent(
                StructuredMessageEvent.Type.Presence,
                PresenceEvent.Presence(
                    PresenceEvent.Presence.Type.Disconnect
                )
            ).toTransportEvent()

        assertThat(result).isEqualTo(expectedEvent)
    }

    @Test
    fun `when PresenceEvent Clear toTransportEvent()`() {
        val result =
            PresenceEvent(
                StructuredMessageEvent.Type.Presence,
                PresenceEvent.Presence(
                    PresenceEvent.Presence.Type.Clear
                )
            ).toTransportEvent()

        assertNull(result)
    }

    @Test
    fun `when PresenceEvent SignIn with Participant data toTransportEvent()`() {
        val givenParticipantData =
            StructuredMessage.Participant(
                firstName = MessageValues.PARTICIPANT_NAME,
                lastName = MessageValues.PARTICIPANT_LAST_NAME,
            )
        val expectedEvent =
            SignedIn(MessageValues.PARTICIPANT_NAME, MessageValues.PARTICIPANT_LAST_NAME)

        val result =
            PresenceEvent(
                StructuredMessageEvent.Type.Presence,
                PresenceEvent.Presence(
                    PresenceEvent.Presence.Type.SignIn
                )
            ).toTransportEvent(givenParticipantData)

        assertThat(result).isEqualTo(expectedEvent)
    }

    @Test
    fun `when PresenceEvent SignIn without Participant data toTransportEvent()`() {
        val expectedEvent = SignedIn()

        val result =
            PresenceEvent(
                StructuredMessageEvent.Type.Presence,
                PresenceEvent.Presence(
                    PresenceEvent.Presence.Type.SignIn
                )
            ).toTransportEvent()

        assertThat(result).isEqualTo(expectedEvent)
    }

    @Test
    fun `validate event Error payload`() {
        val expectedErrorCodePayload = ErrorCode.UnexpectedError
        val expectedErrorMessagePayload = ErrorTest.MESSAGE
        val expectedCorrectiveActionPayload = CorrectiveAction.Unknown
        val expectedErrorEvent =
            Error(
                expectedErrorCodePayload,
                expectedErrorMessagePayload,
                expectedCorrectiveActionPayload
            )
        val givenErrorEvent =
            Error(ErrorCode.UnexpectedError, ErrorTest.MESSAGE, CorrectiveAction.Unknown)

        subject.onEvent(givenErrorEvent)

        verify {
            mockLogger.i(capture(logSlot))
            mockEventListener.invoke(capture(eventSlot))
        }

        (eventSlot[0] as Error).run {
            assertThat(errorCode).isEqualTo(expectedErrorCodePayload)
            assertThat(message).isEqualTo(expectedErrorMessagePayload)
            assertThat(correctiveAction).isEqualTo(expectedCorrectiveActionPayload)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.onEvent(expectedErrorEvent))
    }

    @Test
    fun `validate event AgentTyping payload`() {
        val expectedAgentTypingPayload = 100L
        val expectedAgentTypingEvent = AgentTyping(expectedAgentTypingPayload)
        val givenAgentTypingEvent = AgentTyping(100)

        subject.onEvent(givenAgentTypingEvent)

        verify {
            mockLogger.i(capture(logSlot))
            mockEventListener.invoke(capture(eventSlot))
        }
        (eventSlot[0] as AgentTyping).run {
            assertThat(durationInMilliseconds).isEqualTo(expectedAgentTypingPayload)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.onEvent(expectedAgentTypingEvent))
    }

    @Test
    fun `validate event SignedIn payload`() {
        val givenSignedInEvent = SignedIn(MessageValues.PARTICIPANT_NAME, MessageValues.PARTICIPANT_LAST_NAME)

        subject.onEvent(givenSignedInEvent)

        verify {
            mockLogger.i(capture(logSlot))
            mockEventListener.invoke(capture(eventSlot))
        }
        (eventSlot[0] as SignedIn).run {
            assertThat(firstName).isEqualTo(MessageValues.PARTICIPANT_NAME)
            assertThat(lastName).isEqualTo(MessageValues.PARTICIPANT_LAST_NAME)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.onEvent(givenSignedInEvent))
    }

    @Test
    fun `validate event ConnectionClosed payload`() {
        val expectedReason = ConnectionClosed.Reason.UserSignedIn
        val expectedConnectionClosedEvent = ConnectionClosed(ConnectionClosed.Reason.UserSignedIn)
        val givenConnectionClosedEvent = ConnectionClosed(ConnectionClosed.Reason.UserSignedIn)

        subject.onEvent(givenConnectionClosedEvent)

        verify {
            mockLogger.i(capture(logSlot))
            mockEventListener.invoke(capture(eventSlot))
        }
        (eventSlot[0] as ConnectionClosed).run {
            assertThat(reason).isEqualTo(expectedReason)
        }
        assertThat(logSlot[0].invoke()).isEqualTo(LogMessages.onEvent(expectedConnectionClosedEvent))
    }

    @Test
    fun `validate default constructor`() {
        val subject = EventHandlerImpl()

        assertThat(subject.log.logger.tag).isEqualTo(LogTag.EVENT_HANDLER)
    }
}
