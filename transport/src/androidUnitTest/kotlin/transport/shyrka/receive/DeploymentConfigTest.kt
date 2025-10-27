package transport.shyrka.receive

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.genesys.cloud.messenger.transport.shyrka.WebMessagingJson
import com.genesys.cloud.messenger.transport.shyrka.receive.Apps
import com.genesys.cloud.messenger.transport.shyrka.receive.Auth
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations.AutoStart
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations.ConversationClear
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations.ConversationDisconnect
import com.genesys.cloud.messenger.transport.shyrka.receive.Conversations.Notifications
import com.genesys.cloud.messenger.transport.shyrka.receive.DeploymentConfig
import com.genesys.cloud.messenger.transport.shyrka.receive.FileUpload
import com.genesys.cloud.messenger.transport.shyrka.receive.JourneyEvents
import com.genesys.cloud.messenger.transport.shyrka.receive.LauncherButton
import com.genesys.cloud.messenger.transport.shyrka.receive.Messenger
import com.genesys.cloud.messenger.transport.shyrka.receive.Mode
import com.genesys.cloud.messenger.transport.shyrka.receive.Styles
import com.genesys.cloud.messenger.transport.shyrka.receive.createConversationsVOForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createDeploymentConfigForTesting
import com.genesys.cloud.messenger.transport.shyrka.receive.createMessengerVOForTesting
import com.genesys.cloud.messenger.transport.utility.DeploymentConfigValues
import org.junit.Test

class DeploymentConfigTest {

    @Test
    fun `when DeploymentConfig serialized`() {
        val givenDeploymentConfig = createDeploymentConfigForTesting(
            createMessengerVOForTesting(
                apps = Apps(
                    conversations = createConversationsVOForTesting(
                        notifications = Notifications(enabled = true, Notifications.NotificationContentType.IncludeMessagesContent)
                    )
                )
            )
        )
        val expectedDeploymentConfigAsJson =
            """{"id":"id","version":"1","languages":["en-us","zh-cn"],"defaultLanguage":"en-us","apiEndpoint":"api_endpoint","messenger":{"enabled":true,"apps":{"conversations":{"messagingEndpoint":"messaging_endpoint","conversationClear":{"enabled":true},"notifications":{"enabled":true,"notificationContentType":"IncludeMessagesContent"}}},"styles":{"primaryColor":"red"},"launcherButton":{"visibility":"On"},"fileUpload":{"enableAttachments":false,"modes":[{"fileTypes":["png"],"maxFileSizeKB":100}]}},"journeyEvents":{"enabled":false},"status":"Active","auth":{"enabled":true,"allowSessionUpgrade":true}}"""

        val result = WebMessagingJson.json.encodeToString(givenDeploymentConfig)

        assertThat(result).isEqualTo(expectedDeploymentConfigAsJson)
    }

