package com.genesys.cloud.messenger.androidcomposeprototype.ui.testbed

enum class Command(val description: String) {
    ADD_ATTRIBUTE("addAttribute <key> <value>"),
    ATTACH("attach"),
    AUTHORIZE("authorize"),
    BYE("bye"),
    CLEAR_CONVERSATION("clearConversation"),
    CONNECT("connect"),
    CONNECT_AUTHENTICATED("connectAuthenticated"),
    DELETE("delete <attachmentID>"),
    REFRESH("refreshAttachment <attachmentId>"),
    FILE_ATTACHMENT_PROFILE("fileAttachmentProfile"),
    DEPLOYMENT("deployment"),
    DETACH("detach"),
    HEALTH_CHECK("healthcheck"),
    HISTORY("history"),
    INVALIDATE_CONVERSATION_CACHE("invalidateConversationCache"),
    NEW_CHAT("newChat"),
    OKTA_LOGOUT("oktaLogout"),
    OKTA_SIGN_IN_WITH_PKCE("oktaSignInWithPKCE"),
    SEND("send <msg>"),
    SEND_QUICK_REPLY("sendQuickReply <quickReply>"),
    TYPING("typing"),
    SEND_CARD_REPLY("sendCardReply <card> <action>")
}
