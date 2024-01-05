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
        val expectedAutoStart = Conversations.AutoStart(false)
        val expectedConversationDisconnect = Conversations.ConversationDisconnect(false)
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
            assertThat(auth.enabled).isTrue()
            assertThat(defaultLanguage).isEqualTo(DeploymentConfigValues.DefaultLanguage)
            assertThat(id).isEqualTo(DeploymentConfigValues.Id)
            assertThat(journeyEvents).isEqualTo(expectedJourneyEvents)
            assertThat(journeyEvents.enabled).isFalse()
            assertThat(languages).containsExactly(*expectedLanguages.toTypedArray())
            assertThat(messenger).isEqualTo(expectedMessenger)
            messenger.run {
                assertThat(enabled).isTrue()
                assertThat(apps).isEqualTo(expectedApps)
                apps.conversations.run {
                    assertThat(messagingEndpoint).isEqualTo(DeploymentConfigValues.MessagingEndpoint)
                    assertThat(showAgentTypingIndicator).isFalse()
                    assertThat(showUserTypingIndicator).isFalse()
                    assertThat(autoStart).isEqualTo(expectedAutoStart)
                    assertThat(autoStart.enabled).isFalse()
                    assertThat(conversationDisconnect).isEqualTo(expectedConversationDisconnect)
                    assertThat(conversationDisconnect.enabled).isFalse()
                    assertThat(conversationDisconnect.type).isEqualTo(Conversations.ConversationDisconnect.Type.Send)
                    assertThat(conversationClear).isEqualTo(expectedConversationClear)
                    assertThat(conversationClear.enabled).isTrue()
                }
            }
            assertThat(status).isEqualTo(DeploymentConfigValues.Status)
            assertThat(version).isEqualTo(DeploymentConfigValues.Version)
        }
    }
}