    @Test
    fun `when DeploymentConfig deserialized`() {
        val givenDeploymentConfigAsJson =
            """{"id":"id","version":"1","languages":["en-us","zh-cn"],"defaultLanguage":"en-us","apiEndpoint":"api_endpoint","messenger":{"enabled":true,"apps":{"conversations":{"messagingEndpoint":"messaging_endpoint","conversationClear":{"enabled":true},"notifications":{"enabled":false,"notificationContentType":"IncludeMessagesContent"}}},"styles":{"primaryColor":"red"},"launcherButton":{"visibility":"On"},"fileUpload":{"enableAttachments":false,"modes":[{"fileTypes":["png"],"maxFileSizeKB":100}]}},"journeyEvents":{"enabled":false},"status":"Active","auth":{"enabled":true,"allowSessionUpgrade":true}}"""
        val expectedDeploymentConfig = createDeploymentConfigForTesting()
        val expectedAuth = Auth(enabled = true, allowSessionUpgrade = true)
        val expectedJourneyEvents = JourneyEvents(enabled = false)
        val expectedConversationClear = ConversationClear(enabled = true)
        val expectedNotifications = Notifications(
            enabled = false,
            Notifications.NotificationContentType.IncludeMessagesContent
        )
        val expectedMarkdown = Conversations.Markdown(false)
        val expectedConversations = Conversations(
            messagingEndpoint = DeploymentConfigValues.MESSAGING_ENDPOINT,
            conversationClear = expectedConversationClear,
            notifications = expectedNotifications,
            markdown = expectedMarkdown,
        )
        val expectedApps = Apps(conversations = expectedConversations)
        val expectedStyles = Styles(primaryColor = DeploymentConfigValues.PRIMARY_COLOR)
        val expectedMode = Mode(
            fileTypes = listOf(DeploymentConfigValues.FILE_TYPE),
            maxFileSizeKB = DeploymentConfigValues.MAX_FILE_SIZE
        )
        val expectedLauncherButton =
            LauncherButton(visibility = DeploymentConfigValues.LAUNCHER_BUTTON_VISIBILITY)
        val expectedFileUpload = FileUpload(enableAttachments = false, modes = listOf(expectedMode))
        val expectedMessenger = Messenger(
            enabled = true,
            apps = expectedApps,
            styles = expectedStyles,
            launcherButton = expectedLauncherButton,
            fileUpload = expectedFileUpload,
        )
        val expectedLanguages =
            listOf(DeploymentConfigValues.DEFAULT_LANGUAGE, DeploymentConfigValues.SECONDARY_LANGUAGE)

        val result =
            WebMessagingJson.json.decodeFromString<DeploymentConfig>(givenDeploymentConfigAsJson)

        assertThat(result).isEqualTo(expectedDeploymentConfig)
        result.run {
            assertThat(apiEndpoint).isEqualTo(DeploymentConfigValues.API_ENDPOINT)
            assertThat(auth).isEqualTo(expectedAuth)
            assertThat(defaultLanguage).isEqualTo(DeploymentConfigValues.DEFAULT_LANGUAGE)
            assertThat(id).isEqualTo(DeploymentConfigValues.ID)
            assertThat(journeyEvents).isEqualTo(expectedJourneyEvents)
            assertThat(journeyEvents.enabled).isFalse()
            assertThat(languages).containsExactly(*expectedLanguages.toTypedArray())
            assertThat(messenger).isEqualTo(expectedMessenger)
            messenger.run {
                assertThat(enabled).isTrue()
                assertThat(apps).isEqualTo(expectedApps)
                assertThat(styles).isEqualTo(expectedStyles)
                assertThat(launcherButton).isEqualTo(expectedLauncherButton)
                assertThat(fileUpload).isEqualTo(expectedFileUpload)
            }
            assertThat(status).isEqualTo(DeploymentConfigValues.Status)
            assertThat(version).isEqualTo(DeploymentConfigValues.VERSION)
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
            messagingEndpoint = DeploymentConfigValues.MESSAGING_ENDPOINT,
            showAgentTypingIndicator = true,
            showUserTypingIndicator = true,
            autoStart = AutoStart(enabled = true),
            conversationDisconnect = ConversationDisconnect(
                enabled = true,
                ConversationDisconnect.Type.ReadOnly
            ),
            conversationClear = ConversationClear(enabled = true),
            notifications = Notifications(
                enabled = true,
                notificationContentType = Notifications.NotificationContentType.IncludeMessagesContent
            ),
            markdown = Conversations.Markdown(enabled = true),
        )
        val expectedConversationsAsJson =
            """{"messagingEndpoint":"messaging_endpoint","showAgentTypingIndicator":true,"showUserTypingIndicator":true,"autoStart":{"enabled":true},"conversationDisconnect":{"enabled":true,"type":"ReadOnly"},"conversationClear":{"enabled":true},"notifications":{"enabled":true,"notificationContentType":"IncludeMessagesContent"},"markdown":{"enabled":true}}"""

        val result = WebMessagingJson.json.encodeToString(givenConversations)

        assertThat(result).isEqualTo(expectedConversationsAsJson)
    }

