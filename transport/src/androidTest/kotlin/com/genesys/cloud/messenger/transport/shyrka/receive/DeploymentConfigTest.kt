package com.genesys.cloud.messenger.transport.shyrka.receive

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.utility.DeploymentConfigValues
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Test

class DeploymentConfigTest {

    @Test
    fun `when DeploymentConfig serialized`() {
        val givenDeploymentConfig = createDeploymentConfigForTesting()
        val expectedDeploymentConfigAsJson =
            """{"id":"id","version":"1","languages":["en-us","zh-cn"],"defaultLanguage":"en-us","apiEndpoint":"api_endpoint","messenger":{"enabled":true,"apps":{"conversations":{"messagingEndpoint":"messaging_endpoint","conversationClear":{"enabled":true}}},"styles":{"primaryColor":"red"},"launcherButton":{"visibility":"On"},"fileUpload":{"modes":[{"fileTypes":["png"],"maxFileSizeKB":100}]}},"journeyEvents":{"enabled":false},"status":"Active","auth":{"enabled":true}}"""

        val result = WebMessagingJson.json.encodeToString(givenDeploymentConfig)

        assertThat(result).isEqualTo(expectedDeploymentConfigAsJson)
    }

    @Test
    fun `when DeploymentConfig deserialized`() {
        val givenDeploymentConfigAsJson =
            """{"id":"id","version":"1","languages":["en-us","zh-cn"],"defaultLanguage":"en-us","apiEndpoint":"api_endpoint","messenger":{"enabled":true,"apps":{"conversations":{"messagingEndpoint":"messaging_endpoint","conversationClear":{"enabled":true}}},"styles":{"primaryColor":"red"},"launcherButton":{"visibility":"On"},"fileUpload":{"modes":[{"fileTypes":["png"],"maxFileSizeKB":100}]}},"journeyEvents":{"enabled":false},"status":"Active","auth":{"enabled":true}}"""
        val expectedDeploymentConfig = createDeploymentConfigForTesting()
        val expectedAuth = Auth(enabled = true)
        val expectedJourneyEvents = JourneyEvents(enabled = false)
        val expectedConversationClear = Conversations.ConversationClear(enabled = true)
        val expectedConversations = Conversations(
            messagingEndpoint = DeploymentConfigValues.MessagingEndpoint,
            conversationClear = expectedConversationClear
        )
        val expectedApps = Apps(conversations = expectedConversations)
        val expectedStyles = Styles(primaryColor = DeploymentConfigValues.PrimaryColor)
        val expectedMode = Mode(
            fileTypes = listOf(DeploymentConfigValues.FileType),
            maxFileSizeKB = DeploymentConfigValues.MaxFileSize
        )
        val expectedLauncherButton =
            LauncherButton(visibility = DeploymentConfigValues.LauncherButtonVisibility)
        val expectedFileUpload = FileUpload(modes = listOf(expectedMode))
        val expectedMessenger = Messenger(
            enabled = true,
            apps = expectedApps,
            styles = expectedStyles,
            launcherButton = expectedLauncherButton,
            fileUpload = expectedFileUpload,
        )
        val expectedLanguages =
            listOf(DeploymentConfigValues.DefaultLanguage, DeploymentConfigValues.SecondaryLanguage)

        val result =
            WebMessagingJson.json.decodeFromString<DeploymentConfig>(givenDeploymentConfigAsJson)

        assertThat(result).isEqualTo(expectedDeploymentConfig)
        result.run {
            assertThat(apiEndpoint).isEqualTo(DeploymentConfigValues.ApiEndPoint)
            assertThat(auth).isEqualTo(expectedAuth)
            assertThat(defaultLanguage).isEqualTo(DeploymentConfigValues.DefaultLanguage)
            assertThat(id).isEqualTo(DeploymentConfigValues.Id)
            assertThat(journeyEvents).isEqualTo(expectedJourneyEvents)
            assertThat(journeyEvents.enabled).isFalse()
            assertThat(languages).containsExactly(*expectedLanguages.toTypedArray())
            assertThat(messenger).isEqualTo(expectedMessenger)
            messenger.run {
                assertThat(enabled).isTrue()
                assertThat(apps).isEqualTo(expectedApps)
            }
            assertThat(status).isEqualTo(DeploymentConfigValues.Status)
            assertThat(version).isEqualTo(DeploymentConfigValues.Version)
        }
    }

