package transport.core.messagingclient

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.ErrorCode
import com.genesys.cloud.messenger.transport.core.Result
import com.genesys.cloud.messenger.transport.core.isAuthEnabled
import com.genesys.cloud.messenger.transport.network.WebMessagingApi
import com.genesys.cloud.messenger.transport.shyrka.receive.Auth
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.createDeploymentConfigForTesting
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.reflect.KProperty0

class MCIsAuthEnabledTest {

    private val mockApi = mockk<WebMessagingApi>()

    @Test
    fun `when deployment config is available and auth enabled is true`() = runBlocking {
        val deploymentConfig = createDeploymentConfigForTesting().copy(
            auth = Auth(enabled = true)
        )
        // Note: the configProperty is mockk but the element that tested in practice is deploymentConfig
        val configProperty = createMockProperty(deploymentConfig)

        val result = configProperty.isAuthEnabled(mockApi)

        assertThat(result).isTrue()
    }

    @Test
    fun `when deployment config is available and auth enabled is false`() = runBlocking {
        val deploymentConfig = createDeploymentConfigForTesting().copy(
            auth = Auth(enabled = false)
        )
        val configProperty = createMockProperty(deploymentConfig)

        val result = configProperty.isAuthEnabled(mockApi)

        assertThat(result).isFalse()
    }

    @Test
    fun `when deployment config is null and API returns success with auth enabled`() = runBlocking {
        val configProperty = createMockProperty(null)
        val apiDeploymentConfig = createDeploymentConfigForTesting().copy(
            auth = Auth(enabled = true)
        )
        coEvery { mockApi.fetchDeploymentConfig() } returns Result.Success(apiDeploymentConfig)

        val result = configProperty.isAuthEnabled(mockApi)

        assertThat(result).isTrue()
    }

    @Test
    fun `when deployment config is null and API returns success with auth disabled`() =
        runBlocking {
            val configProperty = createMockProperty(null)
            val apiDeploymentConfig = createDeploymentConfigForTesting().copy(
                auth = Auth(enabled = false)
            )
            coEvery { mockApi.fetchDeploymentConfig() } returns Result.Success(apiDeploymentConfig)

            val result = configProperty.isAuthEnabled(mockApi)

            assertThat(result).isFalse()
        }

    @Test
    fun `when deployment config is null and API returns failure`() = runBlocking {
        val configProperty = createMockProperty(null)
        coEvery { mockApi.fetchDeploymentConfig() } returns Result.Failure(
            ErrorCode.DeploymentConfigFetchFailed,
            "Failed to fetch config"
        )

        val result = configProperty.isAuthEnabled(mockApi)

        assertThat(result).isFalse()
    }

    @Test
    fun `when deployment config is null and API returns unexpected error`() = runBlocking {
        val configProperty = createMockProperty(null)
        coEvery { mockApi.fetchDeploymentConfig() } returns Result.Failure(
            ErrorCode.UnexpectedError,
            "Network error"
        )

        val result = configProperty.isAuthEnabled(mockApi)

        assertThat(result).isFalse()
    }

    private fun createMockProperty(config: DeploymentConfig?): KProperty0<DeploymentConfig?> {
        val mockProperty = mockk<KProperty0<DeploymentConfig?>>()
        every { mockProperty.get() } returns config
        return mockProperty
    }
}