    @Test
    fun `when Conversations deserialized`() {
        val givenConversationsAsJson =
            """{"messagingEndpoint":"messaging_endpoint","showAgentTypingIndicator":true,"showUserTypingIndicator":true,"autoStart":{"enabled":true},"conversationDisconnect":{"enabled":true,"type":"ReadOnly"},"conversationClear":{"enabled":false},"notifications":{"enabled":false,"notificationContentType":"ExcludeMessagesContent"},"markdown":{"enabled":false}}"""
        val expectedConversationClear = ConversationClear(enabled = false)
        val expectedAutoStart = AutoStart(true)
        val expectedConversationDisconnect =
            ConversationDisconnect(true, ConversationDisconnect.Type.ReadOnly)
        val expectedNotifications = Notifications(
            enabled = false,
            notificationContentType = Notifications.NotificationContentType.ExcludeMessagesContent
        )
        val expectedMarkdown = Conversations.Markdown(false)
        val expectedConversations = Conversations(
            messagingEndpoint = DeploymentConfigValues.MESSAGING_ENDPOINT,
            showAgentTypingIndicator = true,
            showUserTypingIndicator = true,
            autoStart = expectedAutoStart,
            conversationDisconnect = expectedConversationDisconnect,
            conversationClear = expectedConversationClear,
            notifications = expectedNotifications,
            markdown = expectedMarkdown,
        )

        val result = WebMessagingJson.json.decodeFromString<Conversations>(givenConversationsAsJson)

        assertThat(result).isEqualTo(expectedConversations)
        result.run {
            assertThat(messagingEndpoint).isEqualTo(DeploymentConfigValues.MESSAGING_ENDPOINT)
            assertThat(showAgentTypingIndicator).isTrue()
            assertThat(showUserTypingIndicator).isTrue()
            assertThat(autoStart).isEqualTo(expectedAutoStart)
            assertThat(autoStart.enabled).isTrue()
            assertThat(conversationDisconnect).isEqualTo(expectedConversationDisconnect)
            assertThat(conversationDisconnect.enabled).isTrue()
            assertThat(conversationDisconnect.type).isEqualTo(ConversationDisconnect.Type.ReadOnly)
            assertThat(conversationClear).isEqualTo(expectedConversationClear)
            assertThat(conversationClear.enabled).isFalse()
        }
    }

    @Test
    fun `when Autostart serialized`() {
        val givenAutostart = AutoStart(enabled = true)
        val expectedAutostartAsJson = """{"enabled":true}"""

        val result = WebMessagingJson.json.encodeToString(givenAutostart)

        assertThat(result).isEqualTo(expectedAutostartAsJson)
    }

    @Test
    fun `when Autostart deserialized`() {
        val givenAutostartAsJson = """{"enabled":true}"""
        val expectedAutostart = AutoStart(enabled = true)

        val result =
            WebMessagingJson.json.decodeFromString<AutoStart>(givenAutostartAsJson)

        assertThat(result).isEqualTo(expectedAutostart)
        assertThat(result.enabled).isTrue()
    }

    @Test
    fun `when ConversationClear serialized`() {
        val givenConversationClear = ConversationClear(enabled = true)
        val expectedConversationClearAsJson = """{"enabled":true}"""

        val result = WebMessagingJson.json.encodeToString(givenConversationClear)

        assertThat(result).isEqualTo(expectedConversationClearAsJson)
    }

    @Test
    fun `when ConversationClear deserialized`() {
        val givenConversationClearAsJson = """{"enabled":true}"""
        val expectedConversationClear = ConversationClear(enabled = true)

        val result = WebMessagingJson.json.decodeFromString<ConversationClear>(
            givenConversationClearAsJson
        )

        assertThat(result).isEqualTo(expectedConversationClear)
        assertThat(result.enabled).isTrue()
    }

    @Test
    fun `when Markdown serialized`() {
        val givenMarkdown = Conversations.Markdown(enabled = true)
        val expectedMarkdownAsJson = """{"enabled":true}"""

        val result = WebMessagingJson.json.encodeToString(givenMarkdown)

        assertThat(result).isEqualTo(expectedMarkdownAsJson)
    }