    @Test
    fun `when Auth serialized`() {
        val givenAuth = Auth(enabled = true)
        val expectedAuthAsJson = """{"enabled":true}"""

        val result = WebMessagingJson.json.encodeToString(givenAuth)

        assertThat(result).isEqualTo(expectedAuthAsJson)
    }

    @Test
    fun `when Auth deserialized`() {
        val givenAuthAsJson = """{"enabled":true}"""
        val expectedAuth = Auth(enabled = true)

        val result = WebMessagingJson.json.decodeFromString<Auth>(givenAuthAsJson)

        assertThat(result).isEqualTo(expectedAuth)
        assertThat(result.enabled).isTrue()
    }

    @Test
    fun `when Conversations serialized`() {
        val givenConversations = Conversations(
            messagingEndpoint = DeploymentConfigValues.MessagingEndpoint,
            showAgentTypingIndicator = true,
            showUserTypingIndicator = true,
            autoStart = Conversations.AutoStart(enabled = true),
            conversationDisconnect = Conversations.ConversationDisconnect(
                enabled = true,
                Conversations.ConversationDisconnect.Type.ReadOnly
            ),
            conversationClear = Conversations.ConversationClear(enabled = true)
        )
        val expectedConversationsAsJson = """{"messagingEndpoint":"messaging_endpoint","showAgentTypingIndicator":true,"showUserTypingIndicator":true,"autoStart":{"enabled":true},"conversationDisconnect":{"enabled":true,"type":"ReadOnly"},"conversationClear":{"enabled":true}}"""

        val result = WebMessagingJson.json.encodeToString(givenConversations)

        assertThat(result).isEqualTo(expectedConversationsAsJson)
    }

    @Test
    fun `when Conversations deserialized`() {
        val givenConversationsAsJson = """{"messagingEndpoint":"messaging_endpoint","showAgentTypingIndicator":true,"showUserTypingIndicator":true,"autoStart":{"enabled":true},"conversationDisconnect":{"enabled":true,"type":"ReadOnly"},"conversationClear":{"enabled":false}}"""
        val expectedConversationClear = Conversations.ConversationClear(enabled = false)
        val expectedAutoStart = Conversations.AutoStart(true)
        val expectedConversationDisconnect = Conversations.ConversationDisconnect(
            true,
            Conversations.ConversationDisconnect.Type.ReadOnly
        )
        val expectedConversations = Conversations(
            messagingEndpoint = DeploymentConfigValues.MessagingEndpoint,
            showAgentTypingIndicator = true,
            showUserTypingIndicator = true,
            autoStart = expectedAutoStart,
            conversationDisconnect = expectedConversationDisconnect,
            conversationClear = expectedConversationClear
        )

        val result = WebMessagingJson.json.decodeFromString<Conversations>(givenConversationsAsJson)

        assertThat(result).isEqualTo(expectedConversations)
        result.run {
            assertThat(messagingEndpoint).isEqualTo(DeploymentConfigValues.MessagingEndpoint)
            assertThat(showAgentTypingIndicator).isTrue()
            assertThat(showUserTypingIndicator).isTrue()
            assertThat(autoStart).isEqualTo(expectedAutoStart)
            assertThat(autoStart.enabled).isTrue()
            assertThat(conversationDisconnect).isEqualTo(expectedConversationDisconnect)
            assertThat(conversationDisconnect.enabled).isTrue()
            assertThat(conversationDisconnect.type).isEqualTo(Conversations.ConversationDisconnect.Type.ReadOnly)
            assertThat(conversationClear).isEqualTo(expectedConversationClear)
            assertThat(conversationClear.enabled).isFalse()
        }
    }

    @Test
    fun `when Autostart serialized`() {
        val givenAutostart = Conversations.AutoStart(enabled = true)
        val expectedAutostartAsJson = """{"enabled":true}"""

        val result = WebMessagingJson.json.encodeToString(givenAutostart)

        assertThat(result).isEqualTo(expectedAutostartAsJson)
    }

