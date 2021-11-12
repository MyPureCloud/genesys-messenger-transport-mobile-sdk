package com.genesys.cloud.messenger.transport.shyrka.send

object TestConfigureSessionRequests {
    fun basic() = """{"token":"token","deploymentId":"deploymentId","guestInformation":${TestGuestInformations.basic()},"action":"configureSession"}"""
}
