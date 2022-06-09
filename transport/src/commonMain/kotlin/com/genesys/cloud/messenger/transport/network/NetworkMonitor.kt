package com.genesys.cloud.messenger.transport.network


internal expect class NetworkMonitor {
    fun setNetworkStateListener(networkStateListener: NetworkStateListener)
    fun start()
    fun stop()
}