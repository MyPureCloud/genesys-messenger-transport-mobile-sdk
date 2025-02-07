package transport.core

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.genesys.cloud.messenger.transport.core.JwtHandler
import com.genesys.cloud.messenger.transport.network.PlatformSocket
import com.genesys.cloud.messenger.transport.shyrka.receive.JwtResponse
import com.genesys.cloud.messenger.transport.util.Platform
import com.genesys.cloud.messenger.transport.utility.AuthTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import transport.util.Request

class JwtHandlerTest {
    private val mockWebSocket: PlatformSocket = mockk(relaxed = true)
    val mockJwtFn: (String) -> Any = mockk(relaxed = true)

    private val subject = JwtHandler(mockWebSocket)

    @ExperimentalCoroutinesApi
    private val threadSurrogate = newSingleThreadContext("main thread")

    @ExperimentalCoroutinesApi
    @Before
    fun setup() {
        Dispatchers.setMain(threadSurrogate)
        coEvery { mockJwtFn(capture(slot)) } returns Unit
    }

    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        threadSurrogate.close()
    }

    private val slot = slot<String>()

    @Test
    fun `when withJwt() and jwtResponse is valid`() = runBlocking {
        val givenExpiry = Platform().epochMillis()
        val givenJwtResponse = JwtResponse(AuthTest.JwtToken, givenExpiry)
        subject.jwtResponse = givenJwtResponse

        subject.withJwt(Request.token, mockJwtFn)

        coVerify { mockJwtFn(AuthTest.JwtToken) }
        coVerify(exactly = 0) { mockWebSocket.sendMessage(Request.jwt) }
        assertThat(slot.captured).isEqualTo(AuthTest.JwtToken)
        subject.jwtResponse.run {
            assertThat(this).isEqualTo(givenJwtResponse)
            assertThat(jwt).isEqualTo(AuthTest.JwtToken)
            assertThat(exp).isEqualTo(givenExpiry)
        }
    }

    @Test
    fun `when withJwt() and jwtResponse is invalid`() = runBlocking {
        val givenJwtResponse = JwtResponse(AuthTest.JwtToken, 0)
        subject.jwtResponse = givenJwtResponse

        subject.withJwt(Request.token, mockJwtFn)

        coVerify {
            mockJwtFn(AuthTest.JwtToken)
            mockWebSocket.sendMessage(Request.jwt)
        }
        assertThat(slot.captured).isEqualTo(AuthTest.JwtToken)
        subject.jwtResponse.run {
            assertThat(this).isEqualTo(givenJwtResponse)
            assertThat(jwt).isEqualTo(AuthTest.JwtToken)
            assertThat(exp).isEqualTo(0)
        }
    }

    @Test
    fun `when clear() after jwt was set`() {
        val expectedJwtResponse = JwtResponse()
        subject.jwtResponse = JwtResponse(AuthTest.JwtToken, Platform().epochMillis())

        subject.clear()

        assertThat(subject.jwtResponse).isEqualTo(expectedJwtResponse)
    }
}