    @Test
    fun `when Markdown deserialized`() {
        val givenMarkdownAsJson = """{"enabled":true}"""
        val expectedMarkdown = Conversations.Markdown(enabled = true)

        val result = WebMessagingJson.json.decodeFromString<Conversations.Markdown>(
            givenMarkdownAsJson
        )

        assertThat(result).isEqualTo(expectedMarkdown)
        assertThat(result.enabled).isTrue()
    }

    @Test
    fun `when ConversationDisconnect serialized`() {
        val givenConversationDisconnect = ConversationDisconnect(
            enabled = true,
            type = ConversationDisconnect.Type.ReadOnly
        )
        val expectedConversationDisconnectAsJson = """{"enabled":true,"type":"ReadOnly"}"""

        val result = WebMessagingJson.json.encodeToString(givenConversationDisconnect)

        assertThat(result).isEqualTo(expectedConversationDisconnectAsJson)
    }

    @Test
    fun `when ConversationDisconnect deserialized`() {
        val givenConversationDisconnectAsJson = """{"enabled":true,"type":"Send"}"""
        val expectedConversationDisconnect = ConversationDisconnect(
            enabled = true,
            type = ConversationDisconnect.Type.Send
        )

        val result = WebMessagingJson.json.decodeFromString<ConversationDisconnect>(
            givenConversationDisconnectAsJson
        )

        assertThat(result).isEqualTo(expectedConversationDisconnect)
        assertThat(result.enabled).isTrue()
        assertThat(result.type).isEqualTo(ConversationDisconnect.Type.Send)
    }

    @Test
    fun `when Notifications serialized`() {
        val givenNotifications = Notifications(
            enabled = true,
            notificationContentType = Notifications.NotificationContentType.IncludeMessagesContent
        )
        val expectedNotificationsAsJson =
            """{"enabled":true,"notificationContentType":"IncludeMessagesContent"}"""

        val result = WebMessagingJson.json.encodeToString(givenNotifications)

        assertThat(result).isEqualTo(expectedNotificationsAsJson)
    }

    @Test
    fun `when Notifications deserialized`() {
        val givenNotificationsAsJson =
            """{"enabled":false,"notificationContentType":"ExcludeMessagesContent"}"""
        val expectedNotifications = Notifications(
            enabled = false,
            notificationContentType = Notifications.NotificationContentType.ExcludeMessagesContent
        )

        val result = WebMessagingJson.json.decodeFromString<Notifications>(givenNotificationsAsJson)

        assertThat(result).isEqualTo(expectedNotifications)
        assertThat(result.enabled).isFalse()
        assertThat(result.notificationContentType).isEqualTo(Notifications.NotificationContentType.ExcludeMessagesContent)
    }

    @Test
    fun `when FileUpload serialized`() {
        val givenMode = Mode(
            fileTypes = listOf(DeploymentConfigValues.FILE_TYPE),
            maxFileSizeKB = DeploymentConfigValues.MAX_FILE_SIZE
        )
        val givenFileUpload = FileUpload(modes = listOf(givenMode))
        val expectedFileUploadAsJson = """{"modes":[{"fileTypes":["png"],"maxFileSizeKB":100}]}"""

        val result = WebMessagingJson.json.encodeToString(givenFileUpload)

        assertThat(result).isEqualTo(expectedFileUploadAsJson)
    }

    @Test
    fun `when FileUpload deserialized`() {
        val givenFileUploadAsJson = """{"modes":[{"fileTypes":["png"],"maxFileSizeKB":100}]}"""
        val expectedFileType = DeploymentConfigValues.FILE_TYPE
        val expectedFileTypes = listOf(expectedFileType)
        val expectedModes = listOf(
            Mode(
                fileTypes = expectedFileTypes,
                maxFileSizeKB = DeploymentConfigValues.MAX_FILE_SIZE
            )
        )
        val expectedFileUpload = FileUpload(modes = expectedModes)

        val result = WebMessagingJson.json.decodeFromString<FileUpload>(givenFileUploadAsJson)

        assertThat(result).isEqualTo(expectedFileUpload)
        assertThat(result.modes[0].fileTypes[0]).isEqualTo(DeploymentConfigValues.FILE_TYPE)
        assertThat(result.modes[0].maxFileSizeKB).isEqualTo(DeploymentConfigValues.MAX_FILE_SIZE)
        assertThat(result.modes).containsExactly(*expectedModes.toTypedArray())
        assertThat(result.modes[0].fileTypes).containsExactly(*expectedFileTypes.toTypedArray())
    }

