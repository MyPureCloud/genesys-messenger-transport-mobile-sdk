# Implementation Plan

- [x] 1. Create simplified SettingsViewModel for deployment configuration
  - Implement SettingsViewModel with only deploymentId and region fields
  - Add BuildConfig default value loading functionality
  - Create validation for deployment settings
  - Remove all theme, notification, and language related code
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. Implement TestBedViewModel core infrastructure
  - Create TestBedViewModel class with state properties from Android reference
  - Add initialization state tracking and isInitialized property
  - Implement command system infrastructure with Command data class
  - Create AuthState sealed class hierarchy
  - _Requirements: 3.1, 3.2_

- [x] 3. Implement TestBedViewModel initialization flow
  - Create init() method with platform context, file selection, and OAuth callbacks
  - Implement MessengerTransportSDK configuration and client creation
  - Set up client event listeners (stateChangedListener, messageListener, eventListener)
  - Add custom attributes and initial state configuration
  - Implement deployment config fetching
  - _Requirements: 3.1, 3.3, 3.4_

- [x] 4. Create platform-specific context handling
  - Define expect/actual PlatformContext classes for Android and iOS
  - Implement getPlatformContext() expect/actual functions
  - Add vault context setup for encrypted/default vault handling
  - Create asset loading functionality for saved attachments
  - _Requirements: 3.5_

- [x] 5. Implement command execution system
  - Create availableCommands list with all commands from Android reference
  - Implement onCommandSend() with command parsing and routing
  - Add command waiting state management
  - Create command validation and error handling
  - _Requirements: 4.1, 4.3, 4.4, 4.5_

- [x] 6. Implement core messaging commands
  - Add doConnect() and doConnectAuthenticated() command implementations
  - Implement doDisconnect() and doSendMessage() commands
  - Create doSendHealthCheck() and fetchNextPage() commands
  - Add doStartNewChat() and doClearConversation() commands
  - _Requirements: 3.2, 4.2_

- [x] 7. Implement authentication commands
  - Add doOktaSignIn() with PKCE support
  - Implement doAuthorize() with auth code handling
  - Create logoutFromOktaSession() and doStepUp() commands
  - Add doWasAuthenticated() and doShouldAuthorize() commands
  - Implement auth token management commands
  - _Requirements: 3.4_

- [x] 8. Implement attachment commands
  - Create doAttach() and doAttachSavedImage() command implementations
  - Add doDetach() and doRefreshAttachmentUrl() commands
  - Implement doFileAttachmentProfile() and doChangeFileName() commands
  - Add file selection handling and attachment state management
  - _Requirements: 3.5_

- [x] 9. Implement push notification commands
  - Add doSynchronizeDeviceToken() command implementation
  - Create doUnregisterFromPush() command
  - Implement push service integration
  - Add device token management functionality
  - _Requirements: 3.2_

- [x] 10. Implement utility and management commands
  - Create doDeployment() for deployment config display
  - Add doInvalidateConversationCache() command
  - Implement doAddCustomAttributes() with key-value parsing
  - Create doIndicateTyping() command
  - Add doRemoveTokenFromVault() and doRemoveAuthRefreshTokenFromVault() commands
  - _Requirements: 3.2_

- [x] 11. Implement socket message processing
  - Create onMessage() event handler for MessageEvent processing
  - Implement onEvent() handler for Event processing
  - Add onClientStateChanged() for state transition handling
  - Create socket message formatting and display logic
  - _Requirements: 3.3, 5.1, 5.3_

- [x] 12. Create SocketMessage data models
  - Implement SocketMessage data class with id, timestamp, type, content
  - Create SocketMessageItem with expansion state
  - Add message type extraction and summary generation
  - Implement message formatting for different event types
  - _Requirements: 5.1, 5.2, 5.5_

- [x] 13. Refactor SettingsScreen to simplified version
  - Remove theme, notification, and language sections
  - Keep only deployment configuration fields
  - Update UI to show deploymentId and region inputs
  - Add region dropdown with available regions list
  - Remove unused UI components and validation
  - _Requirements: 1.1, 1.4, 6.1, 6.2, 6.3_

- [x] 14. Transform ChatScreen to InteractionScreen
  - Rename ChatScreen to InteractionScreen
  - Replace message display with socket message list
  - Remove chat message bubble components
  - Add expandable socket message display
  - _Requirements: 2.1, 2.2, 2.3, 6.4_

- [x] 15. Implement command input interface
  - Replace message input with command input field
  - Add command dropdown with all available commands
  - Implement additional input field for commands requiring parameters
  - Add command execution loading states
  - _Requirements: 2.4, 2.5, 2.6, 4.1, 4.2_

- [x] 16. Create expandable socket message display
  - Implement SocketMessageList composable with expand/collapse functionality
  - Add message type prominence in collapsed view
  - Create detailed message view for expanded state
  - Add JSON formatting for structured message data
  - Implement auto-scroll to latest messages
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 17. Add client state and loading indicators
  - Create client state display in InteractionScreen
  - Add loading indicators for command execution
  - Implement command waiting state UI
  - Add connection status indicators
  - _Requirements: 4.3, 4.4_

- [x] 18. Implement initialization flow in InteractionScreen
  - Add LaunchedEffect for TestBedViewModel initialization
  - Implement settings change detection and reinitialization
  - Add initialization state handling in UI
  - Create error handling for initialization failures
  - _Requirements: 3.1_

- [x] 19. Update navigation and remove unused code
  - Update App.kt navigation to use InteractionScreen instead of ChatScreen
  - Remove unused ChatViewModel and related chat functionality
  - Clean up unused UI components (MessageBubble for chat, etc.)
  - Remove theme management, notification, and language code
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 20. Add error handling and validation
  - Implement TestBedError sealed class hierarchy
  - Add command execution error handling
  - Create connection error handling with retry options
  - Implement authentication error handling
  - Add deployment settings validation
  - _Requirements: 4.5_

- [x] 21. Create unit tests for TestBedViewModel
  - Write tests for command execution and state management
  - Add tests for socket message processing
  - Create tests for authentication flow handling
  - Implement tests for deployment configuration
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 22. Create unit tests for SettingsViewModel
  - Write tests for deployment settings update
  - Add tests for BuildConfig default value loading
  - Create tests for region validation
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 23. Add integration tests for InteractionScreen
  - Create tests for command dropdown functionality
  - Add tests for socket message display and expansion
  - Write tests for command execution and loading states
  - Implement tests for initialization flow
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [x] 24. Create integration tests for SettingsScreen
  - Write tests for simplified settings display
  - Add tests for deployment configuration save/load
  - Create tests for region dropdown functionality
  - _Requirements: 1.1, 1.4_

- [x] 25. Final cleanup and optimization
  - Remove all unused imports and dependencies
  - Clean up build.gradle.kts files
  - Update documentation and code comments
  - Verify platform compatibility for Android and iOS
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 26. Complete remaining actionable TODOs
  - Implement BuildConfig access for default deployment values
  - Add file name storage for attachment functionality
  - Fix System.currentTimeMillis() usage in tests for multiplatform compatibility
  - Fix lint errors in androidComposePrototype
  - Ensure project compiles successfully on both Android and iOS platforms