    @Test
    fun `when Autostart deserialized`() {
        val givenAutostartAsJson = """{"enabled":true}"""
        val expectedAutostart = Conversations.AutoStart(enabled = true)

        val result = WebMessagingJson.json.decodeFromString<Conversations.AutoStart>(givenAutostartAsJson)

        assertThat(result).isEqualTo(expectedAutostart)
        assertThat(result.enabled).isTrue()
    }

    @Test
    fun `when ConversationClear serialized`() {
        val givenConversationClear = Conversations.ConversationClear(enabled = true)
        val expectedConversationClearAsJson = """{"enabled":true}"""

        val result = WebMessagingJson.json.encodeToString(givenConversationClear)

        assertThat(result).isEqualTo(expectedConversationClearAsJson)
    }

    @Test
    fun `when ConversationClear deserialized`() {
        val givenConversationClearAsJson = """{"enabled":true}"""
        val expectedConversationClear = Conversations.ConversationClear(enabled = true)

        val result = WebMessagingJson.json.decodeFromString<Conversations.ConversationClear>(givenConversationClearAsJson)

        assertThat(result).isEqualTo(expectedConversationClear)
        assertThat(result.enabled).isTrue()
    }

    @Test
    fun `when ConversationDisconnect serialized`() {
        val givenConversationDisconnect = Conversations.ConversationDisconnect(enabled = true, type = Conversations.ConversationDisconnect.Type.ReadOnly)
        val expectedConversationDisconnectAsJson = """{"enabled":true,"type":"ReadOnly"}"""

        val result = WebMessagingJson.json.encodeToString(givenConversationDisconnect)

        assertThat(result).isEqualTo(expectedConversationDisconnectAsJson)
    }

    @Test
    fun `when ConversationDisconnect deserialized`() {
        val givenConversationDisconnectAsJson = """{"enabled":true,"type":"Send"}"""
        val expectedConversationDisconnect = Conversations.ConversationDisconnect(enabled = true, type = Conversations.ConversationDisconnect.Type.Send)

        val result = WebMessagingJson.json.decodeFromString<Conversations.ConversationDisconnect>(givenConversationDisconnectAsJson)

        assertThat(result).isEqualTo(expectedConversationDisconnect)
        assertThat(result.enabled).isTrue()
        assertThat(result.type).isEqualTo(Conversations.ConversationDisconnect.Type.Send)
    }

    @Test
    fun `when FileUpload serialized`() {
        val givenMode = Mode(fileTypes = listOf(DeploymentConfigValues.FileType), maxFileSizeKB = DeploymentConfigValues.MaxFileSize)
        val givenFileUpload = FileUpload(listOf(givenMode))
        val expectedFileUploadAsJson = """{"modes":[{"fileTypes":["png"],"maxFileSizeKB":100}]}"""

        val result = WebMessagingJson.json.encodeToString(givenFileUpload)

        assertThat(result).isEqualTo(expectedFileUploadAsJson)
    }

    @Test
    fun `when FileUpload deserialized`() {
        val givenFileUploadAsJson = """{"modes":[{"fileTypes":["png"],"maxFileSizeKB":100}]}"""
        val expectedFileType = DeploymentConfigValues.FileType
        val expectedFileTypes = listOf(expectedFileType)
        val expectedModes = listOf(Mode(fileTypes = expectedFileTypes, maxFileSizeKB = DeploymentConfigValues.MaxFileSize))
        val expectedFileUpload = FileUpload(expectedModes)

        val result = WebMessagingJson.json.decodeFromString<FileUpload>(givenFileUploadAsJson)

        assertThat(result).isEqualTo(expectedFileUpload)
        assertThat(result.modes[0].fileTypes[0]).isEqualTo(DeploymentConfigValues.FileType)
        assertThat(result.modes[0].maxFileSizeKB).isEqualTo(DeploymentConfigValues.MaxFileSize)
        assertThat(result.modes).containsExactly(*expectedModes.toTypedArray())
        assertThat(result.modes[0].fileTypes).containsExactly(*expectedFileTypes.toTypedArray())
    }
}
