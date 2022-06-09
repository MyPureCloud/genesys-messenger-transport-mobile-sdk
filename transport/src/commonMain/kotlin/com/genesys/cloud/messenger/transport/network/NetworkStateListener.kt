package com.genesys.cloud.messenger.transport.network


internal interface NetworkStateListener {
    fun onStateChanged(state: NetworkState)
}

internal enum class NetworkState {
    Available,
    Unavailable,
}
