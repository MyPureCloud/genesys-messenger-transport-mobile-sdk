package com.genesys.cloud.messenger.transport.network


internal actual class NetworkMonitor {

    private var networkStateListener: NetworkStateListener? = null


    actual fun setNetworkStateListener(networkStateListener: NetworkStateListener) {
        this.networkStateListener = networkStateListener
    }

    actual fun start() {
        TODO("Start monitoring network here")
    }

    actual fun stop() {
        networkStateListener = null
        TODO("Stop monitoring network here")
    }

}