    @Test
    fun `when JourneyEvents serialized`() {
        val givenJourneyEvents = JourneyEvents(enabled = true)
        val expectedJourneyEventsAsJson = """{"enabled":true}"""

        val result = WebMessagingJson.json.encodeToString(givenJourneyEvents)

        assertThat(result).isEqualTo(expectedJourneyEventsAsJson)
    }

    @Test
    fun `when JourneyEvents deserialized`() {
        val givenJourneyEventsAsJson = """{"enabled":true}"""
        val expectedJourneyEvents = JourneyEvents(enabled = true)

        val result = WebMessagingJson.json.decodeFromString<JourneyEvents>(givenJourneyEventsAsJson)

        assertThat(result).isEqualTo(expectedJourneyEvents)
        assertThat(result.enabled).isTrue()
    }

    @Test
    fun `when LauncherButton serialized`() {
        val givenLauncherButton = LauncherButton(DeploymentConfigValues.LAUNCHER_BUTTON_VISIBILITY)
        val expectedLauncherButtonAsJson = """{"visibility":"On"}"""

        val result = WebMessagingJson.json.encodeToString(givenLauncherButton)

        assertThat(result).isEqualTo(expectedLauncherButtonAsJson)
    }

    @Test
    fun `when LauncherButton deserialized`() {
        val givenLauncherButtonAsJson = """{"visibility":"On"}"""
        val expectedLauncherButton = LauncherButton(DeploymentConfigValues.LAUNCHER_BUTTON_VISIBILITY)

        val result =
            WebMessagingJson.json.decodeFromString<LauncherButton>(givenLauncherButtonAsJson)

        assertThat(result).isEqualTo(expectedLauncherButton)
        assertThat(result.visibility).isEqualTo(DeploymentConfigValues.LAUNCHER_BUTTON_VISIBILITY)
    }

    @Test
    fun `when Mode serialized`() {
        val givenFileTypes = listOf(DeploymentConfigValues.FILE_TYPE)
        val givenMode = Mode(givenFileTypes, DeploymentConfigValues.MAX_FILE_SIZE)
        val expectedModeAsJson = """{"fileTypes":["png"],"maxFileSizeKB":100}"""

        val result = WebMessagingJson.json.encodeToString(givenMode)

        assertThat(result).isEqualTo(expectedModeAsJson)
    }

    @Test
    fun `when Mode deserialized`() {
        val givenModeAsJson = """{"fileTypes":["png"],"maxFileSizeKB":100}"""
        val expectedFileTypes = listOf(DeploymentConfigValues.FILE_TYPE)
        val expectedMode =
            Mode(listOf(DeploymentConfigValues.FILE_TYPE), DeploymentConfigValues.MAX_FILE_SIZE)

        val result = WebMessagingJson.json.decodeFromString<Mode>(givenModeAsJson)

        assertThat(result).isEqualTo(expectedMode)
        assertThat(result.fileTypes).containsExactly(*expectedFileTypes.toTypedArray())
    }

    @Test
    fun `when Styles serialized`() {
        val expectedRequest = Styles(primaryColor = DeploymentConfigValues.PRIMARY_COLOR)
        val expectedJson = """{"primaryColor":"red"}"""

        val encodedString = WebMessagingJson.json.encodeToString(expectedRequest)
        val decoded = WebMessagingJson.json.decodeFromString<Styles>(expectedJson)

        assertThat(encodedString, "encoded Styles").isEqualTo(expectedJson)
        assertThat(decoded.primaryColor).isEqualTo(DeploymentConfigValues.PRIMARY_COLOR)
    }
}
