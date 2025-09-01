package transport.core

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.core.Configuration
import com.genesys.cloud.messenger.transport.core.isAuthEnabled
import com.genesys.cloud.messenger.transport.network.DeploymentConfigUseCase
import com.genesys.cloud.messenger.transport.network.defaultHttpClient
import com.genesys.cloud.messenger.transport.shyrka.receive.Apps
import com.genesys.cloud.messenger.transport.shyrka.receive.Auth
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.FileUpload
import com.genesys.cloud.messenger.transport.shyrka.receive.JourneyEvents
import com.genesys.cloud.messenger.transport.shyrka.receive.LauncherButton
import com.genesys.cloud.messenger.transport.shyrka.receive.Messenger
import com.genesys.cloud.messenger.transport.shyrka.receive.Styles
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KProperty0

@ExperimentalCoroutinesApi
class IsAuthEnabledTest {

    @MockK
    private lateinit var mockDeploymentConfigProperty: KProperty0<DeploymentConfig?>

    private lateinit var authEnabledConfig: DeploymentConfig
    private lateinit var authDisabledConfig: DeploymentConfig
    private lateinit var testConfiguration: Configuration

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        
        testConfiguration = Configuration(
            deploymentId = "test-deployment-id",
            domain = "test.domain.com",
            logging = false
        )
        
        val messenger = Messenger(
            enabled = true,
            apps = Apps(
                conversations = Conversations(
                    messagingEndpoint = "ws://test.endpoint.com"
                )
            ),
            styles = Styles(primaryColor = "red"),
            launcherButton = LauncherButton(visibility = "On"),
            fileUpload = FileUpload()
        )
        
        authEnabledConfig = DeploymentConfig(
            id = "test-id",
            version = "1.0",
            languages = listOf("en-us"),
            defaultLanguage = "en-us",
            apiEndpoint = "https://api.test.com",
            messenger = messenger,
            journeyEvents = JourneyEvents(enabled = false),
            status = DeploymentConfig.Status.Active,
            auth = Auth(enabled = true, allowSessionUpgrade = false)
        )
        
        authDisabledConfig = authEnabledConfig.copy(
            auth = Auth(enabled = false, allowSessionUpgrade = false)
        )
    }

    @After
    fun tearDown() {
        unmockkConstructor(DeploymentConfigUseCase::class)
    }

    @Test
    fun `when deployment config exists and auth enabled is true`() = runTest {
        every { mockDeploymentConfigProperty.get() } returns authEnabledConfig

        val result = mockDeploymentConfigProperty.isAuthEnabled(testConfiguration)

        assertThat(result).isTrue()
    }

    @Test
    fun `when deployment config exists and auth enabled is false`() = runTest {
        every { mockDeploymentConfigProperty.get() } returns authDisabledConfig

        val result = mockDeploymentConfigProperty.isAuthEnabled(testConfiguration)

        assertThat(result).isFalse()
    }

    @Test
    fun `when deployment config is null and fetch succeeds with auth enabled true`() = runTest {
        // Given
        every { mockDeploymentConfigProperty.get() } returns null
        
        mockkConstructor(DeploymentConfigUseCase::class)
        coEvery { anyConstructed<DeploymentConfigUseCase>().fetch() } returns authEnabledConfig

        val result = mockDeploymentConfigProperty.isAuthEnabled(testConfiguration)

        assertThat(result).isTrue()
    }

    @Test
    fun `when deployment config is null and fetch succeeds with auth enabled false`() = runTest {
        every { mockDeploymentConfigProperty.get() } returns null
        
        mockkConstructor(DeploymentConfigUseCase::class)
        coEvery { anyConstructed<DeploymentConfigUseCase>().fetch() } returns authDisabledConfig

        val result = mockDeploymentConfigProperty.isAuthEnabled(testConfiguration)

        assertThat(result).isFalse()
    }

    @Test
    fun `when deployment config is null and fetch throws exception`() = runTest {
        every { mockDeploymentConfigProperty.get() } returns null
        
        mockkConstructor(DeploymentConfigUseCase::class)
        coEvery { anyConstructed<DeploymentConfigUseCase>().fetch() } throws RuntimeException("Network error")

        val result = mockDeploymentConfigProperty.isAuthEnabled(testConfiguration)

        assertThat(result).isFalse()
    }
} 